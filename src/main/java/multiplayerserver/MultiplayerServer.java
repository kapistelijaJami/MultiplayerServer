package multiplayerserver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.UUID;
import multiplayerserver.packets.MovePacket;
import multiplayerserver.packets.PacketRegistry;
import multiplayerserver.packets.PingPacket;
import multiplayerserver.targets.Target;

public class MultiplayerServer {

    public static void main(String[] args) {
		Scanner scan = new Scanner(System.in);
		System.out.print("Server (Y/n): ");
		String isServer = scan.nextLine();
		
		if (isServer.isBlank() || isServer.toLowerCase().equals("y")) {
			PacketRegistry packetRegistryServer = new PacketRegistry();
			Server server = new Server(Constants.SERVER_PORT, packetRegistryServer);
			
			packetRegistryServer.registerPacket(MovePacket.class);
			packetRegistryServer.registerHandler(PingPacket.class, p -> MultiplayerServer.handlePacket(p, server));
			
			server.start();
			
			try {
				PacketRegistry packetRegistryClient = new PacketRegistry();
				Client client = new Client(InetAddress.getByName("127.0.0.1"), Constants.SERVER_PORT, packetRegistryClient);
				
				packetRegistryClient.registerHandler(MovePacket.class, p -> System.out.println("Host client received move packet!"));
				
				client.connect();
			} catch (UnknownHostException e) {
				e.printStackTrace(System.err);
			}
			
			/*try {
				Thread.sleep(15000);
				server.stop();
			} catch (InterruptedException e) {
				e.printStackTrace(System.err);
			}*/
		} else if (isServer.toLowerCase().equals("n")) {
			try {
				System.out.print("Server IP address: ");
				String ipAddress = scan.nextLine();
				if (ipAddress.isBlank()) {
					ipAddress = "localhost";
				}
				
				PacketRegistry packetRegistryClient = new PacketRegistry();
				Client client = new Client(InetAddress.getByName(ipAddress), Constants.SERVER_PORT, packetRegistryClient);
				
				packetRegistryClient.registerHandler(MovePacket.class, MultiplayerServer::handlePacket);
				packetRegistryClient.registerHandler(PingPacket.class, MultiplayerServer::handlePong);
				
				client.connect();
				
				client.sendPacket(new MovePacket(5, 10), Protocol.UDP);
				
				Thread.sleep(1000);
				
				client.sendPacket(new PingPacket("ping"), Protocol.TCP);
				
				Thread.sleep(1000);
				
				client.sendPacket(new MovePacket(15, 20, Target.ALL), Protocol.UDP);
				
				try {
					Thread.sleep(3000);
					client.stop();
				} catch (InterruptedException e) {
					e.printStackTrace(System.err);
				}
			} catch (UnknownHostException | InterruptedException e) {
				e.printStackTrace(System.err);
			}
		}
    }
	
	public static void handlePacket(MovePacket packet) {
		System.out.println("Packet received! " + packet.x + ", " + packet.y);
	}
	
	public static void handlePacket(PingPacket packet, Server server) {
		System.out.println("Packet received! " + packet.text + ", " + packet.startTime);
		server.sendPacket(packet.senderUuid, new PingPacket("pong", packet.startTime), Protocol.TCP);
	}
	
	public static void handlePong(PingPacket packet) {
		System.out.println("Packet received! " + packet.text + ", " + packet.startTime + ". Round trip time: " + (System.currentTimeMillis() - packet.startTime) + " ms");
	}
}
