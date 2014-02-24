import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.*;
import java.util.Map.Entry;

public class Server {
	private int port;
	private ServerSocket serverSocket;
	/*
	 * BLOCK_TIME (in seconds), is the time in seconds to block the user who has
	 * 3 consecutive fail log in attempts
	 */
	private static final long BLOCK_TIME = 60 * 1000;

	private static final long LAST_HOUR = 1 * 60 * 60 * 1000;
	//private static final long LAST_HOUR = 10 * 1000;

	/* this HashMap stores the username,password pair */
	private HashMap<String, String> users;
	/* current logged in user */
	private HashMap<String, ClientHandler> currentUsers;
	/* this map denote the time when user's last session ends */
	/* if the user is currently logged on, the same user will be dropped from this table*/
	/* when the moment which the last session ends is within LAST_HOUR time*/
	/* we add it into the wholasthr command*/
	private HashMap<String, Long> userLastSessionEnds;
	
	/**
	 * this table documents who blocks whom
	 */
	private HashMap<String, HashSet<String>> blockList;

	/*
	 * this HashMap stores the blocked user, the ip, and the last blocked
	 * starting time
	 */
	/**
	 * the format of the first field is: <username>:<ip address> something like
	 * "192.168.1.1"
	 */
	private HashMap<String, Long> blockedUsers;

