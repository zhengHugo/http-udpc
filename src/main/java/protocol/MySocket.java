package protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;

public class MySocket {
  private InputStream userInputStream;
  private OutputStream userOutputStream;

  private byte[] writeBuffer = new byte[2048];
  private int writeCounter = 0;
  private byte[] readBuffer;
  private int readCounter = 0;

  private MyTcpHost myTcpHost;

  public MySocket(MyTcpHost myTcpHost) {
    this.myTcpHost = myTcpHost;
    this.initStreams();
  }

  public MySocket(String host, int port) throws IOException {

    myTcpHost = new MyTcpHost(host, port);
    this.initStreams();
    myTcpHost.connect();
  }

  public OutputStream getOutputStream() {
    return this.userOutputStream;
  }

  public InputStream getUserInputStream() {
    return this.userInputStream;
  }

  public void close() {
    myTcpHost.close();
  }

  // throws something if exceeding buffer size
  private void ensureCapacity(int minCapacity) throws MessageTooLongException {
    if (writeBuffer.length < minCapacity) {
      throw new MessageTooLongException("Message is too long to send");
    }
  }

  private void initStreams() {
    this.userOutputStream =
        new OutputStream() {

          @Override
          public void write(int b) throws IOException {
            ensureCapacity(writeCounter + 1);
            writeBuffer[writeCounter] = (byte) b;
            writeCounter += 1;
          }

          @Override
          public void flush() throws IOException {
            super.flush();

            try {
              myTcpHost.send(writeBuffer);
            } catch (MessageTooLongException e) {
              e.printStackTrace();
            }
          }
        };

    this.userInputStream =
        new InputStream() {
          @Override
          public int read() throws IOException {
            if (readBuffer[readCounter] == 0) {
              readCounter = 0;
              readBuffer = myTcpHost.receive();
            }
            return readBuffer[readCounter++];
          }
        };
  }
}
