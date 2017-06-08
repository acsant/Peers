JAVA_CC = javac
default:
	$(JAVA_CC) -cp .:"lib/*" *.java
	touch addpeer
	echo 'java -cp .:"lib/*" AddPeer $$1 $$2' > addpeer && chmod +x addpeer
	touch removepeer
	echo 'java -cp .:"lib/*" RemovePeer $$1 $$2' > removepeer && chmod +x removepeer
	touch addcontent
	echo 'java -cp .:"lib/*" AddContent $$1 $$2 $$3' > addcontent && chmod +x addcontent
	touch removecontent
	echo 'java -cp .:"lib/*" RemoveContent $$1 $$2 $$3' > removecontent && chmod +x removecontent
	touch lookupcontent
	echo 'java -cp .:"lib/*" LookupContent $$1 $$2 $$3' > lookupcontent && chmod +x lookupcontent
	touch allkeys
	echo 'java -cp .:"lib/*" AllKeys $$1 $$2' > allkeys && chmod +x allkeys

clean:
	rm -f *.class
	rm addpeer
	rm removepeer
	rm addcontent
	rm removecontent
	rm lookupcontent
	rm allkeys

