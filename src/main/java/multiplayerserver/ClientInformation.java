package multiplayerserver;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.UUID;
import multiplayerserver.packets.Packet;
import multiplayerserver.packets.PacketRegistry;

/**
 * This will be created after receiving a TCP packet. It gets TCP socket, which has the InetAddress and TCP port.
 * The packet includes UUID, but it needs to be parsed before you can set it, so it happens in a setter.
 * It doesn't have a UDP port, that needs to be set when the first UDP packet arrives.
 */
public class ClientInformation implements HasUUID {
	private final InetAddress ipAddress;
	private Socket tcpSocket;
	private int udpPort = -1;
	private UUID uuid = null;
	
    private final PacketRegistry registry;
	
	public ClientInformation(Socket tcpSocket, PacketRegistry registry) {
		this.ipAddress = tcpSocket.getInetAddress();
		this.tcpSocket = tcpSocket;
		this.registry = registry;
	}
	
	public ClientInformation(InetAddress ipAddress, int udpPort, UUID uuid, PacketRegistry registry) {
		this.ipAddress = ipAddress;
		this.udpPort = udpPort;
		this.uuid = uuid;
		this.registry = registry;
	}
	
	public void setTcpSocket(Socket tcpSocket) {
		this.tcpSocket = tcpSocket;
	}
	
	public void setUdpPort(int clientUdpPort) {
		this.udpPort = clientUdpPort;
	}
	
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	
	public InetAddress getIpAddress() {
		return ipAddress;
	}
	
	public Socket getTcpSocket() {
		return tcpSocket;
	}
	
	public int getUdpPort() {
		return udpPort;
	}
	
	@Override
	public UUID getUuid() {
		return uuid;
	}
}
