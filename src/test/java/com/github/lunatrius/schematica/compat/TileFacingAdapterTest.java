package com.github.lunatrius.schematica.compat;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;

import static org.junit.Assert.*;

public class TileFacingAdapterTest {
    @Test public void ordinalsKeepTheirTagTypeAndSideDataMoves() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setShort("facing", (short) 2);
        tag.setShort("face2", (short) 1);
        tag.setByte("hasFaces", (byte) 1);
        TileFacingAdapter.apply(TileFacingAdapter.rulesFor("crazypants.enderio.machine.AbstractMachineEntity"), tag, 'Y');
        assertEquals(5, tag.getShort("facing"));
        assertEquals(net.minecraftforge.common.util.Constants.NBT.TAG_SHORT, tag.getTag("facing").getId());
        assertEquals(1, tag.getShort("face5"));
        assertFalse(tag.hasKey("face2"));
        assertEquals(1, tag.getByte("hasFaces"));

        NBTTagCompound barrel = new NBTTagCompound();
        barrel.setInteger("orientation", 4);
        barrel.setInteger("rotation", 1);
        barrel.setIntArray("sideUpgrades", new int[] {0, 0, 0, 0, 9, 0});
        TileFacingAdapter.apply(TileFacingAdapter.rulesFor("mcp.mobius.betterbarrels.common.blocks.TileEntityBarrel"), barrel, 'x');
        assertEquals(5, barrel.getInteger("orientation"));
        assertEquals(1, barrel.getInteger("rotation"));
        assertEquals(9, barrel.getIntArray("sideUpgrades")[5]);

