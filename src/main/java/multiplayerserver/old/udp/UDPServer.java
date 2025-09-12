package multiplayerserver.old.udp;

import multiplayerserver.ClientInformation;
import multiplayerserver.Constants;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import multiplayerserver.old.PacketHandler;

/*@Deprecated
public class UDPServer implements Runnable {
	private final int serverPort = Constants.SERVER_PORT;
	private DatagramSocket socket;
	private boolean running = false;
	private final PacketHandler handler;
	
	public UDPServer(PacketHandler handler) {
		this.handler = handler;
	}
	
	private void init() {
		try {
			this.socket = new DatagramSocket(serverPort);
		} catch (SocketException e) {
			e.printStackTrace(System.err);
		}
	}
	
	@Override
	public void run() {
		init();
		
		running = true;
		while (running) {
			byte[] data = new byte[1024];
			DatagramPacket packet = new DatagramPacket(data, data.length);
			try {
				socket.receive(packet);
				
				handler.handle(packet);
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
		}
	}
	
	public void sendData(String text, ClientInformation client) {
		sendData(text.getBytes(), client.getIpAddress(), client.getPort());
	}
	
	public void sendData(String text, InetAddress ipAddress, int port) {
		sendData(text.getBytes(), ipAddress, port);
	}
	
	public void sendData(byte[] data, InetAddress ipAddress, int port) {
		try {
			DatagramPacket packet = new DatagramPacket(data, data.length, ipAddress, port);
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
}*/
