package multiplayerserver.targets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import multiplayerserver.ClientInformation;
import multiplayerserver.HasUUID;
import multiplayerserver.Protocol;
import multiplayerserver.Server;
import multiplayerserver.packets.Packet;

public class TargetRegistry {
	private final Map<String, Function<Server, List<ClientInformation>>> resolvers = new HashMap<>();
    private final Server server;
	
	public TargetRegistry(Server server) {
		this.server = server;
		
        registerBuiltInTargets();
	}
	
	private void registerBuiltInTargets() {
		register(Target.ALL, s -> new ArrayList<>(s.getClients())); //All clients //TODO: make sure this doesn't send back to original client
		
		register(Target.SERVER, s -> Collections.emptyList());
		
		register(Target.HOST_CLIENT, s -> {
			ClientInformation host = s.getHostClient();
			return host != null ? List.of(host) : Collections.emptyList();
		});
		
		register(Target.ALL_BUT_HOST_CLIENT, s -> s.getClients().stream()
			.filter(c -> !c.equals(s.getHostClient()))
			.collect(Collectors.toList()));
	}
	
	public void register(Target target, Function<Server, List<ClientInformation>> resolver) {
        resolvers.put(target.getName(), resolver);
    }
	
	public List<ClientInformation> resolve(Target target) {
		if (target == null) { //TODO: See if this is correct for null checking, or if we want to do something else when target is null.
			return Collections.emptyList();
		}
        Function<Server, List<ClientInformation>> resolver = resolvers.get(target.getName());
        if (resolver == null) {
            throw new IllegalArgumentException("Unknown target: " + target.getName());
        }
        return resolver.apply(server);
    }
	
	public void sendToTargets(List<? extends HasUUID> targets, Packet packet, Protocol protocol) {
		server.sendToTargets(targets, packet, protocol);
	}
}
