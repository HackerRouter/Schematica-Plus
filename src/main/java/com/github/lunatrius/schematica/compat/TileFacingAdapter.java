// Facings that mod tile entities save in their NBT, turned with the schematic, by HackerRouter, 2026.
package com.github.lunatrius.schematica.compat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.lunatrius.schematica.api.ISchematicVisualAdapter;
import com.github.lunatrius.schematica.util.SchematicTransform;
import com.github.lunatrius.schematica.world.schematic.BlockMetaTransform;

/**
 * Rotation and mirroring of directions mod tile entities keep in their saved NBT (GTNH 2.8.4 / 2.9.0 sources):
 * ForgeDirection ordinals or names, side-indexed arrays, per-side keys, 16-step rotations and offsets from the tile. The preview tile reads the turned NBT
 * back. Blocks handled here are not also turned through their Block.rotateBlock.
 */
final class TileFacingAdapter implements ISchematicVisualAdapter {
    enum Kind { ORDINAL, NAME, SIDE_KEYS, SIDE_ARRAY, MASK, HORIZONTAL, ROTATION16, OFFSET, ORDINAL_ARRAY, NAMED_SIDES, YAW }

    static final class Rule {
        final Kind kind;
        final String key;
        /** The operations the rule applies to; others leave the value alone (OpenComputers yaw/pitch). */
        final String operations;
        /** HORIZONTAL: the sides the low two bits 0-3 stand for; higher bits are kept. */
        final ForgeDirection[] sides;
        Rule(Kind kind, String key, String operations) { this(kind, key, operations, HORIZONTAL_SIDES); }
        Rule(Kind kind, String key, String operations, ForgeDirection[] sides) { this.kind = kind; this.key = key; this.operations = operations; this.sides = sides; }
    }

    private static final Map<String, List<Rule>> RULES = new LinkedHashMap<>();
    private static final String ALL = "XYZxyz";
    private static final ForgeDirection[] HORIZONTAL_SIDES = {ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.NORTH, ForgeDirection.EAST};

