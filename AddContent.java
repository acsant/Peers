import java.util.*;
import java.lang.Runtime;
import java.io.*;

import java.lang.reflect.Array;
import java.net.*;

public class AddContent {
  public static void main(String[] args) {

    String host = null;
    int port = 0;
    String content = null;
    if ( args.length == 3 ) {
      host = args[0];
      port = Integer.parseInt(args[1]);
      content = args[2];
    }

    Socket next = null;
    try {
      next = new Socket(host, port);
      long initKey = 0;
      Message msg = new Message(CMD.ADDCONTENT, new String[] { host, String.valueOf(port), 
        String.valueOf(initKey), content, String.valueOf(Integer.MAX_VALUE) });
      ObjectOutputStream outStream = new ObjectOutputStream(next.getOutputStream());
      ObjectInputStream iStream = new ObjectInputStream(next.getInputStream());
      outStream.writeObject(msg);
      long key = (long) iStream.readObject();
      System.out.println(key);

    } catch (Exception e) {
      System.err.println("Error: no such peer");
    }


  }
}

// 10.20.177.174 10000
