// Block positions mod tile entities keep in their saved NBT, moved and turned with the schematic, by HackerRouter, 2026.
package com.github.lunatrius.schematica.compat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.tileentity.TileEntity;

import cpw.mods.fml.relauncher.ReflectionHelper;

import com.github.lunatrius.schematica.util.SchematicTransform;

/**
 * Links to other blocks (bound relays and flowers, multiblock masters, quarry areas) are world positions in a tile's
 * NBT. A tile captured from the world gets them in the schematic's coordinates and the marker {@link #MARKER} (the
 * dimension it was saved in); they are then moved with the tile's x/y/z, turned with the schematic and put back in
 * world coordinates when pasted. Tiles without the marker (files from before, other tools) are left alone.
 */
public final class CoordinateLinks {
    public static final String MARKER = "SchematicaLinkDim";

    static final class Spec {
        /** Compound holding the keys ("" for the tile's own tag). */
        final String path;
        /** Key names, %d for numbered lists; y is null for positions on the tile's own layer. */
        final String x, y, z;
        final boolean box;
        /** Dimension of the linked block; links into another dimension are left alone. */
        final String dimension;
        /** path is a list of compounds whose int array x holds x, y and z, or with y set, keys x, y and z. */
        final boolean list;

        Spec(String path, String x, String y, String z, boolean box, String dimension) {
            this(path, x, y, z, box, dimension, false);
        }

        Spec(String path, String x, String y, String z, boolean box, String dimension, boolean list) {
            this.path = path; this.x = x; this.y = y; this.z = z; this.box = box; this.dimension = dimension; this.list = list;
        }
    }

    private static final Map<String, List<Spec>> RULES = new HashMap<>();
    private static Map<?, ?> idToClass;

