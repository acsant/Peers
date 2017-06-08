import java.util.*;
import java.lang.Runtime;
import java.io.*;

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
      ObjectInputStream iStream = new ObjectInputStream(next.getInputStream());
      outStream.writeObject(msg);
      String allkeys = (String) iStream.readObject();
      System.out.println(allkeys);
    } catch (Exception e) {
      System.err.println("Error: no such peer");
    }
  }
}

// 10.20.177.174 10000
