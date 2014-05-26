package nyaschenko.oki.utils;

public class PhotoItem {
	private String mUrlSmall;
	private String mUrlMedium;
	private String mUrlLarge;
	private String id;
	private String userId;
	private boolean liked;
	
	private String text;
	private String commentsCount;
	private String marksCount;
	
	public PhotoItem() {
		liked = false;
	}
	
	public boolean isLiked() {
		return liked;
	}

	public void setLiked(boolean wasLiked) {
		this.liked = wasLiked;
	}

	public String getUrl() {
		return mUrlLarge;
	}
	
	public String getUrlSmall() {
		return mUrlSmall;
	}
	
	public void setUrlSmall(String urlSmall) {
		mUrlSmall = urlSmall;
	}
	
	public String getUrlMedium() {
		return mUrlMedium;
	}
	
	public void setUrlMedium(String urlMedium) {
		mUrlMedium = urlMedium;
	}
	
	public String getUrlLarge() {
		return mUrlLarge;
	}
	
	public void setUrlLarge(String urlLarge) {
		mUrlLarge = urlLarge;
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getUserId() {
		return userId;
	}
	
	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getCommentsCount() {
		return commentsCount;
	}

	public void setCommentsCount(String commentsCount) {
		this.commentsCount = commentsCount;
	}

	public String getMarksCount() {
		return marksCount;
	}

	public void setMarksCount(String marksCount) {
		this.marksCount = marksCount;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
	
	

}
