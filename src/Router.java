import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * Distance vector routing algorithm using UDP.
 * 
 * @author Harsh Patil
 *
 */
public class Router implements Runnable {
	private String id;
	private int routerPort;
	private String[][] neighbors;
	private DatagramSocket socket = null;
	private ApplicationWindow myGUI;
	private DatagramPacket requestPacket;
	ArrayList<String> neighborsList = new ArrayList<String>();
	Hashtable<String, RoutingInfo> myRoutingTable = new Hashtable<String, RoutingInfo>();

	public Router(String id, int routerPort, String[][] nodesCost) throws SocketException {
		this.id = id;
		this.routerPort = routerPort;
		this.neighbors = nodesCost;
		socket = new DatagramSocket(routerPort);
	}

	@Override
	public void run() {
		myGUI = new ApplicationWindow("  Output window for Router #" + id + "  ");
		myGUI.println("Router name: " + id);
		myGUI.println("Router port: " + routerPort);
		for (int i = 0; i < neighbors.length; i++) {
			myGUI.println("Router neighbor: " + neighbors[i][0] + " Cost: " + neighbors[i][1]);
		}
		
		myGUI.println("Initial routing table");
		
		// add neighbors to the list
		for (int i = 0; i < neighbors.length; i++) {
			neighborsList.add(neighbors[i][0] + "/" + neighbors[i][1]);
		}

		for (String j : neighborsList) {
			RoutingInfo destInfo = new RoutingInfo(j.split("/")[0], j.split("/")[0], Integer.parseInt(j.split("/")[1]));
			myRoutingTable.put(j.split("/")[0], destInfo);
		}

		// start listening to incoming packet
		listen(socket);
		for (int i = 0; i < neighbors.length; i++) {
			sendPacket(socket, Integer.parseInt(neighbors[i][0]));
		}
		// listenToCostUpdate();
		new Thread(new inputListener()).start();
	}

