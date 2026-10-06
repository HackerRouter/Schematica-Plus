// Metadata of rotated and mirrored 1.7.10 blocks (Litematica's BlockState rotate/mirror), by HackerRouter, 2026.
package com.github.lunatrius.schematica.world.schematic;

import java.lang.reflect.Field;
import java.util.Collections;

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

    /** Mod blocks facing a horizontal ForgeDirection 2-5 in bits 0-2 (chest and furnace layout). */
    private static final java.util.Set<String> MOD_WALL = new java.util.HashSet<>();
    /** Mod blocks facing any ForgeDirection in bits 0-2. */
    private static final java.util.Set<String> MOD_ANY_SIDE = new java.util.HashSet<>();
    /** Mod blocks with the torch layout (1 E, 2 W, 3 S, 4 N, 5 standing). */
    private static final java.util.Set<String> MOD_TORCH = new java.util.HashSet<>();
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
        MOD_HORIZONTAL.put("galaxyspace.core.block.machine.BlockMachine", GALACTICRAFT);
        MOD_HORIZONTAL.put(gc + "BlockSolar", new ForgeDirection[] {NORTH, SOUTH, WEST, EAST});
        // Witchery: coffins use the bed layout, mirrors keep the side of the block their glass is on (top half + 4)
        MOD_HORIZONTAL.put("com.emoniph.witchery.blocks.BlockCoffin", HORIZONTAL);
        MOD_HORIZONTAL.put("com.emoniph.witchery.blocks.BlockMirror", new ForgeDirection[] {SOUTH, NORTH, EAST, WEST});
        // Automagy: (yaw quarter + 2) & 3 in the low two bits
        for (String block : new String[] {"BlockHourglass", "BlockGolemInhibitor", "BlockChestBase", "BlockGolemTaskmaster", "BlockUnseenScribe",
            "BlockRequisitionTome", "BlockRemoteComparator", "BlockVisReader", "BlockEagerChest", "BlockGreedyChest", "BlockScribePointer"}) {
            MOD_HORIZONTAL.put("tuhljin.automagy.blocks." + block, new ForgeDirection[] {NORTH, EAST, SOUTH, WEST});
        }
        // Et Futurum Requiem: glazed terracotta ((yaw quarter + 2) & 3), looms and stonecutters (front side - 2)
        String efr = "ganymedes01.etfuturum.blocks.";
        MOD_HORIZONTAL.put(efr + "BlockGlazedTerracotta", new ForgeDirection[] {NORTH, EAST, SOUTH, WEST});
        MOD_HORIZONTAL.put(efr + "BlockLoom", new ForgeDirection[] {NORTH, SOUTH, WEST, EAST});
        MOD_HORIZONTAL.put(efr + "BlockStonecutter", new ForgeDirection[] {NORTH, SOUTH, WEST, EAST});
        // Botania fel pumpkins and tiny potatoes, Tinkers' tool tables and battlesigns (E/W/N/S)
        MOD_HORIZONTAL.put("vazkii.botania.common.block.BlockFelPumpkin", HORIZONTAL);
        MOD_HORIZONTAL.put("vazkii.botania.common.block.decor.BlockTinyPotato", HORIZONTAL);
        MOD_HORIZONTAL.put("tconstruct.tools.blocks.EquipBlock", GALACTICRAFT);
        // Draconic generators, Hardcore Ender Expansion enderman heads ((yaw quarter + 2) & 3)
        MOD_HORIZONTAL.put("com.brandon3055.draconicevolution.common.blocks.machine.Generator", HORIZONTAL);
        MOD_HORIZONTAL.put("chylex.hee.block.BlockEndermanHead", new ForgeDirection[] {NORTH, EAST, SOUTH, WEST});
        // SGCraft and ArchitectureCraft blocks with a facing property: index in N, W, S, E
        for (String block : new String[] {"gcewing.sg.blocks.SGBaseBlock", "gcewing.sg.blocks.DHDBlock", "gcewing.sg.blocks.SGInterfaceBlock",
            "gcewing.sg.compat.ic2.IC2PowerBlock", "gcewing.architecture.common.block.BlockSawbench"}) {
            MOD_HORIZONTAL.put(block, new ForgeDirection[] {NORTH, WEST, SOUTH, EAST});
        }
        // Galacticraft dishes (N/S/W/E), Avaritia, OpenPrinter printers, Adventure Backpacks (lamp/redstone flags above)
        MOD_HORIZONTAL.put("micdoodle8.mods.galacticraft.core.blocks.BlockDish", new ForgeDirection[] {NORTH, SOUTH, WEST, EAST});
        MOD_HORIZONTAL.put("fox.spiteful.avaritia.blocks.BlockMatterClusterOpener", new ForgeDirection[] {NORTH, EAST, SOUTH, WEST});
        MOD_HORIZONTAL.put("fox.spiteful.avaritia.compat.botania.BlockInfinitato", HORIZONTAL);
        MOD_HORIZONTAL.put("pcl.openprinter.blocks.BlockPrinter", HORIZONTAL);
        MOD_HORIZONTAL.put("com.darkona.adventurebackpack.block.BlockAdventureBackpack", HORIZONTAL);
        Collections.addAll(MOD_WALL, "de.katzenpapst.amunra.block.BlockARChest", "flaxbeard.thaumicexploration.block.BlockBoundChest",
            "flaxbeard.thaumicexploration.block.BlockThinkTank", "com.glodblock.github.common.block.BlockWalrus",
            "micdoodle8.mods.galacticraft.core.blocks.BlockT1TreasureChest", "micdoodle8.mods.galacticraft.planets.mars.blocks.BlockTier2TreasureChest",
            "micdoodle8.mods.galacticraft.planets.asteroids.blocks.BlockTier3TreasureChest", "mods.railcraft.common.blocks.tracks.BlockTrackElevator");
        Collections.addAll(MOD_ANY_SIDE, "de.keridos.floodlights.block.BlockFL", "pcl.opensecurity.blocks.BlockOSBase",
            "vswe.stevesfactory.blocks.BlockCableCluster", "vswe.stevesfactory.blocks.BlockCableDirectionAdvanced",
            "vswe.stevesfactory.blocks.BlockCableSign", "vswe.stevesfactory.blocks.BlockCableBreaker",
            "com.kentington.thaumichorizons.common.blocks.BlockTransductionAmplifier", "micdoodle8.mods.galacticraft.core.blocks.BlockBrightLamp");
        Collections.addAll(MOD_TORCH, "micdoodle8.mods.galacticraft.core.blocks.BlockGlowstoneTorch",
            "micdoodle8.mods.galacticraft.core.blocks.BlockUnlitTorch", "micdoodle8.mods.galacticraft.core.blocks.BlockSpinThruster");
        // Extra Utilities conveyors: (yaw quarter + 2) % 4
        MOD_HORIZONTAL.put("com.rwtema.extrautils.block.BlockConveyor", new ForgeDirection[] {NORTH, EAST, SOUTH, WEST});
        // BiblioCraft armor stands (top half + 4), printing presses and typesetting tables: (yaw quarter + 1) % 4
        for (String block : new String[] {"BlockArmorStand", "BlockPrintPress", "BlockTypeMachine"}) {
            MOD_HORIZONTAL.put("jds.bibliocraft.blocks." + block, new ForgeDirection[] {EAST, SOUTH, WEST, NORTH});
        }
    }

    private static final java.util.Set<String> HARVESTCRAFT_MACHINES = new java.util.HashSet<>(
        java.util.Arrays.asList("Churn", "Grinder", "Oven", "Presser", "Quern"));
    private static final ForgeDirection[] CATWALK_SIDES = {EAST, WEST, SOUTH, NORTH};
    private static final String[] LADDER_FACINGS = {"north", "south", "west", "east"};

    private BlockMetaTransform() {}

    /**
     * The block a block becomes under an operation: blocks whose facing is part of the registry name (Catwalks caged
     * ladders, one block per side) turn into the block for the turned side; others stay.
     */
    public static Block block(Block block, char op) {
        if (block == null || "XZy".indexOf(op) >= 0 || !block.getClass().getName().equals("com.thecodewarrior.catwalks.block.BlockCagedLadder")) {
            return block;
        }
        String name = cpw.mods.fml.common.registry.GameData.getBlockRegistry().getNameForObject(block);
        String turned = ladderName(name, op);
        if (turned == null || turned.equals(name)) return block;
        Block result = cpw.mods.fml.common.registry.GameData.getBlockRegistry().getObject(turned);
        return result == null || result == net.minecraft.init.Blocks.air ? block : result;
    }

    /** "catwalks:cagedLadder_north_lit..." with its side turned; null when the name has no side. */
    static String ladderName(String name, char op) {
        if (name == null) return null;
        int start = name.indexOf("cagedLadder_");
        if (start < 0) return null;
        start += "cagedLadder_".length();
        for (String facing : LADDER_FACINGS) {
            if (!name.startsWith(facing, start)) continue;
            ForgeDirection side = ForgeDirection.valueOf(facing.toUpperCase(java.util.Locale.ROOT));
            String turned = turn(op, side).name().toLowerCase(java.util.Locale.ROOT);
            return name.substring(0, start) + turned + name.substring(start + facing.length());
        }
        return null;
    }

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

    /**
     * OpenModsLib: the metadata (under the mode's mask) indexes the rotation mode's valid orientations, each a pair of
     * world directions for the block's local x and y. Both are turned and the matching orientation looked up; one the
     * mode does not allow keeps the metadata.
     */
    private static int openMods(Block block, int meta, char op) {
        try {
            Object mode = block.getClass().getMethod("getRotationMode").invoke(block);
            int mask = mode.getClass().getField("mask").getInt(mode);
            if (mask == 0) return meta;
            Object orientation = mode.getClass().getMethod("fromValue", int.class).invoke(mode, meta & mask);
            Class<?> orientationType = orientation.getClass();
            Object x = orientationType.getField("x").get(orientation), y = orientationType.getField("y").get(orientation);
            Class<?> axis = x.getClass();
            java.lang.reflect.Method fromDirection = axis.getMethod("fromDirection", ForgeDirection.class);
            Object tx = fromDirection.invoke(null, turn(op, (ForgeDirection) axis.getField("dir").get(x)));
            Object ty = fromDirection.invoke(null, turn(op, (ForgeDirection) axis.getField("dir").get(y)));
            Object turned = orientationType.getMethod("lookupXY", axis, axis).invoke(null, tx, ty);
            if (turned == null || !(Boolean) mode.getClass().getMethod("isPlacementValid", orientationType).invoke(mode, turned)) return meta;
            int value = (Integer) mode.getClass().getMethod("toValue", orientationType).invoke(mode, turned);
            return meta & ~mask | value & mask;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return meta;
        }
    }

    /** Et Futurum banners: a 16-step rotation when standing (a tile flag), else the wall side 2-5. */
    public static int banner(int meta, char op, boolean standing) {
        return standing ? rotation16(meta & 15, op) : ordinal(meta, 7, op, false, false);
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

    /** Nagastone and snakestone connections: N, S, W, E, down, up. */
    private static final ForgeDirection[] SERPENT = {NORTH, SOUTH, WEST, EAST, DOWN, UP};
    /** Natura bloodwood: the quarter of the 2 x 2 trunk, as its offset from the trunk's axis (vertical, along x, along z). */
    private static final int[][] BLOODWOOD = {{-1, 0, -1}, {1, 0, -1}, {-1, 0, 1}, {1, 0, 1}, {0, 1, 1}, {0, 1, -1}, {0, -1, 1},
        {0, -1, -1}, {-1, 1, 0}, {1, 1, 0}, {-1, -1, 0}, {1, -1, 0}};

    /**
     * Snakestone and nagastone: heads 0-3 join the neighbor on side meta ^ 1, corners join the one below (4 | side) or
     * above (8 | side) and a horizontal side, straight pieces run along x (12), z (13) or y (14).
     */
    static int serpent(int meta, char op) {
        if (meta <= 3) {
            ForgeDirection joined = turn(op, SERPENT[meta ^ 1]);
            return horizontal(joined) ? index(SERPENT, joined) ^ 1 : meta;
        }
        if (meta <= 11) {
            ForgeDirection vertical = turn(op, (meta & 4) != 0 ? DOWN : UP), side = turn(op, SERPENT[meta & 3]);
            if (horizontal(vertical)) { ForgeDirection swap = vertical; vertical = side; side = swap; }
            if (horizontal(vertical) || !horizontal(side)) return meta;
            return (vertical == DOWN ? 4 : 8) | index(SERPENT, side);
        }
        if (meta == 15) return meta;
        ForgeDirection axis = turn(op, meta == 12 ? EAST : meta == 13 ? SOUTH : UP);
        return axis.offsetY != 0 ? 14 : axis.offsetZ != 0 ? 13 : 12;
    }

    static int bloodwood(int meta, char op) {
        if (meta >= BLOODWOOD.length) return meta;
        int[] offset = BLOODWOOD[meta];
        double[] turned = SchematicTransform.point(op, offset[0], offset[1], offset[2], 0, 0, 0);
        for (int i = 0; i < BLOODWOOD.length; i++) {
            if (BLOODWOOD[i][0] == turned[0] && BLOODWOOD[i][1] == turned[1] && BLOODWOOD[i][2] == turned[2]) return i;
        }
        return meta;
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
            // OpenModsLib blocks (OpenBlocks and others): an index into their rotation mode's orientations
            if (name.equals("openmods.block.OpenBlock")) return openMods(block, meta, op);
            if (MOD_WALL.contains(name)) return (meta & 7) >= 2 && (meta & 7) <= 5 ? ordinal(meta, 7, op, false, false) : meta;
            if (MOD_ANY_SIDE.contains(name)) return ordinal(meta, 7, op, true, true);
            if (MOD_TORCH.contains(name)) return direction(meta, 7, 0, ATTACHED, op);
            // Amun-Ra machines and mothership engines: N/S/W/E in bits 2-3 over the sub-block
            if (name.equals("de.katzenpapst.amunra.block.BlockMachineMeta")) return direction(meta, 12, 2, new ForgeDirection[] {NORTH, SOUTH, WEST, EAST}, op);
            // OpenPrinter file cabinets and shredders: the yaw quarter + 1
            if (name.equals("pcl.openprinter.blocks.BlockFileCabinet") || name.equals("pcl.openprinter.blocks.BlockShredder")) {
                return meta >= 1 && meta <= 4 ? direction(meta - 1, 3, 0, HORIZONTAL, op) + 1 : meta;
            }
            // Tinkers' Defense crest mounts: the ladder layout minus one
            if (name.equals("gmail.Lance5057.blocks.CrestMount")) return meta >= 1 && meta <= 4 ? ordinal(meta + 1, 7, op, false, false) - 1 : meta;
            // Gadomancy arcane droppers: facing (any side) + 8 when turned a quarter around it; a Y turn flips that for up/down
            if (name.equals("makeo.gadomancy.common.blocks.BlockArcaneDropper")) {
                int turned = ordinal(meta, 7, op, true, true);
                return (meta & 7) <= 1 && op == 'Y' ? turned ^ 8 : turned;
            }
            // Thaumic Tinkerer dark quartz and Botania decorative quartz pillars use the quartz layout; Forestry logs the log layout
            if (name.equals("thaumic.tinkerer.common.block.quartz.BlockDarkQuartz") || name.equals("vazkii.botania.common.block.decor.quartz.BlockSpecialQuartz")) {
                return meta >= 2 && meta <= 4 ? axis(meta, 7, 2, 3, 4, op) : meta;
            }
            if (name.equals("forestry.arboriculture.blocks.BlockLog")) return axis(meta, 12, 0, 4, 8, op);
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
            // Witchery: furnace-like devices face a horizontal ForgeDirection, skulls are laid out like vanilla skulls
            if (name.equals("com.emoniph.witchery.blocks.BlockDistillery") || name.equals("com.emoniph.witchery.blocks.BlockKettle")
                || name.equals("com.emoniph.witchery.blocks.BlockSpinningWheel") || name.equals("com.emoniph.witchery.blocks.BlockWitchesOven")) {
                return (meta & 7) >= 2 && (meta & 7) <= 5 ? ordinal(meta, 7, op, false, false) : meta;
            }
            if (name.equals("com.emoniph.witchery.blocks.BlockAlluringSkull") || name.equals("com.emoniph.witchery.blocks.BlockWolfHead")) {
                return (meta & 7) == 1 ? meta : ordinal(meta, 7, op, false, false);
            }
            // Pam's HarvestCraft machines face a horizontal ForgeDirection
            if (name.startsWith("com.pam.harvestcraft.BlockPam") && HARVESTCRAFT_MACHINES.contains(name.substring("com.pam.harvestcraft.BlockPam".length()))) {
                return (meta & 7) >= 2 && (meta & 7) <= 5 ? ordinal(meta, 7, op, false, false) : meta;
            }
            // Catwalks: catwalk open sides (8 N, 4 S, 2 W, 1 E), support column axis (0 y, 1 z, 2 x), caged ladder sides
            // relative to the ladder (bits 1 left, 0 right; the ladder's own side is its block, see block())
            if (name.equals("com.thecodewarrior.catwalks.block.BlockCatwalk")) {
                if ("XZy".indexOf(op) >= 0) return meta;
                int result = meta & ~15;
                for (int bit = 0; bit < 4; bit++) if ((meta & 1 << bit) != 0) result |= 1 << index(CATWALK_SIDES, turn(op, CATWALK_SIDES[bit]));
                return result;
            }
            if (name.equals("com.thecodewarrior.catwalks.block.BlockSupportColumn")) {
                if (meta > 2) return meta;
                ForgeDirection axis = turn(op, meta == 0 ? UP : meta == 1 ? SOUTH : EAST);
                return axis.offsetY != 0 ? 0 : axis.offsetZ != 0 ? 1 : 2;
            }
            if (name.equals("com.thecodewarrior.catwalks.block.BlockCagedLadder")) {
                return op == 'x' || op == 'z' ? meta & ~3 | (meta & 1) << 1 | (meta & 2) >> 1 : meta;
            }
            // Thaumcraft mirrors, Extra Utilities transfer nodes and spikes: type * 6 + a side; arcane doors use the door layout
            // Et Futurum Requiem
            if (name.startsWith("ganymedes01.etfuturum.blocks.")) {
                String efrBlock = name.substring("ganymedes01.etfuturum.blocks.".length());
                switch (efrBlock) {
                    case "BlockObserver": case "BlockBarrel": case "BlockEndRod": case "BlockGlowLichen":
                        return ordinal(meta, 7, op, true, true);
                    case "BlockAmethystCluster":
                        return meta < 12 ? meta / 6 * 6 + turn(op, ForgeDirection.getOrientation(meta % 6)).ordinal() : meta;
                    case "BlockBeeHive": {
                        int honey = meta >= 8 ? 6 : 0;
                        return meta - honey >= 2 && meta - honey <= 5 ? ordinal(meta - honey, 7, op, false, false) + honey : meta;
                    }
                    case "BlockPinkPetals":
                        return direction(meta, 12, 2, new ForgeDirection[] {SOUTH, WEST, EAST, NORTH}, op);
                    case "BlockChain": {
                        if (meta > 2) return meta;
                        ForgeDirection axis = turn(op, meta == 0 ? UP : meta == 1 ? EAST : SOUTH);
                        return axis.offsetY != 0 ? 0 : axis.offsetX != 0 ? 1 : 2;
                    }
                    default:
                }
            }
            // Cooking for Blockheads kitchen blocks face a horizontal ForgeDirection
            if (name.equals("net.blay09.mods.cookingforblockheads.block.BlockBaseKitchen")) {
                return (meta & 7) >= 2 && (meta & 7) <= 5 ? ordinal(meta, 7, op, false, false) : meta;
            }
            // Chisel snakestone and Twilight Forest nagastone share a layout; etched nagastone faces any ForgeDirection
            if (name.equals("team.chisel.block.BlockSnakestone") || name.equals("twilightforest.block.BlockTFNagastone")) return serpent(meta, op);
            if (name.equals("twilightforest.block.BlockTFNagastoneEtched")) return ordinal(meta, 7, op, true, true);
            if (name.equals("mods.natura.blocks.trees.LogTwoxTwo")) return bloodwood(meta, op);
            // Tinkers' Construct: drying racks (floor 0 along z, 1 along x; walls 2-5), stone torches (torch layout),
            // landmines (lever layout), conveyors and slime pads (eighths of a turn in bits 0-2)
            if (name.equals("tconstruct.armor.blocks.DryingRack")) {
                if (meta < 2) return op == 'Y' ? meta ^ 1 : meta;
                return meta <= 5 ? ordinal(meta, 7, op, false, false) : meta;
            }
            if (name.equals("tconstruct.world.blocks.StoneTorch")) return direction(meta, 7, 0, ATTACHED, op);
            if (name.equals("tconstruct.mechworks.blocks.BlockLandmine")) return lever(meta, op);
            if (name.equals("tconstruct.world.blocks.ConveyorBase") || name.equals("tconstruct.world.blocks.SlimePad")) {
                int face = meta & 7;
                switch (op) {
                    case 'Y': face = face + 2; break;
                    case 'x': face = 8 - face; break;
                    case 'z': face = 4 - face; break;
                    default: return meta;
                }
                return meta & ~7 | face & 7;
            }
            // Twilight Forest: critters (6 - the side they sit on), trophies (skull layout), stronghold shields (any side),
            // spiral bricks (low two bits E, W, S, N)
            if (name.equals("twilightforest.block.BlockTFCritter")) {
                return direction(meta, 7, 0, new ForgeDirection[] {UNKNOWN, EAST, WEST, SOUTH, NORTH, UP, DOWN}, op);
            }
            if (name.equals("twilightforest.block.BlockTFTrophy")) return (meta & 7) == 1 ? meta : ordinal(meta, 7, op, false, false);
            if (name.equals("twilightforest.block.BlockTFShield")) return ordinal(meta, 7, op, true, true);
            if (name.equals("twilightforest.block.BlockTFSpiralBricks")) return direction(meta, 3, 0, new ForgeDirection[] {EAST, WEST, SOUTH, NORTH}, op);
            // Thaumic Tinkerer animation tablets (redstone + 8) and repairers face a horizontal ForgeDirection
            if (name.equals("thaumic.tinkerer.common.block.BlockAnimationTablet") || name.equals("thaumic.tinkerer.common.block.BlockRepairer")) {
                return (meta & 7) >= 2 && (meta & 7) <= 5 ? ordinal(meta, 7, op, false, false) : meta;
            }
            // Malisis mixers and door factories, Hardcore Ender Expansion void chests: horizontal ForgeDirections;
            // Malisis mixed blocks and player sensors (powered + 8): any side
            if (name.equals("net.malisis.doors.block.BlockMixer") || name.equals("net.malisis.doors.block.DoorFactory")
                || name.equals("chylex.hee.block.BlockVoidChest")) {
                return (meta & 7) >= 2 && (meta & 7) <= 5 ? ordinal(meta, 7, op, false, false) : meta;
            }
            if (name.equals("net.malisis.doors.block.MixedBlock") || name.equals("net.malisis.doors.block.PlayerSensor")) return ordinal(meta, 7, op, true, true);
            // Draconic potentiometers: the side they point away from (lever-like 1-6, + 8 kept)
            if (name.equals("com.brandon3055.draconicevolution.common.blocks.Potentiometer")) {
                return direction(meta, 7, 0, new ForgeDirection[] {UNKNOWN, EAST, WEST, SOUTH, NORTH, DOWN, UP}, op);
            }
            // Hardcore Ender Expansion: obsidian pillars (quartz layout: 2 y, 3 x, 4 z), spooky logs (face side - 1)
            if (name.equals("chylex.hee.block.BlockObsidianSpecial")) return meta >= 2 && meta <= 4 ? axis(meta, 7, 2, 3, 4, op) : meta;
            if (name.equals("chylex.hee.block.BlockSpookyLog")) {
                return meta >= 1 && meta <= 4 ? ordinal(meta + 1, 7, op, false, false) - 1 : meta;
            }
            // Natura dark trees use the log layout
            if (name.equals("mods.natura.blocks.trees.DarkTreeBlock")) return axis(meta, 12, 0, 4, 8, op);
            // Automagy: hungry/finical maws face the side they were placed on
            if (name.equals("tuhljin.automagy.blocks.BlockMawHungry")) return ordinal(meta, 7, op, true, true);
            if (name.equals("thaumcraft.common.blocks.BlockMirror") || name.equals("com.rwtema.extrautils.tileentity.transfernodes.BlockTransferNode")
                || name.equals("tuhljin.automagy.blocks.BlockTallyBase")
                || name.equals("com.rwtema.extrautils.block.BlockSpike")) {
                return meta < 12 ? meta / 6 * 6 + turn(op, ForgeDirection.getOrientation(meta % 6)).ordinal() : meta;
            }
            if (name.equals("thaumcraft.common.blocks.BlockArcaneDoor")) return door(meta, op);
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
