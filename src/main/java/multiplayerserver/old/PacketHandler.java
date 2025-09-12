package multiplayerserver.old;

import java.net.DatagramPacket;
import java.net.Socket;

public interface PacketHandler {
    public void handle(Socket socket, byte[] data);
    public void handle(DatagramPacket packet); //TODO: maybe combine these
}
