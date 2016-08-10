package net.unicon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class Test implements CommandLineRunner {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Test.class, args);
    }
    
    public void run(final String... args) throws Exception {
        System.setProperty("java.net.preferIPv4Stack", "true");
        
        try {
            if (args.length != 1 && args.length != 2) {
                logger.warn("Usage: java Test <https://address.server.edu> [timeout]");
                return;
            }

            logger.info("Received host address " + args[0]);
            URL constructedUrl = new URL(args[0]);
            URLConnection conn = constructedUrl.openConnection();
            if (args.length == 2) {
                conn.setConnectTimeout(Integer.valueOf(args[1]) * 1000);
            } else {
                conn.setConnectTimeout(5000);
            }
            logger.info("Setting connection timeout to " + conn.getConnectTimeout() / 1000 + " second(s).");

            logger.info("Trying to connect to " + args[0]);
            InputStreamReader reader = new InputStreamReader(conn.getInputStream(), "UTF-8");
            BufferedReader in = new BufferedReader(reader);

            in.readLine();

            if (conn instanceof HttpURLConnection) {
                int code = ((HttpURLConnection) conn).getResponseCode();
                logger.info("Response status code received " + code);
            }

            in.close();
            reader.close();

            logger.info("Great! It worked.");

        } catch (Exception e) {
            logger.info("Could not connect to the host address " + args[0]);
            logger.info("The error is: " + e.getMessage());
            logger.info("Here are the details:");
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }

    }
}
