package nyaschenko.oki.utils;

import java.util.ArrayList;

import android.text.format.DateUtils;

public class FeedItem {
	private String message;
	private long timeMillis;
	private String date;
	private ArrayList<String> largePhotos;
	
	public FeedItem(String message, String date, ArrayList<String> photos) {
		this.message = cleanMessage(message);
		this.date = date;
		this.largePhotos = photos;
	}

	public FeedItem() {
		
	}
	
	public long getTimeMillis() {
		return timeMillis;
	}

	public void setTimeMillis(long timeMillis) {
		this.timeMillis = timeMillis;
	}

	public String getMessage() {
		return message;
	}

	public String getDate() {
		return date;
	}

	public ArrayList<String> getLargePhotos() {
		return largePhotos;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public void setLargePhotos(ArrayList<String> largePhotos) {
		this.largePhotos = largePhotos;
	}

	private String cleanMessage(String message) {
		// remove everything between { }
		StringBuilder b = new StringBuilder();
		int inside = 0;
		for (int i = 0; i < message.length(); i++) {
			if (message.charAt(i) == '{') {
				inside++;
				continue;
			}
			if (message.charAt(i) == '}') {
				inside--;
				continue;
			}
			if (inside == 0) {
				b.append(message.charAt(i));
			}
		}
		return b.toString();
	}
	
	public String getPrettyDate() {
		return DateUtils.getRelativeTimeSpanString(timeMillis).toString();
	}
	
	@Override
	public String toString() {
		return "Message length: " + message.length() + " photos: " + largePhotos.size();
	}
	
}
