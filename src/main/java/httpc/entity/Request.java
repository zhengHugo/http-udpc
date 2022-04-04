package httpc.entity;

import httpc.model.HttpMethod;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Request {
  private final HttpMethod httpMethod;
  private final Header header;
  private final URL urlObject;
  private final String body;

  public Request(HttpMethod httpMethod, URL urlObject) {
    this.httpMethod = httpMethod;
    this.urlObject = urlObject;
    this.header = new Header();
    this.body = "";
    this.header.addEntry("Host", urlObject.getHost());
  }

  public Request(HttpMethod httpMethod, URL urlObject, String body) {
    this.httpMethod = httpMethod;
    this.urlObject = urlObject;
    this.header = new Header();
    this.body = body;
    if (this.body.length() > 0) {
      this.addHeader(
          "Content-Length",
          String.valueOf(StandardCharsets.UTF_8.encode(this.body).array().length));
    }
  }

  public HttpMethod getHttpMethod() {
    return this.httpMethod;
  }

  public URL getUrlObject() {
    return this.urlObject;
  }

  public String getBody() {
    return body;
  }

  public String getHeader() {
    return header.toString();
  }

  public void addHeader(String key, String value) {
    this.header.addEntry(key, value);
  }

  @Override
  public String toString() {
    String requestString;
    if (this.httpMethod.equals(HttpMethod.Get)) {
      requestString =
          "GET "
              + this.getUrlObject().getFile()
              + " HTTP/1.0"
              + "\n"
              + this.getHeader()
              + "\n\n";
    } else {
      requestString =
          "POST "
              + this.getUrlObject().getFile()
              + " HTTP/1.0"
              + "\n"
              + this.getHeader()
              + "\n"
              + this.getBody()
              + "\n";
    }
    return requestString;
  }
}
