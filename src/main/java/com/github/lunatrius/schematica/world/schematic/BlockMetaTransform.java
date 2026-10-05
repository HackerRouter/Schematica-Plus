// Metadata of rotated and mirrored 1.7.10 blocks (Litematica's BlockState rotate/mirror), by HackerRouter, 2026.
package com.github.lunatrius.schematica.world.schematic;

import java.lang.reflect.Field;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockEndPortalFrame;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockHugeMushroom;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockPistonExtension;
import net.minecraft.block.BlockQuartz;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockTripWireHook;
import net.minecraft.block.BlockVine;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.lunatrius.schematica.util.SchematicTransform;

import static net.minecraftforge.common.util.ForgeDirection.*;

/**
 * The metadata a vanilla-style block gets when the schematic is rotated (X, Y, Z: 90 degrees as
 * {@link SchematicTransform}) or mirrored (x, y, z). Directions stored in the metadata are turned with the blocks;
 * a state the block cannot hold afterwards (a stair or a wall torch turned to face down) keeps its metadata.
 * Mod blocks extending these vanilla classes use the same layouts. Returns -1 for blocks this table does not know.
 */
public final class BlockMetaTransform {
    /** Direction encoding of BlockDirectional (beds, gates, pumpkins, diodes, cocoa, hooks): 0 S, 1 W, 2 N, 3 E. */
    private static final ForgeDirection[] HORIZONTAL = {SOUTH, WEST, NORTH, EAST};
    /** Stairs: the direction the stair rises toward. */
    private static final ForgeDirection[] STAIRS = {EAST, WEST, SOUTH, NORTH};
    /** Torches, buttons and wall levers: the direction they point away from their support (5: standing). */
    private static final ForgeDirection[] ATTACHED = {UNKNOWN, EAST, WEST, SOUTH, NORTH, UP};
    /** Doors: the direction the placing player looked. */
    private static final ForgeDirection[] DOOR = {EAST, SOUTH, WEST, NORTH};
    /** Trapdoors: the side of their support they hang on. */
    private static final ForgeDirection[] TRAPDOOR = {NORTH, SOUTH, WEST, EAST};
    /** Rails: the two connected sides, then the ascending side for 2-5. */
    private static final ForgeDirection[][] RAILS = {
        {NORTH, SOUTH}, {EAST, WEST}, {EAST, WEST, EAST}, {EAST, WEST, WEST}, {NORTH, SOUTH, NORTH}, {NORTH, SOUTH, SOUTH},
        {SOUTH, EAST}, {SOUTH, WEST}, {NORTH, WEST}, {NORTH, EAST}};
    private static Field standingSign;

    /** Mod blocks (by class, subclasses included) with a horizontal direction in the low two metadata bits. */
    private static final java.util.Map<String, ForgeDirection[]> MOD_HORIZONTAL = new java.util.HashMap<>();
    /** Galacticraft machines: the direction the placing player looked. */
    private static final ForgeDirection[] GALACTICRAFT = {EAST, WEST, NORTH, SOUTH};

    static {
        String gc = "micdoodle8.mods.galacticraft.core.blocks.";
        for (String block : new String[] {"BlockMachine", "BlockMachine2", "BlockMachineTiered", "BlockCargoLoader", "BlockFuelLoader",
            "BlockOxygenCollector", "BlockOxygenCompressor", "BlockOxygenDistributor", "BlockOxygenSealer", "BlockRefinery"}) {
            MOD_HORIZONTAL.put(gc + block, GALACTICRAFT);
        }
        MOD_HORIZONTAL.put("micdoodle8.mods.galacticraft.planets.mars.blocks.BlockMachineMarsT2", GALACTICRAFT);
        MOD_HORIZONTAL.put(gc + "BlockSolar", new ForgeDirection[] {NORTH, SOUTH, WEST, EAST});
    }

    private BlockMetaTransform() {}

    private static ForgeDirection turn(char op, ForgeDirection side) {
        return side == UNKNOWN ? side : SchematicTransform.direction(op, side);
    }

    private static boolean horizontal(ForgeDirection side) { return side.offsetY == 0 && side != UNKNOWN; }

    private static int index(ForgeDirection[] table, ForgeDirection side) {
        for (int i = 0; i < table.length; i++) if (table[i] == side) return i;
        return -1;
    }

    /** A direction stored at `bits` of the metadata in `table`; unchanged when the turned direction is not in it. */
    private static int direction(int meta, int mask, int shift, ForgeDirection[] table, char op) {
        int value = (meta & mask) >> shift;
        if (value >= table.length || table[value] == UNKNOWN) return meta;
        int turned = index(table, turn(op, table[value]));
        return turned < 0 ? meta : (meta & ~mask) | (turned << shift);
    }

