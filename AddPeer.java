import java.util.*;
import java.lang.Runtime;
import java.io.*;

public class AddPeer {

  public static void main(String[] args) {
    try {
      // System.out.println("abcsdfgsd1");
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
      // System.out.println("abcsdfgsd2");
      peer = Runtime.getRuntime().exec(cmd);
      InputStream peerStream = peer.getInputStream();
      InputStream errStream = peer.getErrorStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(peerStream));
      BufferedReader errReader = new BufferedReader(new InputStreamReader(errStream));
      String line = null;
      // System.out.println("abcsdfgsd3");
      if ((line = reader.readLine()) != null) {
        System.out.println(line);
      }

    } catch (IOException e) {
      System.err.println("Error: no such peer");;
    }
  }
}

