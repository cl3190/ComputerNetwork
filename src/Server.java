import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.*;
import java.util.Map.Entry;

public class Server {
	private int port;
	private ServerSocket serverSocket;
	/*
	 * the time in seconds to block the user who has 3 consecutive fail log in
	 * attempts
	 */
	private final int BLOCK_TIME = 60;

	/* this HashMap stores the username,password pair */
	private HashMap<String, String> users;

	public Server(int port) throws IOException, FileNotFoundException,
			RuntimeException {
		this.port = port;
		/* this constructor initialize the server */
		serverSocket = new ServerSocket(port);

		/* fill in the users HashMap with the user_pass.txt file */
		users = new HashMap<String, String>();
		File file = new File("user_pass.txt");
		InputStreamReader read = new InputStreamReader(
				new FileInputStream(file));
		BufferedReader bufferedReader = new BufferedReader(read);
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			String[] pair = line.split("\\s+");
			users.put(pair[0], pair[1]);
		}
		bufferedReader.close();
		read.close();

		/* debugging code start */
		Iterator<Entry<String, String>> it = users.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> pair = (Map.Entry<String, String>) it
					.next();
			System.out.println(pair.getKey() + ":" + pair.getValue());
		}
		/* debugging code ends */

		System.out.println("Server started!");
	}

	public void service() {
		while (true) {
			Socket socket = null;
			try {
				/*
				 * select one client and let one working thread in the
				 * threadPool to runs it
				 */
				socket = serverSocket.accept();
				/* create a thread from every client */
				Thread t = new Thread(new ClientHander(socket));
				t.start();

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	/* This private class is the handler of Client requests */
	private class ClientHander implements Runnable {

		private Socket socket = null;

		public ClientHander(Socket socket) throws SocketException {
			this.socket = socket;
			/* no matter how small the package is, sent it immediately */
			this.socket.setTcpNoDelay(true);
		}

		@Override
		public void run() {
			/* log in parse */

			/* authenticated, now in command parse */
			try {
				while (true) {
					InputStreamReader isr = new InputStreamReader(
							socket.getInputStream());
					BufferedReader br = new BufferedReader(isr);
					String command = "";
					OutputStreamWriter osw = new OutputStreamWriter(
							socket.getOutputStream());
					while (!(command = br.readLine()).equals("")) {
						osw.write( command +'\n');
					}
					
					osw.write("\n");
					osw.flush();


				}
			} catch (IOException ex) {
				System.out.println("Client has exited.Connection closed");
			} catch (Exception ex) {

			}

		}

	}

	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Please enter the port number!");
			return;
		}
		int port = Integer.parseInt(args[0]);

		try {
			new Server(port).service();
		} catch (FileNotFoundException ex) {
			System.out.println("The user_pass.txt does not exists!");
		} catch (IOException ex) {
			System.out
					.println("This port might have been used, or there is a thread pool initialization error");
		} catch (RuntimeException ex) {
			System.out.print("The format of user_pass.txt may not be correct.");
		}
	}

}
