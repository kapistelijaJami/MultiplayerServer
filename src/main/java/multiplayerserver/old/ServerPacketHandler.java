package multiplayerserver;

/*import java.net.DatagramPacket;
import multiplayerserver.tcp.TCPServer;
import java.net.Socket;
import multiplayerserver.udp.UDPServer;

public class ServerPacketHandler implements PacketHandler {
	private UDPServer udpServer;
	
	public void setUDPServer(UDPServer udpServer) {
		this.udpServer = udpServer;
	}
	
	@Override
	public void handle(Socket socket, byte[] data) {
		System.out.println("Server is handling packet");
		printSocketInfo(socket, data);
		
		System.out.println("Sending vastaus!");
		TCPServer.sendData("Vastaus".getBytes(), socket);
	}
	
	@Override
	public void handle(DatagramPacket packet) {
		System.out.println("Server is handling packet");
		printSocketInfo(packet);
		
		System.out.println("Sending vastaus!");
		udpServer.sendData("Vastaus".getBytes(), packet.getAddress(), packet.getPort());
	}
	
	private void printSocketInfo(Socket socket, byte[] data) {
		System.out.println("\tPacket received: " + new String(data));
		System.out.println("\t\tSender IP: " + socket.getInetAddress() + " Port: " + socket.getPort());
	}
	
	private void printSocketInfo(DatagramPacket packet) {
		System.out.println("\tPacket received: " + new String(packet.getData(), 0, packet.getLength()));
		System.out.println("\t\tSender IP: " + packet.getAddress() + " Port: " + packet.getPort());
	}
}*/
