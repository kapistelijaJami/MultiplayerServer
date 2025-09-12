package multiplayerserver.packets;

import java.util.UUID;
import multiplayerserver.targets.Target;

public abstract class Packet {
	public UUID senderUuid;
	public Target target;
	
	public Packet() {}
	
	public Packet(UUID senderUuid) {
		this.senderUuid = senderUuid;
    }
	
	public Packet(Target target) { //TODO: should this still be Target... or just a single Target?
		this.target = target;
    }
	
	public Packet(UUID senderUuid, Target target) { //TODO: should this still be Target... or just a single Target?
		this.senderUuid = senderUuid;
		this.target = target;
    }
}
