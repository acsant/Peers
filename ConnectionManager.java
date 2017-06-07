import java.io.IOException;
import java.net.*;
import java.util.Enumeration;

/**
 * Created by Parsa on 2017-06-06.
 */
public class ConnectionManager {
    private static InetAddress hostAddr;
    private static final int MIN_PORT = 10000;
    private static final int MAX_PORT = 11000;
    private static int connectionPort;

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

    public static ServerSocket getAvailableConnection() throws IOException {
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