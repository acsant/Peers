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
  private static DHT hashTable = new DHT();

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

    // TODO: account for last peer in the network

    // Load balance all current content into other peers
    for (Map.Entry<Long, String> entry : hashTable.getMap().entrySet()) {
      Long key = entry.getKey();
      String content = entry.getValue();
      Message addContentMsg = new Message(CMD.ADDCONTENT, new String[] {
        next.host, String.valueOf(next.port), String.valueOf(key), content, "-1"
      });
      sendMessage(addContentMsg, next.host, next.port);
    }

    log.log("Peer exiting");
    System.exit(0);
  }

  /**
   * Load balance across all the peers
   */
  private static void loadBalance ( String host, int port, int contentCount, int peerCount, ConnectionManager connMan ) {
    if ( peerCount != 0 && host.equals(connMan.getHostName()) && port == connMan.getConnectionPort()) {
      int lower = (int) Math.floor(contentCount / peerCount);
      int upper = (int) Math.ceil(contentCount / peerCount);
      

    }
    contentCount += hashTable.size();
    peerCount ++;
    Message countMsg = new Message( CMD.COUNT, new String[] {
      host, String.valueOf(port), String.valueOf(contentCount), String.valueOf(peerCount)
    });
    sendMessage( countMsg, next.host, next.port );
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

  private static void addContent(String host, int port, Long paramKey, String content, int max, ConnectionManager connMan) {
    log.log("ENTERING ADDCONTENT");
    if ( hashTable.size() == 0 || hashTable.size() < max || (max != -1 && (
              host.equals(connMan.getHostName()) && port == connMan.getConnectionPort()
            )) ) {
      long key = paramKey;
      if ( paramKey == 0 ) {
        key = hashTable.insert(content);
      } else {
        hashTable.put(key, content);
      }
      log.log("Key created: " + Long.toString(key));
    
    // Communicate back to AddContent.java to tell it to print key
    // TODO: ensure the host/port passed is the right one for addContent
    } else {
      Message distContent = new Message(CMD.ADDCONTENT, new String[] {
        host, String.valueOf(port), String.valueOf(paramKey), content, String.valueOf(hashTable.size())
      });
      log.log("finding next storage at : " + next.host + "@" + next.port);
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
    log.log(content); 
    // TODO: need to communicate back to LookupContent.java to tell it to print key
  }

  private static void allKeys(String host, int port) {
    String allKeys = hashTable.getAllKeys();
    log.log(allKeys);
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

        // Load balancing occurs here


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
            case ADDCONTENT:
              addContent(incoming.params[0], Integer.parseInt(incoming.params[1]),
                  Long.parseLong(incoming.params[2]),
                  incoming.params[3], 
                  Integer.parseInt(incoming.params[4]), connMan);
              break;
            case REMOVECONTENT:
              log.log("REMOVECONTENT");
              removeContent(incoming.params[0], Integer.parseInt(incoming.params[1]), Long.parseLong(incoming.params[2]), Boolean.parseBoolean(incoming.params[3]));
              break;
            case LOOKUPCONTENT:
              log.log("LOOKUPCONTENT");
              lookupContent(incoming.params[0], Integer.parseInt(incoming.params[1]), Long.parseLong(incoming.params[2]));
              break;
            case ALLKEYS:
              log.log("ALLKEYS");
              allKeys(incoming.params[0], Integer.parseInt(incoming.params[1]));
              break;
            default:
              break;
          }
        }

      } catch (Exception e) {
        log.log("Exception:" + e.getStackTrace().toString());
      } finally {
        listener.close();
        server.close();
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
