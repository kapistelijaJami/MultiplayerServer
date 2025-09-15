package multiplayerserver.packets;

import multiplayerserver.targets.Target;

public class PingPacket extends Packet {
	public String text;
	public long startTime;
	
	public PingPacket(String text, Target... targets) {
		this.text = text;
		startTime = System.currentTimeMillis();
	}
	
	public PingPacket(String text, long startTime, Target... targets) {
		this.text = text;
		this.startTime = startTime;
	}
}
