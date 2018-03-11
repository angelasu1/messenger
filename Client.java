import java.io.*;
import java.net.*;

public class Client implements Runnable {
	private static Socket clientSocket;
	private static PrintStream outToServer;
	private static BufferedReader inFromServer;
	private static BufferedReader inFromUser;

	public static void main(String[] args) throws UnknownHostException, IOException {

		// Check correct amount of arguments
		if (args.length != 2) {
			System.out.println("Usage: <IP Address / Hostname> <port>");
			System.exit(0);
		}

		// Get srever IP
		String serverName = args[0];
		InetAddress serverAddress = InetAddress.getByName(serverName);

		// Get server port
		int serverPort = Integer.parseInt(args[1]);

		clientSocket = new Socket(serverName, serverPort);

		// Initialise BufferedReader and DataOutputStream
		inFromUser =
			new BufferedReader(new InputStreamReader(System.in));
		outToServer = new PrintStream(clientSocket.getOutputStream());
		inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

		try {
			new Thread(new Client()).start();
			outToServer.println("serverAddress: " + serverAddress);
			while (true) {
				String userInput = inFromUser.readLine().trim();
				outToServer.println(userInput);
				if (userInput.equals("logout")) {
					break;
				}
			}
			inFromServer.close();
			inFromUser.close();
			clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		String response;
		try {
			while ((response = inFromServer.readLine()) != null) {
				if (response.equals("Password: ") || response.equals("Username: ")) {
					System.out.print(response);
				} else {
					System.out.println(response);
					if (response.equals("Invalid Password. Your account has been blocked. Please try again later")
						|| response.equals("Your account is blocked due to multiple login failures. Please try again later")
						|| response.equals("Logged out by server due to inactivity...")
						|| response.equals("Invalid Username. Your IP has been blocked. Please try again later")
						|| response.equals("IP is currently blocked. Please try again later")
						|| response.equals("You are already logged in from another client.")) {
						System.exit(0);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}