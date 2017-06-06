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

  /**
   * Re-sync the network when removing the peer
   */
  private static void removeAndSync () {
    Message setNextsPrev = new Message(CMD.SETPREV, new String[] {
      prev.host, String.valueOf(prev.port)
    });
    sendMessage(setNextsPrev, next.host, next.port);

    Message setPrevsNext = new Message(CMD.SETNEXT, new String[] {
      next.host, String.valueOf(next.port), "true"
    });
    sendMessage(setPrevsNext, prev.host, prev.port);
    prev = next = null;
    log.log("Peer exiting");
    System.exit(0);
  }

  /**
   * Adds peer to the current network
   */
  private static void addPeer( String host, int port, ConnectionManager connMan ) {
    log.log("Adding new Peer : " + host + "@" + port);
    Address newAddr = new Address( host, port );

    Address temp = next;
    // Change next to new next
    log.log("New next is " + newAddr.host + "@" + newAddr.port);
    next = newAddr;
    // Set Next
    Message msg = new Message(CMD.SETNEXT, new String[] {
      temp.host, String.valueOf(temp.port), "false"
    });
    sendMessage( msg, host, port );

    // Set Prev
    Message msgPrev = new Message(CMD.SETPREV, new String[] {
      connMan.getHostName(), String.valueOf(connMan.getConnectionPort())
    });
    sendMessage( msgPrev, host, port );
  }

  /**
   * Set the next peer in the system
   */
  private static void setNext( String host, int port, boolean isRemove, ConnectionManager connMan ) {
    next = new Address( host, port );
    log.log("New next is " + next.host + "@" + next.port);
    // Set next's prev to complete circle
    if (!isRemove) {
      Message msg = new Message (CMD.SETPREV, new String[] {
        connMan.getHostName(), String.valueOf(connMan.getConnectionPort())
      });
      sendMessage(msg, host, port);
    }
  }

  /**
   * Sets the previous peer
   */
  private static void setPrev ( String host, int port ) {
    prev = new Address( host, port );
    log.log("New prev is " + prev.host + "@" + prev.port);
  }

  private static void sendMessage( Message msg, String host, int port ) {
    try {
      Socket clientSocket = null;
      try {
        clientSocket= new Socket( host, port );
        ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
        outputStream.writeObject(msg);
      } catch (IOException e) {
        log.log(e.getMessage());
      } finally {
        if (clientSocket != null)
          clientSocket.close();
      }
    } catch (IOException e) {
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
          log.log("Connection to server of : " + connectionHost + " : " + connectionPort);
          msg = new Message(CMD.ADDPEER, new String[]{
            connMan.getHostName(), String.valueOf(connMan.getConnectionPort())
          });
          // tell peer that was passed in as args to set its next to this newly added node
          sendMessage(msg, connectionHost, connectionPort);
        } else {
          prev = next = new Address(connMan.getHostName(), connMan.getConnectionPort());
        }


        while ( true ) {
          server = listener.accept();
          log.log("Server listening...");

          ObjectInputStream inStream = new ObjectInputStream(server.getInputStream());
          Message incoming = (Message) inStream.readObject();

          log.log("Message Recieved: " + incoming.cmd);
          switch (incoming.cmd) {
            case SETNEXT:
              setNext( incoming.params[0], Integer.parseInt(incoming.params[1]), Boolean.parseBoolean(incoming.params[2]), connMan);
              break;
            case SETPREV:
              setPrev( incoming.params[0], Integer.parseInt(incoming.params[1]));
              break;
            case ADDPEER:
              addPeer(incoming.params[0], Integer.parseInt(incoming.params[1]), connMan);
              break;
            case EXIT:
              removeAndSync();
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
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
