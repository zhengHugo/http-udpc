package httpc.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Header {

  Map<String, String> hashMap = new HashMap<>();

  public void addEntry(String key, String value) {
    this.hashMap.put(key, value);
  }

  @Override
  public String toString() {
    return hashMap.keySet().stream()
        .map(key -> key + ": " + hashMap.get(key))
        .collect(Collectors.joining("\n", "", "\n"));
  }
}
