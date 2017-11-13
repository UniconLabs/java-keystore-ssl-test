package net.unicon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.InetSocketAddress;



public class Test implements ApplicationRunner {
    String USAGE = "Usage: java Test [--proxy=<http://proxy.example.com:port>] <https://address.server.edu> [timeout]";
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Test.class, args);
    }
    
    public void run(ApplicationArguments args) throws Exception {
        System.setProperty("java.net.preferIPv4Stack", "true");
        
        try {
            if (args.getNonOptionArgs().size() != 1 && args.getNonOptionArgs().size() != 2) {
                logger.warn(USAGE);
                return;
            }

            logger.info("Received host address " + args.getNonOptionArgs().get(0));
            URL constructedUrl = new URL(args.getNonOptionArgs().get(0));
            URLConnection conn;
            if (args.containsOption("proxy")) {
                if (args.getOptionValues("proxy").size() != 1) {
                    logger.warn(USAGE);
                    return;
                }
                URL proxyUrl = new URL(args.getOptionValues("proxy").get(0));
                logger.info("Using proxy address " + proxyUrl.toString());
                InetSocketAddress proxyAddr = new InetSocketAddress(proxyUrl.getHost(),proxyUrl.getPort());
                conn = constructedUrl.openConnection(new Proxy(Type.HTTP, proxyAddr));
            } else {
                conn = constructedUrl.openConnection();
            }
            if (args.getNonOptionArgs().size() == 2) {
                conn.setConnectTimeout(Integer.valueOf(args.getNonOptionArgs().get(1)) * 1000);
            } else {
                conn.setConnectTimeout(5000);
            }
            logger.info("Setting connection timeout to " + conn.getConnectTimeout() / 1000 + " second(s).");

            logger.info("Trying to connect to " + args.getNonOptionArgs().get(0));
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
            logger.info("Could not connect to the host address " + args.getNonOptionArgs().get(0));
            logger.info("The error is: " + e.getMessage());
            logger.info("Here are the details:");
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }

    }
}
