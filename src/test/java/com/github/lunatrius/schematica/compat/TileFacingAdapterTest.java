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
}
