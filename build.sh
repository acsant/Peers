JAVA_CC=/usr/bin/javac

echo --Cleaning
rm -f *.jar
rm -f *.class

echo --Cleaning Logs
rm logging.log

echo --Compiling
$JAVA_CC -version
$JAVA_CC -cp .:"lib/*" PSLogger.java
$JAVA_CC -cp .:"lib/*" Peer.java 
$JAVA_CC -cp .:"lib/*" AddPeer.java

