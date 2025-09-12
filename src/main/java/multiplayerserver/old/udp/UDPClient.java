package multiplayerserver.old.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import multiplayerserver.old.PacketHandler;

@Deprecated
public class UDPClient implements Runnable {
	private DatagramSocket socket;
	private boolean running = false;
	private final PacketHandler handler;
	
	public UDPClient(InetAddress serverIpAddress, int serverPort, PacketHandler handler) {
		this.handler = handler;
		try {
			socket = new DatagramSocket();
			socket.connect(serverIpAddress, serverPort);
			printSocketInfo();
		} catch (SocketException e) {
			e.printStackTrace(System.err);
		}
	}

	@Override
	public void run() {
		running = true;
		while (running) {
			byte[] data = new byte[1024];
			DatagramPacket packet = new DatagramPacket(data, data.length);
			try {
				socket.receive(packet);
				
				handler.handle(packet);
			} catch (IOException e) {
				break;
			}
		}
	}
	
	public void sendData(String text) {
		sendData(text.getBytes());
	}
	
	public void sendData(byte[] data) {
		try {
			DatagramPacket packet = new DatagramPacket(data, data.length); //Works without ip and port if socket is connected
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
	
	private void printSocketInfo() {
		System.out.println("client local port: " + socket.getLocalPort() + " client port: " + socket.getPort());
		System.out.println("client local address: " + socket.getLocalAddress() + " client address: " + socket.getInetAddress());
	}
}
