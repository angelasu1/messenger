import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
	private static final int maxClientsCount = 10;
	private static final ClientThread[] threads = new ClientThread[maxClientsCount];
	private static CopyOnWriteArrayList<User> users = scanCredentials();
	private static CopyOnWriteArrayList<BlockedIP> blockedIPs = new CopyOnWriteArrayList<>();

	public static void main(String[] args) throws IOException {

		// Check correct amount of arguments
		if (args.length != 3) {
			System.out.println("Usage: <port> <blockDuration> <timeout>");
			System.exit(0);
		}

		// Get server port
		int serverPort = Integer.parseInt(args[0]);

		// Get block duration
		int blockDuration = Integer.parseInt(args[1]);

		// Get timeout
		int timeout = Integer.parseInt(args[2]);

		// Create server socket
		ServerSocket serverSocket = new ServerSocket(serverPort);
		System.out.println("Server up & ready for connections...");

		while (true) {
			try {
				Socket clientSocket = serverSocket.accept();
				int i = 0;
				for (i = 0; i < maxClientsCount; i++) {
					if (threads[i] == null) {
						(threads[i] = new ClientThread(clientSocket, threads, users, blockedIPs, blockDuration, timeout)).start();
						break;
					}
        		}
        	} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// Scan the credentials
	private static CopyOnWriteArrayList<User> scanCredentials() {
		CopyOnWriteArrayList<User> users = new CopyOnWriteArrayList<>();
		Scanner sc = null;
	    try {
	        sc = new Scanner(new File("credentials.txt"));
	    } catch (FileNotFoundException e) {
	        System.out.println("No credentials.txt file provided.");
		}
	    while (sc.hasNextLine()) {
	        Scanner s = new Scanner(sc.nextLine());
	        String username = s.next();
	        String password = s.next();
	        
	        User newUser = new User(username, password);
			users.add(newUser);
		}
		return users;
	}
}