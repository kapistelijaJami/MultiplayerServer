package multiplayerserver.packets;

public class PingPacket extends Packet {
	public String text;
	public long startTime;
	
	public PingPacket(String text) {
		this.text = text;
		startTime = System.currentTimeMillis();
	}
	
	public PingPacket(String text, long startTime) {
		this.text = text;
		this.startTime = startTime;
	}
}
