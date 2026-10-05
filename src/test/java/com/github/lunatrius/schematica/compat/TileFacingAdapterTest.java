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
}
