
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
    Address temp = next;
    Message setNextsPrev = new Message(CMD.SETPREV, new String[] {
      prev.host, String.valueOf(prev.port)
    });
    sendMessage(setNextsPrev, next.host, next.port);

    Message setPrevsNext = new Message(CMD.SETNEXT, new String[] {
      next.host, String.valueOf(next.port), "true"
    });
    sendMessage(setPrevsNext, prev.host, prev.port);
    prev = next = null;
    // Rebalance
    for (Map.Entry<Long,String> entry : hashTable.getTable().entrySet()) {
      Message reAdd = new Message(CMD.ADDCONTENT, new String[] {
        temp.host, String.valueOf(temp.port), String.valueOf(entry.getKey()), entry.getValue(), String.valueOf(Integer.MAX_VALUE)
      });
      sendMessage(reAdd, temp.host, temp.port);
    }
    System.exit(0);
  }

  /**
   * Distribute all the content when load balancing
   */
  private static void distributeContent( String host, int port, int upper, ConnectionManager connMan ) {
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

  private static int requestPeerCount ( String startHost, int startPort, int peerCount ) {
    if ( next.port == startPort && next.host.equals(startHost) )
      return peerCount + 1;

    peerCount += 1;
    Message countMsg = new Message(CMD.PEERCOUNT, new String[] {
      startHost, String.valueOf(startPort), String.valueOf(peerCount)
    });
    return (Integer) sendMessage(countMsg, next.host, next.port);
  }

  private static int requestContentCount( String startHost, int startPort, int contentCount ) {
    if ( next.port == startPort && next.host.equals(startHost) )
      return contentCount + hashTable.size();

    contentCount += hashTable.size();
    Message countMsg = new Message(CMD.CONTENTCOUNT, new String[] {
      startHost, String.valueOf(startPort), String.valueOf(contentCount)
    });
    return (Integer) sendMessage( countMsg, next.host, next.port );
  }

  /**
   * Get content to redistribute
   */
  private static DHT getOverflowSet(String host, int port, int upper, DHT subset) {
    int i = hashTable.size();
    for ( Map.Entry<Long, String> entry : hashTable.getTable().entrySet()) {
      if ( i > upper) {
        subset.put(entry.getKey(), entry.getValue());
        i--;
      }
    }
    hashTable.removeAll(subset);
    log.log("Host:" + next.port + " " + port + " " + String.valueOf(hashTable.size()));
    if ( (!host.equals(next.host) || port != next.port)  ) {
      Message collectSet = new Message(CMD.SUBSET, new String[] {
        host, String.valueOf(port), String.valueOf(upper)
      });
      DHT extended = (DHT) sendMessage(collectSet, next.host, next.port);
      subset.getTable().putAll(extended.getTable());
    }
    return subset;
  }

  /**
   * Load balance across all the peers
   */
  private static void loadBalance ( String host, int port, int contentCount, int peerCount, ConnectionManager connMan ) {
    if ( contentCount == 0 && peerCount == 0 ) {
      contentCount = requestContentCount( host, port, contentCount );
      log.log("Content count " + contentCount);
      peerCount = requestPeerCount( host, port, peerCount );
      log.log("Peer count " + peerCount);
    }
    if ( (next.host.equals(host) && next.port == port) ||
        (contentCount == 0) ) 
      return;
    int lower = (int) Math.floor(contentCount / peerCount);
    int upper = (int) Math.ceil(contentCount / peerCount);
    log.log("Lower : " + String.valueOf(lower));
    log.log("Upper : " + String.valueOf(upper));
    log.log("dist content");
    //distributeContent( host, port, upper, connMan );
    DHT subset = new DHT();
    log.log("Host: " + host + "@" + port );
    subset = getOverflowSet( host, port, lower, subset ); 
    log.log("after dist: " + subset.size());
    for ( Map.Entry <Long, String> entry : subset.getTable().entrySet()) {
      if ( hashTable.size() < lower )
        hashTable.getTable().put(entry.getKey(), entry.getValue());
    }
    log.log("Subset: " + subset.size() + " hashtabel: " + hashTable.size() );
    subset.removeAll(hashTable);
    log.log("Subset: " + subset.size() + " hashtabel: " + hashTable.size() );


    for ( Map.Entry<Long, String> entry : subset.getTable().entrySet()) {
      Message distRest = new Message(CMD.ADDCONTENT, new String[] {
        connMan.getHostName(), String.valueOf(connMan.getConnectionPort()), String.valueOf(entry.getKey()),
          entry.getValue(), String.valueOf(lower)
      });
      sendMessage(distRest, next.host, next.port);
    }
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

  private static long addContent(String host, int port, Long paramKey, String content, int min, ConnectionManager connMan) {
    int minContent = min; 
    if ( minContent == Integer.MAX_VALUE ) {
      minContent = findMin(connMan.getHostName(), connMan.getConnectionPort(), min);
      log.log("find min returned : " + minContent);
    }
    if ( hashTable.size() == 0 || hashTable.size() == minContent ) {
      long key = paramKey;
      if ( paramKey == 0 ) {
        key = hashTable.insert(content);
      } else {
        hashTable.put(key, content);
      }
      log.log("Key created: " + Long.toString(key));
      return key;

      // Communicate back to AddContent.java to tell it to print key
      // TODO: ensure the host/port passed is the right one for addContent
    } else {
      Message distContent = new Message(CMD.ADDCONTENT, new String[] {
        host, String.valueOf(port), String.valueOf(paramKey), content, String.valueOf(minContent)
      });
      return (long) sendMessage(distContent, next.host, next.port);
    }
  }

  private static boolean removeContent(String host, int port, long key) {
    if ( hashTable.contains(key) ) {
      hashTable.removeByKey(key);
      return true;
    } 
    log.log("comparing " + port + ":" + next.port);
    if (next.host.equals(host) && next.port == port)
      return false;
    Message removeMsg = new Message(CMD.REMOVECONTENT, new String[] {
      host, String.valueOf(port), String.valueOf(key)
    });
    return (boolean) sendMessage(removeMsg, next.host, next.port);

  }

  private static String lookupContent(String host, int port, long key) {
    if ( hashTable.contains(key) ) {
      return (hashTable.retrieve(key));
    }
    
    if ( next.host.equals(host) && next.port == port ) {
      return null;
    }

    Message newLookup = new Message(CMD.LOOKUPCONTENT, new String[] {
      host, String.valueOf(port), String.valueOf(key)
    });
    return (String) sendMessage(newLookup, next.host, next.port);
  }

  private static String allKeys(String host, int port) {
    return hashTable.getAllKeys();
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

          loadBalance(connMan.getHostName(), connMan.getConnectionPort(), 0, 0, connMan);
        } else {
          prev = next = new Address(connMan.getHostName(), connMan.getConnectionPort());
        }
        //Add Peer is done here
        System.out.println(connMan.getHostName() + " " + connMan.getConnectionPort());
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
                long key = addContent(incoming.params[0], Integer.parseInt(incoming.params[1]),
                    Long.parseLong(incoming.params[2]),
                    incoming.params[3], 
                    Integer.parseInt(incoming.params[4]), connMan);
                outStream.writeObject(key);
                break;
              case REMOVECONTENT:
                boolean result = removeContent(incoming.params[0], Integer.parseInt(incoming.params[1]), Long.parseLong(incoming.params[2]));
                loadBalance(incoming.params[0], Integer.parseInt(incoming.params[1]), 0, 0, connMan);
                outStream.writeObject(result);
                break;
              case LOOKUPCONTENT:
                  String content = lookupContent(incoming.params[0], Integer.parseInt(incoming.params[1]), Long.parseLong(incoming.params[2]));
                  outStream.writeObject(content);

                break;
              case ALLKEYS:
                String allkeys = allKeys(incoming.params[0], Integer.parseInt(incoming.params[1]));
                outStream.writeObject(allkeys);
                break;
              case PRINTALL:
                printAllContent( incoming.params[0], Integer.parseInt(incoming.params[1]), connMan);
                outStream.writeObject("success");
                break;
              case CONTENTCOUNT:
                int contentCount = requestContentCount( incoming.params[0], Integer.parseInt(incoming.params[1]), Integer.parseInt(incoming.params[2]));
                outStream.writeObject(contentCount);
                break;
              case PEERCOUNT:
                int peerCount = requestPeerCount( incoming.params[0], Integer.parseInt(incoming.params[1]), Integer.parseInt(incoming.params[2]));
                outStream.writeObject(peerCount);
                break;
              case SUBSET:
                DHT subset = getOverflowSet(incoming.params[0], Integer.parseInt(incoming.params[1]), Integer.parseInt(incoming.params[2]), new DHT());
                outStream.writeObject(subset);
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
