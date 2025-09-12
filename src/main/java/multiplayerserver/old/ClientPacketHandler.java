package multiplayerserver.old;

import multiplayerserver.old.PacketHandler;
import java.net.DatagramPacket;
import java.net.Socket;

public class ClientPacketHandler implements PacketHandler {
	
	@Override
	public void handle(Socket socket, byte[] data) {
		System.out.println("Client is handling packet");
		System.out.println("\tPacket received: " + new String(data));
		System.out.println("\t\tSender IP: " + socket.getInetAddress() + " Port: " + socket.getPort());
	}

	@Override
	public void handle(DatagramPacket packet) {
		System.out.println("Client is handling packet");
		System.out.println("\tPacket received: " + new String(packet.getData()));
		System.out.println("\t\tSender IP: " + packet.getAddress() + " Port: " + packet.getPort());
	}
}
