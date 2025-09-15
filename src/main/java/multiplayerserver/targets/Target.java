package multiplayerserver.targets;

import java.util.UUID;

public class Target {
	private final String type; //This is the main group name, has to be unique
	private final String value; //Can store other information here, for example UUID, or team id etc.
	
	public Target(String type) {
        this(type, null);
    }
	
	public Target(String type, String value) {
        this.type = type;
        this.value = value;
    }
	
	public String getType() {
		return type;
	}
	
	public String getValue() {
		return value;
	}
	
	public static final Target ALL = new Target("all");
    public static final Target SERVER = new Target("server");
    public static final Target HOST_CLIENT = new Target("hostClient");
    public static final Target ALL_BUT_HOST_CLIENT = new Target("allButHostClient");
	
	public static Target createUUIDTarget(UUID uuid) {
		if (uuid == null) { //Can be null when registering the Target
			return new Target("UUIDTarget", null);
		}
		
		return new Target("UUIDTarget", uuid.toString());
	}
}
