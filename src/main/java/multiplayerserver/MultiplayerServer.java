package multiplayerserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;
import multiplayerserver.packets.MovePacket;
import multiplayerserver.packets.PacketRegistry;
import multiplayerserver.packets.PingPacket;
import multiplayerserver.packets.RawDataPacket;
import multiplayerserver.targets.Target;

public class MultiplayerServer {

    public static void main(String[] args) {
		Scanner scan = new Scanner(System.in);
		System.out.print("Server (Y/n): ");
		String isServer = scan.nextLine();
		
		if (isServer.isBlank() || isServer.toLowerCase().equals("y")) {
			PacketRegistry packetRegistryServer = new PacketRegistry();
			Server server = new Server(Constants.SERVER_PORT, packetRegistryServer);
			
			packetRegistryServer.register(PingPacket.class, p -> MultiplayerServer.handlePacket(p, server));
			
			try {
				server.start();
			} catch (IOException e) {
				return;
			}
			
			try {
				PacketRegistry packetRegistryClient = new PacketRegistry();
				Client client = new Client(InetAddress.getByName("127.0.0.1"), Constants.SERVER_PORT, packetRegistryClient);
				
				packetRegistryClient.register(MovePacket.class, p -> System.out.println("Host client received move packet!"));
				packetRegistryClient.register(RawDataPacket.class, p -> System.out.println("Host client received raw data packet! " + p.extraText + " " + Arrays.toString(p.getData())));
				
				client.connect();
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
			
			try {
				Thread.sleep(500);
				System.out.println("\nPress enter to close server.");
				scan.nextLine();
				server.stop();
			} catch (InterruptedException e) {
				e.printStackTrace(System.err);
			}
		} else if (isServer.toLowerCase().equals("n")) {
			try {
				System.out.print("Server IP address: ");
				String ipAddress = scan.nextLine();
				if (ipAddress.isBlank()) {
					ipAddress = "localhost";
				}
				
				PacketRegistry packetRegistryClient = new PacketRegistry();
				Client client = new Client(InetAddress.getByName(ipAddress), Constants.SERVER_PORT, packetRegistryClient);
				
				packetRegistryClient.register(MovePacket.class, MultiplayerServer::handlePacket);
				packetRegistryClient.register(PingPacket.class, MultiplayerServer::handlePong);
				
				client.connect();
				
				client.sendPacket(new MovePacket(5, 10), Protocol.UDP);
				
				Thread.sleep(1000);
				
				client.sendPacket(new PingPacket("ping"), Protocol.TCP);
				
				Thread.sleep(1000);
				
				client.sendPacket(new MovePacket(15, 20, Target.ALL), Protocol.UDP);
				
				Thread.sleep(1000);
				
				byte[] rawBytes = new byte[] {10, 20, 30, 40, 50};
				client.sendPacket(new RawDataPacket(rawBytes, "Hello", Target.ALL), Protocol.TCP);
				
				try {
					Thread.sleep(3000);
					client.stop();
				} catch (InterruptedException e) {
					e.printStackTrace(System.err);
				}
			} catch (UnknownHostException | InterruptedException e) {
				e.printStackTrace(System.err);
			} catch (IOException e) {
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
