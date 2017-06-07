import java.util.*;
import java.lang.Runtime;
import java.io.*;
import javafx.util.Pair;

import java.lang.reflect.Array;
import java.net.*;

public class RemoveContent {
  public static void main(String[] args) {

    String host = null;
    String port = null;
    String key = null;
    if ( args.length == 3 ) {
      host = args[0];
      port = args[1];
      key = args[2];
    }

    try {
      Socket next = new Socket(host, Integer.parseInt(port));
      Message msg = new Message(CMD.REMOVECONTENT, new String[] { host, port, key, "false" });
      ObjectOutputStream outStream = new ObjectOutputStream(next.getOutputStream());
      outStream.writeObject(msg);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
