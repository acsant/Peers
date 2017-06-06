import java.util.*;
import java.net.*;
import java.lang.*;

public class DHT {
  InetAddress host;
  int port;

  Map<Long, String> map = new HashMap<>();

  public DHT () {}

  public long insert(String content) {
    long key = (long) System.nanoTime();
    map.put(key, content);
    return key;
  }

  public String retrieve(long key) {
  	return map.get(key);
  }

  public Boolean removeByKey(long key) {
    if (map.containsKey(key)) {
      map.remove(key);
      return true;
      // log.log("Key was removed");  
    } else {
      return false;
      // log.log("Key is not present");
    }
    
  }

  public String getAllKeys() {
    List<Long> keys = new ArrayList<Long>(map.keySet());
    String stringified = "";
    for (int i = 0; i < keys.size(); i++) {
      if (i > 0) {
        stringified += " ";
      }
      stringified += Long.toString(keys.get(i));
    }
    return stringified;
  }

}
