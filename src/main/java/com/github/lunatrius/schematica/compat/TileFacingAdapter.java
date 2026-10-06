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
    enum Kind { ORDINAL, NAME, SIDE_KEYS, SIDE_ARRAY, MASK, HORIZONTAL, ROTATION16, OFFSET, ORDINAL_ARRAY, NAMED_SIDES, YAW, SIDE_TURN, HORIZONTAL1, ARC_LAMP, PANEL, NAME_KEYS }

    static final class Rule {
        final Kind kind;
        final String key;
        /** The operations the rule applies to; others leave the value alone (OpenComputers yaw/pitch). */
        final String operations;
        /** HORIZONTAL: the sides the low two bits 0-3 stand for; higher bits are kept. HORIZONTAL1: the sides of 1-4. */
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
        for (String tile : new String[] {"TileEntityHowlerAlarm", "TileEntityRangeTrigger", "TileEntityThermo",
            "TileEntityInfoPanelExtender", "TileEntityEnergyCounter", "TileEntityAverageCounter"}) {
            add("shedar.mods.ic2.nuclearcontrol.tileentities." + tile, ordinal("facing"));
        }
        // Nuclear Control panels: the screen's facing with its quarter turn on it (and the advanced panel's tilt)
        add("shedar.mods.ic2.nuclearcontrol.tileentities.TileEntityInfoPanel", new Rule(Kind.PANEL, "facing", ALL));
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
        // Tinkers' casting channels: open outputs, the side last filled from, the horizontal sub tanks by side name
        add("tconstruct.smeltery.logic.CastingChannelLogic", new Rule(Kind.ORDINAL_ARRAY, "validOutputs", ALL), ordinal("LastProvider"),
            new Rule(Kind.NAME_KEYS, "subTank_", ALL));
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
        // ArchitectureCraft shapes: the side their local bottom faces and a quarter turn around it, disconnected sides
        add("gcewing.architecture.common.tile.TileArchitecture", new Rule(Kind.SIDE_TURN, "side", ALL));
        add("gcewing.architecture.common.tile.TileShape", new Rule(Kind.MASK, "Disconnected", ALL));
        // Galacticraft arc lamps: the lit side as 0-3 relative to the side the lamp sits on (the block metadata)
        add("micdoodle8.mods.galacticraft.core.tile.TileEntityArclamp", new Rule(Kind.ARC_LAMP, "Facing", ALL));
        // Blood Magic master ritual stones (and Blood Arsenal's): the ritual's direction, 1 N 2 E 3 S 4 W
        add("WayofTime.alchemicalWizardry.common.tileEntity.TEMasterStone", new Rule(Kind.HORIZONTAL1, "direction", ALL,
            new ForgeDirection[] {ForgeDirection.NORTH, ForgeDirection.EAST, ForgeDirection.SOUTH, ForgeDirection.WEST}));
        // Draconic gates, Thaumic Horizons node monitors/amplifiers/stabilizers, Ender IO reservoirs, Loot Games,
        // Gadomancy sticky jars (and the jar inside), Botanic Horizons automation blocks (N, E, S, W)
        add("com.brandon3055.draconicevolution.common.tileentities.gates.TileGate", ordinal("Output"));
        add("com.kentington.thaumichorizons.common.tiles.TileNodeMonitor", ordinal("dir"));
        add("com.kentington.thaumichorizons.common.tiles.TileTransductionAmplifier", ordinal("dir"));
        add("com.kentington.thaumichorizons.common.tiles.TileVortexStabilizer", ordinal("direction"));
        add("crazypants.enderio.machine.reservoir.TileReservoir", ordinal("front"), ordinal("up"), ordinal("right"));
        add("eu.usrv.legacylootgames.gol.tiles.LegacyGameOfLightTile", ordinal("mTEDirection"));
        add("makeo.gadomancy.common.blocks.tiles.TileStickyJar", ordinal("placedOn"), ordinal("parent/facing"));
        add("net.fuzzycraft.botanichorizons.addons.tileentity.AutomationTileEntity", new Rule(Kind.HORIZONTAL, "face", ALL,
            new ForgeDirection[] {ForgeDirection.NORTH, ForgeDirection.EAST, ForgeDirection.SOUTH, ForgeDirection.WEST}));
        // Thaumic Energistics providers (6 none), Mechworks signal bus placed sides, Steve's Factory Manager breaker placing side
        add("thaumicenergistics.common.tiles.abstraction.TileProviderBase", ordinal("TEAttachSide"));
        add("tmechworks.blocks.logic.SignalBusLogic", new Rule(Kind.MASK, "placedSides", ALL));
        add("vswe.stevesfactory.blocks.TileEntityBreaker", ordinal("Direction"));
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
        apply(rules(tile), data, operation, metadata(tile));
    }

    @Override public void transformPreview(TileEntity tile, char operation) {
        NBTTagCompound data = new NBTTagCompound();
        tile.writeToNBT(data);
        apply(rules(tile), data, operation, metadata(tile));
        tile.readFromNBT(data);
    }

    /** The metadata of the tile's block, already turned with the schematic (SchematicWorld sets it); -1 if unknown. */
    private static int metadata(TileEntity tile) {
        if (tile.blockMetadata >= 0) return tile.blockMetadata;
        try {
            return tile.getWorldObj() == null ? -1 : tile.getWorldObj().getBlockMetadata(tile.xCoord, tile.yCoord, tile.zCoord);
        } catch (RuntimeException e) {
            return -1;
        }
    }

    static void apply(List<Rule> rules, NBTTagCompound root, char operation) { apply(rules, root, operation, -1); }

    static void apply(List<Rule> rules, NBTTagCompound root, char operation, int metadata) {
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
                case HORIZONTAL1: {
                    NBTBase tag = data.getTag(key);
                    if (!(tag instanceof NBTBase.NBTPrimitive)) break;
                    int value = ((NBTBase.NBTPrimitive) tag).func_150287_d();
                    if (value < 1 || value > 4) break;
                    int index = java.util.Arrays.asList(rule.sides).indexOf(SchematicTransform.direction(operation, rule.sides[value - 1]));
                    if (index >= 0) data.setInteger(key, index + 1);
                    break;
                }
                case SIDE_TURN: {
                    int shape = data.hasKey("Shape") ? data.getInteger("Shape") : -1;
                    int[] turned = SideTurn.apply(data.getByte(key), data.getByte("turn"), shape, operation);
                    if (turned == null) break;
                    data.setByte(key, (byte) turned[0]);
                    data.setByte("turn", (byte) turned[1]);
                    if (turned[2] != shape) data.setInteger("Shape", turned[2]);
                    if (Character.isLowerCase(operation) && data.getByte("offsetX") != 0) data.setByte("offsetX", (byte) -data.getByte("offsetX"));
                    break;
                }
                case NAME_KEYS: {
                    // keys "prefix" + ForgeDirection name; only sides that already have a key can take one
                    Map<String, NBTBase> moved = new LinkedHashMap<>();
                    for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
                        if (!data.hasKey(key + side.name())) continue;
                        ForgeDirection turned = SchematicTransform.direction(operation, side);
                        if (!data.hasKey(key + turned.name())) { moved.clear(); break; }
                        moved.put(key + turned.name(), data.getTag(key + side.name()));
                    }
                    for (Map.Entry<String, NBTBase> entry : moved.entrySet()) data.setTag(entry.getKey(), entry.getValue());
                    break;
                }
                case ARC_LAMP: {
                    if (!data.hasKey(key) || metadata < 0 || metadata > 5) break;
                    int turned = arcLamp(metadata, data.getInteger(key), operation);
                    if (turned >= 0) data.setInteger(key, turned);
                    break;
                }
                case PANEL: {
                    NBTBase tag = data.getTag(key);
                    if (!(tag instanceof NBTBase.NBTPrimitive)) break;
                    int facing = ((NBTBase.NBTPrimitive) tag).func_150287_d();
                    if (facing < 0 || facing > 5) break;
                    int[] turned = panel(facing, data.getInteger("rotation"), operation);
                    if (turned == null) break;
                    data.setTag(key, tag instanceof NBTTagByte ? new NBTTagByte((byte) turned[0])
                        : tag instanceof NBTTagShort ? new NBTTagShort((short) turned[0]) : new NBTTagInt(turned[0]));
                    data.setInteger("rotation", turned[1]);
                    if (Character.isLowerCase(operation) && data.hasKey("rotateHor")) data.setByte("rotateHor", (byte) -data.getByte("rotateHor"));
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

    /** TileEntityArclamp.updateEntity: the side a lamp sitting on `side` lights toward for its facing 0-3. */
    static int arcLampSide(int side, int facing) {
        switch (side) {
            case 0: case 1: return facing + 2;
            case 2: return facing < 2 ? facing : 7 - facing;
            case 3: return facing < 2 ? facing : facing + 2;
            case 5: return facing < 2 ? facing : 5 - facing;
            default: return facing;
        }
    }

    /** The facing of a lamp that sits on `side` after the operation and lit the turned way before it; -1 if none. */
    static int arcLamp(int side, int facing, char operation) {
        if (facing < 0 || facing > 3) return -1;
        for (ForgeDirection before : ForgeDirection.VALID_DIRECTIONS) {
            if (SchematicTransform.direction(operation, before).ordinal() != side) continue;
            ForgeDirection lit = SchematicTransform.direction(operation, ForgeDirection.getOrientation(arcLampSide(before.ordinal(), facing)));
            for (int turned = 0; turned < 4; turned++) if (arcLampSide(side, turned) == lit.ordinal()) return turned;
        }
        return -1;
    }

    /** The screen's local x and y (text up at rotation 0) for each panel facing (TileEntityInfoPanelRenderer). */
    private static final ForgeDirection[] PANEL_X = {ForgeDirection.WEST, ForgeDirection.EAST, ForgeDirection.WEST, ForgeDirection.EAST,
        ForgeDirection.SOUTH, ForgeDirection.NORTH};
    private static final ForgeDirection[] PANEL_Y = {ForgeDirection.NORTH, ForgeDirection.NORTH, ForgeDirection.UP, ForgeDirection.UP,
        ForgeDirection.UP, ForgeDirection.UP};

    /** The world direction of the screen's up for a facing and rotation (0 none, 1 -90 degrees, 2 90, 3 180). */
    static ForgeDirection panelUp(int facing, int rotation) {
        switch (rotation & 3) {
            case 1: return PANEL_X[facing];
            case 2: return PANEL_X[facing].getOpposite();
            case 3: return PANEL_Y[facing].getOpposite();
            default: return PANEL_Y[facing];
        }
    }

    /** The {facing, rotation} whose screen faces and points up the turned ways. */
    static int[] panel(int facing, int rotation, char operation) {
        ForgeDirection turned = SchematicTransform.direction(operation, ForgeDirection.getOrientation(facing));
        ForgeDirection up = SchematicTransform.direction(operation, panelUp(facing, rotation));
        for (int r = 0; r < 4; r++) if (panelUp(turned.ordinal(), r) == up) return new int[] {turned.ordinal(), r};
        return null;
    }

    /** The rules for a tile type name, for tests. */
    static List<Rule> rulesFor(String type) { return RULES.getOrDefault(type, new ArrayList<>()); }
}
