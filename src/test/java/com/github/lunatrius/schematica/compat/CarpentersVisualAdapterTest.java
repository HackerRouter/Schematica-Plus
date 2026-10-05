package com.github.lunatrius.schematica.compat;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.Test;

import com.github.lunatrius.schematica.compat.CarpentersVisualAdapter.Kind;

import static org.junit.Assert.*;

public class CarpentersVisualAdapterTest {
    @Test public void slopesAndStairsKeepTheirShapeAndTurnTheirFacings() {
        // Y turns north to east: a wedge rising to the north (UP, NORTH: 8) rises to the east (UP, EAST: 11)
        assertEquals(11, CarpentersVisualAdapter.data(Kind.SLOPE, 8, 'Y'));
        // corner SE (0) -> SW (3); NW (1) -> NE (2)
        assertEquals(3, CarpentersVisualAdapter.data(Kind.SLOPE, 0, 'Y'));
        assertEquals(2, CarpentersVisualAdapter.data(Kind.SLOPE, 1, 'Y'));
        // mirrored on x: west <-> east
        assertEquals(10, CarpentersVisualAdapter.data(Kind.SLOPE, 11, 'x'));
        assertEquals(36 + 2, CarpentersVisualAdapter.data(Kind.SLOPE, 36 + 1, 'x'));
        // upside down: UP NORTH (8) <-> DOWN NORTH (4); a prism has no downward version and stays
        assertEquals(4, CarpentersVisualAdapter.data(Kind.SLOPE, 8, 'y'));
        assertEquals(46, CarpentersVisualAdapter.data(Kind.SLOPE, 46, 'y'));
        // turned about X (down -> south, north -> down): a floor wedge to the north faces south, a corner wedge SE becomes UP EAST
        assertEquals(5, CarpentersVisualAdapter.data(Kind.SLOPE, 4, 'X'));
        assertEquals(11, CarpentersVisualAdapter.data(Kind.SLOPE, 0, 'X'));
        assertEquals(51, CarpentersVisualAdapter.data(Kind.SLOPE, 50, 'Y'));
        assertEquals(60, CarpentersVisualAdapter.data(Kind.SLOPE, 60, 'Y'));
        for (int id = 0; id < 65; id++) {
            int turned = id;
            for (int i = 0; i < 4; i++) turned = CarpentersVisualAdapter.data(Kind.SLOPE, turned, 'Y');
            assertEquals(id, turned);
            assertEquals(id, CarpentersVisualAdapter.data(Kind.SLOPE, CarpentersVisualAdapter.data(Kind.SLOPE, id, 'z'), 'z'));
        }
        assertEquals(26, CarpentersVisualAdapter.data(Kind.STAIRS, 24, 'z'));
        assertEquals(99, CarpentersVisualAdapter.data(Kind.STAIRS, 99, 'Y'));
    }

    @Test public void directionBitsTurnAndOtherBitsStay() {
        // a button on the north face (2), pressed (0x8) -> east face (5)
        assertEquals(0x8 | 5, CarpentersVisualAdapter.data(Kind.SIDED, 0x8 | 2, 'Y'));
        assertEquals(0x30 | 5 << 7, CarpentersVisualAdapter.data(Kind.DAYLIGHT, 0x30 | 2 << 7, 'Y'));
        assertEquals(0x105 | 4 << 4, CarpentersVisualAdapter.data(Kind.GARAGE, 0x105 | 5 << 4, 'x'));
        // a lever on the floor swaps its axis when turned about Y
        assertEquals(0x40 | 1, CarpentersVisualAdapter.data(Kind.LEVER, 1, 'Y'));
        // slab: data 1 is west (DIR_MAP 4) -> north (2), which is data 5
        assertEquals(5, CarpentersVisualAdapter.data(Kind.SLAB, 1, 'Y'));
        assertEquals(0, CarpentersVisualAdapter.data(Kind.SLAB, 0, 'Y'));
        // hatch dir Z_NEG (0) -> X_POS (3), like its rotateBlock(UP); mirrored on y it changes low/high
        assertEquals(0x1 | 3 << 5, CarpentersVisualAdapter.data(Kind.HATCH, 0x1, 'Y'));
        assertEquals(0x8, CarpentersVisualAdapter.data(Kind.HATCH, 0, 'y'));
        // door facing XP (0) -> ZP (1) like its rotateBlock(UP); a mirror changes the hinge side
        assertEquals(1 << 4, CarpentersVisualAdapter.data(Kind.DOOR, 0, 'Y'));
        assertEquals(2 << 4 | 0x8, CarpentersVisualAdapter.data(Kind.DOOR, 0, 'x'));
        // gate on X opening +Z -> on Z opening -X
        assertEquals(0x20 | 0x10, CarpentersVisualAdapter.data(Kind.GATE, 0, 'Y'));
        // bed facing north (rotation 0) -> east (1), head bit kept
        assertEquals(0x8000 | 1 << 13, CarpentersVisualAdapter.data(Kind.BED, 0x8000, 'Y'));
        assertEquals(0x4 | 3, CarpentersVisualAdapter.data(Kind.SAFE, 0x4 | 1, 'x'));
        // collapsible on the floor: the depth of corner -X-Z (shift 18) moves to +X-Z (shift 8)
        assertEquals(1 | 7 << 8, CarpentersVisualAdapter.data(Kind.COLLAPSIBLE, 1 | 7 << 18, 'Y'));
    }

    @Test public void sideFacesMoveWithTheirSides() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (int id : new int[] {2, 6, 9, 20, 22}) {
            NBTTagCompound attribute = new NBTTagCompound();
            attribute.setByte("cbAttribute", (byte) id);
            list.appendTag(attribute);
        }
        tag.setTag("cbAttrList", list);
        for (int side = 0; side < 7; side++) tag.setString("cbChiselDesign_" + side, side == 2 ? "north" : side == 6 ? "base" : "");
        tag.setShort("cbMetadata", (short) 8);
        CarpentersVisualAdapter.transform(Kind.SLOPE, tag, 'Y');
        assertEquals(5, list.getCompoundTagAt(0).getByte("cbAttribute"));
        assertEquals(6, list.getCompoundTagAt(1).getByte("cbAttribute"));
        assertEquals(7 + 5, list.getCompoundTagAt(2).getByte("cbAttribute"));
        assertEquals(20, list.getCompoundTagAt(3).getByte("cbAttribute"));
        assertEquals(22, list.getCompoundTagAt(4).getByte("cbAttribute"));
        assertEquals("north", tag.getString("cbChiselDesign_5"));
        assertEquals("", tag.getString("cbChiselDesign_2"));
        assertEquals("base", tag.getString("cbChiselDesign_6"));
        assertEquals(11, tag.getShort("cbMetadata"));
        assertEquals(Kind.SLOPE, CarpentersVisualAdapter.kind("CarpentersBlocks:blockCarpentersSlope"));
        assertEquals(Kind.NONE, CarpentersVisualAdapter.kind("minecraft:stone"));
    }
}
