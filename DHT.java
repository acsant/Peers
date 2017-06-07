import java.util.*;
import java.net.*;
import java.lang.*;

public class DHT {

  Map<Long, String> map = new HashMap<>();

  public long insert(String content) {
    long key = (long) System.nanoTime();
    map.put(key, content);
    return key;
  }

  public String retrieve(long key) {
  	return map.get(key);
  }

  public void removeByKey(long key) {
    map.remove(key);
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

  public void put(Long key, String value) {
    map.put(key, value);
  }

  public int size() {
    return map.size();
  }

  public boolean contains(Long key) {
    return map.containsKey(key);
  }

}
