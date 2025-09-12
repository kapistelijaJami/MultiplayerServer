package multiplayerserver;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import multiplayerserver.packets.Packet;
import multiplayerserver.packets.PacketRegistry;
import multiplayerserver.targets.TargetRegistry;

public class Server {
	private final int serverPort;
	private ServerSocket tcpSocket;
	private DatagramSocket udpSocket;
	
	private final Map<UUID, ClientInformation> clients = new HashMap<>();
	private final PacketRegistry packetRegistry;
	private TargetRegistry targetRegistry;
	
	private ClientInformation hostClient; //TODO: Set host client
	
	private boolean running = false;
	
	public Server(int serverPort, PacketRegistry registry) {
		this.serverPort = serverPort;
		this.packetRegistry = registry;
	}
	
	public void start() {
		try {
			running = true;
			
			targetRegistry = new TargetRegistry(this);
			
			tcpSocket = new ServerSocket(serverPort);
			udpSocket = new DatagramSocket(serverPort);
			
			System.out.println("Server started!");
			
			new Thread(this::tcpAcceptLoop).start();
			new Thread(this::udpReceiveLoop).start();
		} catch (IOException e) { //TODO: Might need to just throw exception to allow the developer to catch and log themselves.
			e.printStackTrace(System.err);
		}
	}
	
	private void tcpAcceptLoop() {
		System.out.println("Server listening TCP!");
		
		while (running) {
			try {
				Socket clientSocket = tcpSocket.accept();
				clientSocket.setKeepAlive(true);
				System.out.println("New client connected: " + clientSocket.getRemoteSocketAddress());
				
				ClientInformation client = new ClientInformation(clientSocket, packetRegistry);
                
				new Thread(() -> tcpClientLoop(client)).start();
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
		}
	}
	
	private void tcpClientLoop(ClientInformation client) {
		System.out.println("Started listening inputStream!");
		
		try (InputStream in = client.getTcpSocket().getInputStream();
             DataInputStream dataInput = new DataInputStream(in)) {
			
			while (running) {
				int packetLength = dataInput.readInt();
				System.out.println("Received packet length TCP: " + packetLength);
				byte[] packetData = new byte[packetLength];
				dataInput.readFully(packetData);
				System.out.println("Received packet TCP");
				
				/*int length = ByteBuffer.wrap(in.readNBytes(Constants.PACKET_LENGTH_PREFIX_BYTES)).getInt();
				byte[] data = in.readNBytes(length);*/
				
				String payload = new String(packetData);
				
				System.out.println("Payload: " + payload);
				Packet packet = packetRegistry.parsePacket(payload);
				System.out.println("Packet parsed!");
				if (client.getUuid() == null) {
					client.setUuid(packet.senderUuid);

					synchronized (clients) {
						clients.put(client.getUuid(), client);
					}
				}
				
				System.out.println("Sender UUID: " + client.getUuid());
				
				//TODO: Should this be before or after callHandler()?
				//TODO: Should the callHandler be called if server is not in the targets?
				List<ClientInformation> recipients = targetRegistry.resolve(packet.target);
				sendToTargets(recipients, packet, Protocol.TCP);

				packetRegistry.callHandler(packet);
			}
		} catch (EOFException e) {
			System.out.println("Client disconnected!");
        } catch (SocketException e) {
			System.out.println("Connection closed.");
		} catch (IOException e) {
			e.printStackTrace(System.err);
		} finally {
            System.out.println("Client disconnected: " + client.getUuid());
            synchronized (clients) {
				clients.remove(client.getUuid());
			}
        }
	}
	
	private void udpReceiveLoop() {
		byte[] data = new byte[1024];
		DatagramPacket udpPacket = new DatagramPacket(data, data.length);
		
		System.out.println("Server listening UDP!");
		
		while (running) {
			try {
				udpSocket.receive(udpPacket);
				System.out.println("Received packet UDP");
				
				String payload = new String(udpPacket.getData(), 0, udpPacket.getLength());
				
				Packet packet = packetRegistry.parsePacket(payload);
				System.out.println("Packet parsed UDP");
				ClientInformation client = clients.get(packet.senderUuid);
				System.out.println("Sender UUID UDP: " + packet.senderUuid);
				
				if (client == null) continue;
				System.out.println("Client: " + client.getIpAddress() + ", uuid: " + client.getUuid());
				
				if (client.getUdpPort() == -1) {
					client.setUdpPort(udpPacket.getPort());
				}
				
				//TODO: Should this be before or after callHandler()?
				//TODO: Should the callHandler be called if server is not in the targets?
				List<ClientInformation> recipients = targetRegistry.resolve(packet.target);
				sendToTargets(recipients, packet, Protocol.UDP);
				
				System.out.println("sent forward");
				
				packetRegistry.callHandler(packet); //TODO: Should these create new threads? (ChatGPT thinks it's not necessary, and this guarantees sequential execution.)
													//Could add boolean heavyTask to Packet, and only create threads for heavy tasks, or just let handlers create threads.
				System.out.println("called a handler!");
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
		}
	}
	
	public Collection<ClientInformation> getClients() {
		return clients.values();
	}
	
	public ClientInformation getHostClient() {
		return hostClient;
	}
	
	public void sendToTargets(List<? extends HasUUID> targets, Packet packet, Protocol protocol) { //TODO: prevent sending back to original client
		for (HasUUID target : targets) {
			sendPacket(target.getUuid(), packet, protocol);
		}
	}
	
	public void sendPacket(UUID uuid, Packet packet, Protocol protocol) {
		if (protocol == Protocol.TCP) {
			sendTCP(uuid, packet);
		} else if (protocol == Protocol.UDP) {
			sendUDP(uuid, packet);
		}
	}
	
	public void sendPacket(ClientInformation client, Packet packet, Protocol protocol) {
		if (protocol == Protocol.TCP) {
			sendTCP(client, packet);
		} else if (protocol == Protocol.UDP) {
			sendUDP(client, packet);
		}
	}
	
	private void sendTCP(UUID uuid, Packet packet) {
		ClientInformation client = clients.get(uuid);
		if (client == null) return;
		
		sendTCP(client, packet);
	}
	
	private void sendTCP(ClientInformation client, Packet packet) {
		client.sendTCP(packet);
	}
	
	private void sendUDP(UUID uuid, Packet packet) {
		ClientInformation client = clients.get(uuid);
		if (client == null) return;
		
		sendUDP(client, packet);
	}
	
	private void sendUDP(ClientInformation client, Packet packet) {
		try {
			String payload = packetRegistry.serialize(packet);
			byte[] data = payload.getBytes();
			
			DatagramPacket udpPacket = new DatagramPacket(data, data.length, client.getIpAddress(), client.getUdpPort());
			udpSocket.send(udpPacket);
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
	
	//TODO: Create stop() method.
}
