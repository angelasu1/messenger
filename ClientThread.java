import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientThread extends Thread {
	private Socket clientSocket;
	private final ClientThread[] threads;
	private int maxClientsCount;
	private BufferedReader inFromClient;
	private PrintStream outToClient;
	private String username;
	private String password;
	private CopyOnWriteArrayList<User> users;
	private CopyOnWriteArrayList<BlockedIP> blockedIPs;
	private int blockDuration;
	private int timeout;
	private String serverAddress;

	public ClientThread(Socket clientSocket, ClientThread[] threads, CopyOnWriteArrayList<User> users, CopyOnWriteArrayList<BlockedIP> blockedIPs, int blockDuration, int timeout) {
		this.clientSocket = clientSocket;
		this.threads = threads;
		this.users = users;
		this.blockedIPs = blockedIPs;
		this.blockDuration = blockDuration;
		this.timeout = timeout;
		maxClientsCount = threads.length;
	}

	private int lastActive = 0;

	Timer timer = new Timer();
	TimerTask task = new TimerTask() {
		public void run() {
			lastActive++;
			if (lastActive == timeout) {
				outToClient.println("Logged out by server due to inactivity...");
		    	setLastOnline();
		    	logoutNotification();
				closeThread();
			}
		}
	};

	public void startTimer() {
		timer.scheduleAtFixedRate(task, 1000, 1000);
	}

	public void run() {
		int maxClientsCount = this.maxClientsCount;
    	ClientThread[] threads = this.threads;

    	try {
    		inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	    	outToClient = new PrintStream(clientSocket.getOutputStream());

	    	// Get IP Address for current thread
	    	String serverAddr = inFromClient.readLine();
	    	String[] ip = serverAddr.split(" ");
	    	if (ip[0].equals("serverAddress:")) {
	    		serverAddress = ip[1];
	    	}

	    	// Check if IP address is currently blocked
	    	if (IPBlocked()) {
	    		outToClient.println("IP is currently blocked. Please try again later");
	    		closeThread();
	    	}

	    	// Check for valid username
	    	int usernameAttempts = 0;
	    	while (usernameAttempts != 3) {
	    		outToClient.println("Username: ");
	    		username = inFromClient.readLine().trim();
	    		if (validUsername(username)) {
					break;
				} 
				usernameAttempts++;
	    	}

	    	// If invalid username is entered 3 times, block IP address for blockedTime seconds
	    	if (usernameAttempts == 3) {
	    		outToClient.println("Invalid Username. Your IP has been blocked. Please try again later");
	    		blockIPAddress();
	    		closeThread();
	    	}

	    	// Check for correct password
	    	int passwordAttempts = 0;
	    	while (passwordAttempts != 3) {
	    		outToClient.println("Password: ");
		    	password = inFromClient.readLine().trim();
		    	if (wrongPasswordBlock()) {
		    		outToClient.println("Your account is blocked due to multiple login failures. Please try again later");
		    		closeThread();
		    	} else {
		    		if (correctPassword(users, username, password)) {
		    			if (isLoggedIn(this.username)) {
		    				outToClient.println("You are already logged in from another client.");
		    				closeThread();
		    			} else {
		    				User u = findUser(this.username);
		    				u.setLoggedIn(true);
		    				outToClient.println("Welcome to the greatest messaging application ever!");
				    		showOfflineMessages();
				    		loginNotification();
				    		startTimer();
				    		break;
		    			}
		    		}
		    	}
		    	passwordAttempts++;
	    	}

	    	// If password is entered incorrectly 3 times, block user from logging in from any IP address
	    	if (passwordAttempts == 3) {
	    		User u = findUser(this.username);
    			u.setPasswordBlock();
	    		outToClient.println("Invalid Password. Your account has been blocked. Please try again later");
	    		closeThread();
	    	}

	    	while (true) {
	    		lastActive = 0;
	    		String input = inFromClient.readLine();
	    		String[] parts = null;
	    		if (input != null) {
	    			parts = input.split(" ");
	    			if (input.equals("whoelse")) {
		    			whoelse();
		    		} else if (input.equals("logout")) {
		    			setLastOnline();
		    			logoutNotification();
		    			break;
		    		} else if (parts[0].equals("message")) {
		    			if (parts.length < 3) {
		    				outToClient.println("Error: incorrect format. To send message, use \"message <user> <message>\"");
		    			} else {
		    				String[] message = input.split(" ", 3);
							sendMessage(message[1], message[2]);
		    			}
		    		} else if (parts[0].equals("whoelsesince")) {
		    			if (parts.length < 2) {
		    				outToClient.println("Error: incorrect format. To check whoelsesince, use \"whoelsesince <seconds>\"");
		    			} else {
		    				whoelsesince(Integer.parseInt(parts[1]));
		    			}
		    		} else if (parts[0].equals("broadcast")) {
		    			if (parts.length < 2) {
		    				outToClient.println("Error: incorrect format. To broadcast message, use \"broadcast <message>\"");
		    			} else {
		    				String[] message = input.split(" ", 2);
		    				broadcastMessage(message[1]);
		    			}
		    		} else if (parts[0].equals("block")) {
		    			if (parts.length < 2) {
		    				outToClient.println("Error: incorrect format. To block user, use \"block <username>\"");
		    			} else {
		    				blockUser(parts[1]);
		    			}
		    		} else if (parts[0].equals("unblock")) {
		    			if (parts.length < 2) {
		    				outToClient.println("Error: incorrect format. To unblock user, use \"unblock <username>\"");
		    			} else {
		    				unblockUser(parts[1]);
		    			}
		    		} else {
		    			outToClient.println("Error. Invalid command");
		    		}
	    		}
	    	}
	    	closeThread();
    	} catch (IOException e) {
		}
    }

    private void closeThread() {
		for (int i = 0; i < maxClientsCount; i++) {
			if (threads[i] == this) {
				threads[i] = null;
			}
    	}
		try {
    		inFromClient.close();
			outToClient.close();
			clientSocket.close();
			timer.cancel();
			User u = findUser(this.username);
			u.setLoggedIn(false);
    	} catch (IOException e) {
    	}
    }

    private boolean isLoggedIn(String user) {
    	User u = findUser(user);
    	if (u.getLoggedIn()) {
    		return true;
    	}
    	return false;
    }

    private void blockIPAddress() {
    	BlockedIP ip = new BlockedIP(this.serverAddress);
    	blockedIPs.add(ip);
    }

    private boolean IPBlocked() {
    	for (int i = 0; i != blockedIPs.size(); i++) {
    		BlockedIP ip = blockedIPs.get(i);
    		if (serverAddress.equals(ip.getIPAddress())) {
    			Date blockTime = ip.getBlockedTime();
    			Date currTime = new Date();
    			long difference = currTime.getTime() - blockTime.getTime();
    			if ((difference/1000) < blockDuration) {
    				return true;
    			}
    		}
    	}
    	return false;
    }

    private boolean wrongPasswordBlock() {
    	User u = findUser(this.username);
    	Date blockedTime = u.getPasswordBlock();
    	Date currTime = new Date();
    	if (blockedTime != null) {
    		long difference = currTime.getTime() - blockedTime.getTime();
	    	if (difference/1000 < blockDuration) {
	    		return true;
	    	}
    	}
	    return false;
    }

    // start testing
    private void showlastactive() {
    	User u = findUser(this.username);
    	Date lastActive = u.getLastOnline();
    	Date currTime = new Date();
    	long difference = currTime.getTime() - lastActive.getTime();
    	outToClient.println(difference);
    }

    private void showRunningThreads() {
    	int i = 0;
		while(i < maxClientsCount) {
			if (threads[i] != null) {
				outToClient.println("user " + threads[i].username + " is running...");
			}
			i++;
		}
    }
    // end testing

    private void setLastOnline() {
    	User u = findUser(this.username);
    	u.setLastOnline();
    }

    private void whoelsesince(int timesince) {
    	for (int i = 0; i != users.size(); i++) {
    		User u = users.get(i);
    		Date lastOnline = u.getLastOnline();
    		Date currTime = new Date();
    		if (lastOnline != null) {
	    		long difference = currTime.getTime() - lastOnline.getTime();
	    		if ((difference/1000) <= timesince) {
	    			outToClient.println(u.getUsername());
	    		}
    		}
    	}
    	whoelse();
    }

	private boolean correctPassword(CopyOnWriteArrayList<User> users, String username, String password) {
		int j = 0;
		while (j != users.size()) {
			User curr = users.get(j);
			if (username.equals(curr.getUsername())) {
				if (password.equals(curr.getPassword())) {
					curr.resetLastOnline();
					return true;
				} else {
					return false;
				}
			}
			j++;
		}
		return false;
	}

	private boolean validUsername(String username) {
		int i = 0;
		while (i != users.size()) {
			User curr = users.get(i);
			if (username.equals(curr.getUsername())) {
				return true;
			}
			i++;
		}
		return false;
	}

	private void loginNotification() {
		try {
			for (int i = 0; i < maxClientsCount; i++) {
				if (threads[i] != null) {
					if (!(threads[i].username.equals(this.username))) {
						if (!isBlocked(threads[i].username)) {
							threads[i].outToClient.println(this.username + " logged in");
						}
					}
				}
			}
		} catch (NullPointerException n) {
		}
	}

	private void logoutNotification() {
		for (int i = 0; i < maxClientsCount; i++) {
			if (threads[i] != null) {
				if (!(threads[i].username.equals(username))) {
					if (!isBlocked(threads[i].username)) {
						threads[i].outToClient.println(username + " logged out");
					}
				}
			}
		}
	}

	// shows other users currently online
	private void whoelse() {
		for (int i = 0; i < maxClientsCount; i++) {
			if (threads[i] != null) {
				if (!(threads[i].username.equals(username))) {
					String username = threads[i].username;
					outToClient.println(username);
				}
			}
		}
	}

	// send message to one user
	private void sendMessage(String user, String message) {
		int i = 0;
		while(i < maxClientsCount) {
			if (threads[i] != null) {
				if (threads[i].username.equals(user)) {
					break;
				}
			}
			i++;
		}
		if (i == maxClientsCount) {
			if (validUsername(user)) {
				if (isBlockedBy(user)) {
					outToClient.println("Your message could not be delivered as the recipient has blocked you");
				} else {				
					User u = findUser(user);
					String offlineMessage = this.username + ": " + message;
					u.addOfflineMessage(offlineMessage);
				}
			} else {
				outToClient.println("Error. Invalid user");
			}
		} else {
			if (isBlockedBy(user)) {
				outToClient.println("Your message could not be delivered as the recipient has blocked you");
			} else if (isBlocked(user)) {
				outToClient.println("You have blocked this user. To send the user a message, unblock the user");
			} else {
				threads[i].outToClient.println(this.username + ": " + message);
			}
		}
	}

	// send message to all users
	private void broadcastMessage(String message) {
		boolean messageBlocked = false;
		for (int i = 0; i < maxClientsCount; i++) {
			if (threads[i] != null) {
				if (!(threads[i].username.equals(this.username))) {
					if (isBlockedBy(threads[i].username)) {
						messageBlocked = true;
					} else if (isBlocked(threads[i].username)) {
						// don't send message
					} else {
						threads[i].outToClient.println(username + ": " + message);
					}
				}
			}
		}
		if (messageBlocked == true) {
			outToClient.println("Your message could not be delivered to some recipients");
		}
	}

	private void blockUser(String user) {
		if (!validUsername(user)) {
			outToClient.println("Error. " + user + " is not a user");
			return;
		}
		if (user.equals(this.username)) {
			outToClient.println("Error. Cannot block self");
			return;
		}
		User currUser = findUser(this.username);
		currUser.blockUser(user);
		outToClient.println(user + " is blocked");
	}

	private void unblockUser(String user) {
		User currUser = findUser(this.username);
		if (currUser.unblockUser(user)) {
			outToClient.println("unblocked " + user);
		} else {
			outToClient.println("Error. " + user + " was not blocked");
		}
	}

	private void showOfflineMessages() {
		User u = findUser(this.username);
		ArrayList<String> offlineMessages = u.getOfflineMessages();
		if (!offlineMessages.isEmpty()) {
			for (int i = 0; i != offlineMessages.size(); i++) {
				outToClient.println(offlineMessages.get(i));
			}
			u.clearOfflineMessages();
		}
	}

	// start testing
	private void showBlockedUsers() {
		outToClient.println("Blocked users: ");
		User u = findUser(this.username);
		ArrayList<String> blocked = u.getBlockedUsers();
		for (int i = 0; i != blocked.size(); i++) {
			outToClient.println(blocked.get(i));
		}
	}
	// end testing

	// Checks if user has blocked you
	private boolean isBlockedBy(String user) {
		User u = findUser(user);
		ArrayList<String> blocked = u.getBlockedUsers();
		for (int i = 0; i != blocked.size(); i++) {
			if (blocked.get(i).equals(this.username)) {
				return true;
			}
		}
		return false;
	}

	// Check if user is blocked by current client
	private boolean isBlocked(String user) {
		User u = findUser(this.username);
		ArrayList<String> blocked = u.getBlockedUsers();
		for (int i = 0; i != blocked.size(); i++) {
			if (blocked.get(i).equals(user)) {
				return true;
			}
		}
		return false; 
	}

	private User findUser(String un) {
		for (int i = 0; i != users.size(); i++) {
			User u = users.get(i);
			if (u.getUsername().equals(un)) {
				return u;
			}
		}
		return null;
	}
}