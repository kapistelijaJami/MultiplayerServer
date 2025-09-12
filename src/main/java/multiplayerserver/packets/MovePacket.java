package multiplayerserver.packets;

import multiplayerserver.targets.Target;

public class MovePacket extends Packet {
	public int x, y;
	
	public MovePacket(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public MovePacket(int x, int y, Target target) {
		this.x = x;
		this.y = y;
	}
}