    static {
        // IndustrialCraft 2 (and IC2 based: Advanced Solar Panels, Nuclear Control panels, Compact Kinetic Generators)
        add("ic2.core.block.TileEntityBlock", ordinal("facing"));
        // EnderIO
        add("crazypants.enderio.machine.AbstractMachineEntity", ordinal("facing"), sideKeys("face"));
        add("crazypants.enderio.machine.enchanter.TileEnchanter", ordinal("facing"));
        add("crazypants.enderio.machine.light.TileElectricLight", ordinal("face"));
        add("crazypants.enderio.machine.capbank.TileCapBank", sideKeys("face"), sideKeys("faceDisplay"));
        // Storage
        add("cpw.mods.ironchest.TileEntityIronChest", ordinal("facing"));
        add("com.jaquadro.minecraft.storagedrawers.block.tile.TileEntityDrawers", ordinal("Dir"));
        add("com.jaquadro.minecraft.storagedrawers.block.tile.TileEntityController", ordinal("Dir"));
        add("mcp.mobius.betterbarrels.common.blocks.TileEntityBarrel", ordinal("orientation"), ordinal("rotation"),
            sideArray("sideUpgrades"), sideArray("sideMeta"));
        add("com.dreammaster.modbabychest.TileEntityBabyChest", ordinal("facing"));
        add("com.brandon3055.draconicevolution.common.tileentities.TileDraconiumChest", ordinal("facing"));
        // Machines
        add("fox.spiteful.avaritia.tile.TileEntityCompressor", ordinal("Facing"));
        add("fox.spiteful.avaritia.tile.TileEntityNeutron", ordinal("Facing"));
        add("com.brandon3055.draconicevolution.common.tileentities.energynet.TileEnergyTransceiver", ordinal("Facing"));
        add("com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.TileReactorStabilizer", ordinal("Facing"));
        add("com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.TileReactorEnergyInjector", ordinal("Facing"));
        add("emt.tile.TileEntityEMT", ordinal("facing"));
        add("forestry.core.tiles.TileForestry", ordinal("Orientation"));
        add("lumien.randomthings.TileEntities.EnergyDistributors.TileEntityEnergyDistributor", ordinal("facing"));
        add("WayofTime.alchemicalWizardry.common.tileEntity.TEOrientable", ordinal("inputFace"), ordinal("outputFace"));
        // Nuclear Control (IC2 add-on with its own tiles)
        for (String tile : new String[] {"TileEntityInfoPanel", "TileEntityHowlerAlarm", "TileEntityRangeTrigger", "TileEntityThermo",
            "TileEntityInfoPanelExtender", "TileEntityEnergyCounter", "TileEntityAverageCounter"}) {
            add("shedar.mods.ic2.nuclearcontrol.tileentities." + tile, ordinal("facing"));
        }
        // Railcraft
        String railcraft = "mods.railcraft.common.blocks.";
        for (String tile : new String[] {"machine.beta.TileEngine", "machine.alpha.TileTradeStation", "machine.alpha.TileSteamTrap",
            "machine.gamma.TileDispenserCart", "machine.gamma.TileItemUnloaderAdvanced", "machine.gamma.TileLoaderEnergyBase",
            "machine.gamma.TileItemLoaderAdvanced", "machine.gamma.TileRFLoaderBase", "detector.TileDetector"}) {
            add(railcraft + tile, ordinal("direction"));
        }
        for (String tile : new String[] {"machine.beta.TileChestRailcraft", "machine.alpha.TileSteamOven",
            "machine.epsilon.TileForceTrackEmitter", "aesthetics.post.TilePostEmblem"}) {
            add(railcraft + tile, ordinal("facing"));
        }
        add(railcraft + "signals.TileSignalBase", ordinal("Facing"));
        add(railcraft + "signals.TileSwitchBase", ordinal("Facing"));
        add(railcraft + "signals.TileBoxSequencer", ordinal("sideOutput"));
        // Tinkers' Construct
        for (String tile : new String[] {"smeltery.logic.SmelteryLogic", "smeltery.logic.FaucetLogic", "smeltery.logic.SmelteryDrainLogic",
            "tools.logic.FurnaceLogic"}) {
            add("tconstruct." + tile, ordinal("Direction"));
        }
        add("tconstruct.smeltery.logic.CastingBlockLogic", ordinal("direction"));
        add("tconstruct.tools.logic.CraftingStationLogic", ordinal("ChestDirection"));
        // BuildCraft engines and construction markers
        add("buildcraft.core.lib.engines.TileEngineBase", ordinal("orientation"));
        add("buildcraft.builders.TileConstructionMarker", ordinal("direction"));
        // Et Futurum Requiem shulker boxes, Natura netherrack furnace, Thaumic Tinkerer mobilizer, Computronics detector
        add("ganymedes01.etfuturum.tileentities.TileEntityShulkerBox", ordinal("Facing"));
        add("team.chisel.block.tileentity.TileEntityPresent", new Rule(Kind.HORIZONTAL, "rotation", ALL), ordinal("conDir"));
        add("ganymedes01.etfuturum.tileentities.TileEntityGlowLichen", new Rule(Kind.MASK, "State", ALL));
        add("mods.natura.blocks.tech.NetherrackFurnaceLogic", ordinal("Direction"));
        add("thaumic.tinkerer.common.block.tile.TileEntityMobilizer", ordinal("Direction"));
        add("thaumic.tinkerer.common.block.tile.TileRPlacer", ordinal("orientation"));
        add("thaumic.tinkerer.common.block.tile.transvector.TileTransvectorDislocator", ordinal("orientation"));
        add("pl.asie.computronics.integration.railcraft.tile.TileDigitalDetector", ordinal("direction"));
        // Witching Gadgets devices
        for (String tile : new String[] {"TileEntityBlastfurnace", "TileEntityCobbleGen", "TileEntityEssentiaPump", "TileEntityIceGen",
            "TileEntitySnowGen", "TileEntitySpinningWheel", "TileEntityCuttingTable", "TileEntityWallMirror", "TileEntityLabelLibrary"}) {
            add("witchinggadgets.common.blocks.tiles." + tile, ordinal("facing"));
        }
        // Tinkers' Mechworks
        for (String tile : new String[] {"DrawbridgeLogic", "AdvancedDrawbridgeLogic", "FirestarterLogic"}) {
            add("tmechworks.blocks.logic." + tile, ordinal("Direction"));
        }
        add("tmechworks.blocks.logic.SignalTerminalLogic", sideArray("sideChannel"), sideArray("receivingSides"), sideArray("connectedSides"));
        // EnderStorage chests and tanks: the yaw quarter (0 S, 1 W, 2 N, 3 E)
        add("codechicken.enderstorage.storage.item.TileEnderChest", new Rule(Kind.HORIZONTAL, "rot", ALL));
        add("codechicken.enderstorage.storage.liquid.TileEnderTank", new Rule(Kind.HORIZONTAL, "rot", ALL));
        // Galacticraft asteroids: the astro miner base faces ForgeDirection facing + 2, beam receivers any side
        add("micdoodle8.mods.galacticraft.planets.asteroids.tile.TileEntityMinerBase", new Rule(Kind.HORIZONTAL, "facing", ALL,
            new ForgeDirection[] {ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.EAST}));
        add("micdoodle8.mods.galacticraft.planets.asteroids.tile.TileEntityBeamReceiver", ordinal("FacingSide"));
        // Thaumcraft
        String tc = "thaumcraft.common.tiles.";
        for (String tile : new String[] {"TileAlembic", "TileBrainbox", "TileCentrifuge", "TileEldritchCrabSpawner", "TileEldritchLock",
            "TileFluxScrubber", "TileJarFillable", "TileThaumatorium"}) {
            add(tc + tile, ordinal("facing"));
        }
        add(tc + "TileEssentiaCrystalizer", ordinal("face"));
        add(tc + "TileEssentiaReservoir", ordinal("face"));
        for (String tile : new String[] {"TileCrystal", "TileVisRelay", "TileBellows", "TileArcaneLamp", "TileArcaneLampFertility",
            "TileArcaneLampGrowth", "TileArcaneBoreBase"}) {
            add(tc + tile, ordinal("orientation"));
        }
        add(tc + "TileArcaneBore", ordinal("orientation"), ordinal("baseOrientation"));
        add(tc + "TileTube", ordinal("side"), sideArray("open"));
        add(tc + "TileTubeBuffer", sideArray("open"), sideArray("choke"));
        add(tc + "TileBanner", new Rule(Kind.ROTATION16, "facing", ALL));
        // BiblioCraft: ((yaw quarter + 1) % 4), 0 E, 1 S, 2 W, 3 N; weapon cases add 4 when laid flat
        ForgeDirection[] biblio = {ForgeDirection.EAST, ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.NORTH};
        String[][] angles = {{"Bookcase", "bookcaseAngle"}, {"GenericShelf", "genericShelfAngle"}, {"PotionShelf", "potionshelfAngle"},
            {"Label", "labelAngle"}, {"WeaponCase", "caseAngle"}, {"WeaponRack", "rackAngle"}, {"WritingDesk", "deskAngle"},
            {"DinnerPlate", "Angle"}, {"DiscRack", "Angle"}, {"MapFrame", "Angle"}, {"Seat", "Angle"}};
        for (String[] tile : angles) add("jds.bibliocraft.tileentities.TileEntity" + tile[0], new Rule(Kind.HORIZONTAL, tile[1], ALL, biblio));
        for (String tile : new String[] {"Clipboard", "Clock", "FancySign", "FancyWorkbench", "FramedChest", "FurniturePaneler", "Lamp",
            "Lantern", "PaintPress", "Painting", "SwordPedestal", "Typewriter"}) {
            add("jds.bibliocraft.tileentities.TileEntity" + tile, new Rule(Kind.HORIZONTAL, "angle", ALL, biblio));
        }
        // Witchery skulls on the floor: a 16-step rotation like vanilla skulls
        add("com.emoniph.witchery.blocks.BlockAlluringSkull$TileEntityAlluringSkull", new Rule(Kind.ROTATION16, "Rot", ALL));
        add("com.emoniph.witchery.blocks.BlockWolfHead$TileEntityWolfHead", new Rule(Kind.ROTATION16, "Rot", ALL));
        // Extra Utilities: generators keep (yaw quarter + 2) % 4, transfer nodes the pipe their search reached
        add("com.rwtema.extrautils.tileentity.generators.TileEntityGenerator", new Rule(Kind.HORIZONTAL, "rotation", ALL,
            new ForgeDirection[] {ForgeDirection.NORTH, ForgeDirection.EAST, ForgeDirection.SOUTH, ForgeDirection.WEST}));
        add("com.rwtema.extrautils.tileentity.transfernodes.TileEntityTransferNode", ordinal("pipe_dir"), new Rule(Kind.OFFSET, "pipe_", ALL));
        // Automagy: redcrystal face and power source side, its N/E/S/W connections; vis reader output sides (6 all)
        add("tuhljin.automagy.tiles.TileEntityRedcrystal", ordinal("orientation"), ordinal("powerSourceSide"), new Rule(Kind.NAMED_SIDES, "connect", ALL));
        add("tuhljin.automagy.tiles.TileEntityVisReader", new Rule(Kind.ORDINAL_ARRAY, "outputDir", ALL));
        // Malisis multi-block doors (rusty hatches, forcefield doors): the structure's direction
        for (String tile : new String[] {"core.tileentity.MultiBlockTileEntity", "doors.door.tileentity.RustyHatchTileEntity",
            "doors.door.tileentity.ForcefieldTileEntity"}) {
            add("net.malisis." + tile, ordinal("multiBlock/direction"));
        }
        // FloodLights
        add("de.keridos.floodlights.tileentity.TileEntityFL", ordinal("teDirection"));
        // Draconic teleporter stands: the placer's head yaw in degrees
        add("com.brandon3055.draconicevolution.common.tileentities.TileTeleporterStand", new Rule(Kind.YAW, "Rotation", ALL));
        // OpenComputers: yaw is horizontal, pitch UP, DOWN or NORTH (level); tilting is not representable
        add("li.cil.oc.common.tileentity.traits.Rotatable", new Rule(Kind.ORDINAL, "oc:yaw", "Yxz"), new Rule(Kind.ORDINAL, "oc:pitch", "y"));
    }

