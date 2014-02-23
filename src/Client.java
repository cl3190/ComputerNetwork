import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
	private Socket socket;

	public Client(String host, int port) throws IOException {
		socket = new Socket(host, port);
		/* no matter how small the message is, send it immediately */
		socket.setTcpNoDelay(true);
	}

	private void service() throws IOException, RuntimeException {
		/**
		 * the format of the request is: command\n data..data..data..\n \n
		 * 
		 * there cannot be a \n\n in data, it is not allowed
		 * 
		 * where \n is the Enter,the last \n represents an empty line, when the
		 * server detects the empty lines, this request package ends
		 */
		int haha = 0;
		while (true) {
			haha++;

			/*********************************************************************/
			/* this upper part is the senting data part */

			// sent data to server
			OutputStream ops = socket.getOutputStream();
			OutputStreamWriter opsw = new OutputStreamWriter(ops);
			BufferedWriter bw = new BufferedWriter(opsw);
			/* get the command from console */
			BufferedReader input = null;
			StringBuffer sb = new StringBuffer();
			try {
				input = new BufferedReader(new InputStreamReader(System.in));
				String line = null;
				String data = null;
				String command = null;
				// skip the empty command, if the user just type Enter for
				// multiple times
				while ((line = input.readLine()).equals(""))
					;
				/*
				 * skip the starting empty space, the ending empty space is not
				 * displayed anyway, so there is no harm trimming it
				 */
				line = line.trim();
				/* split the input by empty space, getting the command */
				String[] words = line.split("\\s+");
				command = words[0];
				if (words.length > 1) {
					// if there is data, find the position of the first empty
					// space,
					// and get getting the data by substring
					int firstSpace = line.indexOf(' ') + 1;
					data = line.substring(firstSpace);
				} else {
					// if there is only a command but no data,
					// we add a empty space to be data, to make the format of
					// every request the same
					data = " ";
				}
				if (true)
					sb.append(command + " ME\n" + data + "\n");
				else
					sb.append(command + " OTHER\n" + data + "\n");
				// System.out.println("["+command + " ME\n" + data + "\n\n]");
			} catch (IOException ex) {
				// ex.printStackTrace();
				System.out
						.println("Error occurs while reading the input from console");
				break;
			}
			/* making the empty line to symbolize the end of the request package */
			sb.append("\n");
			bw.write(sb.toString());
			bw.flush();

			/*************************************************************************/
			/* below is the receiving data part */

			// receiving data from server
			InputStream ips = socket.getInputStream();
			InputStreamReader ipsr = new InputStreamReader(ips);
			BufferedReader br = new BufferedReader(ipsr);

			/**
			 * the respond format is: <code> <type>\n
			 * data... data... data...\n
			 * \n
			 * 
			 * 
			 * one important note is that data cannot contains \n\n, it is not allowed
			 * 
			 * code represents what kind of operation this respond is, refers to Code.java
			 * <type> is either "me" or "other"
			 * if it is other, then thhis respond is sent from other, it is a message
			 * if it is me, then this is the respond from the reqest above
			 * 
			 * what we are doing here is that, every request will have a respond,
			 * even senting message to other will have a respond,
			 * if the content in the InputStream buffer don't have the respond type "me",
			 * then our request has not been reply yet, so we block until the reply comes,
			 * 
			 * otherwise, the respond has come, we can now output, but be careful here,
			 * we will reorder the output to make the respond always come first,
			 * because user would want to see the respond from the previous request immediately
			 */
			StringBuffer content = new StringBuffer();
			/*
			 * denoting whether the input stream buffer has respond from the
			 * request above
			 */
			boolean hasMeRespond = false;
			LinkedList<String> output = new LinkedList<String>();
			if (br.ready()) {
				while (br.ready()) {
					String line = null;
					while (!(line = br.readLine()).equals("")) {
						content.append(line + '\n');
					}
					content.append('\n');
				}
				String[] respondPackets = content.toString().split("\n\n");

				for (String respond : respondPackets) {
					/* find the ending \n of the head, the first \n */
					//System.out.println("[" + respond + "]");
					int headEnd = respond.indexOf('\n');
					String head = respond.substring(0, headEnd);
					String data = respond.substring(headEnd + 1);
					/*
					 * by splitting head by empty space, we get the code and
					 * Type
					 */
					String[] headParts = head.split("\\s+");
					hasMeRespond = analyzeHead(headParts, data, output);
				}
			}

			/**
			 * if there is no type me respond, then the request above has no
			 * respond yet we block here to wait for the respond for that
			 */
			while (!hasMeRespond) {

				/* block and read the first line, which is the head */
				String head = null;
				head = br.readLine();

				String[] headParts = head.split("\\s+");

				/*
				 * now the head has been read, we keep finding the two
				 * consecutive\n
				 */
				/* the appearance of \n\n symbolize the end of this respond */
				/*
				 * we use pre and cur together to find \n\n, which indicates the
				 * end of the respond
				 */
				/*
				 * the first character of the data part cannot be \n, so we are
				 * safe here
				 */
				char pre = '\n';
				char cur;
				StringBuffer dataSb = new StringBuffer();
				while (!((cur = (char) br.read()) == '\n' && pre == '\n')) {
					dataSb.append(cur);
					pre = cur;
				}
				/* now data is all the data with a \n at the end */
				String data = dataSb.toString();

				/* analyze the head */
				hasMeRespond = analyzeHead(headParts, data, output);
			}

			/* At the end, we got our type me respond, we output everything */
			for (String cur : output) {
				System.out.println(cur);
			}

		}
	}

	/**
	 * this method will analyze the head, it will see whether the type is ME,
	 * then it is the respond from the request in this round, and so add to the
	 * front to the output list, otherwise, we simply add the output to the rear
	 * of the output list,
	 * 
	 * also, according to the different code returned, we will do different
	 * operation
	 * 
	 * the return type is whether this head is a type Me respond
	 * 
	 * @return
	 */
	private boolean analyzeHead(String[] headParts, String data,
			LinkedList<String> output) {
		int code = Integer.parseInt(headParts[0]);
		String type = headParts[1];
		/*
		 * using isMe, we can reorder the output, if it is type Me, we add to
		 * the front
		 */
		/* otherwise, we add to the tail */
		boolean isMe = false;
		if (type.equals(Type.ME)) {

			isMe = true;
		}
		switch (code) {
		default:
			if (isMe) {

				output.addFirst(data + "\n");
			} else {
				output.addLast(data + "\n");
			}
		}
		return isMe;
	}

	public static void main(String[] args) {
		if (args.length != 2) {
			System.out
					.println("Please enter the ip address and port number correctly!");
			return;
		}

		Client client = null;

		try {
			client = new Client(args[0], Integer.parseInt(args[1]));
			client.service();
		} catch (UnknownHostException ex) {
			System.out.println("The host name provided might not exists.");
		} catch (IOException ex) {
			ex.printStackTrace();
			System.out.println("Cannot connect to the server.");
		} catch (RuntimeException ex) {
			/*
			 * this will happens when server is closed, then when receiving
			 * data, inputStream is empty, and so the runtimeException triggers
			 */
			ex.printStackTrace();
			System.out.println("Cannot connect to the server.");
		} finally {
			try {
				if (client != null && client.socket != null)
					client.socket.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
}