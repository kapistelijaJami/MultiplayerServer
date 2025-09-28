package multiplayerserver.packets;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Tracks all the different packet classes, has packet specific handlers, and serializes and parses packets.
 */
public class PacketRegistry {
	private final Map<String, Class<? extends Packet>> classNameToClass = new HashMap<>();
	private final Map<Class<? extends Packet>, Consumer<? extends Packet>> handlers = new HashMap<>();
	
	private Consumer<Packet> defaultHandler;
    private Consumer<Packet> globalHandler;
	
	private final Gson gson = new Gson();
	
	private boolean disableWarnings = false;
	
	public PacketRegistry() {
		registerBuiltInPackets();
	}
	
	private void registerBuiltInPackets() {
		registerPacket(SendUuid.class);
	}
	
	/**
	 * Register a packet for packet parsing.
	 * Server doesn't need to register packets that are just forwarded to clients.
	 * Need to call register() if you want to react to receiving a packet type.
	 * Don't need to call this if you call register().
	 * 
	 * Required for when you want to react to a packet with a global or default handler,
	 * not needed otherwise. If a client can receive unregistered packets that you don't
	 * want to react to, you can disable warnings, or just register them using registerPacket().
	 * 
	 * Custom packet classes need to extend Packet.
	 * <p>
	 * Usage:
	 * <pre>
	 * <code>registry.registerPacket(MovePacket.class)</code>
	 * <code>registry.registerPacket(ChatPacket.class)</code>
	 * </pre>
	 * @param <T>
	 * @param clazz 
	 */
	public <T extends Packet> void registerPacket(Class<T> clazz) {
        if (classNameToClass.containsKey(clazz.getName())) return;
		
		String className = clazz.getName();
        classNameToClass.put(className, clazz);
    }
	
	/**
	 * Register a packet and set a handler for that packet type.
	 * Needs to be called for all the packets you want to handle in that client/server.
	 * Server doesn't need to register packets that are just forwarded to clients.
	 * Use separate PacketRegistry instance for server and clients.
	 * <p>
	 * If you don't want to handle a packet, but want to be able to parse it,
	 * (for example for default/global handler) you need to call registerPacket()
	 * on the packet type.
	 * <p>
	 * Packet classes need to extend Packet.
	 * <p>
	 * Usage: registry.register(MovePacket.class, handlerFunction);
	 * <p>
	 * Create handlerFunction (Consumer) with one of these ways (it accepts a Packet subclass):
	 * <ul>
	 * <li>Lambda expression:
	 * <ul>
	 * <li>One-liner: <code>movePacket -> game.movePlayer(movePacket);</code></li>
	 * <p>
	 * <li>In block form:</li>
	 * 
	 * <pre>{@code
	 *     Consumer<MovePacket> handlerFunction = movePacket -> {
	 *         game.movePlayer(movePacket);
	 *     };}</pre>
	 * </ul>
	 * </li>
	 * <li>Reference a method that has the same input and return value.
	 * <ul>
	 *  <li>You can reference it statically with <code>ClassName::methodName</code></li>
	 *  <li>and non-statically with <code>this::methodName</code> or <code>objectVariable::methodName</code></li>
	 * </ul>
	 * </li>
	 * </ul>
	 * Like this:
	 * <pre>
	 *  <code>registry.register(MovePacket.class, move -> game.movePlayer(move));</code>
	 *  <code>registry.register(ChatPacket.class, packetHandler::addChatMessage);</code>
	 * </pre>
	 * <code>handlerFunction</code> can also be null (or just call registerPacket), in
	 * which case you can set up global handler function, or a default handler function.
	 * You can also use only the global function.
	 * @param <T>
	 * @param clazz 
	 * @param handler 
	 */
	public <T extends Packet> void register(Class<T> clazz, Consumer<T> handler) {
		if (handlers.containsKey(clazz)) return;
		
		if (!classNameToClass.containsKey(clazz.getName())) {
			String className = clazz.getName();
			classNameToClass.put(className, clazz);
		}
		
		if (handler == null) {
			return;
		}
        handlers.put(clazz, handler);
	}
	
	/**
	 * Default handler will be called if no handler is registered for packet type, or it's null.
	 * Packet type needs to be registered with register() or registerPacket().
	 * @param handler 
	 */
	public void setDefaultHandler(Consumer<Packet> handler) {
        this.defaultHandler = handler;
    }
	
	/**
	 * Global handler will be called for every packet received.
	 * Packet type needs to be registered with register() or registerPacket().
	 * @param handler 
	 */
    public void setGlobalHandler(Consumer<Packet> handler) {
        this.globalHandler = handler;
    }
	
	/**
	 * Converts packet to JSON String.
	 * Has packet className at the start, separated with <code>':'</code> -character from the JSON String.
	 * Throws an IllegalArgumentException if packet type not registered.
	 * @param packet
	 * @return 
	 */
	public String serialize(Packet packet) {
        String json = gson.toJson(packet, packet.getClass());
        return packet.getClass().getName() + ":" + json;
    }
	
	/**
	 * Takes the payload String that has "className:" + "{jsonData}" and parses it into a Packet.
	 * Gets the packet type from the registered className.
	 * Prints a warning and returns null if packet type not registered.
	 * @param payload
	 * @return Parsed packet or null if packet type is not registered.
	 */
	public Packet parsePacket(String payload) throws JsonSyntaxException {
		String[] parts = payload.split(":", 2);
        String className = parts[0];
        Class<? extends Packet> clazz = classNameToClass.get(className);
		
		if (clazz == null) {
			if (!disableWarnings) {
				System.err.println("Warning: Received an unregistered packet: " + className + ". Ignoring it.");
			}
			return null;
		}
		
		return gson.fromJson(parts[1], clazz);
	}
	
	public boolean isPacketRegistered(String payload) {
		String[] parts = payload.split(":", 2);
		return classNameToClass.containsKey(parts[0]);
	}
	
	public Packet parseAsBasePacket(String payload) throws JsonSyntaxException {
		String[] parts = payload.split(":", 2);
		return gson.fromJson(parts[1], BasePacket.class);
	}
	
	/**
	 * If warnings are not disabled, then it prints a warning when an unregistered packet is received.
	 * @param b 
	 */
	public void setDisableWarnings(boolean b) {
		disableWarnings = b;
	}
	
	/**
	 * Calls the registered handler with the packet for this packet type.
	 * @param packet 
	 */
	public void callHandler(Packet packet) {
		if (packet == null) return;
        if (globalHandler != null) globalHandler.accept(packet);
		
		Consumer<Packet> handler = (Consumer<Packet>) handlers.get(packet.getClass());
		
        if (handler != null) {
            handler.accept(packet);
        } else if (defaultHandler != null) {
            defaultHandler.accept(packet);
        }
	}
}
