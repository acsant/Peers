import java.util.*;
import java.io.*;

// Marshalling
public class Message implements Serializable {
  CMD cmd;
  String[] params;

  Message (CMD cmd, String[] params) {
    this.cmd = cmd;
    this.params = params;
  }
}

