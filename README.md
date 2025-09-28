# Multiplayer Networking Library

MultiplayerServer is a lightweight Java networking library for simple packet sending over the internet. You can create a `Server` and `Clients` and send custom packets easily.
It abstracts away TCP/UDP socket handling, packet serialization, and client management so you can focus on game logic.

- TCP (reliable messaging) & UDP (fast messaging) support
- Packet registry with automatic serialization and deserialization using Gson
- Flexible packet handling with lambdas or method references
- Targeting system for sending to groups of clients with a single Target (all, server, own team, host client, etc.)


## Getting Started

### 1. Define your packets
All packets extend the `Packet` base class.  
Example: A simple movement packet:

```java
public class MovePacket extends Packet {
    public double x, y;

    public MovePacket(double x, double y, Target... targets) {
        super(targets);
        this.x = x;
        this.y = y;
    }
}
```
Packets only need the data fields and a constructor.

### 2. Register handlers for specific packets
Use `PacketRegistry` to register your packet types and attach handlers.  
Use separate `PacketRegistry` instance for server and clients.  
When receiving a packet, the packet subclass will be parsed and passed to the handler.

```java
PacketRegistry packetRegistry = new PacketRegistry();

//Register MovePacket
packetRegistry.register(MovePacket.class, move -> {
    System.out.println("Player (" + move.senderUuid + ") moved to: " + move.x + ", " + move.y);
});

//Register ChatPacket (where packetHandler is your custom class with methods for each packet type for example)
packetRegistry.register(ChatPacket.class, packetHandler::addChatMessage);
```

### 3. Start a server
```java
server = new Server(port, packetRegistry);
try {
    server.start(); //Will start new threads for TCP and UDP
} catch (IOException e) {
    e.printStackTrace(System.err);
}
```

### 4. Connect a client
```java
try {
    Client client = new Client(InetAddress.getByName(serverIp), serverPort, packetRegistry);
    client.connect(); //Will start new threads for TCP and UDP
} catch (IOException e) {
    e.printStackTrace(System.err);
}
```

### 5. Send a packet
```java
MovePacket movePacket = new MovePacket(player.getX(), player.getY(), Target.ALL);
client.sendPacket(movePacket, Protocol.UDP);
```

## Targeting system
`Server` has `TargetRegistry` object which it uses to pass packets onwards to specific clients based on the `Target` that was sent with the packet.

There are few pre-defined targets, like `Target.ALL`, which sends a packet to everyone except the client that sent the packet.
There is `Target.HOST_CLIENT`, which sends the packet to the client that first connected to the server etc.
And there's a method for creating a `UUID` target which sends a packet to specific client (`Target.createUUIDTarget(uuid)`).

But you can define your own custom targets and register their resolvers with `TargetRegistry`.
The resolver is a `BiFunction` which gets the `Target` and a `ResolveContext` (which has `Server` and `Packet`, and packet has `senderUuid`).
It returns a list of objects which implement `HasUUID` interface (so they have `getUuid()` method). This makes it simple to return a list of your own objects.

```java
public class Targets {
    //For ex. Set up a public static variable for the Target:
    public static final Target OWN_TEAM = new Target("ownTeam");

    //Can also pass data for cases like Target based on team id:
    public static Target createTeamIdTarget(int teamId) {
  	    return new Target("teamIdTarget", String.valueOf(teamId));
  	}
}

//In server creation code:
TargetRegistry targetRegistry = server.getTargetRegistry();

//Registering an example resolver which returns a list of players that belong to the sender's team:
//(You don't have to filter out the packet sender yourself)
targetRegistry.register(Targets.OWN_TEAM, (target, ctx) -> {
    Player player = game.getPlayerByUUID(ctx.packet.senderUuid);
    if (player == null) {
        return new ArrayList<>();
    }
    Team team = player.getTeam();
    if (team == null) {
        return new ArrayList<>();
    }
    return team.getPlayers();
});

//Registering an example resolver which returns a list of players that belong to a specific team based on its id:
//(When registering targets, only the 'type' String needs to be correct for the first argument, so team id -1 works)
targetRegistry.register(Targets.createTeamIdTarget(-1), (target, ctx) -> {
    Optional<Team> team = game.getTeams().stream()
            .filter(t -> t.getId() == Integer.parseInt(target.getValue()))
            .findFirst();
    
    if (team.isEmpty()) {
        return new ArrayList<>();
    }
    
    return team.get().getPlayers();
});
```
Here a `Player` implements `HasUUID` interface, so I can just return a list of `Player` objects.

Now when sending a packet you can pass these as the `Target`, and server will correctly pass it forwards to correct clients:
```java
ChatPacket chatPacket = new ChatPacket("Hello team!", Targets.OWN_TEAM);
client.sendPacket(chatPacket, Protocol.TCP);

ChatPacket chatPacket = new ChatPacket("ff already", Targets.createTeamIdTarget(opponentTeam.getId()));
client.sendPacket(chatPacket, Protocol.TCP);
```
All packets will go to the server regardless of what the `Target` is, and will trigger a handler if it is registered for that `Packet` type.  
You can also specify `Target.SERVER`, which could make the code more readable in the case you want to only send a packet to the server, or just have no target at all.  
You can also send multiple targets with the `Packet`. `Server` will pass it forwards to all clients that belong to any of the `Targets`.

---

See Javadoc for extra info.
