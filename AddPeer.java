import java.util.*;
import java.lang.Runtime;
import java.io.*;

public class AddPeer {

  public static void main(String[] args) {
    try {
      Process peer = null;
      String[] cmd; 
      if ( args.length == 2 ) {
        cmd = new String[] {
          "java", "-cp", System.getProperty("java.class.path"), "Peer", args[0], args[1]
        };
      } else {
        cmd = new String[] {
          "java", "-cp", System.getProperty("java.class.path"), "Peer"
        };
      }
      peer = Runtime.getRuntime().exec(cmd);
      /*InputStream peerStream = peer.getInputStream();
      InputStream errStream = peer.getErrorStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(peerStream));
      BufferedReader errReader = new BufferedReader(new InputStreamReader(errStream));
      String line = null;
      if ( ( line = reader.readLine() ) != null ) {
        System.out.println(line);
      }*/
      
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
