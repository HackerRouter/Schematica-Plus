// Carpenter's Blocks shapes and facings turned with the schematic, by HackerRouter, 2026.
package com.github.lunatrius.schematica.compat;

import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.lunatrius.schematica.api.ISchematicVisualAdapter;
import com.github.lunatrius.schematica.util.SchematicTransform;

import cpw.mods.fml.common.registry.GameData;

/**
 * Carpenter's Blocks (GTNH fork) keep shape and facing in TEBase's cbMetadata, laid out per block (data/*.java), and
 * the cover, dye and overlay of each face and its chisel design per side. Slopes and stairs are looked up by their
 * shape family and facings after the turn; the other blocks have their direction bits turned. Faces move with the
 * sides they are on. Shapes without a turned counterpart (a prism pointing down) keep their data.
 */
final class CarpentersVisualAdapter implements ISchematicVisualAdapter {
    static final String TILE = "com.carpentersblocks.tileentity.TEBase";
    enum Kind { SLOPE, STAIRS, SLAB, SIDED, DAYLIGHT, GARAGE, LEVER, COLLAPSIBLE, HATCH, DOOR, GATE, BED, SAFE, NONE }

    private static final ForgeDirection D = ForgeDirection.DOWN, U = ForgeDirection.UP, N = ForgeDirection.NORTH,
        S = ForgeDirection.SOUTH, W = ForgeDirection.WEST, E = ForgeDirection.EAST;

    /** Slope ids: shape family (wedge sides and wedges are one family) and facings; stairs use the first 28. */
    private static final int WEDGE = 0, INT = 1, EXT = 2, OBL_INT = 3, OBL_EXT = 4, PRISM = 5, PRISM_1P = 6, PRISM_2P = 7,
        PRISM_3P = 8, PRISM_4P = 9, PRISM_WEDGE = 10;
    private static final int[] FAMILY = new int[65];
    private static final ForgeDirection[][] FACINGS = new ForgeDirection[65][];

    static {
        ForgeDirection[][] corners = {{S, E}, {N, W}, {N, E}, {S, W}};
        for (int i = 0; i < 4; i++) slope(i, WEDGE, corners[i]);
        ForgeDirection[] sides = {N, S, W, E};
        for (int i = 0; i < 4; i++) {
            slope(4 + i, WEDGE, D, sides[i]);
            slope(8 + i, WEDGE, U, sides[i]);
        }
        int[] families = {INT, EXT, OBL_INT, OBL_EXT};
        for (int f = 0; f < 4; f++) {
            for (int i = 0; i < 4; i++) {
                slope(12 + f * 8 + i, families[f], D, corners[i][0], corners[i][1]);
                slope(16 + f * 8 + i, families[f], U, corners[i][0], corners[i][1]);
            }
        }
        slope(44, PRISM, D);
        slope(45, PRISM, U);
        for (int i = 0; i < 4; i++) slope(46 + i, PRISM_1P, U, sides[i]);
        slope(50, PRISM_2P, U, N, S);
        slope(51, PRISM_2P, U, W, E);
        for (int i = 0; i < 4; i++) slope(52 + i, PRISM_2P, U, corners[i][0], corners[i][1]);
        slope(56, PRISM_3P, U, N, W, E);
        slope(57, PRISM_3P, U, S, W, E);
        slope(58, PRISM_3P, U, N, S, W);
        slope(59, PRISM_3P, U, N, S, E);
        slope(60, PRISM_4P, U, N, S, W, E);
        for (int i = 0; i < 4; i++) slope(61 + i, PRISM_WEDGE, U, sides[i]);
    }

    private static void slope(int id, int family, ForgeDirection... facings) {
        FAMILY[id] = family;
        FACINGS[id] = facings;
    }

    private static ForgeDirection turn(char operation, ForgeDirection side) { return SchematicTransform.direction(operation, side); }

    private static int turn(char operation, int ordinal) {
        return ordinal >= 0 && ordinal < 6 ? turn(operation, ForgeDirection.getOrientation(ordinal)).ordinal() : ordinal;
    }