    private static Rule ordinal(String key) { return new Rule(Kind.ORDINAL, key, ALL); }
    private static Rule sideKeys(String prefix) { return new Rule(Kind.SIDE_KEYS, prefix, ALL); }
    private static Rule sideArray(String key) { return new Rule(Kind.SIDE_ARRAY, key, ALL); }

    private static void add(String type, Rule... rules) {
        List<Rule> list = new ArrayList<>();
        for (Rule rule : rules) list.add(rule);
        RULES.put(type, list);
    }

    private static List<Rule> rules(Object tile) {
        List<Rule> result = new ArrayList<>();
        for (Map.Entry<String, List<Rule>> entry : RULES.entrySet()) if (Reflect.is(tile, entry.getKey())) result.addAll(entry.getValue());
        return result;
    }

    @Override public String id() { return "plus:tile_facings"; }
    @Override public boolean supports(TileEntity tile) { return !rules(tile).isEmpty(); }
    @Override public NBTTagCompound capture(TileEntity tile) { return null; }
    @Override public void restore(TileEntity tile, NBTTagCompound data) {}
    @Override public boolean replacesDescriptionPacket(TileEntity tile) { return false; }
    @Override public boolean transformsNBT(TileEntity tile) { return true; }

    @Override public void transformNBT(TileEntity tile, NBTTagCompound data, char operation) {
        apply(rules(tile), data, operation);
    }

