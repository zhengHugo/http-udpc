package protocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.util.Timer;
import java.util.TimerTask;

public class MyTcpClient {

  private final int INIT_SEQ_NUM = 0;
  private String destAddress;
  private int destPort;

  private String routerAddress;
  private int routerPort;

  private DatagramSocket datagramSocket;
  private DatagramPacket response;

  private Timer[] timers;
  private final long timeout = 1000;
  private int windowSize;
  private int unAckedPacketNum;

  public MyTcpClient(String host, int port) {
    try {
      datagramSocket = new DatagramSocket();
    } catch (SocketException e) {
      e.printStackTrace();
    }

    this.destAddress = host;
    this.destPort = port;
    this.windowSize = 5;
    timers = new Timer[windowSize];
  }

  public void connect() throws IOException {
    var firstConnectionRequest =
        new MyTcpPacket.Builder()
            .withPacketType(PacketType.SYN)
            .withSequenceNum(INIT_SEQ_NUM)
            .withPeerAddress(destAddress)
            .withPeerPort(destPort)
            .build();

    sendPacket(firstConnectionRequest);
    var firstConnectionResponse = receivePacket();
    if (!firstConnectionResponse.getPacketType().equals(PacketType.SYN_ACK)) {
      throw new IOException("Connection fail");
    }
  }

  void send(byte[] data) throws MessageTooLongException, IOException {
    var ConnectionWithDataRequest =
            new MyTcpPacket.Builder()
                    .withPacketType(PacketType.SYN)
                    .withSequenceNum(INIT_SEQ_NUM + 1)
                    .withPeerAddress(destAddress)
                    .withPeerPort(destPort)
                    .withPayload(data)
                    .build();
    sendPacket(ConnectionWithDataRequest);
    var firstConnectionResponse = receivePacket();
    if (!firstConnectionResponse.getPacketType().equals(PacketType.SYN_ACK)) {
      throw new IOException("Connection fail");
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
    assert receivedTcpPacket != null;
    timers[receivedTcpPacket.getSequenceNum() % windowSize].cancel();
    unAckedPacketNum--;
    return receivedTcpPacket;
  }
}
