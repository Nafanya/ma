package nyaschenko.oki.utils;

public class FriendItem {
	private String uid;
	private String firstName;
	private String lastName;
	private String photo;
	
	public FriendItem(String uid, String firstName, String lastName, String photo) {
		this.uid = uid;
		this.firstName = firstName;
		this.lastName = lastName;
		this.photo = photo;
	}

	public String getName() {
		return firstName + " " + lastName;
	}

	public String getPhoto() {
		return photo;
	}

	public void setPhoto(String photo) {
		this.photo = photo;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	
	
	

}
