package httpc;

import httpc.entity.Request;
import httpc.entity.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import protocol.MySocket;

public class Client {

  int port = 5050;

  /*
  1. Connect to server via socket
  2. Write data to socket
  3. Read response from socket
  4. Compose the response object and return
  */

  public Response sendAndGetRes(Request request) throws IOException {
    MySocket socket = new MySocket(request.getUrlObject().getHost(), port);
    PrintStream out = new PrintStream(socket.getOutputStream(), false);
    BufferedReader in =
        new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

    String responseText;
    System.out.println(request);
    out.println(request);
    out.close();
    responseText = this.readAllResponse(in);
    in.close();
    socket.close();
    return new Response(responseText);
  }

  public String readAllResponse(BufferedReader in) throws IOException {
    StringBuilder stringBuilder = new StringBuilder();
    String line = in.readLine();
    while (line != null) {
      stringBuilder.append(line);
      stringBuilder.append("\n");
      line = in.readLine();
    }
    return stringBuilder.toString();
  }
}
