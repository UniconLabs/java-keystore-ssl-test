Java Keystore SSL/HTTPS Test
====================
Java CLI utility to execute outbound HTTPS calls. 

## Build
```
mvn clean package
```

## Usage

- Download the JAR from [here](https://github.com/UniconLabs/java-keystore-ssl-test/releases)

- Run:

```
java -jar <jar-file-name> [--forced] <https://address.server.edu> [timeout]
```

## Sample

### Successful connection
```
[INFO]  Received host address https://www.google.com
[INFO]  Setting connection timeout to 5 second(s).
[INFO]  Trying to connect to https://www.google.com
[INFO]  Great! It worked.
```

### Failed connection
```
[INFO]  Received host address https://registry.npmjs.org/sailthru-client
[INFO]  Setting connection timeout to 5 second(s).
[INFO]  Trying to connect to https://registry.npmjs.org/sailthru-client
[INFO]  Could not connect to the host address https://registry.npmjs.org/sailthru-client
[INFO]  The error is: sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
[INFO]  Here are the details:
[SEVERE]        sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
Exception in thread "main" java.lang.RuntimeException: javax.net.ssl.SSLHandshakeException: sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
        at Test.main(Test.java:61)
Caused by: javax.net.ssl.SSLHandshakeException: sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
        at sun.security.ssl.Alerts.getSSLException(Alerts.java:192)
        at sun.security.ssl.SSLSocketImpl.fatal(SSLSocketImpl.java:1868)
        at sun.security.ssl.Handshaker.fatalSE(Handshaker.java:276)
        at sun.security.ssl.Handshaker.fatalSE(Handshaker.java:270)
        at sun.security.ssl.ClientHandshaker.serverCertificate(ClientHandshaker.java:1338)
        at sun.security.ssl.ClientHandshaker.processMessage(ClientHandshaker.java:154)
        at sun.security.ssl.Handshaker.processLoop(Handshaker.java:868)
        at sun.security.ssl.Handshaker.process_record(Handshaker.java:804)
        at sun.security.ssl.SSLSocketImpl.readRecord(SSLSocketImpl.java:998)
        at sun.security.ssl.SSLSocketImpl.performInitialHandshake(SSLSocketImpl.java:1294)
        at sun.security.ssl.SSLSocketImpl.startHandshake(SSLSocketImpl.java:1321)
        at sun.security.ssl.SSLSocketImpl.startHandshake(SSLSocketImpl.java:1305)
        at sun.net.www.protocol.https.HttpsClient.afterConnect(HttpsClient.java:515)
        at sun.net.www.protocol.https.AbstractDelegateHttpsURLConnection.connect(AbstractDelegateHttpsURLConnection.java:185)
        at sun.net.www.protocol.http.HttpURLConnection.getInputStream(HttpURLConnection.java:1299)
        at sun.net.www.protocol.https.HttpsURLConnectionImpl.getInputStream(HttpsURLConnectionImpl.java:254)
        at Test.main(Test.java:45)
Caused by: sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
        at sun.security.validator.PKIXValidator.doBuild(PKIXValidator.java:385)
        at sun.security.validator.PKIXValidator.engineValidate(PKIXValidator.java:292)
        at sun.security.validator.Validator.validate(Validator.java:260)
        at sun.security.ssl.X509TrustManagerImpl.validate(X509TrustManagerImpl.java:326)
        at sun.security.ssl.X509TrustManagerImpl.checkTrusted(X509TrustManagerImpl.java:231)
        at sun.security.ssl.X509TrustManagerImpl.checkServerTrusted(X509TrustManagerImpl.java:126)
        at sun.security.ssl.ClientHandshaker.serverCertificate(ClientHandshaker.java:1320)
        ... 12 more
Caused by: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
        at sun.security.provider.certpath.SunCertPathBuilder.engineBuild(SunCertPathBuilder.java:196)
        at java.security.cert.CertPathBuilder.build(CertPathBuilder.java:268)
        at sun.security.validator.PKIXValidator.doBuild(PKIXValidator.java:380)
        ... 18 more
```
 
### Detailed (--forced) Dump

```
INFO  - Received host address https://expired.badssl.com/ 
INFO  - Setting connection timeout to 5 second(s). 
INFO  - HTTPS Connection: setting custom handler 
INFO  - Trying to connect to https://expired.badssl.com/ 
INFO  - Response status code received 200 
INFO  - Detected Truststore: SunJSSE 
INFO  -   Trusted issuers found: 104 
INFO  - Server provided certs:  
INFO  -   subject: CN=*.badssl.com, OU=PositiveSSL Wildcard, OU=Domain Control Validated 
INFO  -   issuer: CN=COMODO RSA Domain Validation Secure Server CA, O=COMODO CA Limited, L=Salford, ST=Greater Manchester, C=GB 
INFO  -   expiration: Wed Apr 08 17:00:00 PDT 2015 - Sun Apr 12 16:59:59 PDT 2015 (invalid) 
INFO  -   trust anchor not matched in trust store (which is expected of the host certificate that is part of a chain) 
INFO  - --- 
INFO  -   subject: CN=COMODO RSA Domain Validation Secure Server CA, O=COMODO CA Limited, L=Salford, ST=Greater Manchester, C=GB 
INFO  -   issuer: CN=COMODO RSA Certification Authority, O=COMODO CA Limited, L=Salford, ST=Greater Manchester, C=GB 
INFO  -   expiration: Tue Feb 11 16:00:00 PST 2014 - Sun Feb 11 15:59:59 PST 2029 (valid) 
INFO  -   trust anchor matched found: CN=COMODO RSA Certification Authority, O=COMODO CA Limited, L=Salford, ST=Greater Manchester, C=GB 
INFO  - --- 
INFO  -   subject: CN=COMODO RSA Certification Authority, O=COMODO CA Limited, L=Salford, ST=Greater Manchester, C=GB 
INFO  -   issuer: CN=AddTrust External CA Root, OU=AddTrust External TTP Network, O=AddTrust AB, C=SE 
INFO  -   expiration: Tue May 30 03:48:38 PDT 2000 - Sat May 30 03:48:38 PDT 2020 (valid) 
INFO  -   trust anchor matched found: CN=AddTrust External CA Root, OU=AddTrust External TTP Network, O=AddTrust AB, C=SE 
INFO  - --- 
INFO  - Completed. See the info above for details. 
```
