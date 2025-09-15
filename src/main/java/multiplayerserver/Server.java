package multiplayerserver;

import com.google.gson.Gson;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import multiplayerserver.packets.Packet;
import multiplayerserver.packets.PacketRegistry;
import multiplayerserver.packets.SendUuid;
import multiplayerserver.targets.ResolveContext;
import multiplayerserver.targets.TargetRegistry;

public class Server {
	private final int serverPort;
	private ServerSocket tcpSocket;
	private DatagramSocket udpSocket;
	
	private final Map<UUID, ClientInformation> clients = new HashMap<>();
	private final PacketRegistry packetRegistry;
	private TargetRegistry targetRegistry;
	private Gson gson = new Gson();
	
	private ClientInformation hostClient;
	
	private boolean running = false;
	
	
	public Server(int serverPort, PacketRegistry registry) {
		this.serverPort = serverPort;
		this.packetRegistry = registry;
		
		targetRegistry = new TargetRegistry(this);
	}
	
	public void start() {
		try {
			running = true;
			
			tcpSocket = new ServerSocket(serverPort);
			udpSocket = new DatagramSocket(serverPort);
			
			printMessage("Server started!");
			
			new Thread(this::tcpAcceptLoop).start();
			new Thread(this::udpReceiveLoop).start();
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
	
	private void tcpAcceptLoop() {
		printMessage("Listening TCP!");
		
		while (running) {
			try {
				Socket clientSocket = tcpSocket.accept();
				clientSocket.setKeepAlive(true);
				printMessage("New client connected: " + clientSocket.getRemoteSocketAddress());
				
				ClientInformation client = new ClientInformation(clientSocket, packetRegistry);
                
				new Thread(() -> tcpClientLoop(client)).start();
			} catch (SocketException e) {
				printMessage("ServerSocket closed! Stopping listener.");
				break;
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
		}
	}
	
	private void tcpClientLoop(ClientInformation client) {
		printMessage("Listening InputStream!");
		
		try (Socket socket = client.getTcpSocket();
				InputStream in = socket.getInputStream();
				DataInputStream dataInput = new DataInputStream(in)) {
			
			while (running) {
				int packetLength = dataInput.readInt();
				byte[] packetData = new byte[packetLength];
				dataInput.readFully(packetData);
				
				String payload = new String(packetData);
				
				if (!packetRegistry.isPacketRegistered(payload)) { //If the packet isn't registered on the server we can still forward it to other clients.
					Packet basePacket = packetRegistry.parseAsBasePacket(payload);
					forwardPayload(basePacket, payload, Protocol.UDP);
					continue;
				}
				
				Packet packet = packetRegistry.parsePacket(payload);
				
				if (client.getUuid() == null) { //First time receiving a packet, set uuid and add to clients list.
					client.setUuid(packet.senderUuid);
					addClient(client);
				} else if (clients.containsKey(client.getUuid()) && !(clients.get(client.getUuid()).equals(client))) {
					//If client was created with UDP, then use the already added client, and add tcpSocket to it.
					ClientInformation temp = client;
					client = clients.get(client.getUuid());
					client.setTcpSocket(temp.getTcpSocket());
				}
				
				//If packet was SendUuid, then we can set udpPort too.
				if (packet instanceof SendUuid) {
					SendUuid p = (SendUuid) packet;
					client.setUdpPort(p.udpPort);
				}
				
				handlePacket(packet, Protocol.TCP);
			}
		} catch (EOFException e) {
			printMessage("Client disconnected normally TCP: " + client.getUuid());
        } catch (SocketException e) {
			printMessage("Socket closed TCP!");
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
		
		printMessage("Listening UDP!");
		
		try {
			while (running) {
				udpSocket.receive(udpPacket);
				
				String payload = new String(udpPacket.getData(), 0, udpPacket.getLength());
				
				if (!packetRegistry.isPacketRegistered(payload)) { //If the packet isn't registered on the server we can still forward it to other clients.
					Packet basePacket = packetRegistry.parseAsBasePacket(payload);
					forwardPayload(basePacket, payload, Protocol.UDP);
					continue;
				}
				
				Packet packet = packetRegistry.parsePacket(payload);
				
				ClientInformation client = clients.get(packet.senderUuid);
				
				if (client == null) { //If first packet was UDP, we create the ClientInformation.
					client = new ClientInformation(udpSocket.getInetAddress(), udpSocket.getPort(), packet.senderUuid, packetRegistry);
					addClient(client);
				}
				
				if (client.getUdpPort() == -1) { //If client was created by TCP, we add the UDP port.
					client.setUdpPort(udpPacket.getPort());
				}
				
				handlePacket(packet, Protocol.UDP);
			}
		} catch (SocketException e) {
			printMessage("Socket was closed UDP! Stopping listener.");
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
	
	/**
	 * Handles the packet.
	 * If handler is registered, it calls that.
	 * Then it resolves the targets and sends it forwards if there were any.
	 * @param packet
	 * @param protocol 
	 */
	private void handlePacket(Packet packet, Protocol protocol) {
		//TODO: Should the callHandler be called if server is not in the targets? (Maybe just have server handle all packets no matter the target, like it is now)
		packetRegistry.callHandler(packet); //TODO: Should these create new threads? (ChatGPT thinks it's not necessary, and this guarantees sequential execution.)
											//Could add boolean heavyTask to Packet, and only create threads for heavy tasks, or just let handlers create threads.
		
		List<? extends HasUUID> targetClients = targetRegistry.resolveTargets(new ResolveContext(this, packet), packet.targets);
		sendToClients(targetClients, packet, protocol);
	}
	
	private void forwardPayload(Packet packet, String payload, Protocol protocol) {
		List<? extends HasUUID> targetClients = targetRegistry.resolveTargets(new ResolveContext(this, packet), packet.targets);
		sendPayloadToClients(targetClients, packet.senderUuid, payload, protocol);
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
	
	public boolean isHostClient(UUID uuid) {
		return isHostClient(clients.get(uuid));
	}
	
	public boolean isHostClient(ClientInformation client) {
		if (hostClient == null || client == null)
			return false;
		
		return hostClient == client;
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
	 * Sends to all clients in the list, except the packet's original sender.
	 * Can take a list of any objects that implement HasUUID interface.
	 * @param clients
	 * @param packet
	 * @param protocol
	 */
	public void sendToClients(List<? extends HasUUID> clients, Packet packet, Protocol protocol) {
		for (HasUUID client : clients) {
			if (client.getUuid().equals(packet.senderUuid)) { //Don't send packet back to sender.
				continue;
			}
			
			sendPacket(client.getUuid(), packet, protocol);
		}
	}
	
	public void sendPayloadToClients(List<? extends HasUUID> clients, UUID senderUuid, String payload, Protocol protocol) {
		for (HasUUID client : clients) {
			if (client.getUuid().equals(senderUuid)) { //Don't send packet back to sender.
				continue;
			}
			
			sendPayload(client.getUuid(), payload, protocol);
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
	
	public void sendPayload(UUID uuid, String payload, Protocol protocol) {
		if (protocol == Protocol.TCP) {
			sendPayloadTCP(uuid, payload);
		} else if (protocol == Protocol.UDP) {
			sendPayloadUDP(uuid, payload);
		}
	}
	
	public void sendPayload(ClientInformation client, String payload, Protocol protocol) {
		if (protocol == Protocol.TCP) {
			sendPayloadTCP(client, payload);
		} else if (protocol == Protocol.UDP) {
			sendPayloadUDP(client, payload);
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
	
	private void sendPayloadTCP(UUID uuid, String payload) {
		ClientInformation client = clients.get(uuid);
		if (client == null) return;
		
		sendPayloadTCP(client, payload);
	}
	
	private void sendPayloadTCP(ClientInformation client, String payload) {
		client.sendTCP(payload);
	}
	
	private void sendUDP(UUID uuid, Packet packet) {
		ClientInformation client = clients.get(uuid);
		if (client == null) return;
		
		sendUDP(client, packet);
	}
	
	private void sendUDP(ClientInformation client, Packet packet) {
		packet.protocol = Protocol.UDP; //Set protocol before sending.

		String payload = packetRegistry.serialize(packet);
		sendPayloadUDP(client, payload);
	}
	
	private void sendPayloadUDP(UUID uuid, String payload) {
		ClientInformation client = clients.get(uuid);
		if (client == null) return;
		
		sendPayloadUDP(client, payload);
	}
	
	private void sendPayloadUDP(ClientInformation client, String payload) {
		try {
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
			printMessage("Client added!");
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
		
		printMessage("Server stopped.");
	}

	public boolean isRunning() {
		return running;
	}
	
	public void printMessage(String message) {
		System.out.println("[Server] " + message);
	}
}