        NBTTagCompound computer = new NBTTagCompound();
        computer.setInteger("oc:yaw", 3);
        computer.setInteger("oc:pitch", 1);
        TileFacingAdapter.apply(TileFacingAdapter.rulesFor("li.cil.oc.common.tileentity.traits.Rotatable"), computer, 'X');
        assertEquals(3, computer.getInteger("oc:yaw"));
        assertEquals(1, computer.getInteger("oc:pitch"));
        TileFacingAdapter.apply(TileFacingAdapter.rulesFor("li.cil.oc.common.tileentity.traits.Rotatable"), computer, 'y');
        assertEquals(0, computer.getInteger("oc:pitch"));
        TileFacingAdapter.apply(TileFacingAdapter.rulesFor("li.cil.oc.common.tileentity.traits.Rotatable"), computer, 'Y');
        assertEquals(4, computer.getInteger("oc:yaw"));
    }

    @Test public void galacticraftAsteroidMinerBaseUsesItsOwnSideTable() {
        net.minecraft.nbt.NBTTagCompound data = new net.minecraft.nbt.NBTTagCompound();
        data.setInteger("facing", 0);
        TileFacingAdapter.apply(TileFacingAdapter.rulesFor("micdoodle8.mods.galacticraft.planets.asteroids.tile.TileEntityMinerBase"), data, 'Y');
        // facing 0 is north (ForgeDirection 0 + 2); turned it faces east, value 3
        assertEquals(3, data.getInteger("facing"));
        TileFacingAdapter.apply(TileFacingAdapter.rulesFor("micdoodle8.mods.galacticraft.planets.asteroids.tile.TileEntityMinerBase"), data, 'x');
        assertEquals(2, data.getInteger("facing"));
    }

    private static NBTTagCompound turned(String type, NBTTagCompound data, String operations) {
        for (char operation : operations.toCharArray()) TileFacingAdapter.apply(TileFacingAdapter.rulesFor(type), data, operation);
        return data;
    }

    @Test public void thaumcraftTubesAndBanners() {
        NBTTagCompound tube = new NBTTagCompound();
        tube.setInteger("side", 2);
        tube.setByteArray("open", new byte[] {1, 1, 0, 1, 1, 1});
        turned("thaumcraft.common.tiles.TileTube", tube, "Y");
        assertEquals(5, tube.getInteger("side"));
        assertArrayEquals(new byte[] {1, 1, 1, 1, 1, 0}, tube.getByteArray("open"));

        NBTTagCompound banner = new NBTTagCompound();
        banner.setByte("facing", (byte) 0);
        turned("thaumcraft.common.tiles.TileBanner", banner, "Y");
        assertEquals(4, banner.getByte("facing"));
        turned("thaumcraft.common.tiles.TileBanner", banner, "x");
        assertEquals(12, banner.getByte("facing"));
        assertEquals(net.minecraftforge.common.util.Constants.NBT.TAG_BYTE, banner.getTag("facing").getId());
    }

    @Test public void biblioCraftAnglesKeepTheirFlatFlag() {
        NBTTagCompound shelf = new NBTTagCompound();
        shelf.setInteger("genericShelfAngle", 0);
        turned("jds.bibliocraft.tileentities.TileEntityGenericShelf", shelf, "Y");
        assertEquals(1, shelf.getInteger("genericShelfAngle"));
        turned("jds.bibliocraft.tileentities.TileEntityGenericShelf", shelf, "z");
        assertEquals(3, shelf.getInteger("genericShelfAngle"));

        NBTTagCompound weaponCase = new NBTTagCompound();
        weaponCase.setInteger("caseAngle", 4 + 2);
        turned("jds.bibliocraft.tileentities.TileEntityWeaponCase", weaponCase, "Y");
        assertEquals(4 + 3, weaponCase.getInteger("caseAngle"));
        turned("jds.bibliocraft.tileentities.TileEntityWeaponCase", weaponCase, "YYYY");
        assertEquals(4 + 3, weaponCase.getInteger("caseAngle"));
    }

    @Test public void extraUtilitiesGeneratorsAndTransferNodeSearch() {
        NBTTagCompound generator = new NBTTagCompound();
        generator.setInteger("rotation", 0);
        turned("com.rwtema.extrautils.tileentity.generators.TileEntityGenerator", generator, "Y");
        assertEquals(1, generator.getInteger("rotation"));
        turned("com.rwtema.extrautils.tileentity.generators.TileEntityGenerator", generator, "x");
        assertEquals(3, generator.getInteger("rotation"));

        NBTTagCompound node = new NBTTagCompound();
        node.setInteger("pipe_x", 0);
        node.setInteger("pipe_y", 1);
        node.setInteger("pipe_z", -3);
        node.setInteger("pipe_dir", 2);
        turned("com.rwtema.extrautils.tileentity.transfernodes.TileEntityTransferNode", node, "Y");
        assertEquals(3, node.getInteger("pipe_x"));
        assertEquals(1, node.getInteger("pipe_y"));
        assertEquals(0, node.getInteger("pipe_z"));
        assertEquals(5, node.getInteger("pipe_dir"));

        NBTTagCompound fresh = new NBTTagCompound();
        fresh.setInteger("pipe_dir", 6);
        turned("com.rwtema.extrautils.tileentity.transfernodes.TileEntityTransferNode", fresh, "Y");
        assertEquals(6, fresh.getInteger("pipe_dir"));
        assertFalse(fresh.hasKey("pipe_x"));
    }

    @Test public void witcherySkullsTurnLikeVanillaSkulls() {
        NBTTagCompound skull = new NBTTagCompound();
        skull.setByte("Rot", (byte) 3);
        turned("com.emoniph.witchery.blocks.BlockWolfHead$TileEntityWolfHead", skull, "Y");
        assertEquals(7, skull.getByte("Rot"));
    }

    @Test public void automagyRedcrystalAndVisReader() {
        NBTTagCompound crystal = new NBTTagCompound();
        crystal.setShort("orientation", (short) 2);
        crystal.setShort("powerSourceSide", (short) -1);
        crystal.setBoolean("connectN", true);
        crystal.setBoolean("connectE", false);
        crystal.setBoolean("connectS", false);
        crystal.setBoolean("connectW", true);
        turned("tuhljin.automagy.tiles.TileEntityRedcrystal", crystal, "Y");
        assertEquals(5, crystal.getShort("orientation"));
        assertEquals(-1, crystal.getShort("powerSourceSide"));
        assertTrue(crystal.getBoolean("connectE"));
        assertTrue(crystal.getBoolean("connectN"));
        assertFalse(crystal.getBoolean("connectS"));
        assertFalse(crystal.getBoolean("connectW"));
        turned("tuhljin.automagy.tiles.TileEntityRedcrystal", crystal, "X");
        assertTrue(crystal.getBoolean("connectE"));

        NBTTagCompound reader = new NBTTagCompound();
        reader.setIntArray("outputDir", new int[] {2, 6, 0, 4});
        turned("tuhljin.automagy.tiles.TileEntityVisReader", reader, "Y");
        assertArrayEquals(new int[] {5, 6, 0, 2}, reader.getIntArray("outputDir"));
    }

    @Test public void nestedKeysAndYaw() {
        NBTTagCompound hatch = new NBTTagCompound();
        NBTTagCompound multi = new NBTTagCompound();
        multi.setInteger("direction", 2);
        hatch.setTag("multiBlock", multi);
        turned("net.malisis.core.tileentity.MultiBlockTileEntity", hatch, "Y");
        assertEquals(5, hatch.getCompoundTag("multiBlock").getInteger("direction"));

        NBTTagCompound stand = new NBTTagCompound();
        stand.setInteger("Rotation", 300);
        turned("com.brandon3055.draconicevolution.common.tileentities.TileTeleporterStand", stand, "Y");
        assertEquals(30, stand.getInteger("Rotation"));
        turned("com.brandon3055.draconicevolution.common.tileentities.TileTeleporterStand", stand, "x");
        assertEquals(330, stand.getInteger("Rotation"));
    }

    @Test public void architectureSideTurnRitualsAndStickyJars() {
        // bottom facing north (side 2) turned clockwise from above faces east (side 5)
        assertArrayEquals(new int[] {5, 0}, SideTurn.apply(2, 0, 'Y'));
        // four turns and two mirrors give every orientation back
        for (int side = 0; side < 6; side++) for (int turn = 0; turn < 4; turn++) {
            int[] value = {side, turn};
            for (int i = 0; i < 4; i++) value = SideTurn.apply(value[0], value[1], 'Y');
            assertArrayEquals(new int[] {side, turn}, value);
            for (char op : "XZxyz".toCharArray()) {
                int[] once = SideTurn.apply(side, turn, op);
                if (Character.isLowerCase(op)) assertArrayEquals(new int[] {side, turn}, SideTurn.apply(once[0], once[1], op));
            }
        }
        assertEquals(1, SideTurn.apply(0, 0, 'y')[0]);
        assertEquals(5, SideTurn.apply(4, 1, 'x')[0]);

        NBTTagCompound shape = new NBTTagCompound();
        turned("gcewing.architecture.common.tile.TileArchitecture", shape, "X");
        assertEquals(3, shape.getByte("side"));

        NBTTagCompound stone = new NBTTagCompound();
        stone.setInteger("direction", 1);
        turned("WayofTime.alchemicalWizardry.common.tileEntity.TEMasterStone", stone, "Y");
        assertEquals(2, stone.getInteger("direction"));
        turned("WayofTime.alchemicalWizardry.common.tileEntity.TEMasterStone", stone, "x");
        assertEquals(4, stone.getInteger("direction"));
        stone.setInteger("direction", 0);
        turned("WayofTime.alchemicalWizardry.common.tileEntity.TEMasterStone", stone, "Y");
        assertEquals(0, stone.getInteger("direction"));

        NBTTagCompound jar = new NBTTagCompound();
        jar.setInteger("placedOn", 2);
        NBTTagCompound parent = new NBTTagCompound();
        parent.setByte("facing", (byte) 3);
        jar.setTag("parent", parent);
        turned("makeo.gadomancy.common.blocks.tiles.TileStickyJar", jar, "Y");
        assertEquals(5, jar.getInteger("placedOn"));
        assertEquals(4, jar.getCompoundTag("parent").getByte("facing"));
    }

    @Test public void arcLampsAndPanels() {
        // a lamp on the floor (1) lighting north (facing 0 -> side 2) lights east (side 5, facing 3) after a turn
        assertEquals(3, TileFacingAdapter.arcLamp(1, 0, 'Y'));
        // on the west wall (4) lighting north (facing 2): now on the north wall (2), lighting east (7 - 2)
        assertEquals(2, TileFacingAdapter.arcLamp(2, 2, 'Y'));
        for (int side = 0; side < 6; side++) for (int facing = 0; facing < 4; facing++) {
            for (char op : "XYZxyz".toCharArray()) {
                int turnedSide = com.github.lunatrius.schematica.util.SchematicTransform.direction(op, net.minecraftforge.common.util.ForgeDirection.getOrientation(side)).ordinal();
                int turned = TileFacingAdapter.arcLamp(turnedSide, facing, op);
                assertTrue(turned >= 0);
                assertEquals(com.github.lunatrius.schematica.util.SchematicTransform.direction(op,
                    net.minecraftforge.common.util.ForgeDirection.getOrientation(TileFacingAdapter.arcLampSide(side, facing))).ordinal(),
                    TileFacingAdapter.arcLampSide(turnedSide, turned));
            }
        }
        NBTTagCompound lamp = new NBTTagCompound();
        lamp.setInteger("Facing", 0);
        TileFacingAdapter.apply(TileFacingAdapter.rulesFor("micdoodle8.mods.galacticraft.core.tile.TileEntityArclamp"), lamp, 'Y', 1);
        assertEquals(3, lamp.getInteger("Facing"));

        // floor panel (facing up) reading north turns to read east (rotation 1), a ceiling panel to rotation 2
        assertArrayEquals(new int[] {1, 1}, TileFacingAdapter.panel(1, 0, 'Y'));
        assertArrayEquals(new int[] {0, 2}, TileFacingAdapter.panel(0, 0, 'Y'));
        assertArrayEquals(new int[] {5, 0}, TileFacingAdapter.panel(2, 0, 'Y'));
        for (int facing = 0; facing < 6; facing++) for (int rotation = 0; rotation < 4; rotation++) {
            int[] value = {facing, rotation};
            for (int i = 0; i < 4; i++) value = TileFacingAdapter.panel(value[0], value[1], 'X');
            assertArrayEquals(new int[] {facing, rotation}, value);
            for (char op : "xyz".toCharArray()) {
                int[] once = TileFacingAdapter.panel(facing, rotation, op);
                assertArrayEquals(new int[] {facing, rotation}, TileFacingAdapter.panel(once[0], once[1], op));
            }
        }
        NBTTagCompound panel = new NBTTagCompound();
        panel.setShort("facing", (short) 1);
        panel.setInteger("rotation", 1);
        panel.setByte("rotateHor", (byte) 5);
        panel.setByte("rotateVert", (byte) 3);
        turned("shedar.mods.ic2.nuclearcontrol.tileentities.TileEntityInfoPanel", panel, "x");
        assertEquals(1, panel.getShort("facing"));
        assertEquals(2, panel.getInteger("rotation"));
        assertEquals(-5, panel.getByte("rotateHor"));
        assertEquals(3, panel.getByte("rotateVert"));
    }

    @Test public void architectureMirrors() {
        // a roof outer corner mirrored across x is the same corner a quarter turn about its local y; LH cornices turn RH
        assertArrayEquals(new int[] {0, 3, 1}, SideTurn.apply(0, 0, 1, 'x'));
        assertArrayEquals(new int[] {0, 0, 41}, SideTurn.apply(0, 0, 40, 'x'));
        assertArrayEquals(new int[] {0, 0, 91}, SideTurn.apply(0, 0, 91, 'x'));
        assertArrayEquals(new int[] {5, 0, 1}, SideTurn.apply(2, 0, 1, 'Y'));
        for (int shape = 0; shape < 120; shape++) for (int side = 0; side < 6; side++) for (int turn = 0; turn < 4; turn++) {
            for (char op : "xyz".toCharArray()) {
                int[] once = SideTurn.apply(side, turn, shape, op);
                assertArrayEquals(new int[] {side, turn, shape}, SideTurn.apply(once[0], once[1], once[2], op));
            }
        }
        NBTTagCompound tile = new NBTTagCompound();
        tile.setInteger("Shape", 50);
        tile.setByte("offsetX", (byte) 4);
        tile.setInteger("Disconnected", 1 << 2);
        java.util.List<TileFacingAdapter.Rule> rules = new java.util.ArrayList<>(TileFacingAdapter.rulesFor("gcewing.architecture.common.tile.TileArchitecture"));
        rules.addAll(TileFacingAdapter.rulesFor("gcewing.architecture.common.tile.TileShape"));
        TileFacingAdapter.apply(rules, tile, 'z');
        assertEquals(51, tile.getInteger("Shape"));
        assertEquals(-4, tile.getByte("offsetX"));
        assertEquals(1 << 3, tile.getInteger("Disconnected"));
        TileFacingAdapter.apply(rules, tile, 'Y');
        assertEquals(51, tile.getInteger("Shape"));
        assertEquals(-4, tile.getByte("offsetX"));
        assertEquals(1 << 4, tile.getInteger("Disconnected"));
    }

    @Test public void castingChannels() {
        NBTTagCompound channel = new NBTTagCompound();
        channel.setIntArray("validOutputs", new int[] {0, 2, 5});
        channel.setInteger("LastProvider", 4);
        for (String side : new String[] {"NORTH", "SOUTH", "WEST", "EAST"}) {
            NBTTagCompound tank = new NBTTagCompound();
            tank.setString("side", side);
            channel.setTag("subTank_" + side, tank);
        }
        turned("tconstruct.smeltery.logic.CastingChannelLogic", channel, "Y");
        assertArrayEquals(new int[] {0, 5, 3}, channel.getIntArray("validOutputs"));
        assertEquals(2, channel.getInteger("LastProvider"));
        assertEquals("NORTH", channel.getCompoundTag("subTank_EAST").getString("side"));
        assertEquals("WEST", channel.getCompoundTag("subTank_NORTH").getString("side"));
        // tilted, the sub tanks would need up/down keys the channel does not read: they stay
        turned("tconstruct.smeltery.logic.CastingChannelLogic", channel, "X");
        assertEquals("NORTH", channel.getCompoundTag("subTank_EAST").getString("side"));
    }
}
