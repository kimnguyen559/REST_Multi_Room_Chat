package domain;

public class Message {
	private String id;				// unique id
	private String message;			// message text
	private Message next;			// the message following this message
	private String excludedUser;	// user that should not be receiving this message
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public Message getNext() {
		return next;
	}
	public void setNext(Message next) {
		this.next = next;
	}
	public String getExcludedUser() {
		return excludedUser;
	}
	public void setExcludedUser(String excludedUser) {
		this.excludedUser = excludedUser;
	}
}