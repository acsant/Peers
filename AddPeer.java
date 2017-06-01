import java.util.*;
import java.lang.Runtime;
import java.io.*;

public class AddPeer {
  public static void main(String[] args) {
    try {
      Process peer = Runtime.getRuntime().exec("java Peer");
      InputStream peerStream = peer.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(peerStream));
      String line = "";
      if ( ( line = reader.readLine() ) != null ) {
        System.out.println(line);
      }
      
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
