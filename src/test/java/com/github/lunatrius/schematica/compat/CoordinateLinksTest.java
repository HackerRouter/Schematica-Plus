package com.github.lunatrius.schematica.compat;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import org.junit.Test;

import vazkii.botania.common.block.tile.TileLightRelay;

import static org.junit.Assert.*;

public class CoordinateLinksTest {
    private static NBTTagCompound relay(int x, int y, int z) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", "schematicaTestLightRelay");
        tag.setInteger("bindX", x);
        tag.setInteger("bindY", y);
        tag.setInteger("bindZ", z);
        return tag;
    }

    @Test public void capturedLinksMoveTurnAndPasteWithTheTile() {
        TileEntity.addMapping(TileLightRelay.class, "schematicaTestLightRelay");
        TileLightRelay tile = new TileLightRelay();
        NBTTagCompound tag = relay(105, 64, -20);
        CoordinateLinks.capture(tile, tag, 100, 60, -30);
        assertTrue(CoordinateLinks.local(tag));
        assertEquals(5, tag.getInteger("bindX"));
        assertEquals(4, tag.getInteger("bindY"));
        assertEquals(10, tag.getInteger("bindZ"));

        // a cell (x, z) of an 8 x 5 x 12 schematic turned by Y goes to (11 - z, x)
        CoordinateLinks.transform(tile, tag, 'Y', 8, 5, 12);
        assertEquals(1, tag.getInteger("bindX"));
        assertEquals(4, tag.getInteger("bindY"));
        assertEquals(5, tag.getInteger("bindZ"));

        tag.setInteger("x", 2);
        tag.setInteger("y", 1);
        tag.setInteger("z", 3);
        CoordinateLinks.paste(tag, 202, 71, 303);
        assertFalse(CoordinateLinks.local(tag));
        assertEquals(201, tag.getInteger("bindX"));
        assertEquals(74, tag.getInteger("bindY"));
        assertEquals(305, tag.getInteger("bindZ"));
    }

    @Test public void unboundAndUnmarkedLinksStay() {
        TileLightRelay tile = new TileLightRelay();
        NBTTagCompound unbound = relay(0, -1, 0);
        CoordinateLinks.capture(tile, unbound, 100, 60, 100);
        assertEquals(0, unbound.getInteger("bindX"));
        assertEquals(-1, unbound.getInteger("bindY"));

        NBTTagCompound old = relay(105, 64, -20);
        CoordinateLinks.shift(tile, old, 3, 3, 3);
        CoordinateLinks.transform(tile, old, 'Y', 8, 5, 12);
        assertEquals(105, old.getInteger("bindX"));
        assertEquals(-20, old.getInteger("bindZ"));
    }

    @Test public void nestedListedBoxedAndFlatLinks() {
        NBTTagCompound flower = new NBTTagCompound();
        flower.setInteger(CoordinateLinks.MARKER, 0);
        NBTTagCompound sub = new NBTTagCompound();
        sub.setInteger("collectorX", 3);
        sub.setInteger("collectorY", 2);
        sub.setInteger("collectorZ", 1);
        flower.setTag("subTileCmp", sub);
        CoordinateLinks.transform(CoordinateLinks.rulesFor("vazkii.botania.common.block.tile.TileSpecialFlower"), flower, 'x', 10, 4, 4);
        assertEquals(6, flower.getCompoundTag("subTileCmp").getInteger("collectorX"));
        assertEquals(1, flower.getCompoundTag("subTileCmp").getInteger("collectorZ"));

        NBTTagCompound relay = new NBTTagCompound();
        relay.setInteger(CoordinateLinks.MARKER, 0);
        relay.setInteger("LinkCount", 2);
        for (int i = 0; i < 2; i++) {
            relay.setInteger("X_LinkedDevice_" + i, i);
            relay.setInteger("Y_LinkedDevice_" + i, 10);
            relay.setInteger("Z_LinkedDevice_" + i, -i);
        }
        CoordinateLinks.shift(CoordinateLinks.rulesFor("com.brandon3055.draconicevolution.common.tileentities.energynet.TileRemoteEnergyBase"),
            relay, 100, 0, 50);
        assertEquals(101, relay.getInteger("X_LinkedDevice_1"));
        assertEquals(49, relay.getInteger("Z_LinkedDevice_1"));
        assertEquals(100, relay.getInteger("X_LinkedDevice_0"));

        NBTTagCompound quarry = new NBTTagCompound();
        quarry.setInteger(CoordinateLinks.MARKER, 0);
        NBTTagCompound box = new NBTTagCompound();
        box.setInteger("xMin", 2); box.setInteger("yMin", 0); box.setInteger("zMin", 1);
        box.setInteger("xMax", 5); box.setInteger("yMax", 3); box.setInteger("zMax", 4);
        quarry.setTag("box", box);
        quarry.setDouble("headPosX", 3.0);
        quarry.setDouble("headPosY", 3.0);
        quarry.setDouble("headPosZ", 2.0);
        CoordinateLinks.transform(CoordinateLinks.rulesFor("buildcraft.builders.TileQuarry"), quarry, 'x', 10, 4, 6);
        assertEquals(4, quarry.getCompoundTag("box").getInteger("xMin"));
        assertEquals(7, quarry.getCompoundTag("box").getInteger("xMax"));
        assertEquals(6.0, quarry.getDouble("headPosX"), 0);
        assertEquals(net.minecraftforge.common.util.Constants.NBT.TAG_DOUBLE, quarry.getTag("headPosX").getId());

        NBTTagCompound mobilizer = new NBTTagCompound();
        mobilizer.setInteger(CoordinateLinks.MARKER, 0);
        mobilizer.setInteger("FirstRelayX", 1);
        mobilizer.setInteger("FirstRelayZ", 2);
        java.util.List<CoordinateLinks.Spec> specs = CoordinateLinks.rulesFor("thaumic.tinkerer.common.block.tile.TileEntityMobilizer");
        CoordinateLinks.transform(specs, mobilizer, 'X', 4, 4, 4);
        assertEquals(2, mobilizer.getInteger("FirstRelayZ"));
        CoordinateLinks.transform(specs, mobilizer, 'Y', 4, 4, 4);
        assertEquals(1, mobilizer.getInteger("FirstRelayX"));
        assertEquals(1, mobilizer.getInteger("FirstRelayZ"));
    }

    @Test public void linksIntoAnotherDimensionStay() {
        NBTTagCompound mirror = new NBTTagCompound();
        mirror.setInteger(CoordinateLinks.MARKER, 0);
        mirror.setInteger("linkX", 5);
        mirror.setInteger("linkY", 5);
        mirror.setInteger("linkZ", 5);
        mirror.setInteger("linkDim", -1);
        java.util.List<CoordinateLinks.Spec> specs = CoordinateLinks.rulesFor("thaumcraft.common.tiles.TileMirror");
        CoordinateLinks.shift(specs, mirror, 10, 10, 10);
        assertEquals(5, mirror.getInteger("linkX"));
        mirror.setInteger("linkDim", 0);
        CoordinateLinks.shift(specs, mirror, 10, 10, 10);
        assertEquals(15, mirror.getInteger("linkX"));
    }

    @Test public void positionListsAndGalacticraftMultiblockParts() {
        NBTTagCompound inventarium = new NBTTagCompound();
        inventarium.setInteger(CoordinateLinks.MARKER, 0);
        net.minecraft.nbt.NBTTagList nodes = new net.minecraft.nbt.NBTTagList();
        NBTTagCompound node = new NBTTagCompound();
        node.setIntArray("pos", new int[] {110, 64, 205});
        nodes.appendTag(node);
        inventarium.setTag("Nodes", nodes);
        CoordinateLinks.shift(CoordinateLinks.rulesFor("tuhljin.automagy.tiles.TileEntityInventarium"), inventarium, -100, -60, -200);
        assertArrayEquals(new int[] {10, 4, 5}, inventarium.getTagList("Nodes", 10).getCompoundTagAt(0).getIntArray("pos"));

        NBTTagCompound dummy = new NBTTagCompound();
        dummy.setInteger(CoordinateLinks.MARKER, 0);
        NBTTagCompound main = new NBTTagCompound();
        main.setInteger("x", 1); main.setInteger("y", 0); main.setInteger("z", 2);
        dummy.setTag("mainBlockPosition", main);
        CoordinateLinks.transform(CoordinateLinks.rulesFor("micdoodle8.mods.galacticraft.core.tile.TileEntityMulti"), dummy, 'Y', 3, 1, 3);
        assertEquals(0, dummy.getCompoundTag("mainBlockPosition").getInteger("x"));
        assertEquals(1, dummy.getCompoundTag("mainBlockPosition").getInteger("z"));
    }

    @Test public void columnHeightsMoveAndFlipButDoNotTilt() {
        NBTTagCompound lure = new NBTTagCompound();
        lure.setInteger(CoordinateLinks.MARKER, 0);
        lure.setInteger("yTop", 70);
        lure.setInteger("yBottom", 64);
        java.util.List<CoordinateLinks.Spec> specs = CoordinateLinks.rulesFor("tuhljin.automagy.tiles.TileEntityMobLure");
        CoordinateLinks.shift(specs, lure, 5, -60, 5);
        assertEquals(10, lure.getInteger("yTop"));
        CoordinateLinks.transform(specs, lure, 'X', 3, 12, 3);
        assertEquals(10, lure.getInteger("yTop"));
        CoordinateLinks.transform(specs, lure, 'y', 3, 12, 3);
        assertEquals(1, lure.getInteger("yTop"));
        assertEquals(7, lure.getInteger("yBottom"));
        NBTTagCompound pylon = new NBTTagCompound();
        pylon.setInteger(CoordinateLinks.MARKER, 0);
        pylon.setInteger("bossY", -1);
        CoordinateLinks.shift(CoordinateLinks.rulesFor("tuhljin.automagy.tiles.TileEntityThaumostaticPylon"), pylon, 0, 5, 0);
        assertEquals(-1, pylon.getInteger("bossY"));
    }

    @Test public void panelScreensAndArcLampAirBlocks() {
        NBTTagCompound panel = new NBTTagCompound();
        panel.setInteger(CoordinateLinks.MARKER, 0);
        NBTTagCompound screen = new NBTTagCompound();
        screen.setInteger("minX", 100); screen.setInteger("minY", 64); screen.setInteger("minZ", 200);
        screen.setInteger("maxX", 102); screen.setInteger("maxY", 64); screen.setInteger("maxZ", 200);
        panel.setTag("screenData", screen);
        java.util.List<CoordinateLinks.Spec> specs = CoordinateLinks.rulesFor("shedar.mods.ic2.nuclearcontrol.tileentities.TileEntityInfoPanel");
        CoordinateLinks.shift(specs, panel, -100, -64, -200);
        CoordinateLinks.transform(specs, panel, 'Y', 3, 1, 3);
        NBTTagCompound turned = panel.getCompoundTag("screenData");
        assertArrayEquals(new int[] {2, 0, 0, 2, 0, 2}, new int[] {turned.getInteger("minX"), turned.getInteger("minY"), turned.getInteger("minZ"),
            turned.getInteger("maxX"), turned.getInteger("maxY"), turned.getInteger("maxZ")});

        NBTTagCompound lamp = new NBTTagCompound();
        lamp.setInteger(CoordinateLinks.MARKER, 0);
        net.minecraft.nbt.NBTTagList air = new net.minecraft.nbt.NBTTagList();
        NBTTagCompound block = new NBTTagCompound();
        block.setInteger("x", 105); block.setInteger("y", 70); block.setInteger("z", 210);
        air.appendTag(block);
        lamp.setTag("AirBlocks", air);
        CoordinateLinks.shift(CoordinateLinks.rulesFor("micdoodle8.mods.galacticraft.core.tile.TileEntityArclamp"), lamp, -100, -64, -200);
        NBTTagCompound moved = lamp.getTagList("AirBlocks", 10).getCompoundTagAt(0);
        assertArrayEquals(new int[] {5, 6, 10}, new int[] {moved.getInteger("x"), moved.getInteger("y"), moved.getInteger("z")});
    }

    @Test public void sensorCardTargetsInPanels() {
        NBTTagCompound panel = new NBTTagCompound();
        panel.setInteger(CoordinateLinks.MARKER, 0);
        net.minecraft.nbt.NBTTagList items = new net.minecraft.nbt.NBTTagList();
        NBTTagCompound card = new NBTTagCompound(), tag = new NBTTagCompound();
        tag.setInteger("x", 101); tag.setInteger("y", 65); tag.setInteger("z", 202);
        card.setTag("tag", tag);
        NBTTagCompound upgrade = new NBTTagCompound();
        upgrade.setShort("id", (short) 4000);
        items.appendTag(card);
        items.appendTag(upgrade);
        panel.setTag("Items", items);
        java.util.List<CoordinateLinks.Spec> specs = CoordinateLinks.rulesFor("shedar.mods.ic2.nuclearcontrol.tileentities.TileEntityInfoPanel");
        CoordinateLinks.shift(specs, panel, -100, -64, -200);
        CoordinateLinks.transform(specs, panel, 'Y', 3, 2, 3);
        NBTTagCompound moved = panel.getTagList("Items", 10).getCompoundTagAt(0).getCompoundTag("tag");
        assertArrayEquals(new int[] {0, 1, 1}, new int[] {moved.getInteger("x"), moved.getInteger("y"), moved.getInteger("z")});
        assertFalse(panel.getTagList("Items", 10).getCompoundTagAt(1).hasKey("tag"));
    }

    @Test public void railcraftSignalPairings() {
        NBTTagCompound signal = new NBTTagCompound();
        signal.setInteger(CoordinateLinks.MARKER, 0);
        NBTTagCompound block = new NBTTagCompound();
        net.minecraft.nbt.NBTTagList pairings = new net.minecraft.nbt.NBTTagList();
        NBTTagCompound pair = new NBTTagCompound(), other = new NBTTagCompound();
        pair.setIntArray("coords", new int[] {0, 110, 64, 200});
        other.setIntArray("coords", new int[] {-1, 110, 64, 200});
        pairings.appendTag(pair);
        pairings.appendTag(other);
        block.setTag("pairings", pairings);
        net.minecraft.nbt.NBTTagList cache = new net.minecraft.nbt.NBTTagList();
        NBTTagCompound entry = new NBTTagCompound(), key = new NBTTagCompound();
        key.setInteger("dim", 0); key.setInteger("x", 105); key.setInteger("y", 63); key.setInteger("z", 201);
        entry.setTag("key", key);
        entry.setTag("value", key.copy());
        cache.appendTag(entry);
        block.setTag("trackCache", cache);
        signal.setTag("SignalBlock", block);
        CoordinateLinks.shift(CoordinateLinks.rulesFor("mods.railcraft.common.blocks.signals.TileSignalBase"), signal, -100, -60, -200);
        net.minecraft.nbt.NBTTagList moved = signal.getCompoundTag("SignalBlock").getTagList("pairings", 10);
        assertArrayEquals(new int[] {0, 10, 4, 0}, moved.getCompoundTagAt(0).getIntArray("coords"));
        assertArrayEquals(new int[] {-1, 110, 64, 200}, moved.getCompoundTagAt(1).getIntArray("coords"));
        NBTTagCompound cached = signal.getCompoundTag("SignalBlock").getTagList("trackCache", 10).getCompoundTagAt(0);
        assertEquals(5, cached.getCompoundTag("key").getInteger("x"));
        assertEquals(3, cached.getCompoundTag("value").getInteger("y"));
    }

    @Test public void smelteryAreaAndServants() {
        NBTTagCompound smeltery = new NBTTagCompound();
        smeltery.setInteger(CoordinateLinks.MARKER, 0);
        smeltery.setIntArray("MinPos", new int[] {101, 61, 201});
        smeltery.setIntArray("MaxPos", new int[] {103, 62, 202});
        java.util.List<CoordinateLinks.Spec> specs = CoordinateLinks.rulesFor("tconstruct.smeltery.logic.SmelteryLogic");
        CoordinateLinks.shift(specs, smeltery, -100, -60, -200);
        assertArrayEquals(new int[] {1, 1, 1}, smeltery.getIntArray("MinPos"));
        CoordinateLinks.transform(specs, smeltery, 'Y', 5, 3, 4);
        // (x, y, z) -> (3 - z, y, x): (1, 1, 1)..(3, 2, 2) becomes (1, 1, 1)..(2, 2, 3)
        assertArrayEquals(new int[] {1, 1, 1}, smeltery.getIntArray("MinPos"));
        assertArrayEquals(new int[] {2, 2, 3}, smeltery.getIntArray("MaxPos"));

        NBTTagCompound drain = new NBTTagCompound();
        drain.setInteger(CoordinateLinks.MARKER, 0);
        drain.setInteger("xCenter", 102); drain.setInteger("yCenter", 61); drain.setInteger("zCenter", 200);
        CoordinateLinks.shift(CoordinateLinks.rulesFor("mantle.blocks.abstracts.MultiServantLogic"), drain, -100, -60, -200);
        assertEquals(2, drain.getInteger("xCenter"));
        assertEquals(1, drain.getInteger("yCenter"));
    }
}
