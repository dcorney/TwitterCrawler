package simpleTwitterCrawler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.json.DataObjectFactory;

/**
 * Helper class for listening to twitter events and storing new statuses
 * 
 */
public class SimpleStatusUpdatesListener implements StatusListener {

	private List<String> _deletionIdsQueue = new ArrayList<String>();
	private String fileRoot;
	private int recentTwitterCount;
	private long recentTwitterTimestamp;

	private boolean writeJSON=true;
	
	BufferedWriter bwJSON=null;
	BufferedWriter bwTxt =null;


	public SimpleStatusUpdatesListener(String filename, boolean writeJSON) {
		this.writeJSON = writeJSON;
		this.fileRoot =filename;
		recentTwitterCount = 0;
		recentTwitterTimestamp = System.currentTimeMillis();
		
		//Open two files (JSON + txt) and store handles to files for later writing
		File fileText = new File(filename + "_tweets.txt");
		
		
		try {
			OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(fileText.getAbsoluteFile()),Charset.forName("UTF-8").newEncoder());
			bwTxt = new BufferedWriter(fw);

			if(writeJSON==true){
				File fileJSON = new File(filename + "_tweets.json");
				fw = new OutputStreamWriter(new FileOutputStream(fileJSON.getAbsoluteFile()),Charset.forName("UTF-8").newEncoder());			
				bwJSON = new BufferedWriter(fw);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}


	/**
	 * Saves full tweet to JSON file and save just timestamp + tweet ID + tweet-text to separate text file
	 */
	@Override
	public void onStatus(Status status) {
		// process each Tweet
		String id = Long.toString(status.getId());
		if (_deletionIdsQueue.contains(id)) {
			System.out.println("Deletion notice: id must not be stored. ");
			_deletionIdsQueue.remove(id);
			return;
		}

		
//		if(recentTwitterCount==0){
//		  System.out.println(status.getText().replaceAll("\\n|\\r"," "));
//		}

		//Only store English-language tweets
		if(status.getLang().equals("en")){
			String msg=status.getText();
			msg=msg.replaceAll("\n|\r", " ");		//replace line breaks to improve readabiltiy
			Date created=status.getCreatedAt();

			try {
				bwTxt.write(created.toString() + "\t" + id + "\t" + msg);
				bwTxt.newLine();
				bwTxt.flush();		//keep file up-to-date

				if(writeJSON==true){
					String statusJson = DataObjectFactory.getRawJSON(status);
					statusJson =statusJson .replaceAll("\n|\r", " ");
					bwJSON.write(statusJson);		//just writing status.toString creates a Java-style string, without JSON formatting... 
					bwJSON.newLine();
					bwJSON.flush();		//keep file up-to-date
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//		else{
		//			System.out.println("Test: Not English-language sender: " + status.getText());
		//		}

		recentTwitterCount++;


	}

	@Override
	public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {

	}

	@Override
	public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
		System.err.println("Got TrackLimitationNotice!\n" + numberOfLimitedStatuses);
	}

	@Override
	public void onScrubGeo(long userId, long upToStatusId) {

	}

	@Override
	public void onException(Exception ex) {
		ex.printStackTrace();
	}


	public float getSpeed() {

		long elapsedTimeMillis = System.currentTimeMillis()
				- recentTwitterTimestamp;
		float rate = 0;
		if (elapsedTimeMillis > 0) {
			rate = (float) ((1000.0 * recentTwitterCount) / (float) elapsedTimeMillis);
		}
		// System.out.println("Recent speed: " + rate + " tweets per second");
		// reset counter for next request:
		recentTwitterCount = 0;
		recentTwitterTimestamp = System.currentTimeMillis();

		return rate;
	}

	@Override
	public void onStallWarning(StallWarning arg0) {
		System.err.println("Stall warning!\n"+ arg0);

	}

	/**
	 * Empty buffers & close files
	 */
	public void stopStreaming() {		 
		try {
			bwTxt.flush();			
			bwTxt.close();
			if(writeJSON==true){
				bwJSON.flush();			
				bwJSON.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}