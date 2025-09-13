package multiplayerserver.targets;

import java.util.UUID;

public class Target {
	private final String groupName;
	private UUID uuid;
	
	public Target(String groupName) {
        this.groupName = groupName;
    }
	
	/**
	 * Sends packet to specific UUID.
	 * These don't need to be registered in TargetRegistry.
	 * @param uuid 
	 */
	public Target(UUID uuid) {
		this.groupName = "specificUuid";
        this.uuid = uuid;
    }
	
	public String getGroupName() {
		return groupName;
	}
	
	public UUID getUuid() {
		return uuid;
	}
	
	public static final Target ALL = new Target("all");
    public static final Target SERVER = new Target("server");
    public static final Target HOST_CLIENT = new Target("hostClient");
    public static final Target ALL_BUT_HOST_CLIENT = new Target("allButHostClient");
    public static final Target UUID = new Target("specificUuid");
}
