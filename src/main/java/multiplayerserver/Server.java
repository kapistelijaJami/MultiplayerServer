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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import multiplayerserver.packets.Packet;
import multiplayerserver.packets.PacketRegistry;
import multiplayerserver.packets.SendUuid;
import multiplayerserver.targets.TargetRegistry;

public class Server {
	private final int serverPort;
	private ServerSocket tcpSocket;
	private DatagramSocket udpSocket;
	
	private final Map<UUID, ClientInformation> clients = new HashMap<>();
	private final PacketRegistry packetRegistry;
	private TargetRegistry targetRegistry;
	
	private ClientInformation hostClient;
	
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
			
			System.out.println("[Server] Server started!");
			
			new Thread(this::tcpAcceptLoop).start();
			new Thread(this::udpReceiveLoop).start();
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
	
	private void tcpAcceptLoop() {
		System.out.println("[Server] Listening TCP!");
		
		while (running) {
			try {
				Socket clientSocket = tcpSocket.accept();
				clientSocket.setKeepAlive(true);
				System.out.println("[Server] New client connected: " + clientSocket.getRemoteSocketAddress());
				
				ClientInformation client = new ClientInformation(clientSocket, packetRegistry);
                
				new Thread(() -> tcpClientLoop(client)).start();
			} catch (SocketException e) {
				System.out.println("[Server] ServerSocket closed! Stopping listener.");
				break;
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
		}
	}
	
	private void tcpClientLoop(ClientInformation client) {
		System.out.println("[Server] Listening InputStream!");
		
		try (Socket socket = client.getTcpSocket();
				InputStream in = socket.getInputStream();
				DataInputStream dataInput = new DataInputStream(in)) {
			
			while (running) {
				int packetLength = dataInput.readInt();
				byte[] packetData = new byte[packetLength];
				dataInput.readFully(packetData);
				System.out.println("[Server] Received packet TCP");
				
				String payload = new String(packetData);
				Packet packet = packetRegistry.parsePacket(payload);
				System.out.println("[Server] Packet parsed!");
				
				if (client.getUuid() == null) { //First time receiving a packet, set uuid and add to clients list.
					client.setUuid(packet.senderUuid);
					addClient(client);
				} else if (clients.containsKey(client.getUuid()) && !(clients.get(client.getUuid()).equals(client))) {
					//If client was created with UDP, then use the already added client, and add tcpSocket to it.
					ClientInformation temp = client;
					client = clients.get(client.getUuid());
					client.setTcpSocket(temp.getTcpSocket());
					System.out.println("[Server] TCP socket set!");
				}
				
				//If packet was SendUuid, then we can set udpPort too.
				if (packet instanceof SendUuid) {
					SendUuid p = (SendUuid) packet;
					client.setUdpPort(p.udpPort);
				}
				
				System.out.println("[Server] Client: " + client.getIpAddress() + ", uuid: " + client.getUuid());
				
				handlePacket(packet, Protocol.TCP);
			}
		} catch (EOFException e) {
			System.out.println("[Server] Client disconnected normally TCP: " + client.getUuid());
        } catch (SocketException e) {
			System.out.println("[Server] Socket closed TCP!");
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
		
		synchronized (clients) {
			clients.remove(client.getUuid());
		}
	}
	
	private void udpReceiveLoop() {
		byte[] data = new byte[1024];
		DatagramPacket udpPacket = new DatagramPacket(data, data.length);
		
		System.out.println("[Server] Listening UDP!");
		
		try {
			while (running) {
				udpSocket.receive(udpPacket);
				System.out.println("[Server] Received packet UDP");
				
				String payload = new String(udpPacket.getData(), 0, udpPacket.getLength());
				Packet packet = packetRegistry.parsePacket(payload);
				System.out.println("[Server] Packet parsed UDP");
				
				
				ClientInformation client = clients.get(packet.senderUuid);
				
				if (client == null) { //If first packet was UDP, we create the ClientInformation.
					client = new ClientInformation(udpSocket.getInetAddress(), udpSocket.getPort(), packet.senderUuid, packetRegistry);
					addClient(client);
				}
				
				System.out.println("[Server] Client: " + client.getIpAddress() + ", uuid: " + client.getUuid());
				
				if (client.getUdpPort() == -1) { //If client was created by TCP, we add the UDP port.
					client.setUdpPort(udpPacket.getPort());
				}
				
				handlePacket(packet, Protocol.UDP);
			}
		} catch (SocketException e) {
			System.out.println("[Server] Socket was closed UDP! Stopping listener.");
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
	
	private void handlePacket(Packet packet, Protocol protocol) {
		//TODO: Should the callHandler be called if server is not in the targets? (Maybe just have server handle all packets no matter the target)
		packetRegistry.callHandler(packet); //TODO: Should these create new threads? (ChatGPT thinks it's not necessary, and this guarantees sequential execution.)
											//Could add boolean heavyTask to Packet, and only create threads for heavy tasks, or just let handlers create threads.
		
		List<? extends HasUUID> targetClients = targetRegistry.resolveTargets(packet.target);
		sendToTargets(targetClients, packet, protocol);
	}
	
	public ClientInformation getClient(UUID uuid) {
		return clients.get(uuid);
	}
	
	public List<ClientInformation> getClients() {
		return new ArrayList<>(clients.values());
	}
	
	public List<ClientInformation> getAllClientsExcept(UUID uuid) {
		ClientInformation excluded = clients.get(uuid);
		return clients.values().stream()
				.filter(c -> c != excluded)
				.collect(Collectors.toList());
	}
	
	public ClientInformation getHostClient() {
		return hostClient;
	}
	
	public void setHostClient(UUID uuid) {
		ClientInformation client = clients.get(uuid);
		if (client == null) {
			System.err.println("Client doesn't exist!");
		} else {
			hostClient = client;
		}
	}
	
	public void setHostClient(HasUUID obj) {
		setHostClient(obj.getUuid());
	}
	
	public PacketRegistry getPacketRegistry() {
		return packetRegistry;
	}
	
	public TargetRegistry getTargetRegistry() {
		return targetRegistry;
	}
	
	/**
	 * Sends to all targets in the list, except the packet's original sender.
	 * Can take a list of any objects that implement HasUUID interface.
	 * @param targets
	 * @param packet
	 * @param protocol
	 */
	public void sendToTargets(List<? extends HasUUID> targets, Packet packet, Protocol protocol) {
		for (HasUUID target : targets) {
			if (target.getUuid().equals(packet.senderUuid)) { //Don't send packet back to sender.
				continue;
			}
			
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
	
	private void addClient(ClientInformation client) {
		synchronized (clients) {
			if (clients.isEmpty()) {
				hostClient = client; //First client that connects is the host.
			}
			clients.put(client.getUuid(), client);
			System.out.println("[Server] Client added!");
		}
	}
	
	public void stop() {
		running = false;
		try {
			synchronized (clients) {
				for (ClientInformation client : clients.values()) {
					client.getTcpSocket().close();
				}
				clients.clear();
			}
			tcpSocket.close();
			udpSocket.close();
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
		
		System.out.println("Server stopped.");
	}
}
