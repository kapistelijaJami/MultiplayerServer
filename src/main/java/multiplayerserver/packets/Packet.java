package multiplayerserver.packets;

import java.util.UUID;
import multiplayerserver.targets.Target;

public abstract class Packet {
	public UUID senderUuid;
	public Target[] targets;
	
	public Packet() {}
	
	public Packet(UUID senderUuid) {
		this.senderUuid = senderUuid;
    }
	
	public Packet(Target... targets) {
		this.targets = targets;
    }
	
	public Packet(UUID senderUuid, Target... targets) {
		this.senderUuid = senderUuid;
		this.targets = targets;
    }
}
