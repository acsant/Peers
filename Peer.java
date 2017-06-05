import javafx.util.Pair;

import java.lang.reflect.Array;
import java.util.*;
import java.net.*;
import java.io.*;

public class Peer {

  // Enable Logging
  static PSLogger log;

  public enum CMD {
    EXIT,
    ADDPEER
  }

  // Marshalling
  private static class Message implements Serializable {
    CMD cmd;
    String[] params;

    Message (CMD cmd, String[] params) {
      this.cmd = cmd;
      this.params = params;
    }
  }

  private static class ConnectionManager {
    private InetAddress hostAddr;
    private static final int MIN_PORT = 10000;
    private static final int MAX_PORT = 11000;
    private int connectionPort;

    ConnectionManager() throws SocketException {
      hostAddr = getNextNonLoopbackAddr();
    }

    /**
     * Credit: This code snippet was taken from :
     * http://www.java2s.com/Code/Java/Network-Protocol/FindsalocalnonloopbackIPv4address.htm
     */
    private InetAddress getNextNonLoopbackAddr() throws SocketException {
      Enumeration<NetworkInterface> ifaceList = NetworkInterface.getNetworkInterfaces();
      while ( ifaceList.hasMoreElements() ) {
        NetworkInterface iface = ifaceList.nextElement();
        Enumeration<InetAddress> addresses = iface.getInetAddresses();

        while ( addresses.hasMoreElements() ) {
          InetAddress addr = addresses.nextElement();
          if ( addr instanceof Inet4Address && !addr.isLoopbackAddress() ) {
            return addr;
          }
        }
      }
      return null;
    }

    public ServerSocket getAvailableConnection() throws IOException {
      // Generate random port
      int port = MIN_PORT;
      ServerSocket conn = new ServerSocket();
      while ( port <= MAX_PORT ) {
        try {
          conn.bind(new InetSocketAddress(hostAddr, port));;
          break;
        } catch (IOException e) {
          port++;
        }
      }

      connectionPort = port;
      conn.setReuseAddress(false);
      return conn;
    }

    public int getConnectionPort() {
      return connectionPort;
    }

    public String getHostName() {
      return hostAddr.getHostAddress();
    }
  }

  private static void addPeer(String host, int port, ConnectionManager connMan) {
    if (host.equals(connMan.getHostName()) && port == connMan.getConnectionPort()) {
      return;
    }
    try {
      Socket next = new Socket(host, port);
      Message msg = new Message(CMD.ADDPEER, new String[] {connMan.getHostName(), String.valueOf(connMan.getConnectionPort())});
      ObjectOutputStream outStream = new ObjectOutputStream(next.getOutputStream());
      outStream.writeObject(msg);
    } catch (Exception e) {
      log.log(e.getMessage());
    }
  }

  public static void main(String[] args) {
    // for serializing/deserializing message into/from streams
    Message msg;

    // list of all active peers in the network
    ArrayList<Pair<InetAddress, Integer>> Peers = new ArrayList<>();

    String connectionHost = null;
    int connectionPort = 0;

    if ( args.length == 2 ) {
      connectionHost = args[0];
      connectionPort = Integer.parseInt(args[1]);
    }

    ConnectionManager connMan;
    Socket clientSocket = null;
    ServerSocket listener = null;
    try {
      Socket server = null;
      try {
        connMan = new ConnectionManager();
        listener = connMan.getAvailableConnection(); 
        log = new PSLogger(Peer.class.getName(),
            "Peer@" + connMan.getHostName() + ":" + connMan.getConnectionPort());
        log.log("Connected at : " + connMan.getHostName() + " " + connMan.getConnectionPort());
        if ( connectionHost != null ) {
          log.log("Connection to server of : " + connectionHost + " : " + connectionPort);
          clientSocket = new Socket(connectionHost, connectionPort);
          ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
          //TBD
          String [] msgArgs = null;
          msg = new Message(CMD.ADDPEER, args);
          // serialize message and send to server
          outputStream.writeObject(msg);
        }

        while ( true ) {
          server = listener.accept();
          log.log("Server listening...");

          ObjectInputStream inStream = new ObjectInputStream(server.getInputStream());
          Message incoming = (Message) inStream.readObject();

          log.log("Message Recieved: " + incoming.cmd);
          switch (incoming.cmd) {
            case ADDPEER:
              addPeer(incoming.params[0], Integer.parseInt(incoming.params[1]), connMan);
              break;
            case EXIT:
              break;
            default:
              break;
          }
        }

      } catch (Exception e) {
        log.log(e.getMessage());
      } finally {
        listener.close();
        server.close();
        if ( clientSocket != null )
          clientSocket.close();
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
