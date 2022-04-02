package protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class MySocket {
  private InputStream userInputStream;
  private OutputStream userOutputStream;

  private String routerAddress;
  private int routerPort;

  private String destAddress;
  private int destPort;

  private byte[] buf = new byte[1024];
  private int counter = 0;

  private MyTcpClient myTcpClient;


  public MySocket(String host, int port) throws IOException {

    myTcpClient = new MyTcpClient(host, port);
    this.initStreams();

    // TODO: connect
    myTcpClient.connect();

  }

  public OutputStream getOutputStream() {
    return this.userOutputStream;
  }

  public InputStream getUserInputStream() {
    return this.userInputStream;
  }

  public void close() {
    myTcpClient.close();
  }

  // throws something if exceeding buffer size
  private void ensureCapacity(int minCapacity) throws Exception {
    if (buf.length < minCapacity) {
      throw new MessageTooLongException("Message is too long to send");
    }
  }

  private void initStreams() {
    this.userOutputStream =
        new OutputStream() {

          @Override
          public void write(int b) throws IOException {
            try {
              ensureCapacity(counter + 1);
            } catch (Exception e) {
              e.printStackTrace();
            }
            buf[counter] = (byte) b;
            counter += 1;
          }

          @Override
          public void flush() throws IOException {
            super.flush();

            myTcpClient.send(buf);

            /*
            TODO: an example of how to build a UDP packet from TCP payload:
              TCP payload -> TCP packet == UDP payload -> UDP request packet
            */
            MyTcpPacket myTcpPacket = null;
            try {
              myTcpPacket =
                  new MyTcpPacket.Builder()
                      .withPacketType(PacketType.SYN)
                      .withSequenceNum(0)
                      .withPeerAddress(destAddress)
                      .withPeerPort(destPort)
                      .withPayload(buf)
                      .build();
            } catch (MessageTooLongException e) {
              e.printStackTrace();
            }

            assert myTcpPacket != null;
            var udpPayload = myTcpPacket.toBytes();
            // send packet
            var udpRequestPacket =
                new DatagramPacket(
                    udpPayload,
                    udpPayload.length,
                    InetAddress.getByName(routerAddress),
                    routerPort);
          }
        };

    this.userInputStream =
        new InputStream() {
          @Override
          public int read() throws IOException {
            //TODO
            return 0;
//            if (counter < response.getLength()) {
//              counter += 1;
//              return buf[counter] & 0xff;
//            } else {
//              return -1;
//            }
          }
        };
  }
}
