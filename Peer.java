import java.util.*;
import java.net.*;
import java.io.*;

public class Peer {

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
          if ( addr instanceof Inet6Address && !addr.isLoopbackAddress() ) {
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
          System.out.println("Try connect: " + hostAddr.getHostName() + " " + port);
          conn.bind(new InetSocketAddress(hostAddr, port));;
          break;
        } catch (IOException e) {
          port++;
        }
      }
      System.out.println("Created socket for : " + hostAddr.getHostAddress() + " " + port);
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
    //Socket clientSocket = null;
    ServerSocket listener = null;
    try {
      connMan = new ConnectionManager();
      //if (args.length == 0)
      //  clientSocket = connMan.getAvailableConnection();
      //else
      //  clientSocket = new Socket(connectionHost, connectionPort);
      
      System.out.println(connMan.getHostName() + " " + connMan.getConnectionPort());
      while ( true )
        listener = connMan.getAvailableConnection(); 
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
