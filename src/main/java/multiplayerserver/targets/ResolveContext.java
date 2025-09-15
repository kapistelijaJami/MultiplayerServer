package multiplayerserver.targets;

import multiplayerserver.Server;
import multiplayerserver.packets.Packet;

public class ResolveContext {
	public final Server server;
	public final Packet packet;
	
	public ResolveContext(Server server, Packet packet) {
		this.server = server;
		this.packet = packet;
	}
}