	/**
	 * Listens to key events from the UI's text field
	 */
	private class inputListener implements Runnable {
		public void run() {
			String text;
			while (true) {
				try {
					text = myGUI.getInput();
					text = text.replaceAll("\n", "");
					if (text.equals("") || text == null) {

					} else {
						StringTokenizer st = new StringTokenizer(text, ":");
						String destination = st.nextToken();
						String cost = st.nextToken();

						// insert the new cost
						RoutingInfo newINfo = new RoutingInfo(destination, destination, Integer.parseInt(cost));
						myRoutingTable.put(destination, newINfo);
						Thread.sleep(2000);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Method which runs a thread to send a packet to the neighbor port
	 * 
	 * @param socket
	 * @param neighborPort
	 */
	public void sendPacket(DatagramSocket socket, int neighborPort) {
		Thread sendingThread = new Thread("New sendThread") {
			public void run() {
				try {
					Hashtable<String, RoutingInfo> newTable = generateTable(String.valueOf(neighborPort));
					Packet msg = new Packet(newTable, String.valueOf(routerPort),id);

					// host address
					InetAddress aHost = InetAddress.getByName("localhost");

					// serialize the sending packet
					byte[] sendBuf = serializePacket(msg);
					DatagramPacket replyPacket = new DatagramPacket(sendBuf, sendBuf.length, aHost, neighborPort);
					//send update every one second
					Thread.sleep(1000);
					socket.send(replyPacket);

				} catch (Exception e) {
					System.out.println("Error: " + e.getMessage());
				}
			}
		};
		sendingThread.start();
	}

	/**
	 * Method which runs a thread to listen on an input connection. Calls the
	 * printTable() and updateLinkCost() methods through it
	 * 
	 * @param sock
	 */
	public void listen(DatagramSocket sock) {
		Thread thread = new Thread("New listenThread") {
			public void run() {
				byte[] recvBuf = new byte[9000];
				try {
					while (true) {
						requestPacket = new DatagramPacket(recvBuf, recvBuf.length);
						myGUI.println("Own table ");
						printTable();
						sock.receive(requestPacket);

						Packet receivedpacket = (Packet) deserializePacket(requestPacket.getData(),
								requestPacket.getOffset(), requestPacket.getLength());
						//sleep for readability
						Thread.sleep(1000);
						myGUI.println("After update received from: " + receivedpacket.source);						
						updateLinkCost(receivedpacket);
						printTable();
					}

				} catch (Exception e) {
					System.out.println("Error: " + e.getMessage());
				}
			}
		};
		thread.start();
	}

	/**
	 * Serialize the packet to be sent over UDP connection
	 * 
	 * @param packet
	 *            Packet object to send
	 * @return
	 * @throws IOException
	 */
	public byte[] serializePacket(Packet packet) throws IOException {
		ByteArrayOutputStream bStream = new ByteArrayOutputStream();
		ObjectOutputStream oStream = new ObjectOutputStream(bStream);
		oStream.flush();
		oStream.writeObject(packet);
		oStream.flush();
		byte[] serializedByte = bStream.toByteArray();
		return serializedByte;
	}

	/**
	 * DeSerialize the incoming byte on receiving it via UDP to a Packet object
	 * 
	 * @param bytes
	 *            Incoming bytes
	 * @param offset
	 *            Offset of data inside the byte
	 * @param length
	 *            Length of data
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private Object deserializePacket(byte[] bytes, int offset, int length) throws IOException, ClassNotFoundException {
		ByteArrayInputStream bStream = new ByteArrayInputStream(bytes, offset, length);
		ObjectInputStream oStream = new ObjectInputStream(new BufferedInputStream(bStream));
		Object packet = new Object();
		packet = oStream.readObject();
		return packet;
	}

	/**
	 * Prints the routingTable for the router
	 * 
	 */
	public void printTable() {
		String heading = String.format("%20s%20s%25s", "ADDRESS", "NEXT HOP", "COST");
		myGUI.println(heading);
		myGUI.println("===========================================");
		for (Entry<String, RoutingInfo> tableEntry : myRoutingTable.entrySet()) {
			String line = String.format("%20s%20s%20s", getIPfromPort(tableEntry.getValue().destPort),
					getIPfromPort(tableEntry.getValue().nextPort), tableEntry.getValue().hopCount);
			myGUI.println(line);
		}
		myGUI.println("===========================================");
		myGUI.println();
	}

	/**
	 * Generates routing table from the current table
	 * 
	 * @param port
	 * @return
	 */
	public Hashtable<String, RoutingInfo> generateTable(String port) {
		Hashtable<String, RoutingInfo> tempTable = new Hashtable<>();
		for (Entry<String, RoutingInfo> tableEntry : myRoutingTable.entrySet())
			if (!tableEntry.getValue().destPort.equals(port) && !tableEntry.getValue().nextPort.equals(port))
				tempTable.put(tableEntry.getKey(), tableEntry.getValue());
		return tempTable;
	}

	/**
	 * Update the link cost in the routing table each time the method is called. 
	 * Send the packet generating a new routing table from the newly generated
	 * link cost
	 * 
	 * @param incommingPacket
	 */
	public void updateLinkCost(Packet incommingPacket) {
		for (Entry<String, RoutingInfo> tableEntry : incommingPacket.routingTable.entrySet()) {
			if (tableEntry.getKey().toString().equals(routerPort))
				continue;
			else if (myRoutingTable.get(tableEntry.getKey()) == null) {
				RoutingInfo newDest = new RoutingInfo(tableEntry.getValue().destPort, incommingPacket.source,
						tableEntry.getValue().hopCount + getCost(incommingPacket.source));
				myRoutingTable.put(tableEntry.getValue().destPort, newDest);
			} else if ((myRoutingTable.get(tableEntry.getKey()).hopCount) > (tableEntry.getValue().hopCount)
					+ getCost(incommingPacket.source)) {
				myRoutingTable.get(tableEntry.getKey()).hopCount = tableEntry.getValue().hopCount
						+ getCost(incommingPacket.source);
				myRoutingTable.get(tableEntry.getKey()).nextPort = incommingPacket.source;
			}
		}
		for (int i = 0; i < neighbors.length; i++) {
			sendPacket(socket, Integer.parseInt(neighbors[i][0]));
		}
	}

	
	/**
	 * get the cost of source
	 * 
	 * @param source
	 * @return
	 */
	private int getCost(String source) {
		for (int i = 0; i < neighborsList.size(); i++) {
			if (neighborsList.get(i).split("/")[0].equals(source)) {
				return Integer.parseInt(neighborsList.get(i).split("/")[1]);
			}
		}
		return 0;
	}

	/**
	 * Converts the given formated Ip address to the actual port number
	 * 
	 * @param iP
	 * @return
	 */
	private static int getPortFromIP(String iP) {
		StringTokenizer st = new StringTokenizer(iP, ".");
		int firstOctet = Integer.parseInt(st.nextToken());
		int secondOctet = Integer.parseInt(st.nextToken());
		int thirdOctet = Integer.parseInt(st.nextToken());
		int fourthOctet = Integer.parseInt(st.nextToken());

		String binaryString = String.format("%08d", Integer.parseInt(Integer.toString(firstOctet, 2))) + ""
				+ String.format("%08d", Integer.parseInt(Integer.toString(secondOctet, 2))) + ""
				+ String.format("%08d", Integer.parseInt(Integer.toString(thirdOctet, 2))) + ""
				+ String.format("%08d", Integer.parseInt(Integer.toString(fourthOctet, 2)));
		// System.out.println(binaryString);
		int port = Integer.parseInt(binaryString, 2);
		return port;
	}

	/**
	 * Converts the given actual port number into the formated IP address
	 * 
	 * @param destPort
	 * @return
	 */
	private String getIPfromPort(String destPort) {
		int binaryPort = Integer.parseInt(destPort);

		String b = Integer.toString(binaryPort, 2);
		String padded = "00000000000000000000000000000000".substring(b.length()) + b;

		String[] stringArray = padded.split("(?<=\\G.{8})");
		int firstOctet = Integer.parseInt(stringArray[0], 2);
		int secondOctet = Integer.parseInt(stringArray[1], 2);
		int thirdOctet = Integer.parseInt(stringArray[2], 2);
		int fourthOctet = Integer.parseInt(stringArray[3], 2);

		String binaryString = String.format("%03d", firstOctet) + "." + String.format("%03d", secondOctet) + "."
				+ String.format("%03d", thirdOctet) + "." + String.format("%03d", fourthOctet);

		return binaryString;
	}
	
	/**
	 * @param args
	 * @throws SocketException
	 * @throws FileNotFoundException
	 */
	public static void main(String args[]) throws SocketException, FileNotFoundException {
		String[] config = new String[4];
		int count = 0;
		config[0] = "configA.txt";
		config[1] = "configB.txt";
		config[2] = "configC.txt";
		config[3] = "configD.txt";
		
		// read from the text file the configurations and start a router 
		// for each file 
		for (String s : config) {
			Scanner sc1 = new Scanner(new FileReader(s));
			String idandPort = sc1.nextLine();
			StringTokenizer st = new StringTokenizer(idandPort, ":");
			String id = st.nextToken();
			String IP = st.nextToken();
			sc1.close();

			Scanner countScanner = new Scanner(new FileReader(s));
			int RouterPort = getPortFromIP(IP);

			while (countScanner.hasNextLine()) {
				countScanner.nextLine();
				count++;
			}
			countScanner.close();

			String[][] nodesCost = new String[count - 1][2];

			Scanner sc2 = new Scanner(new FileReader(s));
			sc2.nextLine();
			for (int i = 0; i < (count - 1); i++) {
				String portandCost = sc2.nextLine();
				StringTokenizer st2 = new StringTokenizer(portandCost, ":");
				st2.nextToken();
				nodesCost[i][0] = String.valueOf(getPortFromIP(st2.nextToken().toString()));
				nodesCost[i][1] = st2.nextToken();
			}
			sc2.close();
			count = 0;
			
			// start a router thread
			new Thread(new Router(id, RouterPort, nodesCost)).start();
		}
	}
}