    /** A slope or stairs id after the operation; the id itself when the turned shape does not exist. */
    static int shape(int id, int count, char operation) {
        if (id < 0 || id >= count) return id;
        int[] wanted = flags(FACINGS[id], operation);
        for (int other = 0; other < count; other++) {
            if (FAMILY[other] == FAMILY[id] && Arrays.equals(flags(FACINGS[other], '\0'), wanted)) return other;
        }
        return id;
    }

    private static int[] flags(ForgeDirection[] facings, char operation) {
        int[] result = new int[6];
        for (ForgeDirection facing : facings) result[(operation == '\0' ? facing : turn(operation, facing)).ordinal()] = 1;
        return result;
    }

    /** {+X, +Z, -X, -Z} (Hinge facing) and {-Z, +Z, -X, +X} (Hatch dir). */
    private static final ForgeDirection[] HINGE = {E, S, W, N}, HATCH = {N, S, W, E};

    private static int index(ForgeDirection[] values, ForgeDirection side) {
        for (int i = 0; i < values.length; i++) if (values[i] == side) return i;
        return -1;
    }

    /** Beds and safes store the player's rotation; the block faces opposite to Direction.directionToFacing. */
    private static final ForgeDirection[] ROTATION = {N, E, S, W};

    static int data(Kind kind, int data, char operation) {
        switch (kind) {
            case SLOPE: return shape(data, 65, operation);
            case STAIRS: return shape(data, 28, operation);
            case SLAB: {
                int[] map = {4, 5, 0, 1, 2, 3};
                if (data < 1 || data > 6) return data;
                int turned = turn(operation, map[data - 1]);
                for (int i = 0; i < 6; i++) if (map[i] == turned) return i + 1;
                return data;
            }
            case SIDED: case COLLAPSIBLE: {
                int result = data & ~0x7 | turn(operation, data & 0x7);
                return kind == Kind.COLLAPSIBLE ? quadrants(result, data & 0x7, operation) : result;
            }
            case DAYLIGHT: return data & ~0x380 | turn(operation, (data & 0x380) >> 7) << 7;
            case GARAGE: return data & ~0x70 | turn(operation, (data & 0x70) >> 4) << 4;
            case LEVER: {
                int result = data & ~0x7 | turn(operation, data & 0x7);
                return operation == 'Y' && (data & 0x7) < 2 ? result ^ 0x40 : result;
            }
            case HATCH: {
                if (operation == 'y') return data ^ 0x8;
                int dir = index(HATCH, turn(operation, HATCH[(data & 0x60) >> 5]));
                return dir < 0 ? data : data & ~0x60 | dir << 5;
            }
            case DOOR: {
                int result = data;
                if (operation == 'y') return data ^ 0x80;
                int facing = index(HINGE, turn(operation, HINGE[(data & 0x30) >> 4]));
                if (facing >= 0) result = result & ~0x30 | facing << 4;
                return Character.isLowerCase(operation) ? result ^ 0x8 : result;
            }
            case GATE: {
                boolean onX = (data & 0x20) == 0, positive = (data & 0x10) == 0;
                ForgeDirection open = onX ? (positive ? S : N) : (positive ? E : W);
                ForgeDirection turned = turn(operation, open);
                if (turned.offsetY != 0) return data;
                boolean nowOnX = turned.offsetZ != 0, nowPositive = turned.offsetX + turned.offsetZ > 0;
                return data & ~0x30 | (nowOnX ? 0 : 0x20) | (nowPositive ? 0 : 0x10);
            }
            case BED: case SAFE: {
                int shift = kind == Kind.BED ? 13 : 0;
                int rot = index(ROTATION, turn(operation, ROTATION[(data >> shift) & 0x3]));
                return rot < 0 ? data : data & ~(0x3 << shift) | rot << shift;
            }
            default: return data;
        }
    }

