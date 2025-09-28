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
				int packetLength = dataInput.readInt();
				byte[] packetData = new byte[packetLength];
				dataInput.readFully(packetData);
				
				String payload = new String(packetData);
				
				try {
					Packet packet = packetRegistry.parsePacket(payload);
					
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
				
				String payload = new String(udpPacket.getData(), 0, udpPacket.getLength());
				
				try {
					Packet packet = packetRegistry.parsePacket(payload);
					
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
			
			String payload = packetRegistry.serialize(packet);
			byte[] data = payload.getBytes();
			
			out.write(ByteBuffer.allocate(Constants.PACKET_LENGTH_PREFIX_BYTES).putInt(data.length).array());
			out.write(data);
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
			
			String payload = packetRegistry.serialize(packet);
			byte[] data = payload.getBytes();
			
			DatagramPacket udpPacket = new DatagramPacket(data, data.length);
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