    /** A direction stored as a ForgeDirection ordinal in the low bits. */
    private static int ordinal(int meta, int mask, char op, boolean allowUp, boolean allowDown) {
        int value = meta & mask;
        if (value > 5) return meta;
        ForgeDirection turned = turn(op, ForgeDirection.getOrientation(value));
        if (turned == UP && !allowUp || turned == DOWN && !allowDown) return meta;
        return (meta & ~mask) | turned.ordinal();
    }

    /** A 0-15 rotation counted clockwise from above (standing signs, floor skulls). */
    public static int rotation16(int rotation, char op) {
        switch (op) {
            case 'Y': return (rotation + 4) & 15;
            case 'x': return (16 - rotation) & 15;
            case 'z': return (8 - rotation) & 15;
            default: return rotation;
        }
    }

    private static boolean standing(BlockSign sign) {
        try {
            if (standingSign == null) {
                standingSign = BlockSign.class.getDeclaredField("field_149967_b");
                standingSign.setAccessible(true);
            }
            return standingSign.getBoolean(sign);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    private static int axis(int meta, int mask, int y, int x, int z, char op) {
        int value = meta & mask;
        ForgeDirection axis = value == y ? UP : value == x ? EAST : value == z ? SOUTH : UNKNOWN;
        if (axis == UNKNOWN) return meta;
        ForgeDirection turned = turn(op, axis);
        int result = turned.offsetY != 0 ? y : turned.offsetX != 0 ? x : z;
        return (meta & ~mask) | result;
    }

    /** A stair is the union of a half block on its `half` side and one on its `facing` side, so the two may swap. */
    private static int stairs(int meta, char op) {
        ForgeDirection facing = turn(op, STAIRS[meta & 3]), half = turn(op, (meta & 4) != 0 ? UP : DOWN);
        if (!horizontal(facing)) { ForgeDirection swap = facing; facing = half; half = swap; }
        if (!horizontal(facing) || horizontal(half)) return meta;
        return (meta & ~7) | index(STAIRS, facing) | (half == UP ? 4 : 0);
    }

    private static int lever(int meta, char op) {
        int value = meta & 7;
        ForgeDirection facing, axis;
        switch (value) {
            case 0: facing = DOWN; axis = EAST; break;
            case 7: facing = DOWN; axis = SOUTH; break;
            case 5: facing = UP; axis = SOUTH; break;
            case 6: facing = UP; axis = EAST; break;
            default: facing = ATTACHED[value]; axis = UP;
        }
        facing = turn(op, facing);
        axis = turn(op, axis);
        int result;
        if (horizontal(facing)) result = index(ATTACHED, facing);
        else if (facing == UP) result = axis.offsetZ != 0 ? 5 : 6;
        else result = axis.offsetZ != 0 ? 7 : 0;
        return (meta & 8) | result;
    }

    /** A rail shape (0-9), or for rails that cannot curve (powered, detector, Railcraft boosters) shape 0-5 plus bit 8. */
    public static int rail(int meta, char op, boolean powered) {
        int shape = powered ? meta & 7 : meta;
        if (shape >= RAILS.length || "XZy".indexOf(op) >= 0) return meta;
        ForgeDirection[] sides = RAILS[shape];
        ForgeDirection a = turn(op, sides[0]), b = turn(op, sides[1]), up = sides.length > 2 ? turn(op, sides[2]) : null;
        for (int i = 0; i < RAILS.length; i++) {
            ForgeDirection[] candidate = RAILS[i];
            if ((candidate.length > 2) != (up != null) || up != null && candidate[2] != up) continue;
            if (candidate[0] == a && candidate[1] == b || candidate[0] == b && candidate[1] == a) {
                if (powered && i > 5) return meta;
                return powered ? (meta & ~7) | i : i;
            }
        }
        return meta;
    }

    private static int vine(int meta, char op) {
        if ("XZy".indexOf(op) >= 0) return meta;
        int result = 0;
        for (int i = 0; i < 4; i++) {
            if ((meta & 1 << i) == 0) continue;
            int turned = index(HORIZONTAL, turn(op, HORIZONTAL[i]));
            result |= 1 << turned;
        }
        return result;
    }

    /** Huge mushroom caps 1-9 are the cells of a 3 x 3 grid, north-west first. */
    private static int mushroom(int meta, char op) {
        if (meta < 1 || meta > 9 || "XZy".indexOf(op) >= 0) return meta;
        int dx = (meta - 1) % 3 - 1, dz = (meta - 1) / 3 - 1;
        double[] p = SchematicTransform.point(op, dx, 0, dz, 0, 0, 0);
        return (int) Math.round(p[2] + 1) * 3 + (int) Math.round(p[0] + 1) + 1;
    }

    private static int door(int meta, char op) {
        if ((meta & 8) != 0) return op == 'x' || op == 'z' ? meta ^ 1 : meta;
        return direction(meta, 3, 0, DOOR, op);
    }

    /** Mod blocks keeping a direction in their metadata; -1 when unknown. */
    private static int modBlock(Block block, int meta, char op) {
        for (Class<?> type = block.getClass(); type != null && type != Block.class; type = type.getSuperclass()) {
            String name = type.getName();
            ForgeDirection[] table = MOD_HORIZONTAL.get(name);
            if (table != null) {
                return direction(meta, 3, 0, table, op);
            }
            // Mars machines: the terraformer (0-3) stores the placer's look as N/S/W/E, cryogenic chamber and launch controller as E/W/N/S
            if (name.equals("micdoodle8.mods.galacticraft.planets.mars.blocks.BlockMachineMars")) {
                return direction(meta, 3, 0, meta < 4 ? new ForgeDirection[] {NORTH, SOUTH, WEST, EAST} : GALACTICRAFT, op);
            }
            if (name.equals("micdoodle8.mods.galacticraft.core.blocks.BlockTelemetry") || name.equals("micdoodle8.mods.galacticraft.core.blocks.BlockScreen")) {
                return ordinal(meta, 7, op, false, false);
            }
            // Botania: bellows, pumps, incense plates and avatars face a horizontal ForgeDirection, red string blocks any
            if (name.equals("vazkii.botania.common.block.mana.BlockBellows") || name.equals("vazkii.botania.common.block.mana.BlockPump")
                || name.equals("vazkii.botania.common.block.BlockIncensePlate") || name.equals("vazkii.botania.common.block.BlockAvatar")) {
                return meta >= 2 && meta <= 5 ? ordinal(meta, 7, op, false, false) : meta;
            }
            if (name.equals("vazkii.botania.common.block.string.BlockRedString")) return ordinal(meta, 7, op, true, true);
            // BuildCraft pipes: the extraction or output side of wooden, iron and similar pipes
            if (name.equals("buildcraft.transport.BlockGenericPipe")) {
                return meta < 6 ? turn(op, ForgeDirection.getOrientation(meta)).ordinal() : meta;
            }
            // Draconic Evolution flow gates: type * 6 + facing ordinal
            if (name.equals("com.brandon3055.draconicevolution.common.blocks.machine.FlowGate")) {
                int kind = meta / 6;
                ForgeDirection turned = turn(op, ForgeDirection.getOrientation(meta % 6));
                return kind * 6 + turned.ordinal();
            }
        }
        return -1;
    }

    public static int transform(Block block, int meta, char op) {
        if (block == null) return -1;
        if (block instanceof BlockStairs) return stairs(meta, op);
        if (block instanceof BlockSlab) {
            if (block.isOpaqueCube()) return meta;
            ForgeDirection half = turn(op, (meta & 8) != 0 ? UP : DOWN);
            return horizontal(half) ? meta : (meta & 7) | (half == UP ? 8 : 0);
        }
        if (block instanceof BlockLog || block instanceof BlockRotatedPillar) return axis(meta, 12, 0, 4, 8, op);
        if (block instanceof BlockQuartz) return meta >= 2 && meta <= 4 ? axis(meta, 7, 2, 3, 4, op) : meta;
        if (block instanceof BlockPistonBase || block instanceof BlockPistonExtension || block instanceof BlockDispenser) return ordinal(meta, 7, op, true, true);
        if (block instanceof BlockHopper) return ordinal(meta, 7, op, false, true);
        if (block instanceof BlockFurnace || block instanceof BlockChest || block instanceof BlockEnderChest || block instanceof BlockLadder) {
            return ordinal(meta, 7, op, false, false);
        }
        if (block instanceof BlockSign) return standing((BlockSign) block) ? rotation16(meta & 15, op) : ordinal(meta, 7, op, false, false);
        if (block instanceof BlockSkull) return (meta & 7) == 1 ? meta : ordinal(meta, 7, op, false, false);
        if (block instanceof BlockTorch) return direction(meta, 7, 0, ATTACHED, op);
        if (block instanceof BlockButton) {
            ForgeDirection turned = turn(op, ATTACHED[Math.min(meta & 7, 5)]);
            return horizontal(turned) ? (meta & 8) | index(ATTACHED, turned) : meta;
        }
        if (block instanceof BlockLever) return lever(meta, op);
        if (block instanceof BlockRailBase) return rail(meta, op, ((BlockRailBase) block).isPowered());
        if (block instanceof BlockDoor) return door(meta, op);
        if (block instanceof BlockTrapDoor) {
            if (op == 'y') return meta ^ 8;
            return direction(meta, 3, 0, TRAPDOOR, op);
        }
        if (block instanceof BlockVine) return vine(meta, op);
        if (block instanceof BlockHugeMushroom) return mushroom(meta, op);
        if (block instanceof BlockAnvil) return op == 'Y' ? (meta & ~3) | ((meta + 1) & 3) : meta;
        if (block instanceof BlockDoublePlant) return (meta & 8) != 0 ? direction(meta, 3, 0, HORIZONTAL, op) : meta;
        if (block instanceof BlockDirectional || block instanceof BlockEndPortalFrame || block instanceof BlockTripWireHook) {
            return direction(meta, 3, 0, HORIZONTAL, op);
        }
        return modBlock(block, meta, op);
    }
}
