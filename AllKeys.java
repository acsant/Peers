import java.util.*;
import java.lang.Runtime;
import java.io.*;
import javafx.util.Pair;

import java.lang.reflect.Array;
import java.net.*;

public class AllKeys {
  public static void main(String[] args) {

    String host = null;
    int port = 0;
    if ( args.length == 2 ) {
      host = args[0];
      port = Integer.parseInt(args[1]);
    }

    try {
      Socket next = new Socket(host, port);
      Message msg = new Message(CMD.ALLKEYS, new String[] { host, Integer.toString(port) });
      ObjectOutputStream outStream = new ObjectOutputStream(next.getOutputStream());
      outStream.writeObject(msg);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

// 10.20.177.174 10000
