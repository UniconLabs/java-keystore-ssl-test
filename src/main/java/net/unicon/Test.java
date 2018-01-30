package net.unicon;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.security.cert.X509Certificate;
import java.util.stream.Collectors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;

public class Test implements ApplicationRunner
{
	public static final int DEFAULT_TIMEOUT = 5;

	public static final String KEY_STORE = "javax.net.ssl.keyStore";

	public static final String KEY_STORE_PASSWD = "javax.net.ssl.keyStorePassword";

	public static final String TRUST_STORE = "javax.net.ssl.trustStore";

	public static final String TRUST_STORE_PASSWD = "javax.net.ssl.trustStorePassword";

	final String newLine = System.getProperty("line.separator");

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	public static void main(String[] args) throws Exception
	{
		SpringApplication.run(Test.class, args);
	}

	public void run(final ApplicationArguments args) throws Exception
	{
		System.setProperty("java.net.preferIPv4Stack", "true");
		boolean debugMode = false;

		try
		{
			if (!validArguments(args))
			{
				return;
			}
			debugMode = getDebugMode(args);
			disableCertificateCheck(args);
			String receivedUrl = args.getNonOptionArgs().get(0);
			logger.info("Received host address " + receivedUrl);
			setKeystoreInfo(args, debugMode);

			URL constructedUrl = new URL(receivedUrl);
			URLConnection conn;

			Proxy proxy = getProxy(args);

			if (proxy == null)
			{
				conn = constructedUrl.openConnection();
			}
			else
			{
				conn = constructedUrl.openConnection(proxy);
			}

			conn.setConnectTimeout(getConnectionTimeout(args));
			logger.info("Trying to connect to " + receivedUrl);

			InputStreamReader reader = new InputStreamReader(conn.getInputStream(), "UTF-8");
			BufferedReader in = new BufferedReader(reader);

			final String response = in.lines().collect(Collectors.joining(newLine));
			if (conn instanceof HttpURLConnection)
			{
				int code = ((HttpURLConnection)conn).getResponseCode();
				logger.info("Response status code received " + code);
			}

			in.close();
			reader.close();

			if (debugMode)
			{
				logger.debug("Response from the server was:");
				logger.debug(response);
			}
			logger.info("Great! It worked.");
		}
		catch (Exception e)
		{
			logger.info("Could not connect to the host address " + args.getNonOptionArgs().get(0));
			logger.info("The error is: " + e.getMessage());
			logger.info("Here are the details:");
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	private void setNonValidationHttps()
	{
		logger.info("Turning off https validation on certificate...");
		TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager()
		{
			public java.security.cert.X509Certificate[] getAcceptedIssuers()
			{
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType)
			{
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType)
			{
			}
		}};

		try
		{
			// Install the all-trusting trust manager
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

			// Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new HostnameVerifier()
			{
				public boolean verify(String hostname, SSLSession session)
				{
					return true;
				}
			};
		}
		catch (Exception err)
		{
			logger.error(err.getMessage(), err);
		}
	}

	/**
	 * Set the keystore information on the JDK if the user specified an alternate keystore or truststore.
	 * 
	 * @param args
	 *           The program arguments.
	 * @param debug
	 *           Wheither or not to show debugging information on the output.
	 */
	private void setKeystoreInfo(ApplicationArguments args, boolean debug)
	{
		if (args.containsOption("keystore"))
		{
			String keyStoreLocation = findLocation(args.getOptionValues("keystore").get(0));
			System.setProperty(KEY_STORE, keyStoreLocation);
			if (debug)
			{
				logger.debug("Set keystore location to: " + keyStoreLocation);
			}
			if (args.containsOption("keypass"))
			{
				System.setProperty(KEY_STORE_PASSWD, args.getOptionValues("keypass").get(0));
			}
		}

		if (args.containsOption("truststore"))
		{
			String trustStoreLocation = findLocation(args.getOptionValues("truststore").get(0));
			System.setProperty(TRUST_STORE, trustStoreLocation);
			if (debug)
			{
				logger.debug("Set truststore location to: " + trustStoreLocation);
			}
			if (args.containsOption("trustpass"))
			{
				System.setProperty(TRUST_STORE_PASSWD, args.getOptionValues("trustpass").get(0));
			}
		}
	}

	/**
	 * Get the connection timeout for the connection. If the user did not specify a timout, a default timeout will be used instead.
	 * The timout is a value in seconds.
	 * 
	 * @param args
	 *           The program arguments.
	 * @return The actual value to express the nr of seconds for the timeout.
	 */
	private int getConnectionTimeout(ApplicationArguments args)
	{
		int timeout = DEFAULT_TIMEOUT;
		if (args.getNonOptionArgs().size() == 2)
		{
			timeout = Integer.valueOf(args.getNonOptionArgs().get(1));
		}
		logger.info("Setting connection timeout to " + timeout + " second(s).");
		return timeout * 1000;
	}

	/**
	 * Checks if the user specified a proxy, and if so, tries to configure a proxy for the connection. The proxy configuration
	 * consists of a proxy URL and optionally a user and password.
	 * 
	 * @param args
	 *           The program arguments.
	 * @return The configured proxy, or null if the user did not specifyone.
	 * @throws IOException
	 *            If the proxy could not be configured.
	 */
	private Proxy getProxy(ApplicationArguments args) throws IOException
	{
		URLConnection conn;
		if (args.containsOption("proxy"))
		{
			if (args.containsOption("proxyUser") && args.containsOption("proxyPass"))
			{
				final String proxyUser = args.getOptionValues("proxyUser").get(0);
				final String proxyPass = args.getOptionValues("proxyPass").get(0);

				Authenticator authenticator = new Authenticator()
				{

					public PasswordAuthentication getPasswordAuthentication()
					{
						return (new PasswordAuthentication(proxyUser, proxyPass.toCharArray()));
					}
				};
				logger.info("Setting proxy user: " + proxyUser + " with a proxyPass xxxxx");
				Authenticator.setDefault(authenticator);
			}

			URL proxyUrl = new URL(args.getOptionValues("proxy").get(0));
			logger.info("Using proxy address " + proxyUrl.toString());
			InetSocketAddress proxyAddr = new InetSocketAddress(proxyUrl.getHost(), proxyUrl.getPort());
			return new Proxy(Type.HTTP, proxyAddr);
		}
		else
		{
			return null;
		}
	}

	/**
	 * Check the progam arguments to see if the debug option was specified.
	 * 
	 * @param args
	 *           The program arguments.
	 * @return Returns true if debug was specified, otherwise returns false.
	 */
	private boolean getDebugMode(ApplicationArguments args)
	{
		boolean debugMode = false;
		if (args.containsOption("debug"))
		{
			System.setProperty("javax.net.debug", "ssl");
			debugMode = true;
			logger.debug("Current location is: " + getCurrentPath());
		}
		return debugMode;
	}

	private boolean disableCertificateCheck(ApplicationArguments args)
	{
		boolean disableCertificate = false;
		if (args.containsOption("no-verify"))
		{
			setNonValidationHttps();
			disableCertificate = true;
			logger.debug("Disabled verification of the certificate.");
		}
		return disableCertificate;
	}

	/**
	 * Validate the arguments that were used to start this program. When the arguments are not valid, a manual is shown on the output
	 * as well.
	 * 
	 * @param args
	 *           The program arguments.
	 * @return true if arguments are valid, false otherwise.
	 */
	private boolean validArguments(ApplicationArguments args)
	{
		boolean valid = true;
		if (args.getNonOptionArgs().size() < 1)
		{
			logger.warn("Please specify a valid URL to test.");
			valid = false;
		}

		if (args.containsOption("proxy") && args.getOptionValues("proxy").size() != 1)
		{
			logger.warn("Please specify a valid proxy URL.");
			valid = false;
		}

		if (args.containsOption("keystore") && args.getOptionValues("keystore").size() != 1)
		{
			logger.warn("Please specify a valid keystore.");
			valid = false;
		}

		if (args.containsOption("keyPass") && args.getOptionValues("keyPass").size() != 1)
		{
			logger.warn("Please specify a valid password for keystore.");
			valid = false;
		}

		if (args.containsOption("truststore") && args.getOptionValues("truststore").size() != 1)
		{
			logger.warn("Please specify a valid truststore.");
			valid = false;
		}

		if (args.containsOption("trustPass") && args.getOptionValues("trustPass").size() != 1)
		{
			logger.warn("Please specify a valid password for truststore.");
			valid = false;
		}

		if (!valid)
		{
			logger.warn(getManual());
		}
		return valid;
	}

	/**
	 * Find a specific file on the specified path. This path can be a full path or a relative path.
	 * 
	 * @param path
	 *           The file to find on the file system.
	 * @return The full path of the file that was found.
	 * @throws IllegalArgumentException
	 *            if the specified file was not found.
	 */
	private String findLocation(String path)
	{
		String location = null;
		String currentPath = getCurrentPath();
		String pathSeparator = System.getProperty("file.separator");
		if (!path.startsWith(pathSeparator))
		{
			// Relative path was specified.
			location = currentPath + pathSeparator + path;
		}
		else
		{
			// Full path was specified.
			location = path;
		}
		File file = new File(location);
		if (!file.exists())
		{
			throw new IllegalArgumentException("The specified file: " + path + " was not found.");
		}
		return file.getAbsolutePath();
	}

	/**
	 * Find the current path from which this application is being executed. This path can be reused to fetch other files such as key-
	 * and truststore.
	 * 
	 * @return The current folder path from where this application is running.
	 */
	private String getCurrentPath()
	{
		String path = Test.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		// Get rid of the file protocol
		if (path.startsWith("file"))
		{
			path = path.substring(5);
		}

		try
		{
			String jarFile = URLDecoder.decode(path, "UTF-8");
			String jarLocation = null;
			// Remove everything after the ! sign before the jar file name.
			if (jarFile.indexOf("!") == -1)
			{
				jarLocation = jarFile;
			}
			else
			{
				jarLocation = jarFile.substring(0, jarFile.indexOf("!"));
			}
			File file = new File(jarLocation);
			return file.getParent();
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Get a String containing the usage of this program.
	 * 
	 * @return
	 */
	private String getManual()
	{
		return newLine + newLine + "Usage: java -jar <ProgramJar> OPTIONS URL TIMEOUT " + newLine + "OPTIONS: " + newLine
					+ "   [--debug] Specify if you want debug trace output." + newLine
					+ "   [--no-verify] Turn off SSL verification of the certificate." + newLine
					+ "   [--proxy=<http://proxy.example.com:port>] A valid URL to the proxy host (with port)." + newLine
					+ "   [--proxyUser=<user> --proxyPass=<pass>] If you specify a proxyUser, also specify password." + newLine
					+ "   [--keystore=<keystore.jks>] The location for an alternative keystore (full path or relative to current dir)."
					+ newLine //
					+ "   [--keypass] The password for the keystore." + newLine
					+ "   [--truststore=<truststore.jks>] The location of an alternative truststore (full path or relative to current dir)."
					+ "   [--trustpass] The password for the truststore." + newLine + "	" + newLine + newLine
					+ "URL: A valid URL such as: https://address.server.edu " + newLine + newLine
					+ "TIMEOUT: timeout in seconds (default is 5)." + newLine;
	}
}
