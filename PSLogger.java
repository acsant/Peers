import java.util.*;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;

public class PSLogger {
  String prefix;
  static Logger log;
  
  PSLogger(String name, String prefix) {
    PropertyConfigurator.configure("log4j.properties");
    log = Logger.getLogger(name);

    this.prefix = prefix;
  }

  public void log(String message) {
    log.info(prefix + " : " + message);
  }

}
