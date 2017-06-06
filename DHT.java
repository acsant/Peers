import java.util.*;
import java.net.*;
import java.lang.*;

public class DHT {
  InetAddress host;
  int port;

  Map<Integer, String> map = new HashMap<>();

  public DHT () {}

  public int insert(String content) {
    int key = (int) System.nanoTime();
    map.put(key, content);
    return key;
  }

}
