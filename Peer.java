import java.util.*;
import java.net.*;
import java.io.*;

public class Peer {

  // Enable Logging
  static PSLogger log;

  public enum CMD {
    EXIT,
  }

  // Marshalling
  private static class Message {
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

  public static void main(String[] args) {
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
      try {
        connMan = new ConnectionManager();
        listener = connMan.getAvailableConnection(); 
        log = new PSLogger(Peer.class.getName(),
            "Peer@" + connMan.getHostName() + ":" + connMan.getConnectionPort());
        log.log("Connected at : " + connMan.getHostName() + " " + connMan.getConnectionPort());
        if ( connectionHost != null )
          clientSocket = new Socket(connectionHost, connectionPort);
        while ( true ) {
          Socket server = listener.accept();
          InetAddress connectedHost = server.getInetAddress();
          int connectedPort = server.getPort();
          Socket prevConnection = new Socket(connectedHost, connectedPort);
          server.close();
          prevConnection.close();
        }
      } finally {
        listener.close();
        clientSocket.close();
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
