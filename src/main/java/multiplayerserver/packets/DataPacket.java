package multiplayerserver.packets;

import multiplayerserver.targets.Target;

public abstract class DataPacket extends Packet {
	public int dataLength;			//How many bytes are at the end of the packet
	private transient byte[] data;	//Not serialized by Gson
	
	public DataPacket(byte[] data, Target... targets) {
		super(targets);
        this.data = data;
        this.dataLength = (data != null) ? data.length : 0;
    }
	
    public byte[] getData() {
        return data;
    }
	
    public void setData(byte[] data) {
        this.data = data;
        this.dataLength = (data != null) ? data.length : 0;
    }
}