	/* this constructor initialize the server */
	public Server(int port) throws IOException, FileNotFoundException,
			RuntimeException {
		this.port = port;
		blockedUsers = new HashMap<String, Long>();
		serverSocket = new ServerSocket(port);
		currentUsers = new HashMap<String, ClientHandler>();
		userLastSessionEnds = new HashMap<String,Long>();
		blockList = new HashMap<String,HashSet<String>>();

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
				Thread t = new Thread(new ClientHandler(socket));
				t.start();

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	/* This private class is the handler of Client requests */
	private class ClientHandler implements Runnable {

		private Socket socket = null;
		private String username = null;

		public ClientHandler(Socket socket) throws SocketException {
			this.socket = socket;
			/* no matter how small the package is, sent it immediately */
			this.socket.setTcpNoDelay(true);
		}

		@Override
		public void run() {
			/* log in parse */
			/**
			 * in the login parse, the command must be valid, otherwise we are
			 * receiving a network attack
			 * 
			 */
			try {
				/**
				 * the format of the login request is: login\n username\n
				 * password\n \n
				 */
				/*
				 * the first time receiving request, check whether this ip is
				 * blocked
				 */
				InputStreamReader isr = new InputStreamReader(
						socket.getInputStream());
				BufferedReader br = new BufferedReader(isr);
				OutputStreamWriter osw = new OutputStreamWriter(
						socket.getOutputStream());
				String head = br.readLine();
				String username = br.readLine();
				String password = br.readLine();
				br.readLine(); // skipping the last line
				String unameIpPair = username + ":"
						+ socket.getInetAddress().getHostAddress();
				if (Command.getCommand(head) != Command.LOGIN) {
					/*
					 * the command is not login command, we return the unknown
					 * command reply
					 */
					osw.write(Code.UNKNOWN_COMMAND + "\n \n\n");
					osw.flush();
				}
				/**
				 * see if this Ip is blocked previously and still in blocking
				 * time
				 */
				if (blockedUsers.containsKey(unameIpPair)) {
					long now = System.currentTimeMillis();
					long remainMilliSec = now - blockedUsers.get(unameIpPair);
					if (remainMilliSec < BLOCK_TIME) {
						/* this user is still blocked, send the LOGIN_DENY reply */
						int remainSec = (int) (BLOCK_TIME - remainMilliSec) / 1000;
						osw.write(Code.LOG_DENY + "\n" + remainSec + "\n\n");
						osw.flush();
						/* close the socket */
						socket.close();
						return;
					}
				}

				/**
				 * if the control flow gets here, we know that the user is not
				 * blocked from access, we will examine the username and pass
				 * for 3 times, if all three time fail, we send the LOGIN_DENY
				 * reply
				 * 
				 * the first 2 times is the same, but the last time is
				 * different, so the for loop below loops for 2 times, the last
				 * time is dealt with specifically
				 */

				/**
				 * we use this table to document the first user exceeds the 3
				 * failure times String is username, Integer the number of
				 * failure attempts associated with this username
				 */
				HashMap<String, Integer> attemptsTable = new HashMap<String, Integer>();

				while (true) {
					/* we get the username, pass from above or the previous turn */
					/* now checking whether it is correct */
					if (users.containsKey(username)
							&& users.get(username).equals(password)) {
						if (currentUsers.containsKey(username)) {
							/* this username has been logged in from elsewhere */
							osw.write(Code.LOG_ALREADY_LOGGED + "\n \n\n");
							osw.flush();
						} else {
							/*
							 * the username is correct, add to currentUsers
							 * table
							 */
							currentUsers.put(username, this);
							/*drop from the last log in session table, because we are login now*/
							userLastSessionEnds.remove(username);
							/* send the LOGIN_SUCCESS reply */
							osw.write(Code.LOG_SUCCESS + "\n"+username+"\n\n");
							osw.flush();
							
							/*storing the username*/
							this.username = username;

							break;
						}
					} else if (!users.containsKey(username)) {
						/* told the user this username does not exists */
						osw.write(Code.LOG_UNAME_NOT_EXIST + "\n \n\n");
						osw.flush();
					} else {
						if (attemptsTable.containsKey(username)) {
							int failTime = attemptsTable.get(username);
							if (failTime >= 3) {
								/*
								 * this username has at least 3 fail attempts
								 * before
								 */
								/*
								 * send the LOG_DENY reply, and add to block
								 * table
								 */
								osw.write(Code.LOG_DENY + "\n"
										+ (int) (BLOCK_TIME / 1000) + "\n\n");
								osw.flush();
								/* add to block table */
								blockedUsers.put(username
										+ ":"
										+ socket.getInetAddress()
												.getHostAddress(),
										System.currentTimeMillis());
								/* close the socket */
								socket.close();
								return;
							}
							attemptsTable.put(username, failTime + 1);
						} else {
							attemptsTable.put(username, 1);
						}
						osw.write(Code.LOG_FAIL + "\n \n\n");
						osw.flush();
						/* add this attempt to the attempts table */

					}

					/* getting the input for the next user login attempt */
					head = br.readLine();
					username = br.readLine();
					password = br.readLine();
					br.readLine(); // skipping the last line
					if (Command.getCommand(head) != Command.LOGIN) {
						/*
						 * the command is not login command, we return the
						 * unknown command reply
						 */
						osw.write(Code.UNKNOWN_COMMAND + "\n \n\n");
						osw.flush();
					}
				}

			} catch (Exception ex) {
				ex.printStackTrace();
				return;
			}

			/* authenticated, now in command parse */
			try {
				/**
				 * the format of the respond is 
				 * Code <type>\n 
				 * data... data...data...\n 
				 * \n
				 * 
				 * type is either ME or OTHER, if it is ME, then the respond is
				 * the respond from the request in this round, if it is OTHER,
				 * then the respond is sent by other people, for example,
				 * broadcast
				 * 
				 * the client use the type to see whether it has received the
				 * respond from his request in this round, if the ME respond has
				 * not been received, the client will wait until it gets to
				 * clien, please refers to the Client class,
				 * 
				 * here, in the run method, this while loop only respond to the
				 * request made by the user, so the type is always ME
				 */
				String type = "ME";
				while (true) {
					InputStreamReader isr = new InputStreamReader(
							socket.getInputStream());
					BufferedReader br = new BufferedReader(isr);

					/**
					 * locking the output stream! because if not, the broadcast
					 * message, or private message might insert into the the
					 * middle of our reply frame!!
					 */
					synchronized (this) {
						OutputStreamWriter osw = new OutputStreamWriter(
								socket.getOutputStream());

						/**
						 * the format of the request is: command\n data ...
						 * data... data... \n \n
						 */

						String command = null;
						/* skips the empty lines */
						while ((command = br.readLine()).equals(""))
							;
						System.out.println("["+command+"]");

						StringBuffer dataSb = new StringBuffer();
						char pre = '\n';
						char cur;
						/*
						 * read out the data until the end of this packet has
						 * reached, i.e. the \n\n
						 */
						while (!((cur = (char) br.read()) == '\n' && pre == '\n')) {
							dataSb.append(cur);
							pre = cur;
						}
						dataSb.deleteCharAt(dataSb.length() - 1);
						String data = dataSb.toString();
						System.out.println(data);

						/*
						 * see what command did the user enter, and takes
						 * different options
						 */
						switch (Command.getCommand(command)) {
						case WHOELSE:
							StringBuffer userListSb = new StringBuffer();
							Iterator<Entry<String, ClientHandler>> it = currentUsers.entrySet().iterator();
							int userCount=0;
							while (it.hasNext()) {
								Map.Entry<String, ClientHandler> pair = (Map.Entry<String, ClientHandler>) it
										.next();
								String u = pair.getKey();
								if(!u.equals(this.username))
									userListSb.append(u+"\n");
								userCount++;
							}
							if(userCount==1)
								userListSb.append("No other user are currently logged in.\n");
							
							osw.write(Code.WHO_ELSE_RESPOND+" "+type+"\n"+userListSb.toString()+"\n");
							osw.flush();
							break;
						case WHOLASTHR:
							StringBuffer lastHrListSb = new StringBuffer();
							int lastCount=0;
							/*get the full list of user who are currently login*/
							Iterator<Entry<String, ClientHandler>> curIter = currentUsers.entrySet().iterator();
							while (curIter.hasNext()) {
								Map.Entry<String, ClientHandler> pair = (Map.Entry<String, ClientHandler>) curIter
										.next();
								String u = pair.getKey();
								if(!u.equals(this.username))
									lastHrListSb.append(u+"\n");
								lastCount++;
							}
							/*add the user who the last time he/she logout is within 1 hour*/
							Iterator<Entry<String, Long>> lastHrIter = userLastSessionEnds.entrySet().iterator();
							while (lastHrIter.hasNext()) {
								Map.Entry<String, Long> pair = (Map.Entry<String, Long>) lastHrIter
										.next();
								String u = pair.getKey();
								Long lastLogout = pair.getValue();
								/*because we ensure that the current login user is not in this list, we 
								 * don't need to check whether the username is the current user*/
								if((System.currentTimeMillis()-lastLogout)<LAST_HOUR){
									lastHrListSb.append(u+"\n");
									lastCount++;
								}
								
							}
							if(lastCount==1)
								lastHrListSb.append("No one else were logged in during the last "+(int)(LAST_HOUR/1000)+" seconds.\n");
							
							osw.write(Code.WHO_LAST_HOUR+" "+type+"\n"+lastHrListSb.toString()+"\n");
							osw.flush();
							break;
						case BLOCK:
							if(!users.containsKey(data)){
								/*cannot find the user the client want to block*/
								osw.write(Code.BLOCK_NO_SUCH_USER+" "+type+"\nThe user you are blocking does not exists.\n\n");
								osw.flush();
							}else if(this.username.equals(data)){
								/*blocking the client's own username*/
								osw.write(Code.BLOCK_YOUSELF+" "+type+"\nError! You cannot block yourself!\n\n");
								osw.flush();
							}else{
								/*now, add the username being block to the block list*/
								if(blockList.containsKey(this.username)){
									HashSet<String> blockedUsers = blockList.get(this.username);
									blockedUsers.add(data);
								}else{
									HashSet<String> blockedUsers = new HashSet<String>();
									blockedUsers.add(data);
									blockList.put(this.username, blockedUsers);
								}
								osw.write(Code.BLOCK_SUCCSS+" "+type+"\nYou have successfully blocked "+data+" from sending you messages.\n\n");
								osw.flush();
							}
							break;
						case UNBLOCK:
							if(!users.containsKey(data)){
								/*cannot find the user the client want to block*/
								osw.write(Code.UNBLOCK_NO_SUCH_USER+" "+type+"\nThe user you are unblocking does not exists.\n\n");
								osw.flush();
							}else if(this.username.equals(data)){
								/*blocking the client's own username*/
								osw.write(Code.UNBLOCK_YOUSELF+" "+type+"\nError! You cannot unblock yourself!\n\n");
								osw.flush();
							}else{
								/*now, drop the username being unblock from the block list*/
								HashSet<String> blockedUsers = blockList.get(this.username);
								blockedUsers.remove(data);
								osw.write(Code.UNBLOCK_SUCCSS+" "+type+"\nYou have successfully unblocked "+data+" from sending you messages.\n\n");
								osw.flush();
							}
							break;
						case LOGOUT:
							/*clearing the current user table*/
							currentUsers.remove(this.username);
							userLastSessionEnds.put(this.username, System.currentTimeMillis());
							osw.write(Code.LOG_OUT_REPLY+" "+type+"\nBye "+this.username+", you have now logged out.\n\n");
							osw.flush();
							socket.close();
							return;
						default:
							/* the defualt is we don't understand the command */
							/* we just return the Code.UNKNOWN_COMMAND */
							osw.write(Code.UNKNOWN_COMMAND + " "+type+"\nThe server cannot understands this command.\n\n");
							osw.flush();
						}

					}

				}
			}  catch (Exception ex) {
				ex.printStackTrace();
				currentUsers.remove(this.username);
				userLastSessionEnds.put(this.username, System.currentTimeMillis());
				System.out.println("Client has exited.Connection closed.");
				try{
					socket.close();
				}catch(Exception e){
					ex.printStackTrace();
				}
			}

		}

		/**
		 * this method provide the way to sent message to this client must use
		 * synchronized to protect from concurrently accessing the output stream
		 * 
		 * type is either "private" or "broadcast"
		 */
		public synchronized void sendMessage(String message, String type) {

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
