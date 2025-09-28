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

/**
 * Keeps track of different targets for simple packet delivery.
 * Resolver is a BiFunction which gets the Target and a ResolveContext as a parameter.
 * ResolveContext contains the Server object and Packet (which has the senderUuid).
 * Resolver can use the server to get clients and it returns a list of objects
 * (which implement HasUUID interface) who this target applies to. (Clients/players etc.)
 * <p>
 * Built-in targets (which you don't need to register yourself) are:
 *     ALL, SERVER, HOST_CLIENT, ALL_BUT_HOST_CLIENT, and UUIDTarget.
 * <p>
 * Can register new targets and resolvers like this:
 * <pre>{@code
 * targetRegistry.register(customTarget, (target, ctx) -> {
 *     return ctx.server.getClients();
 * });}</pre>
 * <p>
 * You can also use your own variables and objects inside the code block that were defined before the
 * lambda function and return a list of your own objects, as long as they implement HasUUID interface.
 */
public class TargetRegistry {
	private final Map<String, BiFunction<Target, ResolveContext, List<? extends HasUUID>>> resolvers = new HashMap<>();
	
	private boolean disableWarnings = false;
	
	public TargetRegistry() {
        registerBuiltInTargets();
	}
	
	private void registerBuiltInTargets() {
		register(Target.ALL, (t, ctx) -> new ArrayList<>(ctx.server.getClients()));
		
		register(Target.SERVER, (t, ctx) -> Collections.emptyList()); //TODO: See what to do with server, since it's not a client, and this returns a list of clients.
																	  //Could either remove the server target, or have it not do anything, like it is now.
		
		register(Target.HOST_CLIENT, (t, ctx) -> {
			ClientInformation host = ctx.server.getHostClient();
			return host != null ? List.of(host) : Collections.emptyList();
		});
		
		register(Target.ALL_BUT_HOST_CLIENT, (t, ctx) -> ctx.server.getAllClientsExcept(ctx.server.getHostClient().getUuid()));
		
		register(Target.createUUIDTarget(null), (t, ctx) -> {
			ClientInformation client = ctx.server.getClient(UUID.fromString(t.getValue()));
			return client != null ? List.of(client) : Collections.emptyList();
		});
	}
	
	/**
	 * Register a resolver for a target.
	 * Resolver is a BiFunction, it takes Target and ResolveContext, and
	 * returns a list of objects that implement HasUUID interface.
	 * ResolveContext has Server and Packet (which has the senderUuid).
	 * @param target
	 * @param resolver 
	 */
	public void register(Target target, BiFunction<Target, ResolveContext, List<? extends HasUUID>> resolver) {
        resolvers.put(target.getType(), resolver);
    }
	
	private <T extends HasUUID> List<T> resolveTargets(Target target, ResolveContext ctx) {
		if (target == null) {
			return new ArrayList<>();
		}
		
        BiFunction<Target, ResolveContext, List<? extends HasUUID>> resolver = resolvers.get(target.getType());
		
        if (resolver == null) {
			if (!disableWarnings) {
				System.err.println("Warning: Unknown target: " + target.getType());
			}
			return Collections.EMPTY_LIST;
        }
		
        return (List<T>) resolver.apply(target, ctx);
    }
	
	public <T extends HasUUID> List<T> resolveTargets(ResolveContext ctx, Target... targets) {
		if (targets == null || targets.length == 0) {
			return new ArrayList<>();
		}
		
		List<T> list = new ArrayList<>();
		
		for (Target target : targets) {
			addAllIfAbsent(list, resolveTargets(target, ctx));
		}
		return list;
	}
	
	private <T extends HasUUID> void addAllIfAbsent(List<T> list, List<T> newItems) {
		Set<UUID> existing = list.stream()
				.map(item -> item.getUuid())
				.collect(Collectors.toSet());
		
		for (T newItem : newItems) {
			if (existing.add(newItem.getUuid())) { //Add returns false if already present
				list.add(newItem);
			}
		}
	}
	
	/**
	 * If warnings are not disabled, then it prints a warning when an unknown target is received.
	 * @param b 
	 */
	public void setDisableWarnings(boolean b) {
		disableWarnings = b;
	}
}
