package simpleTwitterCrawler;

import twitter4j.ConnectionLifeCycleListener;
import twitter4j.FilterQuery;
import twitter4j.StatusListener;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Wrapper around twitter4j library  
 *
 */
public class TwitterApi {
	private twitter4j.conf.Configuration twitterConfig = null;
	private TwitterStream stream = null;
	private Twitter restAPI = null;

	public TwitterApi() {
		this.twitterConfig = getTwitterConfiguration();
	
	}
	
	public boolean isStreaming() {
		if (stream == null)
			return false;

		return true;
	}

	/**
	 * Begin a new crawl
	 */
	public boolean startFiltering(StatusListener sl, FilterQuery query) {
		if (isStreaming()) {
			System.err.println("Stream is already open.");
			return false;
		}

		this.stream = new TwitterStreamFactory(twitterConfig).getInstance();

		stream.addListener(sl);
		stream.filter(query);

		return true;
	}

	public void updateFiltering(StatusListener sl, FilterQuery query, long lengthKeywords, int lengthUsers) {
		if (this.isStreaming())
			this.stopStreaming();
		if (lengthKeywords != 0 || lengthUsers != 0)
			this.startFiltering(sl, query);
	}

	/**
	 * Provides random sample of tweets
	 */
	public boolean startSampling(StatusListener sl) throws IllegalStateException, TwitterException {
		if (isStreaming()) {
			System.out.println("Stream is already open.");
			return false;
		}

		this.stream = new TwitterStreamFactory(twitterConfig).getInstance();

		stream.addListener(sl);
		stream.sample();

		return true;
	}

	public void stopStreaming() {
		// if the system isn't running, then ignore this call
		if (stream == null)
			return;
		// request the sampling service to stop
		stream.addConnectionLifeCycleListener(new ConnectionLifeCycleListener() {
			@Override
			public void onConnect() {
			}

			@Override
			public void onDisconnect() {
			}

			@Override
			public void onCleanUp() {
				// when the disconnection and shutdown is complete, remove the
				// reference to the stream
				stream = null;
			}
		});

		stream.shutdown();
		// block until the sample service has stopped
		while (stream != null) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
	}

	/*
	 * Set up access to Twitter API
	 */
	private twitter4j.conf.Configuration getTwitterConfiguration() {

		// create and initialize a configuration object
		ConfigurationBuilder tConfig = new ConfigurationBuilder();
		
		tConfig.setOAuthConsumerKey(Configuration.getConsumerKey());
		tConfig.setOAuthConsumerSecret(Configuration.getConsumerSecret());
		tConfig.setOAuthAccessToken(Configuration.getAccessToken());
		tConfig.setOAuthAccessTokenSecret(Configuration.getAccessTokenSecret());
		tConfig.setHttpRetryCount(5);
		tConfig.setGZIPEnabled(true);
		tConfig.setJSONStoreEnabled(true);
		
		//From http://stackoverflow.com/questions/21124202/twitter-camel-using-https-ssl
				//Twitter now (Jan2014) insists on https connections.
		tConfig.setRestBaseURL("https://api.twitter.com/1.1/");
		tConfig.setStreamBaseURL("https://stream.twitter.com/1.1/");
		tConfig.setSiteStreamBaseURL("https://sitestream.twitter.com/1.1/");
		tConfig.setUserStreamBaseURL("https://userstream.twitter.com/1.1/");
		tConfig.setOAuthRequestTokenURL("https://api.twitter.com/oauth/request_token");
		tConfig.setOAuthAccessTokenURL("https://api.twitter.com/oauth/access_token");
		tConfig.setOAuthAuthorizationURL("https://api.twitter.com/oauth/authorize");
		tConfig.setOAuthAuthenticationURL("https://api.twitter.com/oauth/authenticate");
			   
		if (!Configuration.getHttpProxyHost().isEmpty())
			tConfig.setHttpProxyHost(Configuration.getHttpProxyHost());
		if (Configuration.getHttpProxyPort() >= 0)
			tConfig.setHttpProxyPort(Configuration.getHttpProxyPort());

		return tConfig.build();
	}



}
