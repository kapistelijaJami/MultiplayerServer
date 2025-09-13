package multiplayerserver.targets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import multiplayerserver.ClientInformation;
import multiplayerserver.HasUUID;
import multiplayerserver.Protocol;
import multiplayerserver.Server;
import multiplayerserver.packets.Packet;

/**
 * Keeps track of different targets for simple packet delivery.
 * Resolver is a BiFunction which gets server and target as a parameter.
 * It can use the server to get clients and it returns a list of objects who
 * this target applies to that implement HasUUID interface. (Clients/players etc.)
 * <p>
 * Can register new targets and resolvers like this:
 * <pre>{@code
 * targetRegistry.register(customTarget, (s, t) -> {
 *     return s.getClients();
 * });}</pre>
 * <p>
 * You can also use your own variables and objects inside the code block that were defined before the
 * lambda function and return a list of your own objects, as long as they implement HasUUID interface.
 */
public class TargetRegistry {
	private final Map<String, BiFunction<Server, Target, List<HasUUID>>> resolvers = new HashMap<>();
    private final Server server;
	
	public TargetRegistry(Server server) {
		this.server = server;
		
        registerBuiltInTargets();
	}
	
	private void registerBuiltInTargets() {
		register(Target.ALL, (s, t) -> new ArrayList<>(s.getClients()));
		
		register(Target.SERVER, (s, t) -> Collections.emptyList()); //TODO: See what to do with server, since it's not a client, and this returns a list of clients.
																	//Could either remove the server target, or have it not do anything, like it is now.
		
		register(Target.HOST_CLIENT, (s, t) -> {
			ClientInformation host = s.getHostClient();
			return host != null ? List.of(host) : Collections.emptyList();
		});
		
		register(Target.ALL_BUT_HOST_CLIENT, (s, t) -> s.getClients().stream()
			.filter(c -> !c.equals(s.getHostClient()))
			.collect(Collectors.toList()));
		
		register(Target.UUID, (s, t) -> {
			ClientInformation client = s.getClient(t.getUuid());
			return client != null ? List.of(client) : Collections.emptyList();
		});
	}
	
	public void register(Target target, BiFunction<Server, Target, List<HasUUID>> resolver) {
        resolvers.put(target.getGroupName(), resolver);
    }
	
	public List<HasUUID> resolveTargets(Target target) {
		if (target == null) { //TODO: See if we want to do null checking, or if we want to do something else when target is null, probably messages meant for server will be null.
			return Collections.emptyList();
		}
		
        BiFunction<Server, Target, List<HasUUID>> resolver = resolvers.get(target.getGroupName());
		
        if (resolver == null) {
            throw new IllegalArgumentException("Unknown target: " + target.getGroupName());
        }
		
        return resolver.apply(server, target);
    }
	
	public void sendToTargets(List<? extends HasUUID> targets, Packet packet, Protocol protocol) {
		server.sendToTargets(targets, packet, protocol);
	}
}
