package multiplayerserver.old.tcp;

import multiplayerserver.ClientInformation;
import multiplayerserver.Constants;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import multiplayerserver.old.PacketHandler;

//TODO: Maybe just have a single Server class that has listenTCP, sendTCP, listenUDP, sendUDP -methods that are called with different threads.
/*@Deprecated
public class TCPServer implements Runnable {
	private final int serverPort = Constants.SERVER_PORT;
	private ServerSocket serverSocket;
	private boolean running = false;
	private final PacketHandler handler;
	
	public TCPServer(PacketHandler handler) {
		this.handler = handler;
	}
	
	public void init() {
		try {
			this.serverSocket = new ServerSocket(serverPort);
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
	
	@Override
	public void run() {
		init();
		
		running = true;
		while (running) {
			try {
				Socket clientSocket = serverSocket.accept();
				
				new Thread(new TCPInputStreamWorker(clientSocket, handler)).start(); //TODO: these might need to be in a list to be able to close them
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
		}
	}
	
	public void closeConnection() {
		try {
			//TODO: needs to go through all inputStreamWorkers and stop them, then close all clientSockets, and close main socket.
			
			this.serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
	
	public static void sendData(String text, ClientInformation client) {
		sendData(text.getBytes(), client);
	}
	
	public static void sendData(byte[] data, ClientInformation client) {
		sendData(data, client.getTCPSocket());
	}
	
	public static void sendData(byte[] data, Socket socket) {
		try {
			OutputStream out = socket.getOutputStream();
			
			out.write(ByteBuffer.allocate(Constants.PACKET_LENGTH_PREFIX_BYTES).putInt(data.length).array());
			out.write(data);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
}*/
