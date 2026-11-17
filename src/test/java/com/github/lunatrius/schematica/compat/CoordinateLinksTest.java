package com.github.lunatrius.schematica.compat;

import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.Constants;

import org.junit.Test;
import vazkii.botania.common.block.tile.TileLightRelay;

import static org.junit.Assert.*;

public class CoordinateLinksTest {
    private static NBTTagCompound mirror(int dimension) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(CoordinateLinks.MARKER, 0);
        tag.setInteger("x", 2); tag.setInteger("y", 1); tag.setInteger("z", 3);
        tag.setInteger("linkX", 5); tag.setInteger("linkY", 4); tag.setInteger("linkZ", 6);
        tag.setInteger("linkDim", dimension);
        return tag;
    }

    @Test public void pastedLocalMirrorUsesTheDestinationDimension() {
        NBTTagCompound tag = mirror(0);
        tag.setString("owner", "fixture-owner");
        tag.setInteger("frequency", 37);
        CoordinateLinks.paste(CoordinateLinks.rulesFor("thaumcraft.common.tiles.TileMirror"), tag, 102, 71, 203, -1);
        assertEquals(105, tag.getInteger("linkX"));
        assertEquals(74, tag.getInteger("linkY"));
        assertEquals(206, tag.getInteger("linkZ"));
        assertEquals(-1, tag.getInteger("linkDim"));
        assertFalse(CoordinateLinks.local(tag));
        assertEquals("fixture-owner", tag.getString("owner"));
        assertEquals(37, tag.getInteger("frequency"));
    }

    @Test public void foreignUnboundAndUnmarkedMirrorLinksAreNotRebound() {
        List<CoordinateLinks.Spec> rules = CoordinateLinks.rulesFor("thaumcraft.common.tiles.TileMirror");
        NBTTagCompound foreign = mirror(-1);
        CoordinateLinks.paste(rules, foreign, 102, 71, 203, -1);
        assertEquals(5, foreign.getInteger("linkX"));
        assertEquals(-1, foreign.getInteger("linkDim"));
        NBTTagCompound unbound = mirror(0);
        unbound.setInteger("linkY", -1);
        CoordinateLinks.paste(rules, unbound, 102, 71, 203, -1);
        assertEquals(5, unbound.getInteger("linkX"));
        assertEquals(0, unbound.getInteger("linkDim"));
        NBTTagCompound unmarked = mirror(0);
        unmarked.removeTag(CoordinateLinks.MARKER);
        NBTTagCompound before = (NBTTagCompound) unmarked.copy();
        CoordinateLinks.paste(rules, unmarked, 102, 71, 203, -1);
        assertEquals(before, unmarked);
    }

    @Test public void changingOnlyTheDimensionStillRebindsALocalLink() {
        NBTTagCompound tag = mirror(0);
        CoordinateLinks.paste(CoordinateLinks.rulesFor("thaumcraft.common.tiles.TileMirror"), tag, 2, 1, 3, -1);
        assertEquals(5, tag.getInteger("linkX"));
        assertEquals(-1, tag.getInteger("linkDim"));
    }

    @Test public void pastedRailcraftPairingsAndNestedTrackCacheUseTheirOriginalDimension() {
        NBTTagCompound tag = new NBTTagCompound(), block = new NBTTagCompound();
        tag.setInteger(CoordinateLinks.MARKER, 0);
        NBTTagList pairings = new NBTTagList();
        NBTTagCompound local = new NBTTagCompound(), foreign = new NBTTagCompound();
        local.setIntArray("coords", new int[]{0, 3, 2, 5});
        foreign.setIntArray("coords", new int[]{-1, 110, 64, 200});
        pairings.appendTag(local); pairings.appendTag(foreign);
        block.setTag("pairings", pairings);
        NBTTagList cache = new NBTTagList();
        NBTTagCompound entry = new NBTTagCompound(), key = new NBTTagCompound();
        key.setInteger("dim", 0); key.setInteger("x", 3); key.setInteger("y", 2); key.setInteger("z", 5);
        entry.setTag("key", key); entry.setTag("value", key.copy());
        entry.getCompoundTag("value").setInteger("dim", -1);
        cache.appendTag(entry); block.setTag("trackCache", cache); tag.setTag("SignalBlock", block);
        CoordinateLinks.paste(CoordinateLinks.rulesFor("mods.railcraft.common.blocks.signals.TileSignalBase"), tag, 100, 70, 200, -1);
        assertArrayEquals(new int[]{-1, 103, 72, 205}, local.getIntArray("coords"));
        assertArrayEquals(new int[]{-1, 110, 64, 200}, foreign.getIntArray("coords"));
        assertEquals(-1, key.getInteger("dim"));
        assertEquals(103, key.getInteger("x"));
        assertEquals(3, entry.getCompoundTag("value").getInteger("x"));
    }

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
        CoordinateLinks.paste(tag, 202, 71, 303, 0);
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
        assertEquals(Constants.NBT.TAG_DOUBLE, quarry.getTag("headPosX").getId());

        NBTTagCompound mobilizer = new NBTTagCompound();
        mobilizer.setInteger(CoordinateLinks.MARKER, 0);
        mobilizer.setInteger("FirstRelayX", 1);
        mobilizer.setInteger("FirstRelayZ", 2);
        List<CoordinateLinks.Spec> specs = CoordinateLinks.rulesFor("thaumic.tinkerer.common.block.tile.TileEntityMobilizer");
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
        List<CoordinateLinks.Spec> specs = CoordinateLinks.rulesFor("thaumcraft.common.tiles.TileMirror");
        CoordinateLinks.shift(specs, mirror, 10, 10, 10);
        assertEquals(5, mirror.getInteger("linkX"));
        mirror.setInteger("linkDim", 0);
        CoordinateLinks.shift(specs, mirror, 10, 10, 10);
        assertEquals(15, mirror.getInteger("linkX"));
    }

    @Test public void positionListsAndGalacticraftMultiblockParts() {
        NBTTagCompound inventarium = new NBTTagCompound();
        inventarium.setInteger(CoordinateLinks.MARKER, 0);
        NBTTagList nodes = new NBTTagList();
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
        List<CoordinateLinks.Spec> specs = CoordinateLinks.rulesFor("tuhljin.automagy.tiles.TileEntityMobLure");
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
        List<CoordinateLinks.Spec> specs = CoordinateLinks.rulesFor("shedar.mods.ic2.nuclearcontrol.tileentities.TileEntityInfoPanel");
        CoordinateLinks.shift(specs, panel, -100, -64, -200);
        CoordinateLinks.transform(specs, panel, 'Y', 3, 1, 3);
        NBTTagCompound turned = panel.getCompoundTag("screenData");
        assertArrayEquals(new int[] {2, 0, 0, 2, 0, 2}, new int[] {turned.getInteger("minX"), turned.getInteger("minY"), turned.getInteger("minZ"),
            turned.getInteger("maxX"), turned.getInteger("maxY"), turned.getInteger("maxZ")});

        NBTTagCompound lamp = new NBTTagCompound();
        lamp.setInteger(CoordinateLinks.MARKER, 0);
        NBTTagList air = new NBTTagList();
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
        NBTTagList items = new NBTTagList();
        NBTTagCompound card = new NBTTagCompound(), tag = new NBTTagCompound();
        tag.setInteger("x", 101); tag.setInteger("y", 65); tag.setInteger("z", 202);
        card.setTag("tag", tag);
        NBTTagCompound upgrade = new NBTTagCompound();
        upgrade.setShort("id", (short) 4000);
        items.appendTag(card);
        items.appendTag(upgrade);
        panel.setTag("Items", items);
        List<CoordinateLinks.Spec> specs = CoordinateLinks.rulesFor("shedar.mods.ic2.nuclearcontrol.tileentities.TileEntityInfoPanel");
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
        NBTTagList pairings = new NBTTagList();
        NBTTagCompound pair = new NBTTagCompound(), other = new NBTTagCompound();
        pair.setIntArray("coords", new int[] {0, 110, 64, 200});
        other.setIntArray("coords", new int[] {-1, 110, 64, 200});
        pairings.appendTag(pair);
        pairings.appendTag(other);
        block.setTag("pairings", pairings);
        NBTTagList cache = new NBTTagList();
        NBTTagCompound entry = new NBTTagCompound(), key = new NBTTagCompound();
        key.setInteger("dim", 0); key.setInteger("x", 105); key.setInteger("y", 63); key.setInteger("z", 201);
        entry.setTag("key", key);
        entry.setTag("value", key.copy());
        cache.appendTag(entry);
        block.setTag("trackCache", cache);
        signal.setTag("SignalBlock", block);
        CoordinateLinks.shift(CoordinateLinks.rulesFor("mods.railcraft.common.blocks.signals.TileSignalBase"), signal, -100, -60, -200);
        NBTTagList moved = signal.getCompoundTag("SignalBlock").getTagList("pairings", 10);
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
        List<CoordinateLinks.Spec> specs = CoordinateLinks.rulesFor("tconstruct.smeltery.logic.SmelteryLogic");
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
        NBTTagCompound slave = new NBTTagCompound(), controller = new NBTTagCompound();
        slave.setInteger(CoordinateLinks.MARKER, 0);
        controller.setInteger("x", 103); controller.setInteger("y", 64); controller.setInteger("z", 205);
        slave.setTag("Controller", controller);
        CoordinateLinks.shift(CoordinateLinks.rulesFor("com.jaquadro.minecraft.storagedrawers.block.tile.TileEntitySlave"), slave, -100, -60, -200);
        assertEquals(5, slave.getCompoundTag("Controller").getInteger("z"));
        assertEquals(1, drain.getInteger("yCenter"));
    }

    private static NBTTagCompound reactorPart(int rx, int ry, int rz) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(CoordinateLinks.MARKER, 0);
        tag.setInteger("x", 4); tag.setInteger("y", 2); tag.setInteger("z", 1);
        tag.setInteger("reactorRelX", rx); tag.setInteger("reactorRelY", ry); tag.setInteger("reactorRelZ", rz);
        return tag;
    }

    private static int[] rel(NBTTagCompound tag) {
        return new int[] {tag.getInteger("reactorRelX"), tag.getInteger("reactorRelY"), tag.getInteger("reactorRelZ")};
    }

    @Test public void nuclearHorizonsReactorOffsetsSurviveMovesAndPastes() {
        String[] tiles = {"TileAccessHatch", "TileFluidPort", "TileReactorChamber", "TileRedstonePort", "TileThermalSensor"};
        for (String tile : tiles) {
            List<CoordinateLinks.Spec> rules = CoordinateLinks.rulesFor("com.recursive_pineapple.nuclear_horizons.reactors.tile." + tile);
            assertFalse(tile, rules.isEmpty());
            NBTTagCompound tag = reactorPart(-1, 0, 2);
            CoordinateLinks.shift(rules, tag, 7, -3, 11);
            assertArrayEquals(tile, new int[] {-1, 0, 2}, rel(tag));
            CoordinateLinks.paste(rules, tag, 120, 64, -30, 0);
            assertArrayEquals(tile, new int[] {-1, 0, 2}, rel(tag));
            assertFalse(CoordinateLinks.local(tag));
        }
    }

    @Test public void nuclearHorizonsReactorOffsetsTurnAsVectors() {
        List<CoordinateLinks.Spec> rules = CoordinateLinks.rulesFor("com.recursive_pineapple.nuclear_horizons.reactors.tile.TileAccessHatch");
        NBTTagCompound turned = reactorPart(-1, 0, 2);
        CoordinateLinks.transform(rules, turned, 'Y', 9, 5, 7);
        assertArrayEquals(new int[] {-2, 0, -1}, rel(turned));
        NBTTagCompound mirrored = reactorPart(-1, 0, 2);
        CoordinateLinks.transform(rules, mirrored, 'x', 9, 5, 7);
        assertArrayEquals(new int[] {1, 0, 2}, rel(mirrored));
        NBTTagCompound tilted = reactorPart(0, 1, 0);
        CoordinateLinks.transform(rules, tilted, 'X', 9, 5, 7);
        assertArrayEquals(new int[] {0, 0, -1}, rel(tilted));
        NBTTagCompound unlinked = reactorPart(0, 0, 0);
        CoordinateLinks.transform(rules, unlinked, 'Y', 9, 5, 7);
        assertArrayEquals(new int[] {0, 0, 0}, rel(unlinked));
    }

    private static NBTTagCompound remoteInterface(int dimension, int turns) {
        NBTTagCompound tag = new NBTTagCompound(), position = new NBTTagCompound();
        tag.setInteger(CoordinateLinks.MARKER, 0);
        tag.setInteger("x", 1); tag.setInteger("y", 1); tag.setInteger("z", 1);
        position.setInteger("dimension", dimension);
        position.setInteger("x", 3); position.setInteger("y", 2); position.setInteger("z", 0);
        tag.setTag("position", position);
        tag.setInteger("axisY", turns);
        return tag;
    }

    @Test public void remoteInterfaceTargetsFollowPastesWithinTheirDimension() {
        List<CoordinateLinks.Spec> rules = CoordinateLinks.rulesFor("remoteio.common.tile.TileRemoteInterface");
        NBTTagCompound local = remoteInterface(0, 1);
        CoordinateLinks.paste(rules, local, 101, 65, 201, 7);
        NBTTagCompound position = local.getCompoundTag("position");
        assertEquals(103, position.getInteger("x"));
        assertEquals(66, position.getInteger("y"));
        assertEquals(200, position.getInteger("z"));
        assertEquals(7, position.getInteger("dimension"));
        assertEquals(1, local.getInteger("axisY"));
        NBTTagCompound foreign = remoteInterface(-1, 1);
        CoordinateLinks.paste(rules, foreign, 101, 65, 201, 7);
        assertEquals(3, foreign.getCompoundTag("position").getInteger("x"));
        assertEquals(-1, foreign.getCompoundTag("position").getInteger("dimension"));
    }

    @Test public void remoteInterfaceTurnsAreKeptOnRotationsAndReversedOnHorizontalMirrors() {
        List<CoordinateLinks.Spec> rules = CoordinateLinks.rulesFor("remoteio.common.tile.TileRemoteInterface");
        NBTTagCompound turned = remoteInterface(0, 1);
        CoordinateLinks.transform(rules, turned, 'Y', 4, 3, 2);
        assertEquals(1, turned.getInteger("axisY"));
        assertEquals(1, turned.getCompoundTag("position").getInteger("x"));
        assertEquals(3, turned.getCompoundTag("position").getInteger("z"));
        for (char mirror : new char[] {'x', 'z'}) {
            NBTTagCompound mirrored = remoteInterface(0, 1);
            CoordinateLinks.transform(rules, mirrored, mirror, 4, 3, 2);
            assertEquals(3, mirrored.getInteger("axisY"));
        }
        NBTTagCompound flipped = remoteInterface(0, 2);
        CoordinateLinks.transform(rules, flipped, 'y', 4, 3, 2);
        assertEquals(2, flipped.getInteger("axisY"));
        NBTTagCompound straight = remoteInterface(0, 0);
        CoordinateLinks.transform(rules, straight, 'x', 4, 3, 2);
        assertEquals(0, straight.getInteger("axisY"));
    }

    private static NBTTagList blockPositions(int[]... positions) {
        NBTTagList list = new NBTTagList();
        for (int[] position : positions) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setInteger("x", position[0]); entry.setInteger("y", position[1]); entry.setInteger("z", position[2]);
            list.appendTag(entry);
        }
        return list;
    }

    private static int[] blockPosition(NBTTagCompound tag, String key, int index) {
        NBTTagCompound entry = tag.getTagList(key, Constants.NBT.TAG_COMPOUND).getCompoundTagAt(index);
        return new int[] {entry.getInteger("x"), entry.getInteger("y"), entry.getInteger("z")};
    }

    @Test public void galaxiaStationLinksMoveAndTurnWithTheStation() {
        String station = "com.gtnewhorizons.galaxia.registry.celestial.station.";
        List<CoordinateLinks.Spec> rules = new java.util.ArrayList<>(CoordinateLinks.rulesFor(station + "TileStationBase"));
        rules.addAll(CoordinateLinks.rulesFor(station + "TileStation"));
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(CoordinateLinks.MARKER, 0);
        tag.setInteger("x", 0); tag.setInteger("y", 0); tag.setInteger("z", 0);
        tag.setTag("airlocks", blockPositions(new int[] {2, 1, 0}, new int[] {0, 1, 3}));
        tag.setTag("plugs", blockPositions(new int[] {1, 0, 1}));
        tag.setTag("attachments", blockPositions(new int[] {3, 2, 3}));
        CoordinateLinks.transform(rules, tag, 'Y', 4, 3, 4);
        assertArrayEquals(new int[] {3, 1, 2}, blockPosition(tag, "airlocks", 0));
        assertArrayEquals(new int[] {0, 1, 0}, blockPosition(tag, "airlocks", 1));
        assertArrayEquals(new int[] {2, 0, 1}, blockPosition(tag, "plugs", 0));
        assertArrayEquals(new int[] {0, 2, 3}, blockPosition(tag, "attachments", 0));
        CoordinateLinks.paste(rules, tag, 100, 80, -50, 0);
        assertArrayEquals(new int[] {103, 81, -48}, blockPosition(tag, "airlocks", 0));
        assertArrayEquals(new int[] {100, 82, -47}, blockPosition(tag, "attachments", 0));
        NBTTagCompound airlock = new NBTTagCompound();
        airlock.setInteger(CoordinateLinks.MARKER, 0);
        airlock.setInteger("x", 2); airlock.setInteger("y", 1); airlock.setInteger("z", 0);
        airlock.setTag("stationControllers", blockPositions(new int[] {0, 0, 0}));
        CoordinateLinks.paste(CoordinateLinks.rulesFor(station + "TileEntityAirlock"), airlock, 12, 41, 10, 0);
        assertArrayEquals(new int[] {10, 40, 10}, blockPosition(airlock, "stationControllers", 0));
    }

    private static NBTTagCompound quarryArea(int lowX, int lowZ, int highX, int highZ) {
        NBTTagCompound holder = new NBTTagCompound(), area = new NBTTagCompound();
        area.setInteger("lowX", lowX); area.setInteger("lowZ", lowZ); area.setInteger("highX", highX); area.setInteger("highZ", highZ);
        holder.setTag("area", area);
        return holder;
    }

    private static NBTTagCompound voidQuarry() {
        NBTTagCompound tag = quarryArea(2, 3, 20, 9);
        tag.setInteger(CoordinateLinks.MARKER, 0);
        tag.setInteger("x", 0); tag.setInteger("y", 10); tag.setInteger("z", 0);
        tag.setString("state", "RUNNING");
        tag.setInteger("dx", 5); tag.setInteger("dy", 7); tag.setInteger("dz", 4);
        tag.setInteger("lowerYBound", 1); tag.setInteger("upperYBound", 12);
        NBTTagList next = new NBTTagList();
        next.appendTag(quarryArea(21, 3, 25, 4));
        tag.setTag("nextAreas", next);
        return tag;
    }

    private static int[] area(NBTTagCompound holder) {
        NBTTagCompound area = holder.getCompoundTag("area");
        return new int[] {area.getInteger("lowX"), area.getInteger("lowZ"), area.getInteger("highX"), area.getInteger("highZ")};
    }

    private static final String EXCESS = "com.fouristhenumber.utilitiesinexcess.common.tileentities.";

    @Test public void voidQuarryAreasAndCursorFollowAPasteOffTheChunkGrid() {
        NBTTagCompound tag = voidQuarry();
        CoordinateLinks.paste(CoordinateLinks.rulesFor(EXCESS + "TileEntityVoidQuarry"), tag, 7, 13, -5, 0);
        assertArrayEquals(new int[] {9, -2, 27, 4}, area(tag));
        assertArrayEquals(new int[] {28, -2, 32, -1}, area(tag.getTagList("nextAreas", 10).getCompoundTagAt(0)));
        assertEquals(12, tag.getInteger("dx"));
        assertEquals(10, tag.getInteger("dy"));
        assertEquals(-1, tag.getInteger("dz"));
        assertEquals(4, tag.getInteger("lowerYBound"));
        assertEquals(15, tag.getInteger("upperYBound"));
    }

    @Test public void voidQuarryRestartsItsCursorInTheTurnedArea() {
        List<CoordinateLinks.Spec> rules = CoordinateLinks.rulesFor(EXCESS + "TileEntityVoidQuarry");
        NBTTagCompound turned = voidQuarry();
        CoordinateLinks.transform(rules, turned, 'Y', 30, 20, 10);
        assertArrayEquals(new int[] {0, 2, 6, 20}, area(turned));
        assertArrayEquals(new int[] {5, 21, 6, 25}, area(turned.getTagList("nextAreas", 10).getCompoundTagAt(0)));
        assertEquals(0, turned.getInteger("dx"));
        assertEquals(12, turned.getInteger("dy"));
        assertEquals(2, turned.getInteger("dz"));
        NBTTagCompound flipped = voidQuarry();
        CoordinateLinks.transform(rules, flipped, 'y', 30, 20, 10);
        assertArrayEquals(new int[] {2, 3, 20, 9}, area(flipped));
        assertEquals(7, flipped.getInteger("lowerYBound"));
        assertEquals(18, flipped.getInteger("upperYBound"));
        assertEquals(18, flipped.getInteger("dy"));
        NBTTagCompound tilted = voidQuarry();
        NBTTagCompound before = (NBTTagCompound) tilted.copy();
        CoordinateLinks.transform(rules, tilted, 'X', 30, 20, 10);
        assertEquals(before, tilted);
    }

    @Test public void smartPumpRestartsItsScanWhenPlacedElsewhereButNotWhenSaved() {
        List<CoordinateLinks.Spec> rules = CoordinateLinks.rulesFor(EXCESS + "TileEntitySmartPump");
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(CoordinateLinks.MARKER, 0);
        tag.setInteger("x", 3); tag.setInteger("y", 60); tag.setInteger("z", 3);
        tag.setInteger("chunkX", 6); tag.setInteger("chunkZ", -2); tag.setInteger("currentY", 41);
        tag.setByte("xInChunk", (byte) 7); tag.setByte("zInChunk", (byte) 9); tag.setByte("currentChunk", (byte) 4);
        tag.setBoolean("finished", true);
        NBTTagCompound unmoved = (NBTTagCompound) tag.copy();
        CoordinateLinks.shift(rules, unmoved, 0, 0, 0);
        assertEquals(41, unmoved.getInteger("currentY"));
        CoordinateLinks.paste(rules, tag, 108, 60, 3, 0);
        assertEquals(Integer.MIN_VALUE, tag.getInteger("currentY"));
        assertEquals(0, tag.getByte("xInChunk"));
        assertEquals(0, tag.getByte("zInChunk"));
        assertEquals(0, tag.getByte("currentChunk"));
        assertFalse(tag.getBoolean("finished"));
    }
}
