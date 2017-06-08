JAVA_CC=/usr/bin/javac

echo --Killing all peers
killall -9 java

echo --Cleaning
rm -f *.jar
rm -f *.class

echo --Cleaning Logs
rm logging.log

echo --Compiling
$JAVA_CC -version
$JAVA_CC -cp .:"lib/*" *.java
#$JAVA_CC -cp .:"lib/*" PSLogger.java
#$JAVA_CC -cp .:"lib/*" Peer.java 
#$JAVA_CC -cp .:"lib/*" AddPeer.java
#$JAVA_CC -cp .:"lib/*" RemovePeer.java
#$JAVA_CC -cp .:"lib/*" CMD.java
#$JAVA_CC -cp .:"lib/*" Message.java
#$JAVA_CC -cp .:"lib/*" ConnectionManager.java
#$JAVA_CC -cp .:"lib/*" AddContent.java
#$JAVA_CC -cp .:"lib/*" DHT.java
#$JAVA_CC -cp .:"lib/*" RemoveContent.java


