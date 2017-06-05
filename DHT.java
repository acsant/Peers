import java.util.*;
import java.net.*;
import java.lang.*;

public class DHT {
  InetAddress host;
  int port;

  Map<Integer, String> map = new HashMap<>();

  DHT ( InetAddress host, int port ) {
    this.host = host;
    this.port = port;
  }

  public int insert(String content) {
    int key = (int) System.nanoTime();
    map.put(key, content);
    return key;
  }

}