    /** Collapsible block corner depths (5 bits each: XZNN at 18, XZNP at 13, XZPN at 8, XZPP at 3) on a floor or ceiling. */
    private static int quadrants(int data, int dir, char operation) {
        if (dir > 1 || operation != 'Y' && operation != 'x' && operation != 'z') return data;
        int[] shifts = {18, 13, 8, 3};
        int[][] corners = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
        int result = data;
        for (int shift : shifts) result &= ~(0x1F << shift);
        for (int i = 0; i < 4; i++) {
            double[] moved = SchematicTransform.point(operation, corners[i][0], 0, corners[i][1], 0, 0, 0);
            for (int j = 0; j < 4; j++) {
                if (corners[j][0] == (int) moved[0] && corners[j][1] == (int) moved[2]) result |= (data >> shifts[i] & 0x1F) << shifts[j];
            }
        }
        return result;
    }

    /** Covers (0-5), dyes (7-12) and overlays (14-19) of each side, and the side chisel designs; 6, 13 and 20 are the block's own. */
    static void faces(NBTTagCompound tag, char operation) {
        NBTTagList list = tag.getTagList("cbAttrList", NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound attribute = list.getCompoundTagAt(i);
            int id = attribute.getByte("cbAttribute") & 255;
            int group = id < 7 ? 0 : id < 14 ? 7 : id < 21 ? 14 : -1;
            if (group < 0 || id - group == 6) continue;
            attribute.setByte("cbAttribute", (byte) (group + turn(operation, id - group)));
        }
        String[] designs = new String[6];
        for (int side = 0; side < 6; side++) designs[side] = tag.getString("cbChiselDesign_" + side);
        for (int side = 0; side < 6; side++) {
            if (tag.hasKey("cbChiselDesign_" + side)) tag.setString("cbChiselDesign_" + turn(operation, side), designs[side]);
        }
    }

    static Kind kind(String name) {
        if (name == null || !name.startsWith("CarpentersBlocks:blockCarpenters")) return Kind.NONE;
        switch (name.substring("CarpentersBlocks:blockCarpenters".length())) {
            case "Slope": return Kind.SLOPE;
            case "Stairs": return Kind.STAIRS;
            case "Block": return Kind.SLAB;
            case "Button": case "PressurePlate": case "Torch": case "Ladder": return Kind.SIDED;
            case "CollapsibleBlock": return Kind.COLLAPSIBLE;
            case "DaylightSensor": return Kind.DAYLIGHT;
            case "GarageDoor": return Kind.GARAGE;
            case "Lever": return Kind.LEVER;
            case "Hatch": return Kind.HATCH;
            case "Door": return Kind.DOOR;
            case "Gate": return Kind.GATE;
            case "Bed": return Kind.BED;
            case "Safe": return Kind.SAFE;
            default: return Kind.NONE;
        }
    }

    static void transform(Kind kind, NBTTagCompound tag, char operation) {
        faces(tag, operation);
        if (!tag.hasKey("cbMetadata")) return;
        boolean narrow = tag.hasKey("cbMetadata", NBT.TAG_SHORT);
        int data = narrow ? tag.getShort("cbMetadata") & 0xFFFF : tag.getInteger("cbMetadata");
        int turned = data(kind, data, operation);
        if (narrow) tag.setShort("cbMetadata", (short) turned);
        else tag.setInteger("cbMetadata", turned);
    }

    private static Kind kind(TileEntity tile) {
        try {
            Block block = tile.getBlockType();
            return kind(block == null ? null : GameData.getBlockRegistry().getNameForObject(block));
        } catch (RuntimeException e) {
            return Kind.NONE;
        }
    }

    @Override public String id() { return "plus:carpenters_blocks"; }
    @Override public boolean supports(TileEntity tile) { return Reflect.is(tile, TILE); }
    @Override public NBTTagCompound capture(TileEntity tile) { return null; }
    @Override public void restore(TileEntity tile, NBTTagCompound data) {}
    @Override public boolean replacesDescriptionPacket(TileEntity tile) { return false; }
    @Override public boolean transformsNBT(TileEntity tile) { return true; }

    @Override public void transformNBT(TileEntity tile, NBTTagCompound data, char operation) {
        transform(kind(tile), data, operation);
    }

    @Override public void transformPreview(TileEntity tile, char operation) {
        NBTTagCompound data = new NBTTagCompound();
        tile.writeToNBT(data);
        transform(kind(tile), data, operation);
        tile.readFromNBT(data);
    }
}
