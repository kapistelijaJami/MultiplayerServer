package multiplayerserver.old.tcp;

import multiplayerserver.Constants;
import multiplayerserver.old.PacketHandler;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;

@Deprecated
public class TCPInputStreamWorker implements Runnable {
	private final Socket socket;
	private boolean running = false;
    private final PacketHandler packetHandler;
	
	public TCPInputStreamWorker(Socket socket, PacketHandler packetHandler) {
		this.socket = socket;
		this.packetHandler = packetHandler;
	}
	
	//TODO: maybe instead of creating a worker class, we just create a worker thread and give it a function inside server or client with lambda which reads the input.
	
	@Override
	public void run() {
		running = true;
		try (InputStream in = socket.getInputStream()) {
			while (running) {
				int length = ByteBuffer.wrap(in.readNBytes(Constants.PACKET_LENGTH_PREFIX_BYTES)).getInt();
				byte[] data = in.readNBytes(length);
				packetHandler.handle(socket, data);
			}
		} catch (SocketException e) {
			System.out.println("Connection closed.");
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}
	
	public void stop() {
		running = false;
	}
}
