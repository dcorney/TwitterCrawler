package simpleTwitterCrawler;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.lang.ArrayUtils;

import twitter4j.*;

/**
 * Utility to crawl Twitter's Streaming API by following a pre-specified list of accounts ("news hounds").
 * Uses twitter4j for the interface
 */
public class SimpleTwitterCrawler {

	static DateFormat formatter = new SimpleDateFormat("HH:mm:ss dd MM yyyy ZZZZ");
	private TwitterApi api;
	SimpleStatusUpdatesListener listener;		//Twitter stream listener
	FilterQuery query = new FilterQuery();		//combination of user ids and keywords

	static String twitterConfigFile1;
	static String twitterConfigFile2;
	static boolean hourlyChunks;			//if true, then create new time-stamped file every hour. Else put everything into one big file
	static boolean writeJSON=true;			//if true, statusListeners also output JSON (otherwise just plain text)

	/**
	 * Initialise a crawler (i.e. a listener with filters) based on list of Twitter
	 * ids. Store tweets in text file.
	 */
	private void startCrawl(long[] ids, String fileroot, TwitterApi napi) throws Exception {
		this.api = napi;
		List<String> track = new ArrayList<String>(); // for keywords - will be empty though.
		query.follow(ids);

		// create listener - which actually gets the tweets
		listener = new SimpleStatusUpdatesListener(fileroot,writeJSON);
		api.updateFiltering(listener, query, track.size(), (ids == null) ? 0: ids.length);

		System.out.println("Started crawling " + ids.length	+ " hounds into file (writeJSON=" + writeJSON + ")");
	}


	/**
	 * Stop existing crawler
	 */
	private void stopCrawl() {
		api.stopStreaming();
		listener.stopStreaming();
	}

	/**
	 * Load in pre-created text file of a pack of news hounds. Each line should
	 * have one twitter ID and one name, comma separated; or else a long comma-separated list on one line.
	 * The crawler will then fetch tweets from any of these accounts.
	 */