    static {
        // Thaumcraft mirrors and the light blocks of arcane lamps
        add("thaumcraft.common.tiles.TileMirror", point("", "linkX", "linkY", "linkZ", "linkDim"));
        add("thaumcraft.common.tiles.TileMirrorEssentia", point("", "linkX", "linkY", "linkZ", "linkDim"));
        add("thaumcraft.common.tiles.TileArcaneLampLight", triple("source"));
        // Botania light relays and flowers bound to spreaders, pools and (spectranthemum) a target
        add("vazkii.botania.common.block.tile.TileLightRelay", triple("bind"));
        add("vazkii.botania.common.block.tile.TileSpecialFlower", point("subTileCmp", "collectorX", "collectorY", "collectorZ", null),
            point("subTileCmp", "poolX", "poolY", "poolZ", null), point("subTileCmp", "bindX", "bindY", "bindZ", null));
        // Thaumic Tinkerer mobilizer relays (same layer)
        add("thaumic.tinkerer.common.block.tile.TileEntityRelay", point("", "PartnerX", null, "PartnerZ", null));
        add("thaumic.tinkerer.common.block.tile.transvector.TileTransvector", point("", "xt", "yt", "zt", null));
        add("thaumic.tinkerer.common.block.tile.TileEntityMobilizer", point("", "FirstRelayX", null, "FirstRelayZ", null),
            point("", "SecondRelayX", null, "SecondRelayZ", null));
        // Galacticraft beam reflectors and receivers, multiblock parts (also Galaxy Space solar panels), Stargate rings and DHDs, Ra's mothership boosters
        add("micdoodle8.mods.galacticraft.planets.asteroids.tile.TileEntityBeamOutput", triple("Target"));
        add("micdoodle8.mods.galacticraft.core.tile.TileEntityMulti", point("mainBlockPosition", "x", "y", "z", null));
        add("micdoodle8.mods.galacticraft.planets.asteroids.tile.TileEntityTelepadFake", point("mainBlockPosition", "x", "y", "z", null));
        add("gcewing.sg.tileentities.SGBaseTE", triple("linked"));
        add("gcewing.sg.guis.DHDTE", triple("linked"));
        add("gcewing.sg.tileentities.SGRingTE", triple("base"));
        add("de.katzenpapst.amunra.tile.TileEntityMothershipEngineBooster", triple("master"));
        // Single links: wireless lever, EnderIO light nodes, Malisis door parts, Nuclear Control extenders, signal terminals
        add("lumien.randomthings.TileEntities.TileEntityWirelessLever", triple("target"));
        add("crazypants.enderio.machine.light.TileLightNode", triple("parent"));
        for (String tile : new String[] {"core.tileentity.MultiBlockTileEntity", "doors.door.tileentity.RustyHatchTileEntity",
            "doors.door.tileentity.ForcefieldTileEntity"}) {
            add("net.malisis." + tile, point("multiBlock", "x", "y", "z", null));
        }
        add("net.malisis.doors.door.tileentity.MultiTile", triple("mainBlock"));
        add("shedar.mods.ic2.nuclearcontrol.tileentities.TileEntityInfoPanelExtender", triple("core"));
        // Nuclear Control screens spanning several panels, Galacticraft arc lamps' lit air blocks
        add("shedar.mods.ic2.nuclearcontrol.tileentities.TileEntityInfoPanel", new Spec("screenData", "minX", "minY", "minZ", true, null));
        add("micdoodle8.mods.galacticraft.core.tile.TileEntityArclamp", new Spec("AirBlocks", "x", "y", "z", false, null, true));
        add("tmechworks.blocks.logic.SignalTerminalLogic", point("", "BusX", "BusY", "BusZ", null));
        // Draconic Evolution energy relays/transceivers and reactor parts
        add("com.brandon3055.draconicevolution.common.tileentities.energynet.TileRemoteEnergyBase",
            point("", "X_LinkedDevice_%d", "Y_LinkedDevice_%d", "Z_LinkedDevice_%d", null));
        add("com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.TileReactorStabilizer",
            point("", "X_Master", "Y_Master", "Z_Master", null));
        add("com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.TileReactorEnergyInjector",
            point("", "X_Master", "Y_Master", "Z_Master", null));
        // BuildCraft: quarry, filler, builder and architect areas, the quarry's target and head, markers
        Spec area = new Spec("box", "xMin", "yMin", "zMin", true, null);
        add("buildcraft.builders.TileQuarry", area, triple("target"), point("", "headPosX", "headPosY", "headPosZ", null));
        add("buildcraft.builders.TileFiller", area);
        add("buildcraft.builders.TileBuilder", area);
        add("buildcraft.builders.TileArchitect", area);
        add("buildcraft.core.TilePathMarker", point("", "x0", "y0", "z0", null), point("", "x1", "y1", "z1", null));
        add("buildcraft.core.TileMarker", point("vectO", "i", "j", "k", null), point("vect0", "i", "j", "k", null),
            point("vect1", "i", "j", "k", null), point("vect2", "i", "j", "k", null));
        // Automagy: links to inventories, mirrors, targets and the inventarium's nodes and memory users
        String automagy = "tuhljin.automagy.tiles.";
        for (String tile : new String[] {"TileEntityGolemTaskmaster", "TileEntityRemoteComparator", "TileEntityRequisitionTome",
            "TileEntityUnseenScribe", "TileEntityMirrorMultiDest"}) {
            add(automagy + tile, triple("link"));
        }
        add(automagy + "TileEntityTallyBase", triple("target"));
        add(automagy + "TileEntityVisReader", triple("target"));
        add(automagy + "TileEntityNethermind", triple("currentRune"));
        add(automagy + "TileEntityRedcrystalMerc", point("", "mirrorX", "mirrorY", "mirrorZ", "mirrorDim"),
            point("", "mirrorLinkedX", "mirrorLinkedY", "mirrorLinkedZ", "mirrorLinkedDim"));
        add(automagy + "TileEntityMobLure", point("", null, "yTop", null, null), point("", null, "yBottom", null, null));
        add(automagy + "TileEntityThaumostaticController", point("", null, "spireY", null, null));
        add(automagy + "TileEntityThaumostaticPylon", point("", null, "bossY", null, null));
        add(automagy + "TileEntityInventarium", new Spec("Nodes", "pos", null, null, false, null, true),
            new Spec("MemoryUsers", "pos", null, null, false, null, true));
        // Extra Utilities energy nodes: the receivers they found
        add("com.rwtema.extrautils.tileentity.transfernodes.TileEntityTransferNodeEnergy", point("", "cx%d", "cy%d", "cz%d", null));
    }

    private CoordinateLinks() {}

    private static Spec triple(String prefix) { return point("", prefix + "X", prefix + "Y", prefix + "Z", null); }

    private static Spec point(String path, String x, String y, String z, String dimension) {
        return new Spec(path, x, y, z, false, dimension);
    }