    @Override public void transformPreview(TileEntity tile, char operation) {
        NBTTagCompound data = new NBTTagCompound();
        tile.writeToNBT(data);
        apply(rules(tile), data, operation);
        tile.readFromNBT(data);
    }

    static void apply(List<Rule> rules, NBTTagCompound root, char operation) {
        for (Rule rule : rules) {
            if (rule.operations.indexOf(operation) < 0) continue;
            // "compound/key" for a key inside a compound
            NBTTagCompound data = root;
            String key = rule.key;
            int slash = key.lastIndexOf('/');
            if (slash >= 0) {
                if (!root.hasKey(key.substring(0, slash), 10)) continue;
                data = root.getCompoundTag(key.substring(0, slash));
                key = key.substring(slash + 1);
            }
            switch (rule.kind) {
                case ORDINAL: {
                    NBTBase tag = data.getTag(key);
                    if (!(tag instanceof NBTBase.NBTPrimitive)) break;
                    int value = ((NBTBase.NBTPrimitive) tag).func_150287_d();
                    if (value < 0 || value > 5) break;
                    int turned = SchematicTransform.direction(operation, ForgeDirection.getOrientation(value)).ordinal();
                    data.setTag(key, tag instanceof NBTTagByte ? new NBTTagByte((byte) turned)
                        : tag instanceof NBTTagShort ? new NBTTagShort((short) turned) : new NBTTagInt(turned));
                    break;
                }
                case NAME:
                    if (!data.hasKey(key, 8)) break;
                    try {
                        ForgeDirection side = ForgeDirection.valueOf(data.getString(key));
                        if (side != ForgeDirection.UNKNOWN) data.setString(key, SchematicTransform.direction(operation, side).name());
                    } catch (IllegalArgumentException ignored) {}
                    break;
                case SIDE_KEYS: {
                    Map<String, NBTBase> moved = new LinkedHashMap<>();
                    for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
                        String sideKey = key + side.ordinal();
                        if (!data.hasKey(sideKey)) continue;
                        moved.put(key + SchematicTransform.direction(operation, side).ordinal(), data.getTag(sideKey));
                        data.removeTag(sideKey);
                    }
                    for (Map.Entry<String, NBTBase> entry : moved.entrySet()) data.setTag(entry.getKey(), entry.getValue());
                    break;
                }
                case SIDE_ARRAY:
                    if (data.hasKey(key, 11) && data.getIntArray(key).length == 6) {
                        int[] values = data.getIntArray(key).clone();
                        SchematicTransform.sides(operation, values);
                        data.setIntArray(key, values);
                    } else if (data.hasKey(key, 7) && data.getByteArray(key).length == 6) {
                        byte[] values = data.getByteArray(key).clone();
                        SchematicTransform.sides(operation, values);
                        data.setByteArray(key, values);
                    }
                    break;
                case HORIZONTAL: {
                    NBTBase tag = data.getTag(key);
                    if (!(tag instanceof NBTBase.NBTPrimitive)) break;
                    int raw = ((NBTBase.NBTPrimitive) tag).func_150287_d();
                    ForgeDirection turned = SchematicTransform.direction(operation, rule.sides[raw & 3]);
                    int index = java.util.Arrays.asList(rule.sides).indexOf(turned);
                    if (index < 0) break;
                    index |= raw & ~3;
                    data.setTag(key, tag instanceof NBTTagByte ? new NBTTagByte((byte) index)
                        : tag instanceof NBTTagShort ? new NBTTagShort((short) index) : new NBTTagInt(index));
                    break;
                }
                case ROTATION16: {
                    NBTBase tag = data.getTag(key);
                    if (!(tag instanceof NBTBase.NBTPrimitive)) break;
                    int rotation = BlockMetaTransform.rotation16(((NBTBase.NBTPrimitive) tag).func_150287_d() & 15, operation);
                    data.setTag(key, tag instanceof NBTTagByte ? new NBTTagByte((byte) rotation)
                        : tag instanceof NBTTagShort ? new NBTTagShort((short) rotation) : new NBTTagInt(rotation));
                    break;
                }
                case OFFSET: {
                    String x = key + "x", y = key + "y", z = key + "z";
                    if (!data.hasKey(x) && !data.hasKey(y) && !data.hasKey(z)) break;
                    double[] moved = SchematicTransform.point(operation, data.getInteger(x), data.getInteger(y), data.getInteger(z), 0, 0, 0);
                    data.setInteger(x, (int) Math.round(moved[0]));
                    data.setInteger(y, (int) Math.round(moved[1]));
                    data.setInteger(z, (int) Math.round(moved[2]));
                    break;
                }
                case ORDINAL_ARRAY:
                    if (data.hasKey(key, 11)) {
                        int[] values = data.getIntArray(key).clone();
                        for (int i = 0; i < values.length; i++) {
                            if (values[i] >= 0 && values[i] <= 5) values[i] = SchematicTransform.direction(operation, ForgeDirection.getOrientation(values[i])).ordinal();
                        }
                        data.setIntArray(key, values);
                    }
                    break;
                case NAMED_SIDES: {
                    Map<String, NBTBase> moved = new LinkedHashMap<>();
                    for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
                        String sideKey = key + side.name().charAt(0);
                        if (side.offsetY != 0 || !data.hasKey(sideKey)) continue;
                        ForgeDirection turned = SchematicTransform.direction(operation, side);
                        if (turned.offsetY != 0) { moved.clear(); break; }
                        moved.put(key + turned.name().charAt(0), data.getTag(sideKey));
                    }
                    if (moved.isEmpty()) break;
                    for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) if (side.offsetY == 0) data.removeTag(key + side.name().charAt(0));
                    for (Map.Entry<String, NBTBase> entry : moved.entrySet()) data.setTag(entry.getKey(), entry.getValue());
                    break;
                }
                case YAW: {
                    NBTBase tag = data.getTag(key);
                    if (!(tag instanceof NBTBase.NBTPrimitive)) break;
                    double yaw = ((NBTBase.NBTPrimitive) tag).func_150286_g();
                    double turned = operation == 'Y' ? yaw + 90 : operation == 'x' ? -yaw : operation == 'z' ? 180 - yaw : yaw;
                    turned = ((turned % 360) + 360) % 360;
                    if (tag instanceof net.minecraft.nbt.NBTTagFloat) data.setFloat(key, (float) turned);
                    else if (tag instanceof net.minecraft.nbt.NBTTagDouble) data.setDouble(key, turned);
                    else data.setInteger(key, (int) Math.round(turned));
                    break;
                }
                case MASK: {
                    NBTBase tag = data.getTag(key);
                    if (!(tag instanceof NBTBase.NBTPrimitive)) break;
                    int mask = SchematicTransform.sideMask(operation, ((NBTBase.NBTPrimitive) tag).func_150287_d());
                    data.setTag(key, tag instanceof NBTTagByte ? new NBTTagByte((byte) mask)
                        : tag instanceof NBTTagShort ? new NBTTagShort((short) mask) : new NBTTagInt(mask));
                    break;
                }
            }
        }
    }

    /** The rules for a tile type name, for tests. */
    static List<Rule> rulesFor(String type) { return RULES.getOrDefault(type, new ArrayList<>()); }
}