	public static long[] importUsersFile(String filename, int maxToRead) {
		//TODO: Move to separate 'utils' class
		ArrayList<Long> ids = new ArrayList<Long>();

		try {
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = reader.readLine()) != null & (maxToRead>0) ) {
				long userId = -1;
				StringTokenizer st = new StringTokenizer(line, ",");
				if (st.countTokens() == 2) {
					userId = Long.parseLong(st.nextToken());
					String username = st.nextToken();
					ids.add(userId);
					maxToRead--;
				} else if(st.countTokens()==1) {
					userId = Long.parseLong(line);
					ids.add(userId);
					maxToRead--;
				} else{	//long CSV list
					while (st.hasMoreTokens()){
						userId = Long.parseLong(st.nextToken());
						ids.add(userId);
						maxToRead--;
						if(maxToRead<=0){break;}
					}
				}
			}
			reader.close();

		} catch (FileNotFoundException e) {
			System.err.println("Importing news hound list - file not found: "+ filename );
			return null;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (java.lang.NumberFormatException e) {
			System.out.println("Invalid users file: " + filename + ". Each line should be 'twitterId,username'");
		}


		Long[] idArray = new Long[ids.size()];
		idArray = ids.toArray(idArray);

		System.out.println("Loaded " + idArray.length + " hounds from '" + filename + "'");
		return ArrayUtils.toPrimitive(idArray);
	}

	/**
	 * Create a new Twitter API object based on a config file (with OAuth keys)
	 * defined by parameter
	 */
	private static TwitterApi makeTwitterApi(String configFilename) {
		Configuration.setConfig(configFilename);
		TwitterApi api = new TwitterApi();

		//System.out.println("App-only authentication? " + Configuration.getApplicationOnlyAuthEnabled());
		return api;
	}

	/**
	 * Wait for specified number of seconds, e.g. while a crawl takes places.
	 */
	public static void waitNsecs(int k) {
		long time0, time1;
		time0 = System.currentTimeMillis();
		do {
			time1 = System.currentTimeMillis();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while ((time1 - time0) < k * 1000);
	}

	/**
	 * Wait N seconds and show speed of all crawlers (in tweets-per-second) every few seconds
	 */
	public static void waitNsecs(int k, ArrayList<SimpleTwitterCrawler> mcList) {
		long time0, time1;
		time0 = System.currentTimeMillis();
		do {
			time1 = System.currentTimeMillis();
			try {
				Thread.sleep(15000);	//display current stream speed every X milliseconds
				for (SimpleTwitterCrawler thismc : mcList) {
					float tps=thismc.listener.getSpeed();
					System.out.print(String.format("%4.1f tps\t" , tps));
				}
				System.out.println();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while ((time1 - time0) < k * 1000);
	}

	/**
	 * Processes date strings as produced by Twitter APIs
	 */
	public static Date formatTwitterDate(String created_at){
		DateFormat formatReader = new SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZZ yyyy");
		try {
			return formatReader.parse(created_at);
		} catch (ParseException e) {
			System.err.println("Date parsing error");
			e.printStackTrace();
		}
		return null;
	}



	/**
	 * Wait until a specified clock-time, e.g. before stating a crawl.
	 * Specify start time as yyyy-MM-dd HH:mm:ss
	 */
	public static void pauseUntil(String startTimeStr){
		//TODO: Move to separate 'utils' class
		String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
		Date startTime;
		try {
			startTime = sdf.parse(startTimeStr);

			long now = System.currentTimeMillis();
			while((now-startTime.getTime()<0)){
				if(now-startTime.getTime()<-10000){
					System.out.println("Will start crawl in " + (startTime.getTime() - now)/60000 + " mins");
					Thread.sleep(60*1000);
				}
				else{
					System.out.print(".");
					Thread.sleep(1000);
				}
				now = System.currentTimeMillis();
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Start a single crawl of a specified list of Twitter accounts
	 */
	public static void simpleCrawl(String collName, String twitterConfigFile, String houndsFile, String startTimeStr, int crawlLengthMins) {

		ArrayList<SimpleTwitterCrawler> mcList = new ArrayList<SimpleTwitterCrawler>();

		int maxToRead = 5000;
		System.out.println("Starting crawlers at " + new Date().toString());
		long[] thisIdList = importUsersFile(houndsFile, maxToRead);

		pauseUntil(startTimeStr);

		SimpleTwitterCrawler mc = new SimpleTwitterCrawler();
		TwitterApi api = makeTwitterApi(twitterConfigFile);
		try {
			mc.startCrawl(thisIdList, collName, api);
		} catch (Exception e) {
			e.printStackTrace();
		}
		mcList.add(mc);

		waitNsecs(crawlLengthMins * 60);

		System.out.println("\n\nStopping crawlers at " + new Date().toString());
		for (SimpleTwitterCrawler thismc : mcList) {
			thismc.stopCrawl();

		}


	}


	/**
	 *  Initialise a new pair of simultaneous crawls
	 */
	public static void pairedCrawl(String list1File, String out1File, String list2File,String out2File, int crawlLengthMins) throws Exception{

		ArrayList<SimpleTwitterCrawler> mcList = new ArrayList<SimpleTwitterCrawler>();

		int maxToRead = 5000;	//how many accounts to crawl in each crawler
		long[] thisIdList = importUsersFile(list1File, maxToRead);
		long[] thatIdList =null;
		if(list2File!=null){
			thatIdList =importUsersFile(list2File, maxToRead);
		}


		SimpleTwitterCrawler mc = new SimpleTwitterCrawler();
		TwitterApi api = makeTwitterApi(twitterConfigFile1);
		mc.startCrawl(thisIdList, out1File, api);
		mcList.add(mc);
		if(thatIdList!=null){		//optionally create second crawler
			SimpleTwitterCrawler mc2 = new SimpleTwitterCrawler();
			TwitterApi api2 = makeTwitterApi(twitterConfigFile2);
			mc2.startCrawl(thatIdList, out2File, api2);
			mcList.add(mc2);
		}


		System.out.println("Crawling from " + list1File + " into " + out1File + " for " + crawlLengthMins + " mins.");
		if(list2File!=null){
			System.out.println("Crawling from " + list2File + " into " + out2File + " for " + crawlLengthMins + " mins.");
		}

		waitNsecs(crawlLengthMins * 60, mcList);	//wait for actual crawl to happen.

		System.out.println("\n\nStopping crawlers...");
		for (SimpleTwitterCrawler thismc : mcList) {
			thismc.stopCrawl();
		}

	}

	/**
	 * Initialise a new crawl, based on parameters specified in external crawler.config properties file.
	 * If two lists of news hounds (twitter accounts) are given, they will be crawled in parallel. Or a
	 * single list can be used on its own.
	 */
	public static void initCrawl(){
		//TODO: Generalise to allow n lists (though be aware of Twitter's API limits)
		//TODO: Allow config files to be specified on command line
		String startTime="";
		int crawlMins=0;
		String houndlist1="";
		String houndlist2="";
		String houndLabel1="";
		String houndLabel2="";
		Properties prop = new Properties();
		try
		{
			// the configuration file name
			String fileName = "config\\crawler.config";
			InputStream is = new FileInputStream(fileName);
			System.out.println("Reading config file: " + fileName);

			// load the properties file
			prop.load(is);

			startTime = prop.getProperty("start.date") +" " + prop.getProperty("start.time");
			String crawlLengthStr = prop.getProperty("duration.mins");

			crawlMins=Integer.parseInt(crawlLengthStr);
			houndlist1=prop.getProperty("newshounds.1.file");
			houndlist2=prop.getProperty("newshounds.2.file");
			if(houndlist1!=null){houndlist1="config\\" +houndlist1;}
			if(houndlist2!=null){houndlist2="config\\" +houndlist2;}
			houndLabel1=prop.getProperty("newshounds.1.label");
			houndLabel2=prop.getProperty("newshounds.2.label");
			twitterConfigFile1=prop.getProperty("twitter.1.config");
			twitterConfigFile2=prop.getProperty("twitter.2.config");
			if(twitterConfigFile1!=null){twitterConfigFile1="config\\" +twitterConfigFile1;}
			if(twitterConfigFile2!=null){twitterConfigFile2="config\\" +twitterConfigFile2;}

			hourlyChunks=false;
			String hourlyChunksStr=prop.getProperty("hourly.chunks");
			if(hourlyChunksStr.equalsIgnoreCase("true")){
				hourlyChunks=true;
			}

			writeJSON=true;
			String writeJSONStr=prop.getProperty("write.JSON");
			if(writeJSONStr.equalsIgnoreCase("false")){
				writeJSON=false;
			}


		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}

		pauseUntil(startTime);

		String now = new SimpleDateFormat("ddMMyy_HHmm").format(Calendar.getInstance().getTime());
		String out1File=houndLabel1+"_"+now;
		String out2File=houndLabel2+"_"+now;
		try {

			if(hourlyChunks==false){
				//one long uninterrupted crawl
				pairedCrawl(houndlist1, out1File, houndlist2,out2File,crawlMins);

			}else{
				//series of hour-long chunked crawls. Crawl for an hour, then stop & re-start, saving tweets to a new file each time.
				int numCrawls=(int)(Math.ceil(crawlMins/60.0F));
				for(int cr=0;cr<numCrawls;cr++){
					now = new SimpleDateFormat("ddMMyy_HHmm").format(Calendar.getInstance().getTime());
					out1File=houndLabel1+"_"+now;
					out2File=houndLabel2+"_"+now;
					System.out.println("Starting crawl " + (cr+1) + " of " + numCrawls + " at " + now);
					pairedCrawl(houndlist1, out1File, houndlist2,out2File, crawlMins);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Done!");
	}

	public static void main(String[] args) throws Exception {
		System.setProperty("org.apache.commons.logging.Log","org.apache.commons.logging.impl.NoOpLog"); //suppress log messages
		initCrawl();

	}
}