    private static void add(String type, Spec... specs) {
        List<Spec> list = new ArrayList<>();
        Collections.addAll(list, specs);
        RULES.put(type, list);
    }

    static List<Spec> specs(Class<?> type) {
        List<Spec> result = new ArrayList<>();
        for (Class<?> parent = type; parent != null; parent = parent.getSuperclass()) {
            List<Spec> specs = RULES.get(parent.getName());
            if (specs != null) result.addAll(specs);
        }
        return result;
    }

    /** The rules for a tile type name, for tests. */
    static List<Spec> rulesFor(String type) { return RULES.getOrDefault(type, new ArrayList<>()); }

    public static boolean has(TileEntity tile) { return tile != null && !specs(tile.getClass()).isEmpty(); }

    public static boolean local(NBTTagCompound tag) { return tag != null && tag.hasKey(MARKER); }

    /** The links of a tile captured from the world, in the coordinates of a schematic whose origin is at (ox, oy, oz). */
    public static void capture(TileEntity tile, NBTTagCompound tag, int ox, int oy, int oz) {
        List<Spec> specs = specs(tile.getClass());
        if (specs.isEmpty()) return;
        int dimension = tile.hasWorldObj() && tile.getWorldObj().provider != null ? tile.getWorldObj().provider.dimensionId : 0;
        tag.setInteger(MARKER, dimension);
        apply(specs, tag, p -> new double[] {p[0] - ox, p[1] - oy, p[2] - oz}, (char) 0);
    }

    /** Moves the links of a schematic tile whose x/y/z moves by (dx, dy, dz). */
    public static void shift(TileEntity tile, NBTTagCompound tag, int dx, int dy, int dz) {
        if (local(tag)) shift(specs(tile.getClass()), tag, dx, dy, dz);
    }

    static void shift(List<Spec> specs, NBTTagCompound tag, int dx, int dy, int dz) {
        if (dx != 0 || dy != 0 || dz != 0) apply(specs, tag, p -> new double[] {p[0] + dx, p[1] + dy, p[2] + dz}, (char) 0);
    }

    /** Turns the links of a schematic tile with the schematic (sizes before the operation). */
    public static void transform(TileEntity tile, NBTTagCompound tag, char operation, int width, int height, int length) {
        if (local(tag)) transform(specs(tile.getClass()), tag, operation, width, height, length);
    }

    static void transform(List<Spec> specs, NBTTagCompound tag, char operation, int width, int height, int length) {
        apply(specs, tag, p -> SchematicTransform.point(operation, p[0], p[1], p[2], width - 1, height - 1, length - 1),
            operation);
    }

    /** A pasted tile's NBT (x/y/z still in the schematic) put at (x, y, z) in the world; returns the tag. */
    public static NBTTagCompound paste(NBTTagCompound tag, int x, int y, int z) {
        if (!local(tag)) return tag;
        Class<?> type = typeOf(tag.getString("id"));
        if (type != null) shift(specs(type), tag, x - tag.getInteger("x"), y - tag.getInteger("y"), z - tag.getInteger("z"));
        tag.removeTag(MARKER);
        return tag;
    }

    /** A world tile's NBT moved from its x/y/z to (x, y, z) within the world. */
    public static void move(TileEntity tile, NBTTagCompound tag, int x, int y, int z) {
        List<Spec> specs = specs(tile.getClass());
        if (!specs.isEmpty()) {
            boolean marked = tag.hasKey(MARKER);
            if (!marked) tag.setInteger(MARKER, tile.hasWorldObj() && tile.getWorldObj().provider != null ? tile.getWorldObj().provider.dimensionId : 0);
            shift(specs, tag, x - tag.getInteger("x"), y - tag.getInteger("y"), z - tag.getInteger("z"));
            if (!marked) tag.removeTag(MARKER);
        }
    }

