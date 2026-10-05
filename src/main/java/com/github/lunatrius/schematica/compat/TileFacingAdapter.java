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

/**
 * Rotation and mirroring of directions mod tile entities keep in their saved NBT (GTNH 2.8.4 / 2.9.0 sources):
 * ForgeDirection ordinals or names, side-indexed arrays and per-side keys. The preview tile reads the turned NBT
 * back. Blocks handled here are not also turned through their Block.rotateBlock.
 */
final class TileFacingAdapter implements ISchematicVisualAdapter {
    enum Kind { ORDINAL, NAME, SIDE_KEYS, SIDE_ARRAY, MASK, HORIZONTAL }

    static final class Rule {
        final Kind kind;
        final String key;
        /** The operations the rule applies to; others leave the value alone (OpenComputers yaw/pitch). */
        final String operations;
        Rule(Kind kind, String key, String operations) { this.kind = kind; this.key = key; this.operations = operations; }
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
        add("mods.natura.blocks.tech.NetherrackFurnaceLogic", ordinal("Direction"));
        add("thaumic.tinkerer.common.block.tile.TileEntityMobilizer", ordinal("Direction"));
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

    static void apply(List<Rule> rules, NBTTagCompound data, char operation) {
        for (Rule rule : rules) {
            if (rule.operations.indexOf(operation) < 0) continue;
            switch (rule.kind) {
                case ORDINAL: {
                    NBTBase tag = data.getTag(rule.key);
                    if (!(tag instanceof NBTBase.NBTPrimitive)) break;
                    int value = ((NBTBase.NBTPrimitive) tag).func_150287_d();
                    if (value < 0 || value > 5) break;
                    int turned = SchematicTransform.direction(operation, ForgeDirection.getOrientation(value)).ordinal();
                    data.setTag(rule.key, tag instanceof NBTTagByte ? new NBTTagByte((byte) turned)
                        : tag instanceof NBTTagShort ? new NBTTagShort((short) turned) : new NBTTagInt(turned));
                    break;
                }
                case NAME:
                    if (!data.hasKey(rule.key, 8)) break;
                    try {
                        ForgeDirection side = ForgeDirection.valueOf(data.getString(rule.key));
                        if (side != ForgeDirection.UNKNOWN) data.setString(rule.key, SchematicTransform.direction(operation, side).name());
                    } catch (IllegalArgumentException ignored) {}
                    break;
                case SIDE_KEYS: {
                    Map<String, NBTBase> moved = new LinkedHashMap<>();
                    for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
                        String key = rule.key + side.ordinal();
                        if (!data.hasKey(key)) continue;
                        moved.put(rule.key + SchematicTransform.direction(operation, side).ordinal(), data.getTag(key));
                        data.removeTag(key);
                    }
                    for (Map.Entry<String, NBTBase> entry : moved.entrySet()) data.setTag(entry.getKey(), entry.getValue());
                    break;
                }
                case SIDE_ARRAY:
                    if (data.hasKey(rule.key, 11) && data.getIntArray(rule.key).length == 6) {
                        int[] values = data.getIntArray(rule.key).clone();
                        SchematicTransform.sides(operation, values);
                        data.setIntArray(rule.key, values);
                    } else if (data.hasKey(rule.key, 7) && data.getByteArray(rule.key).length == 6) {
                        byte[] values = data.getByteArray(rule.key).clone();
                        SchematicTransform.sides(operation, values);
                        data.setByteArray(rule.key, values);
                    }
                    break;
                case HORIZONTAL: {
                    NBTBase tag = data.getTag(rule.key);
                    if (!(tag instanceof NBTBase.NBTPrimitive)) break;
                    int value = ((NBTBase.NBTPrimitive) tag).func_150287_d() & 3;
                    ForgeDirection turned = SchematicTransform.direction(operation, HORIZONTAL_SIDES[value]);
                    int index = java.util.Arrays.asList(HORIZONTAL_SIDES).indexOf(turned);
                    if (index < 0) break;
                    data.setTag(rule.key, tag instanceof NBTTagByte ? new NBTTagByte((byte) index)
                        : tag instanceof NBTTagShort ? new NBTTagShort((short) index) : new NBTTagInt(index));
                    break;
                }
                case MASK: {
                    NBTBase tag = data.getTag(rule.key);
                    if (!(tag instanceof NBTBase.NBTPrimitive)) break;
                    int mask = SchematicTransform.sideMask(operation, ((NBTBase.NBTPrimitive) tag).func_150287_d());
                    data.setTag(rule.key, tag instanceof NBTTagByte ? new NBTTagByte((byte) mask)
                        : tag instanceof NBTTagShort ? new NBTTagShort((short) mask) : new NBTTagInt(mask));
                    break;
                }
            }
        }
    }

    /** The rules for a tile type name, for tests. */
    static List<Rule> rulesFor(String type) { return RULES.getOrDefault(type, new ArrayList<>()); }
}
