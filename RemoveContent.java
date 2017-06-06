import java.util.*;
import java.lang.Runtime;
import java.io.*;
import javafx.util.Pair;

import java.lang.reflect.Array;
import java.net.*;

public class RemoveContent {
  public static void main(String[] args) {

    String host = null;
    int port = 0;
    long key = 0;
    if ( args.length == 3 ) {
      host = args[0];
      port = Integer.parseInt(args[1]);
      key = Long.parseLong(args[2]);
    }

    try {
      Socket next = new Socket(host, port);
      Message msg = new Message(CMD.REMOVECONTENT, new String[] { host, Integer.toString(port), Long.toString(key) });
      ObjectOutputStream outStream = new ObjectOutputStream(next.getOutputStream());
      outStream.writeObject(msg);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

// 10.20.177.174 10000