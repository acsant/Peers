import javafx.util.Pair;

import java.lang.reflect.Array;
import java.util.*;
import java.net.*;
import java.io.*;

public class Peer {

  // References to next and prev
  static Address prev = null;
  static Address next = null;
  static Socket server = null;
  public static class Address implements Serializable {
    String host;
    int port;

    Address( String host, int port ) {
      this.host = host;
      this.port = port;
    }
  }

  // Enable Logging
  static PSLogger log;
  private static DHT hashTable;

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
    System.exit(0);
  }

  /**
   * Distribute all the content when load balancing
   */
  private static void distributeContent( String host, int port, int upper, boolean isEnd, ConnectionManager connMan ) {
    if ( next.host.equals(host) && next.port == port )
      isEnd = true;

    if ( hashTable.size() > upper ) {
      int i = hashTable.size();
      for (Map.Entry<Long, String> entry : hashTable.getTable().entrySet()) {
        hashTable.removeByKey(entry.getKey());
        Message distContent = new Message(CMD.ADDCONTENT, new String[] {
          connMan.getHostName(), String.valueOf(connMan.getConnectionPort()), String.valueOf(entry.getKey()), entry.getValue(), String.valueOf(upper)
        });

        sendMessage(distContent, host, port);
      }
    }
  }

  /**
   * Load balance across all the peers
   */
  private static void loadBalance ( String host, int port, int contentCount, int peerCount, ConnectionManager connMan ) {
    if ( peerCount != 0 && host.equals(connMan.getHostName()) && port == connMan.getConnectionPort()) {
      int lower = (int) Math.floor(contentCount / peerCount);
      int upper = (int) Math.ceil(contentCount / peerCount);

      distributeContent( host, port, upper, false, connMan ); 
    }
    contentCount += hashTable.size();
    peerCount ++;
    Message countMsg = new Message( CMD.COUNT, new String[] {
      host, String.valueOf(port), String.valueOf(contentCount), String.valueOf(peerCount)
    });
    sendMessage( countMsg, next.host, next.port );
  }

  private static Address requestPeerLink ( String host, int port, boolean prev ) {
    Socket clientSocket = null;
    Address linkInfo = null;
    try {
      try {
        clientSocket = new Socket(host, port);
        ObjectOutputStream outStream = new ObjectOutputStream(clientSocket.getOutputStream());
        ObjectInputStream inStream = new ObjectInputStream(clientSocket.getInputStream());
        Message getLink = new Message(CMD.GETLINK, new String[] { String.valueOf(prev) });
        outStream.writeObject(getLink);
        linkInfo = (Address) inStream.readObject();
        return linkInfo;
      } catch (Exception e) {
        log.log(e.getMessage());
      } finally {
        if ( clientSocket != null )
          clientSocket.close();
      }
    } catch (IOException e ) {
      log.log("IOException in rpl");
    }

    return linkInfo;
  }

  /**
   * Adds peer to the current network
   */
  private static void addPeer( String host, int port, ConnectionManager connMan ) {
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

    Message nextsPrev = new Message(CMD.SETPREV, new String[] {
      newAddr.host, String.valueOf(newAddr.port)
    });
    sendMessage(nextsPrev, temp.host, temp.port);

    // Set Prev
    Message msgPrev = new Message(CMD.SETPREV, new String[] {
      connMan.getHostName(), String.valueOf(connMan.getConnectionPort())
    });
    sendMessage( msgPrev, host, port );
    //loadBalance(connMan.getHostName(), connMan.getConnectionPort(), 0, 0, connMan);
  }

  /**
   * Set the next peer in the system
   */
  private static void setNext( String host, int port, boolean isRemove, ConnectionManager connMan ) {
    next = new Address( host, port );
    log.log("New next is " + next.host + "@" + next.port);
    // Set next's prev to complete circle
  }

  /**
   * Sets the previous peer
   */
  private static void setPrev ( String host, int port ) {
    prev = new Address( host, port );
    log.log("New prev is " + prev.host + "@" + prev.port);
  }

  private static Object sendMessage( Message msg, String host, int port ) {
    Object result = null;
    try {
      
      Socket clientSocket = null;
      try {
        clientSocket= new Socket( host, port );
        ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
        ObjectInputStream inStream = new ObjectInputStream(clientSocket.getInputStream());
        
        outputStream.writeObject(msg);
        log.log("Wait for response");
        result = inStream.readObject();
        log.log("unblock");
      } catch (Exception e) {
        log.log(e.getMessage());
      } finally {
        if (clientSocket != null)
          clientSocket.close();
      }
    } catch (IOException e) {
      log.log(e.getMessage());
    }
    return result;
  }

  private static int findMin(String startHost, int startPort, int currentMin) {
    if ( next.host.equals(startHost) && next.port == startPort ) {
      log.log("reached end");
      return Math.min(currentMin, hashTable.size());
    }
    Message recurse = new Message(CMD.FINDMIN, new String[] {
      startHost, String.valueOf(startPort), String.valueOf(currentMin)
    });
    log.log("calling find min at " + next.host + "@" + next.port);
    return (Integer) sendMessage(recurse, next.host, next.port);
  }

  private static void addContent(String host, int port, Long paramKey, String content, int min, ConnectionManager connMan) {
    int minContent = min; 
    if ( minContent == Integer.MAX_VALUE ) {
      minContent = findMin(connMan.getHostName(), connMan.getConnectionPort(), min);
      log.log("find min returned : " + minContent);
    }
    if ( hashTable.size() == 0 || hashTable.size() == min ) {
      long key = paramKey;
      if ( paramKey == 0 ) {
        key = hashTable.insert(content);
      } else {
        hashTable.put(key, content);
      }
      log.log("Key created: " + Long.toString(key));
      return;

      // Communicate back to AddContent.java to tell it to print key
      // TODO: ensure the host/port passed is the right one for addContent
    } else {
      Message distContent = new Message(CMD.ADDCONTENT, new String[] {
        host, String.valueOf(port), String.valueOf(paramKey), content, String.valueOf(minContent)
      });
      sendMessage(distContent, next.host, next.port);
    }
  }

  private static void removeContent(String host, int port, long key, boolean visitedAll) {
    if ( visitedAll ) {
      log.log("No such content");
    } else if ( hashTable.contains(key) ) {
      hashTable.removeByKey(key);
    } else {
      String checkedAll = "false";
      if (next.host.equals(host) && next.port == port)
        checkedAll = "true";
      Message removeMsg = new Message(CMD.REMOVECONTENT, new String[] {
        host, String.valueOf(port), String.valueOf(key), checkedAll
      });
      sendMessage(removeMsg, next.host, next.port);
    }
  }

  private static void lookupContent(String host, int port, long key) {
    String content = hashTable.retrieve(key);
    // TODO: need to communicate back to LookupContent.java to tell it to print key
  }

  private static void allKeys(String host, int port) {
    String allKeys = hashTable.getAllKeys();
  }

  private static void printAllContent( String host, int port, ConnectionManager connMan ) {

    log.log("Next host: " + next.port);

    log.log("=====================================================================");
    for (Map.Entry<Long, String> entry : hashTable.getTable().entrySet()) {
      log.log(entry.getKey() + " " + entry.getValue());
    }

    if ( host.equals(next.host) && port == next.port )
      return;
    Message printMsg = new Message(CMD.PRINTALL, new String[] {
      host, String.valueOf(port)
    });

    sendMessage(printMsg, next.host, next.port);
  }

  public static void main(String[] args) {
    // for serializing/deserializing message into/from streams
    Message msg;
    // list of all active peers in the network
    ArrayList<Pair<InetAddress, Integer>> Peers = new ArrayList<>();

    hashTable = new DHT();

    String connectionHost = null;
    int connectionPort = 0;

    if ( args.length == 2 ) {
      connectionHost = args[0];
      connectionPort = Integer.parseInt(args[1]);
    }

    ConnectionManager connMan;
    ServerSocket listener = null;
    try {
      try {
        connMan = new ConnectionManager();
        listener = connMan.getAvailableConnection();
        Address currPeerAddress = new Address(connMan.getHostName(), connMan.getConnectionPort());

        log = new PSLogger(Peer.class.getName(),
            "Peer@" + connMan.getHostName() + ":" + connMan.getConnectionPort());
        log.log("Connected at : " + connMan.getHostName() + " " + connMan.getConnectionPort());


        if ( connectionHost != null ) {
          Address nextsPrev = requestPeerLink(connectionHost, connectionPort, true);

          log.log("Setting next to : " + connectionPort );
          next = new Address(connectionHost, connectionPort);
          log.log("Setting prev to : " + nextsPrev.port);
          prev = new Address(nextsPrev.host, nextsPrev.port);

          Message nextsPrevMsg = new Message(CMD.SETPREV, new String[] {
            connMan.getHostName(), String.valueOf(connMan.getConnectionPort())
          });
          sendMessage(nextsPrevMsg, connectionHost, connectionPort);

          Message prevsNextMsg = new Message(CMD.SETNEXT, new String[] {
            connMan.getHostName(), String.valueOf(connMan.getConnectionPort()), "false"
          });
          sendMessage(prevsNextMsg, nextsPrev.host, nextsPrev.port);

        } else {
          prev = next = new Address(connMan.getHostName(), connMan.getConnectionPort());
        }

        while ( true ) {
          server = listener.accept();

          ObjectInputStream inStream = new ObjectInputStream(server.getInputStream());
          ObjectOutputStream outStream = new ObjectOutputStream(server.getOutputStream());
          Message incoming = (Message) inStream.readObject();

          log.log("Message Recieved: " + incoming.cmd);
          try {
          switch (incoming.cmd) {
            case GETLINK:
              outStream.writeObject(prev);
              break;
            case SETNEXT:
              setNext( incoming.params[0], Integer.parseInt(incoming.params[1]), Boolean.parseBoolean(incoming.params[2]), connMan);
              outStream.writeObject("success");
              break;
            case SETPREV:
              setPrev( incoming.params[0], Integer.parseInt(incoming.params[1]));
              outStream.writeObject("success");
              break;
            case ADDPEER:
              addPeer(incoming.params[0], Integer.parseInt(incoming.params[1]), connMan);
              outStream.writeObject("success");
              break;
            case EXIT:
              removeAndSync();
              outStream.writeObject("success");
              break;
            case FINDMIN:
              Integer min = findMin(incoming.params[0], Integer.parseInt(incoming.params[1]), Integer.parseInt(incoming.params[2]));
              log.log("min: " + min);
              outStream.writeObject(min);
              break;
            case ADDCONTENT:
              System.out.println(incoming.params.length);
              addContent(incoming.params[0], Integer.parseInt(incoming.params[1]),
                  Long.parseLong(incoming.params[2]),
                  incoming.params[3], 
                  Integer.parseInt(incoming.params[4]), connMan);
              outStream.writeObject("success");
              break;
            case REMOVECONTENT:
              removeContent(incoming.params[0], Integer.parseInt(incoming.params[1]), Long.parseLong(incoming.params[2]), Boolean.parseBoolean(incoming.params[3]));
              outStream.writeObject("success");
              break;
            case LOOKUPCONTENT:
              lookupContent(incoming.params[0], Integer.parseInt(incoming.params[1]), Long.parseLong(incoming.params[2]));
              outStream.writeObject("success");

              break;
            case ALLKEYS:
              allKeys(incoming.params[0], Integer.parseInt(incoming.params[1]));
              outStream.writeObject("success");
              break;
            case PRINTALL:
              printAllContent( incoming.params[0], Integer.parseInt(incoming.params[1]), connMan);
              outStream.writeObject("success");
              break;
            default:
              break;
          }
          } finally {
            server.close();
          }

        }

      } catch (Exception e) {
        e.printStackTrace();
        log.log("Exception:" + e.getMessage());
      } finally {
        listener.close();
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
