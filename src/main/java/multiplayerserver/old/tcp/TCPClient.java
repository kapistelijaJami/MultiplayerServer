package multiplayerserver.old.tcp;

import multiplayerserver.Constants;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import multiplayerserver.old.PacketHandler;

@Deprecated
public class TCPClient {
	private Socket socket;
	private TCPInputStreamWorker inputStreamWorker;
	private final PacketHandler handler;
	
	public TCPClient(InetAddress serverIP, int port, PacketHandler handler) {
		this.handler = handler;
		try {
			this.socket = new Socket(serverIP, port);
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
	
	public void start() {
		inputStreamWorker = new TCPInputStreamWorker(socket, handler);
		
		new Thread(inputStreamWorker).start();
	}
	
	public void sendData(String text) {
		sendData(this.socket, text.getBytes());
	}
	
	public void sendData(Socket socket, String text) {
		sendData(socket, text.getBytes());
	}
	
	public static void sendData(Socket socket, byte[] data) {
		try {
			OutputStream out = socket.getOutputStream();
			
			out.write(ByteBuffer.allocate(Constants.PACKET_LENGTH_PREFIX_BYTES).putInt(data.length).array());
			out.write(data);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
	
	public void closeConnection() {
		inputStreamWorker.stop();
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
}
