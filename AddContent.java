import java.util.*;
import java.lang.Runtime;
import java.io.*;
import javafx.util.Pair;

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
        String.valueOf(initKey), content, "-1" });
    ObjectOutputStream outStream = new ObjectOutputStream(next.getOutputStream());
    outStream.writeObject(msg);

    ObjectInputStream inputStream = new ObjectInputStream(next.getInputStream());
    Message printToConsoleMsg = (Message) inputStream.readObject();

    switch (printToConsoleMsg.cmd) {
      case PEERNOTFOUND:
        System.out.println("Error: No such peer");
        break;
      case PRINTKEY:
        System.out.println(printToConsoleMsg.params[0]);
        break;
      default:
        System.out.println("An unexpected error occurred when adding content.");
    }

    } catch (Exception e) {
      e.printStackTrace();
    }


  }
}

// 10.20.177.174 10000