    private static Class<?> typeOf(String id) {
        try {
            if (idToClass == null) idToClass = ReflectionHelper.getPrivateValue(TileEntity.class, null, "nameToClassMap", "field_145855_i");
            Object type = idToClass.get(id);
            return type instanceof Class ? (Class<?>) type : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private interface Mapping { double[] map(double[] point); }

    /**
     * Maps every position of the specs (operation 0 for a move). Positions without a y (on the tile's layer) skip
     * X, Z and y; y-only positions (in the tile's column) skip X and Z.
     */
    private static void apply(List<Spec> specs, NBTTagCompound root, Mapping mapping, char operation) {
        boolean vertical = operation != 0 && "XZy".indexOf(operation) >= 0, tilting = operation == 'X' || operation == 'Z';
        int dimension = root.getInteger(MARKER);
        for (Spec spec : specs) {
            if (spec.list) {
                positions(root, spec, mapping);
                continue;
            }
            NBTTagCompound tag = spec.path.isEmpty() ? root : root.hasKey(spec.path, 10) ? root.getCompoundTag(spec.path) : null;
            if (tag == null || spec.y == null && vertical || spec.x == null && tilting) continue;
            if (spec.dimension != null && tag.hasKey(spec.dimension) && tag.getInteger(spec.dimension) != dimension) continue;
            if (spec.box) {
                box(tag, spec, mapping);
                continue;
            }
            if (spec.x == null) {
                if (tag.hasKey(spec.y) && number(tag, spec.y) >= 0) put(tag, spec.y, mapping.map(new double[] {0, number(tag, spec.y), 0})[1]);
                continue;
            }
            boolean list = spec.x.contains("%d");
            for (int i = 0; ; i++) {
                String x = String.format(spec.x, i), z = String.format(spec.z, i), y = spec.y == null ? null : String.format(spec.y, i);
                if (!tag.hasKey(x) || !tag.hasKey(z) || y != null && !tag.hasKey(y)) break;
                double py = y == null ? 0 : number(tag, y);
                if (py >= 0) {
                    double[] moved = mapping.map(new double[] {number(tag, x), py, number(tag, z)});
                    put(tag, x, moved[0]);
                    if (y != null) put(tag, y, moved[1]);
                    put(tag, z, moved[2]);
                }
                if (!list) break;
            }
        }
    }

    private static void positions(NBTTagCompound root, Spec spec, Mapping mapping) {
        net.minecraft.nbt.NBTTagList list = root.getTagList(spec.path, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            if (spec.y != null) {
                if (!entry.hasKey(spec.x) || !entry.hasKey(spec.y) || !entry.hasKey(spec.z) || entry.getInteger(spec.y) < 0) continue;
                double[] moved = mapping.map(new double[] {entry.getInteger(spec.x), entry.getInteger(spec.y), entry.getInteger(spec.z)});
                put(entry, spec.x, moved[0]);
                put(entry, spec.y, moved[1]);
                put(entry, spec.z, moved[2]);
                continue;
            }
            int[] pos = entry.getIntArray(spec.x);
            if (pos.length != 3 || pos[1] < 0) continue;
            double[] moved = mapping.map(new double[] {pos[0], pos[1], pos[2]});
            entry.setIntArray(spec.x, new int[] {(int) Math.round(moved[0]), (int) Math.round(moved[1]), (int) Math.round(moved[2])});
        }
    }

    /** A box of the spec's minimum keys and the same keys with Min/min turned into Max/max. */
    private static void box(NBTTagCompound tag, Spec spec, Mapping mapping) {
        String[] min = {spec.x, spec.y, spec.z}, max = new String[3];
        for (int i = 0; i < 3; i++) max[i] = min[i].replace("Min", "Max").replace("min", "max");
        for (String key : min) if (!tag.hasKey(key) || tag.getInteger(key) == Integer.MAX_VALUE) return;
        double[] a = mapping.map(new double[] {tag.getInteger(min[0]), tag.getInteger(min[1]), tag.getInteger(min[2])});
        double[] b = mapping.map(new double[] {tag.getInteger(max[0]), tag.getInteger(max[1]), tag.getInteger(max[2])});
        for (int i = 0; i < 3; i++) {
            tag.setInteger(min[i], (int) Math.round(Math.min(a[i], b[i])));
            tag.setInteger(max[i], (int) Math.round(Math.max(a[i], b[i])));
        }
    }

    private static double number(NBTTagCompound tag, String key) {
        NBTBase value = tag.getTag(key);
        return value instanceof NBTBase.NBTPrimitive ? ((NBTBase.NBTPrimitive) value).func_150286_g() : 0;
    }

    private static void put(NBTTagCompound tag, String key, double value) {
        NBTBase old = tag.getTag(key);
        if (old instanceof NBTTagDouble) tag.setDouble(key, value);
        else if (old instanceof NBTTagShort) tag.setShort(key, (short) Math.round(value));
        else tag.setInteger(key, (int) Math.round(value));
    }
}
