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

    try {
      ConnectionManager connMan = new ConnectionManager();
      Socket next = new Socket(host, port);
      Message msg = new Message(CMD.ADDCONTENT, new String[] { host, Integer.toString(port), content });
      ObjectOutputStream outStream = new ObjectOutputStream(next.getOutputStream());
      outStream.writeObject(msg);

      // Accept server socket connections to output key
      ServerSocket listener = connMan.getAvailableConnection();
      Socket server = listener.accept();

      ObjectInputStream inStream = new ObjectInputStream(server.getInputStream());
      Message incoming = (Message) inStream.readObject();

      // output key
      System.out.println(incoming.params[0]);



    } catch (Exception e) {
      e.printStackTrace();
    }


  }
}

// 10.20.177.174 10000
