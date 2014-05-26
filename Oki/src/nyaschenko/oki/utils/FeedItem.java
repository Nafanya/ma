package nyaschenko.oki.utils;

import java.util.ArrayList;

import android.text.format.DateUtils;

public class FeedItem {
	private String message;
	private long timeMillis;
	private String date;
	private ArrayList<String> largePhotos;
	private ArrayList<String> smallPhotos;
	
	private String likes;
	private String comments;
	
	public FeedItem(String message, String date, ArrayList<String> photos) {
		this.message = cleanMessage(message);
		this.date = date;
		long cnt = 1;
		for (int i = 0; i < date.length(); i++) {
			cnt *= (int) date.charAt(i);
		}
		this.largePhotos = photos;
		this.likes = Long.toString(Math.abs(cnt % 73));
		this.comments = Long.toString(Math.abs((cnt * 37) % 46));
	}

	public FeedItem() {
		
	}
	
	public String getLikes() {
		return likes;
	}

	public void setLikes(String likes) {
		this.likes = likes;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public ArrayList<String> getSmallPhotos() {
		return smallPhotos;
	}

	public void setSmallPhotos(ArrayList<String> smallPhotos) {
		this.smallPhotos = smallPhotos;
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
