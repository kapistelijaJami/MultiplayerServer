package multiplayerserver.packets;

import java.util.UUID;

public class SendUuid extends Packet {
	public SendUuid(UUID uuid) {
		super(uuid);
	}
}
