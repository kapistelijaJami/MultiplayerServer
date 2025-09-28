package multiplayerserver.packets;

import multiplayerserver.targets.Target;

/**
 * Example packet that has data field which is sent as raw data and is not serialized by Gson.
 * It extends DataPacket, which provides the transient data field.
 */
public class RawDataPacket extends DataPacket {
	public String extraText;
	
	public RawDataPacket(byte[] data, String extraText, Target... targets) {
		super(data, targets); //Pass the raw data to the constructor of the super class
		
		this.extraText = extraText;
	}
}
