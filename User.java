import java.util.ArrayList;
import java.util.Date;


public class User {
	private String username;
	private String password;
	private ArrayList<String> blockedUsers;
	private ArrayList<String> offlineMessages;
	private boolean loggedIn;

	private Date lastOnline = null;
	private Date passwordBlock = null;

	public User(String u, String p) {
		this.username = u;
		this.password = p;
		loggedIn = false;
		blockedUsers = new ArrayList<String>();
		offlineMessages = new ArrayList<String>();
	}

	public String getUsername() {
		return this.username;
	}

	public String getPassword() {
		return this.password;
	}

	public void blockUser(String user) {
		for (int i = 0; i != blockedUsers.size(); i++) {
			if (blockedUsers.get(i).equals(user)) {
				return;
			}
		}
		blockedUsers.add(user);
	}

	public boolean unblockUser(String user) {
		for (int i = 0; i != blockedUsers.size(); i++) {
			String curr = blockedUsers.get(i);
			if (curr.equals(user)) {
				blockedUsers.remove(i);
				return true;
			}
		}
		return false;
	}

	public ArrayList<String> getBlockedUsers() {
		return blockedUsers;
	}

	public void addOfflineMessage(String message) {
		offlineMessages.add(message);
	}

	public ArrayList<String> getOfflineMessages() {
		return offlineMessages;
	}

	public void clearOfflineMessages() {
		offlineMessages.clear();
	}

	public Date getLastOnline() {
		return lastOnline;
	}

	public void setLastOnline() {
		lastOnline = new Date();
	}

	public Date getPasswordBlock() {
		return passwordBlock;
	}

	public void setPasswordBlock() {
		passwordBlock = new Date();
	}

	public void resetLastOnline() {
		lastOnline = null;
	}

	public Boolean getLoggedIn() {
		return loggedIn;
	}

	public void setLoggedIn(boolean l) {
		loggedIn = l;
	}

}