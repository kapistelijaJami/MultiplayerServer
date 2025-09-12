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
			
			System.out.println("Client connected!");
			
			new Thread(this::listenTCP).start();
			new Thread(this::listenUDP).start();
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
	
	private void listenTCP() {
		System.out.println("Client listening TCP!");
		
		try (InputStream in = tcpSocket.getInputStream();
             DataInputStream dataInput = new DataInputStream(in)) {
			
			while (running) {
				int packetLength = dataInput.readInt();
				System.out.println("Read packet length: " + packetLength);
				byte[] packetData = new byte[packetLength];
				dataInput.readFully(packetData);
				System.out.println("Read fully!");
				
				String payload = new String(packetData);
				
				Packet packet = packetRegistry.parsePacket(payload);
				
				packetRegistry.callHandler(packet);
			}
		} catch (EOFException e) {
			System.out.println("Client disconnected!");
        } catch (SocketException e) {
			System.out.println("Connection closed.");
		} catch (IOException e) {
			e.printStackTrace(System.err);
		} finally {
            System.out.println("Client disconnected!");
        }
	}
	
	private void listenUDP() {
		byte[] data = new byte[1024];
		DatagramPacket udpPacket = new DatagramPacket(data, data.length);
		
		System.out.println("Client listening UDP!");
		
		while (running) {
			try {
				udpSocket.receive(udpPacket);
				
				String payload = new String(udpPacket.getData(), 0, udpPacket.getLength());
				
				Packet packet = packetRegistry.parsePacket(payload);
				
				packetRegistry.callHandler(packet);
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
		}
	}
	
	public void sendPacket(Packet packet, Protocol protocol) {
		System.out.println("Sending packet! Protocol: " + protocol);
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
	
	//TODO: Create stop() method.
}
