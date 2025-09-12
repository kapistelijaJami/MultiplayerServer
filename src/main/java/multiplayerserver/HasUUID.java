package multiplayerserver;

import java.util.UUID;

/**
 * Implement this in your own clients/players for easy targeting with filtering on some game specific property.
 * Then you can just send them in a list to server's sendToTargets(), and server sends them to the clients straight from your list.
 */
public interface HasUUID {
	public UUID getUuid();
}
