package httpc.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Response {
  private final String wholeText;

  public Response(String text) {
    this.wholeText = text;
  }

  public String getWholeText() {
    return wholeText;
  }

  public String getBody() {
    String[] lines = wholeText.split("\n");
    int emptyLineIndex = new ArrayList<>(List.of(lines)).indexOf("");
    return Arrays.stream(Arrays.copyOfRange(lines, emptyLineIndex + 1, lines.length))
        .collect(Collectors.joining("\n", "", "\n"));
  }
}
