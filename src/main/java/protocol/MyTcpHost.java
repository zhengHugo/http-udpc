package protocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;

public class MyTcpHost {

  private String destAddress;
  private int destPort;

  private int listenPort;

  private String routerAddress = "127.0.0.1";
  private int routerPort = 3000;

  private DatagramSocket datagramSocket;
  private DatagramPacket response;

  private final long timeout = 1000;
  private final int INIT_SEQ_NUM = 0;
  private final int DEFAULT_LISTEN_PORT = 5050;
  private final int windowSize = 5;

  private int sequenceNum = INIT_SEQ_NUM;
  private Timer[] timers;
  private int unAckedPacketNum;

  private HostState state;

  public MyTcpHost(int port) throws SocketException {
    datagramSocket = new DatagramSocket();
    this.listenPort = port;
    this.state = HostState.LISTEN;
  }

  public MyTcpHost(String host, int port) throws SocketException {
    datagramSocket = new DatagramSocket(listenPort);

    this.destAddress = host;
    this.destPort = port;
    this.listenPort = DEFAULT_LISTEN_PORT;
    this.state = HostState.LISTEN;
    timers = new Timer[windowSize];
  }

  public void connect() throws IOException {
    if (!state.equals(HostState.LISTEN)) {
      throw new MySocketException("Connection has already opened");
    }
    sequenceNum = INIT_SEQ_NUM;
    var firstConnectionRequest =
        new MyTcpPacket.Builder()
            .withPacketType(PacketType.SYN)
            .withSequenceNum(sequenceNum++)
            .withPeerAddress(destAddress)
            .withPeerPort(destPort)
            .build();

    sendPacket(firstConnectionRequest);
    var firstConnectionResponse = receivePacket();
    if (!firstConnectionResponse.getPacketType().equals(PacketType.SYN_ACK)
        || firstConnectionResponse.getSequenceNum() != sequenceNum - 1) {
      throw new IOException("Connection fail");
    }
  }

  public void send(byte[] data) throws MessageTooLongException, IOException {
    if (unAckedPacketNum < windowSize) {
      var packetBuilder =
          new MyTcpPacket.Builder()
              .withSequenceNum(sequenceNum++)
              .withPeerAddress(destAddress)
              .withPeerPort(destPort)
              .withPayload(data);
      MyTcpPacket requestPacket = null;
      if (this.state.equals(HostState.SYNSENT)) {
        requestPacket = packetBuilder.withPacketType(PacketType.ACK).build();
      } else if (this.state.equals(HostState.ESTAB)) {
        requestPacket = packetBuilder.withPacketType(PacketType.DATA).build();
      }
      assert requestPacket != null;
      sendPacket(requestPacket);
      this.state = HostState.ESTAB;
      this.unAckedPacketNum++;
    }
  }

  public byte[] receive() throws IOException {
    var incomingPacket = receivePacket();
    if (incomingPacket.getPacketType().equals(PacketType.ACK)
        && incomingPacket.getSequenceNum() == sequenceNum) {
      timers[sequenceNum].cancel();
      this.unAckedPacketNum--;
      return incomingPacket.getPayload();
    } else {
      return receive();
    }
  }

  public void accept() throws IOException {
    var incomingPacket = receivePacket();
    if (this.state.equals(HostState.LISTEN)
        && incomingPacket.getPacketType().equals(PacketType.SYN)
        && incomingPacket.getSequenceNum() == INIT_SEQ_NUM) {
      var firstConnectionResponse =
          new MyTcpPacket.Builder()
              .withPacketType(PacketType.SYN_ACK)
              .withPeerAddress(incomingPacket.getPeerAddress())
              .withPeerPort(incomingPacket.getPeerPort())
              .withSequenceNum(incomingPacket.getSequenceNum())
              .build();
      sendPacket(firstConnectionResponse);
      this.state = HostState.SYN_RCVD;
    }
  }

  void close() {}

  private void sendPacket(MyTcpPacket packet) throws IOException {
    var udpPayload = packet.toBytes();
    // send packet
    var udpRequestPacket =
        new DatagramPacket(
            udpPayload, udpPayload.length, InetAddress.getByName(routerAddress), routerPort);

    timers[packet.getSequenceNum() % windowSize].schedule(
        new TimerTask() {
          @Override
          public void run() {
            try {
              sendPacket(packet);
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        },
        timeout);
    datagramSocket.send(udpRequestPacket);
    unAckedPacketNum++;
  }

  private MyTcpPacket receivePacket() throws IOException {
    byte[] buf = new byte[4095];
    response = new DatagramPacket(buf, buf.length);
    datagramSocket.receive(response);
    var receivedTcpPacket = MyTcpPacket.fromByte(response.getData());
    timers[receivedTcpPacket.getSequenceNum() % windowSize].cancel();
    unAckedPacketNum--;
    return receivedTcpPacket;
  }
}

enum HostState {
  LISTEN,
  SYNSENT,
  SYN_RCVD,
  ESTAB
}
