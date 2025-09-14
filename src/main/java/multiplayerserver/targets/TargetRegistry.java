package multiplayerserver.targets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
	private final Map<String, BiFunction<Server, Target, List<? extends HasUUID>>> resolvers = new HashMap<>();
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
		
		register(Target.ALL_BUT_HOST_CLIENT, (s, t) -> s.getAllClientsExcept(s.getHostClient().getUuid()));
		
		register(Target.UUID, (s, t) -> {
			ClientInformation client = s.getClient(t.getUuid());
			return client != null ? List.of(client) : Collections.emptyList();
		});
	}
	
	public void register(Target target, BiFunction<Server, Target, List<? extends HasUUID>> resolver) {
        resolvers.put(target.getGroupName(), resolver);
    }
	
	private <T extends HasUUID> List<T> resolveTargets(Target target) {
		if (target == null) {
			return new ArrayList<>();
		}
		
        BiFunction<Server, Target, List<? extends HasUUID>> resolver = resolvers.get(target.getGroupName());
		
        if (resolver == null) {
            throw new IllegalArgumentException("Unknown target: " + target.getGroupName());
        }
		
        return (List<T>) resolver.apply(server, target);
    }
	
	public <T extends HasUUID> List<T> resolveTargets(Target... targets) {
		if (targets == null || targets.length == 0) {
			return new ArrayList<>();
		}
		
		List<T> list = new ArrayList<>();
		
		for (Target target : targets) {
			addAllIfAbsent(list, resolveTargets(target));
		}
		return list;
	}
	
	private <T extends HasUUID> void addAllIfAbsent(List<T> list, List<T> newItems) {
		Set<UUID> existing = list.stream()
				.map(newItem -> newItem.getUuid())
				.collect(Collectors.toSet());
		
		for (T newItem : newItems) {
			if (existing.add(newItem.getUuid())) { //Add returns false if already present
				list.add(newItem);
			}
		}
	}
	
	public void sendToTargets(List<? extends HasUUID> targets, Packet packet, Protocol protocol) {
		server.sendToClients(targets, packet, protocol);
	}
}
