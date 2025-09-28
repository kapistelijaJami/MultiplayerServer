package multiplayerserver;

import com.google.gson.JsonSyntaxException;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.UUID;
import multiplayerserver.packets.DataPacket;
import multiplayerserver.packets.Packet;
import multiplayerserver.packets.PacketRegistry;
import multiplayerserver.packets.SendUuid;

public class Client implements HasUUID {
	private final InetAddress serverIP;
	private final int serverPort;
	private Socket tcpSocket;
	private DatagramSocket udpSocket;
	private final UUID uuid;
	
	private final PacketRegistry packetRegistry;
	
	private boolean running = false;
	
	public Client(InetAddress serverIP, int serverPort, PacketRegistry registry) {
		this(serverIP, serverPort, UUID.randomUUID(), registry);
	}
	
	public Client(InetAddress serverIP, int serverPort, UUID uuid, PacketRegistry registry) { //TODO: Allow separate ports for TCP and UDP
		this.serverIP = serverIP;
		this.serverPort = serverPort;
		this.packetRegistry = registry;
		
		this.uuid = uuid;
	}
	
	public void connect() throws IOException { //TODO: Make it possible to choose the protocol
		try {
			running = true;
			
			tcpSocket = new Socket(serverIP, serverPort);
			tcpSocket.setKeepAlive(true);
			
			udpSocket = new DatagramSocket();
			udpSocket.connect(serverIP, serverPort);
			printMessage("Client connected!");
			
			new Thread(this::listenTCP).start();
			new Thread(this::listenUDP).start();
			
			sendPacket(new SendUuid(uuid, udpSocket.getLocalPort()), Protocol.TCP); //Sending UUID and udpPort to the server.
		} catch (BindException e) {
			printMessage("TCP port already in use");
			throw e;
		} catch (SocketException e) {
			closeQuietly(tcpSocket);
			printMessage("UDP Socket failed to bind");
			BindException be = new BindException(e.getMessage());
			be.initCause(e);
			throw be;
		}
	}
	
	private void listenTCP() {
		printMessage("Listening TCP!");
		
		try (InputStream in = tcpSocket.getInputStream();
				DataInputStream dataInput = new DataInputStream(in)) {
			
			while (running) {
				dataInput.readInt(); //Total length not needed in the client
				int jsonLength = dataInput.readInt();
				byte[] jsonBytes = new byte[jsonLength];
				dataInput.readFully(jsonBytes);
				
				String json = new String(jsonBytes);
				
				try {
					Packet packet = packetRegistry.parsePacket(json);
					
					if (packet instanceof DataPacket) { //Manually read and set the raw data if packet is DataPacket
						DataPacket dataPacket = (DataPacket) packet;
						byte[] rawBytes = new byte[dataPacket.dataLength];
						dataInput.readFully(rawBytes);
						
						dataPacket.setData(rawBytes);
					}
					
					packetRegistry.callHandler(packet);
				} catch (JsonSyntaxException e) {
					e.printStackTrace(System.err);
				}
			}
		} catch (EOFException e) {
			printMessage("Server closed connection TCP. Stopping listener.");
			stop(); //Stopping whole client if the connection ended.
        } catch (SocketException e) {
			printMessage("Connection closed TCP. Stopping listener.");
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
	
	private void listenUDP() {
		byte[] data = new byte[1024];
		DatagramPacket udpPacket = new DatagramPacket(data, data.length);
		
		printMessage("Listening UDP!");
		
		try {
			while (running) {
				udpSocket.receive(udpPacket);
				
				ByteBuffer buf = ByteBuffer.wrap(udpPacket.getData(), 0, udpPacket.getLength());
				int jsonLength = buf.getInt();
				
				byte[] jsonBytes = new byte[jsonLength];
				buf.get(jsonBytes);
				
				String json = new String(jsonBytes);
				
				try {
					Packet packet = packetRegistry.parsePacket(json);
					
					if (packet instanceof DataPacket) { //Manually read and set the raw data if packet is DataPacket
						DataPacket dataPacket = (DataPacket) packet;
						byte[] rawBytes = new byte[dataPacket.dataLength];
						buf.get(rawBytes);
						
						dataPacket.setData(rawBytes);
					}
					
					packetRegistry.callHandler(packet);
				} catch (JsonSyntaxException e) {
					e.printStackTrace(System.err);
				}
			}
		} catch (SocketException e) {
			printMessage("Connection closed UDP. Stopping listener.");
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
	
	public void sendPacket(Packet packet, Protocol protocol) {
		if (protocol == Protocol.TCP) {
			sendTCP(packet);
		} else if (protocol == Protocol.UDP) {
			sendUDP(packet);
		}
	}
	
	private void sendTCP(Packet packet) {
		try {
			if (packet.senderUuid == null) { //Client can ignore uuid, it will be set here.
				packet.senderUuid = getUuid();
			}
			packet.protocol = Protocol.TCP; //Also set protocol before sending.
			
			OutputStream out = tcpSocket.getOutputStream();
			
			String json = packetRegistry.serialize(packet);
			byte[] jsonBytes = json.getBytes();
			byte[] rawBytes = null;
			
			if (packet instanceof DataPacket) {
				DataPacket dataPacket = (DataPacket) packet;
				rawBytes = dataPacket.getData();
			}
			
			int totalLength = jsonBytes.length + (rawBytes != null ? rawBytes.length : 0);
			
			out.write(ByteBuffer.allocate(Constants.PACKET_LENGTH_PREFIX_BYTES).putInt(totalLength).array());
			out.write(ByteBuffer.allocate(Constants.PACKET_LENGTH_PREFIX_BYTES).putInt(jsonBytes.length).array());
			out.write(jsonBytes);
			if (rawBytes != null) {
				out.write(rawBytes);
			}
			out.flush();
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
	
	private void sendUDP(Packet packet) {
		try {
			if (packet.senderUuid == null) { //Client can ignore uuid, it will be set here.
				packet.senderUuid = getUuid();
			}
			packet.protocol = Protocol.UDP; //Also set protocol before sending.
			
			String json = packetRegistry.serialize(packet); //TODO: Check that the packet isn't too large for UDP
			byte[] jsonBytes = json.getBytes();
			byte[] rawBytes = null;
			
			if (packet instanceof DataPacket) {
				DataPacket dataPacket = (DataPacket) packet;
				rawBytes = dataPacket.getData();
			}
			
			int totalLength = Constants.PACKET_LENGTH_PREFIX_BYTES + jsonBytes.length + (rawBytes != null ? rawBytes.length : 0);
			
			ByteBuffer buffer = ByteBuffer.allocate(totalLength);
			buffer.putInt(jsonBytes.length);
			buffer.put(jsonBytes);
			if (rawBytes != null) {
				buffer.put(rawBytes);
			}
			
			DatagramPacket udpPacket = new DatagramPacket(buffer.array(), buffer.array().length);
			udpSocket.send(udpPacket);
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
	
	@Override
	public UUID getUuid() {
		return uuid;
	}
	
	public void stop() {
		running = false;
		try {
			tcpSocket.close();
			udpSocket.close();
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
		
		printMessage("Client stopped.");
	}
	
	public boolean isRunning() {
		return running;
	}
	
	public void printMessage(String message) { //TODO: Make possible to disable printing.
		System.out.println("[Client] " + message);
	}
	
	private void closeQuietly(AutoCloseable c) {
		if (c != null) {
			try {
				c.close();
			} catch (Exception ignored) {}
		}
	}
}
