import java.util.*;
import java.lang.Runtime;
import java.io.*;

import java.lang.reflect.Array;
import java.net.*;

public class PrintContent {
  public static void main(String[] args) {


    String host = args[0];
    String port = args[1];
    Socket next = null;
    try {
      next = new Socket(host, Integer.parseInt(port));
      long initKey = 0;
      System.out.println("printin at " + host + "@" + port);
      Message msg = new Message(CMD.PRINTALL, new String[] { host, port });
      ObjectOutputStream outStream = new ObjectOutputStream(next.getOutputStream());
      outStream.writeObject(msg);

    } catch (Exception e) {
      e.printStackTrace();
    }


  }
}

// 10.20.177.174 10000
