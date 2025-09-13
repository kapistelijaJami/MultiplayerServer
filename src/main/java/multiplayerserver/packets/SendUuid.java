package multiplayerserver.packets;

import java.util.UUID;

public class SendUuid extends Packet {
	public int udpPort = -1;
	
	public SendUuid(UUID uuid, int udpPort) {
		super(uuid);
		
		this.udpPort = udpPort;
	}
}
