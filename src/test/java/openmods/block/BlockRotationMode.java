package openmods.block;

import openmods.geometry.Orientation;

public enum BlockRotationMode {
    FOUR_DIRECTIONS;

    public final int mask = 3;

    public Orientation fromValue(int value) { return Orientation.values()[value]; }
    public int toValue(Orientation dir) { return dir.ordinal(); }
    public boolean isPlacementValid(Orientation dir) { return true; }
}
