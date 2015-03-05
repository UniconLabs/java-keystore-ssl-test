import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;

import java.util.logging.ConsoleHandler;
import java.util.logging.SimpleFormatter;
import java.util.logging.Logger;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Level;

public class Test extends Formatter {
  public static void main(String[] args) throws Exception {
    System.setProperty("java.net.preferIPv4Stack" , "true");
    
    Logger theLogger = Logger.getLogger(Test.class.getName());
    theLogger.setUseParentHandlers(false);
    
    ConsoleHandler handler = new ConsoleHandler();
    handler.setFormatter(new Test());
    theLogger.addHandler(handler);
      
    try {
      if (args.length != 1 && args.length != 2) {
        theLogger.warning("Usage: java Test <https://address.server.edu> [timeout]");
        return;
      }
      
      theLogger.info("Received host address " + args[0]);
      URL constructedUrl = new URL(args[0]);
      
      URLConnection conn = constructedUrl.openConnection();
      
      
      if (args.length == 2) {
        conn.setConnectTimeout(Integer.valueOf(args[1]) * 1000);
      } else {
        conn.setConnectTimeout(5000);
      }
      theLogger.info("Setting connection timeout to " + conn.getConnectTimeout() / 1000 + " second(s).");
      
      theLogger.info("Trying to connect to " + args[0]);
      InputStreamReader reader = new InputStreamReader(conn.getInputStream(), "UTF-8");
      BufferedReader in = new BufferedReader(reader);
      
      in.readLine();
      
      if (conn instanceof HttpURLConnection) {
        int code = ((HttpURLConnection)conn).getResponseCode();
        theLogger.info("Response status code received " + code);
      }
      
      in.close();
      reader.close();
   
      theLogger.info("Great! It worked.");
      
    } catch (Exception e) {
      theLogger.info("Could not connect to the host address " + args[0]);
      theLogger.info("The error is: " + e.getMessage());
      theLogger.info("Here are the details:");
      theLogger.log(Level.SEVERE, e.getMessage(), e);
      
      throw new RuntimeException(e);
    }
  }

  public String format(LogRecord record) {
    StringBuffer sb = new StringBuffer();
    
    sb.append("[");
    sb.append(record.getLevel().getName());
    sb.append("]\t");
     
    sb.append(formatMessage(record));
    sb.append("\n");

    return sb.toString();
  }
}
