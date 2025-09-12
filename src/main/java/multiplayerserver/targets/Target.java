package multiplayerserver.targets;

public class Target {
	private final String name;
	
	public Target(String name) {
        this.name = name;
    }
	
	public String getName() {
		return name;
	}
	
	public static final Target ALL = new Target("all");
    public static final Target SERVER = new Target("server");
    public static final Target HOST_CLIENT = new Target("hostClient");
    public static final Target ALL_BUT_HOST_CLIENT = new Target("allButHostClient");
}
