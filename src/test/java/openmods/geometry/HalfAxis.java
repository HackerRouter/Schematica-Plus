package openmods.geometry;

import net.minecraftforge.common.util.ForgeDirection;

public enum HalfAxis {
    NEG_X(ForgeDirection.WEST), POS_X(ForgeDirection.EAST), NEG_Y(ForgeDirection.DOWN), POS_Y(ForgeDirection.UP),
    NEG_Z(ForgeDirection.NORTH), POS_Z(ForgeDirection.SOUTH);

    public final ForgeDirection dir;

    HalfAxis(ForgeDirection dir) { this.dir = dir; }

    public static HalfAxis fromDirection(ForgeDirection dir) {
        for (HalfAxis axis : values()) if (axis.dir == dir) return axis;
        return null;
    }
}
