package multiplayerserver;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
		this.serverIP = serverIP;
		this.serverPort = serverPort;
		this.packetRegistry = registry;
		
		this.uuid = UUID.randomUUID();
	}
	
	public void connect() {
		try {
			running = true;
			
			tcpSocket = new Socket(serverIP, serverPort);
			tcpSocket.setKeepAlive(true);
			
			udpSocket = new DatagramSocket();
			udpSocket.connect(serverIP, serverPort);
			System.out.println("[Client] Client connected!");
			
			new Thread(this::listenTCP).start();
			new Thread(this::listenUDP).start();
			
			sendPacket(new SendUuid(uuid), Protocol.TCP);
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
	
	private void listenTCP() {
		System.out.println("[Client] Listening TCP!");
		
		try (InputStream in = tcpSocket.getInputStream();
				DataInputStream dataInput = new DataInputStream(in)) {
			
			while (running) {
				int packetLength = dataInput.readInt();
				System.out.println("[Client] Received packet length TCP: " + packetLength);
				byte[] packetData = new byte[packetLength];
				dataInput.readFully(packetData);
				System.out.println("[Client] Received packet TCP");
				
				String payload = new String(packetData);
				System.out.println("[Client] Payload: " + payload);
				
				Packet packet = packetRegistry.parsePacket(payload);
				System.out.println("[Client] Packet parsed!");
				
				packetRegistry.callHandler(packet);
			}
		} catch (EOFException e) {
			System.out.println("[Client] Server closed connection TCP. Stopping listener."); //TODO: Could stop the whole client as well?
        } catch (SocketException e) {
			System.out.println("[Client] Connection closed TCP. Stopping listener.");
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
	
	private void listenUDP() {
		byte[] data = new byte[1024];
		DatagramPacket udpPacket = new DatagramPacket(data, data.length);
		
		System.out.println("[Client] Listening UDP!");
		
		try {
			while (running) {
				udpSocket.receive(udpPacket);
				
				String payload = new String(udpPacket.getData(), 0, udpPacket.getLength());
				
				Packet packet = packetRegistry.parsePacket(payload);
				
				packetRegistry.callHandler(packet);
			}
		} catch (SocketException e) {
			System.out.println("[Client] Connection closed UDP. Stopping listener.");
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
	
	public void sendPacket(Packet packet, Protocol protocol) {
		System.out.println("[Client] Sending packet! Protocol: " + protocol);
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
		
		System.out.println("Client stopped.");
	}
}
