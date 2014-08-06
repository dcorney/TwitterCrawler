package simpleTwitterCrawler;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

/**
 * Read Twitter OAuth settings from local XML file 
 *
 */
public class Configuration {

	private static final String CONSUMER_KEY = "twitter.oauth.consumer[@key]";
	private static final String CONSUMER_SECRET = "twitter.oauth.consumer[@secret]";
	private static final String ACCESS_TOKEN = "twitter.oauth.access[@token]";
	private static final String ACCESS_TOKEN_SECRET = "twitter.oauth.access[@secret]";
	private static final String HTTP_PROXY_HOST = "proxy[@host]";
	private static final String HTTP_PROXY_PORT = "proxy[@port]";
	private static final String APPLICATION_ONLY_AUTH_ENABLED="twitter.oauth2.appOnly[@flag]";

	private static XMLConfiguration config;

	public static void setConfig(String configFile) {
		config = new XMLConfiguration();
		config.setFileName(configFile);
		try {
			config.load();
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return the consumerKey
	 */
	public static String getConsumerKey() {
		return config.getString(CONSUMER_KEY);
	}

	/**
	 * @return the consumerSecret
	 */
	public static String getConsumerSecret() {
		return config.getString(CONSUMER_SECRET);
	}

	/**
	 * @return the accessToken
	 */
	public static String getAccessToken() {
		return config.getString(ACCESS_TOKEN);
	}

	/**
	 * @return the accessTokenSecret
	 */
	public static String getAccessTokenSecret() {
		return config.getString(ACCESS_TOKEN_SECRET);
	}

	/**
	 * @return the httpProxyHost
	 */
	public static String getHttpProxyHost() {
		return config.containsKey(HTTP_PROXY_HOST) ? config
				.getString(HTTP_PROXY_HOST) : "";
	}

	/**
	 * @return the httpProxyPort
	 */
	public static int getHttpProxyPort() {
		return config.containsKey(HTTP_PROXY_PORT) ? config
				.getInt(HTTP_PROXY_PORT) : -1;
	}

	public static boolean getApplicationOnlyAuthEnabled() {
		String flag=config.getString(APPLICATION_ONLY_AUTH_ENABLED);
		if(flag==null){return false;}
		return flag.equalsIgnoreCase("true");
	}
}
