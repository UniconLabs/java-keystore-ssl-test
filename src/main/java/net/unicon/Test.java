package net.unicon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.InetSocketAddress;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Test implements ApplicationRunner {
    String USAGE = "Usage: java Test [--proxy=<http://proxy.example.com:port>] [--forced] <https://address.server.edu> [timeout]";
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Test.class, args);
    }
    
    public void run(ApplicationArguments args) throws Exception {
        System.setProperty("java.net.preferIPv4Stack", "true");
        URLConnection conn = null;
        try {
            if (args.getNonOptionArgs().size() != 1 && args.getNonOptionArgs().size() != 2) {
                logger.warn(USAGE);
                return;
            }

            logger.info("Received host address " + args.getNonOptionArgs().get(0));
            URL constructedUrl = new URL(args.getNonOptionArgs().get(0));

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

            if (conn instanceof HttpsURLConnection && args.containsOption("forced")) {
                ((HttpsURLConnection) conn).setSSLSocketFactory(getSSLContext().getSocketFactory());
                logger.info("HTTPS Connection: setting custom handler");
            }

            logger.info("Trying to connect to " + args.getNonOptionArgs().get(0));
            InputStreamReader reader = new InputStreamReader(conn.getInputStream(), "UTF-8");
            BufferedReader in = new BufferedReader(reader);

            in.readLine();

            if (conn instanceof HttpURLConnection) {
                int code = ((HttpURLConnection) conn).getResponseCode();
                logger.info("Response status code received " + code);
            }



            if (args.containsOption("forced")) {
                connectionReport(conn);
                logger.info("Completed. See the info above for details.");
            } else{
                logger.info("Great! It worked.");
            }

            in.close();
            reader.close();

        } catch (Exception e) {
            logger.info("Could not connect to the host address " + args.getNonOptionArgs().get(0));
            logger.info("The error is: " + e.getMessage());
            logger.info("Here are the exception details:");
            logger.error(e.getMessage(), e);
            logger.info("Run again with '--forced'.");
            throw new RuntimeException(e);
        }
    }

    protected void connectionReport(URLConnection connection) {
        if (!(connection instanceof HttpsURLConnection)) {
            logger.info("This is not an HTTPS-based connection.");
            return;
        }

        X509TrustManager[] trustManagers = getSystemTrustManagers();
        HttpsURLConnection httpsConnection = (HttpsURLConnection)connection;
        Certificate[] certificates;

        try {
            certificates = httpsConnection.getServerCertificates();
        } catch (SSLPeerUnverifiedException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }

        X509Certificate[] serverCertificates = Arrays.copyOf(certificates, certificates.length, X509Certificate[].class);

        logger.info("Server provided certs: ");
        for (X509Certificate certificate : serverCertificates) {

            String validity;
            try {
                certificate.checkValidity();
                validity = "valid";
            } catch (CertificateExpiredException e) {
                validity = "invalid";
            } catch (CertificateNotYetValidException e) {
                validity = "invalid";
            }

            logger.info("  subject: {}", certificate.getSubjectDN().getName());
            logger.info("  issuer: {}", certificate.getIssuerDN().getName());
            logger.info("  expiration: {} - {} ({})", certificate.getNotBefore(), certificate.getNotAfter(), validity);
            logger.info("  trust anchor {}", checkTrustedCertStatus(certificate, trustManagers));
            logger.info("---");
        }
    }

    protected X509TrustManager[] getSystemTrustManagers() {
        TrustManagerFactory trustManagerFactory = null;
        try {
            trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore)null);
        } catch (NoSuchAlgorithmException e) {

        } catch (KeyStoreException e) {

        }

        logger.info("Detected Truststore: {}", trustManagerFactory.getProvider().getName());
        List<X509TrustManager> x509TrustManagers = new ArrayList<X509TrustManager>();

        for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
            if (trustManager instanceof X509TrustManager) {
                X509TrustManager x509TrustManager = (X509TrustManager)trustManager;
                logger.info("  Trusted issuers found: " + x509TrustManager.getAcceptedIssuers().length);

                x509TrustManagers.add(x509TrustManager);
            }
        }

        return x509TrustManagers.toArray(new X509TrustManager[]{});
    }
    protected String checkTrustedCertStatus(X509Certificate certificate, X509TrustManager[] trustManagers) {

        for (X509TrustManager trustManager : trustManagers) {
            for (X509Certificate trustedCert : trustManager.getAcceptedIssuers()) {
                try {
                    certificate.verify(trustedCert.getPublicKey());
                    return "matched found: " + trustedCert.getIssuerDN().getName();

                } catch (CertificateException e) {
                    logger.trace("{}: {}", trustedCert.getIssuerDN().getName(), e.getMessage());
                } catch (NoSuchAlgorithmException e) {
                    logger.trace("{}: {}", trustedCert.getIssuerDN().getName(), e.getMessage());
                } catch (InvalidKeyException e) {
                    logger.trace("{}: {}", trustedCert.getIssuerDN().getName(), e.getMessage());
                } catch (NoSuchProviderException e) {
                    logger.trace("{}: {}", trustedCert.getIssuerDN().getName(), e.getMessage());
                } catch (SignatureException e) {
                    logger.trace("{}: {}", trustedCert.getIssuerDN().getName(), e.getMessage());
                }
            }
        }

        return "not matched in trust store (which is expected of the host certificate that is part of a chain)";
    }

    protected SSLContext getSSLContext() {
        try {
            SSLContext sslCtx = SSLContext.getInstance("TLS");
            sslCtx.init(null, new TrustManager[]{ new X509TrustManager() {

                private X509Certificate[] accepted;

                @Override
                public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                    accepted = xcs;
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return accepted;
                }
            }}, null);

            return sslCtx;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
