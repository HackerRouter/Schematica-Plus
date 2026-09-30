package appeng.parts.networking;

import java.util.EnumSet;
import net.minecraftforge.common.util.ForgeDirection;

public class PartCable {
    public EnumSet<ForgeDirection> connections = EnumSet.of(ForgeDirection.NORTH, ForgeDirection.SOUTH);
    public final int[] channelsOnSide = {0, 0, 8, 8, 0, 0};
    public boolean powered = true;
    public Object network = new Object();
    public void writeToStream() { throw new AssertionError("Client capture must not query the server network"); }
}
