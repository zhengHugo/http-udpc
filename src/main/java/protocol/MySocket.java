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

  DatagramSocket datagramSocket;
  DatagramPacket response;

  public MySocket(String host, int port) {
    try {
      datagramSocket = new DatagramSocket();
    } catch (SocketException e) {
      e.printStackTrace();
    }

    this.destAddress = host;
    this.destPort = port;
    this.initStreams();

    // TODO: Client initialize a connection: 3 handshakes


  }

  public OutputStream getOutputStream() {
    return this.userOutputStream;
  }

  public InputStream getUserInputStream() {
    return this.userInputStream;
  }

  public void close() {}

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
            // send packet
            DatagramPacket request =
                new DatagramPacket(buf, counter, InetAddress.getByName(routerAddress), routerPort);
            datagramSocket.send(request);

            // receive into the same buffer, start from the beginning
            counter = 0;

            response = new DatagramPacket(buf, buf.length);
            datagramSocket.receive(response);

            // TODO: handle MyTCP headers

          }
        };

    this.userInputStream =
        new InputStream() {
          @Override
          public int read() throws IOException {
            if (counter < response.getLength()) {
              counter += 1;
              return buf[counter] & 0xff;
            } else {
              return -1;
            }
          }
        };
  }
}
