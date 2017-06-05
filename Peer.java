import javafx.util.Pair;

import java.lang.reflect.Array;
import java.util.*;
import java.net.*;
import java.io.*;

public class Peer {

  // References to next and prev
  static Address prev = null;
  static Address next = null;

  public static class Address {
    String host;
    int port;
  
    Address( String host, int port ) {
      this.host = host;
      this.port = port;
    }
  }

  // Enable Logging
  static PSLogger log;

  public enum CMD {
    EXIT,
    SETNEXT,
    SETPREV
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

  private static void setLink(CMD linkDir, String host, int port, ConnectionManager connMan) {
    if (linkDir == CMD.SETPREV) {
      prev = new Address(host, port);
    } else if (linkDir == CMD.SETNEXT) {
      if (prev == null && next == null) {
        next = new Address(host, port);
      }
      try {
        Socket clientSocket = new Socket(host, port);

        ObjectOutputStream inStream = new ObjectOutputStream(clientSocket.getOutputStream());
        Message setPrevMsg = new Message(CMD.SETPREV, new String[]{
                connMan.getHostName(), String.valueOf(connMan.getConnectionPort())
        });

        inStream.writeObject(setPrevMsg);

      } catch (Exception e) {
        log.log(e.getMessage());
      }
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
        Address currPeerAddress = new Address(connMan.getHostName(), connMan.getConnectionPort());

        log = new PSLogger(Peer.class.getName(),
            "Peer@" + connMan.getHostName() + ":" + connMan.getConnectionPort());
        log.log("Connected at : " + connMan.getHostName() + " " + connMan.getConnectionPort());


        if ( connectionHost != null ) {
          // set prev of new peer to peer passed in as args
          prev = new Address(connectionHost, connectionPort);

          log.log("Connection to server of : " + connectionHost + " : " + connectionPort);
          clientSocket = new Socket(connectionHost, connectionPort);
          ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
          msg = new Message(CMD.SETNEXT, new String[]{
                  connMan.getHostName(), String.valueOf(connMan.getConnectionPort())
          });
          // tell peer that was passed in as args to set its next to this newly added node
          outputStream.writeObject(msg);
        }

        while ( true ) {
          server = listener.accept();
          log.log("Server listening...");

          ObjectInputStream inStream = new ObjectInputStream(server.getInputStream());
          Message incoming = (Message) inStream.readObject();

          Address oldNext = next;
          // setNext
          next = new Address(incoming.params[0], Integer.parseInt(incoming.params[1]));
          // make socket call back to initial peer

          log.log("Message Recieved: " + incoming.cmd);
          switch (incoming.cmd) {
            case SETNEXT:
              setLink(CMD.SETNEXT,incoming.params[0], Integer.parseInt(incoming.params[1]), connMan);
              break;
            case SETPREV:
              setLink(CMD.SETPREV,incoming.params[0], Integer.parseInt(incoming.params[1]), connMan);
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
