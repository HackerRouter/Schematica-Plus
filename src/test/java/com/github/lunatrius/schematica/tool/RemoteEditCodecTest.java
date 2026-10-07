package com.github.lunatrius.schematica.tool;

import java.lang.reflect.Method;
import java.util.BitSet;
import java.util.Collections;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import org.junit.BeforeClass;
import org.junit.Test;

import com.github.lunatrius.schematica.api.SchematicRegion;
import cpw.mods.fml.common.registry.GameData;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class RemoteEditCodecTest {
    private static Block first, second;

    @BeforeClass public static void register() throws Exception {
        first = block("test:remote_first", 4081);
        second = block("test:remote_second", 4082);
    }

    private static Block block(String name, int id) throws Exception {
        if (GameData.getBlockRegistry().containsKey(name)) return GameData.getBlockRegistry().getObject(name);
        Block block = new Block(Material.rock) {};
        Method add = Block.blockRegistry.getClass().getDeclaredMethod("add", int.class, String.class, Object.class, BitSet.class);
        add.setAccessible(true);
        add.invoke(Block.blockRegistry, id, name, block, new BitSet());
        return block;
    }

    /** A paste of first/second in a 2x1x2 box, with a tile, an entity, a region mask and an entity removal box. */
    private static NBTTagCompound paste() {
        WorldEditJob job = new WorldEditJob(UUID.randomUUID(), 0, WorldEditJob.Kind.PASTE, -5, 70, 9, 2, 1, 2, null, 0, null, 0,
            true, ReplaceBehavior.WITH_NON_AIR);
        job.setRegions(Collections.singletonList(new SchematicRegion("Box", 0, 0, 0, 1, 0, 0)));
        job.removeEntitiesIn(Collections.singletonList(new SchematicRegion("Box", -5, 70, 9, -4, 70, 10)));
        NBTTagCompound tag = job.write();
        NBTTagList palette = new NBTTagList();
        palette.appendTag(new NBTTagString("test:remote_first"));
        palette.appendTag(new NBTTagString("test:remote_second"));
        tag.setTag("palette", palette);
        tag.setByteArray("cells", new byte[] {0, 0, 0, 1, 0, 1, 0, 0});
        tag.setByteArray("meta", new byte[] {0, 3, 7, 15});
        NBTTagCompound tile = new NBTTagCompound();
        tile.setInteger("index", 1);
        NBTTagCompound chest = new NBTTagCompound();
        chest.setString("id", "Chest");
        tile.setTag("data", chest);
        NBTTagList tiles = new NBTTagList();
        tiles.appendTag(tile);
        tag.setTag("tiles", tiles);
        NBTTagCompound pig = new NBTTagCompound();
        pig.setString("id", "Pig");
        NBTTagList entities = new NBTTagList();
        entities.appendTag(pig);
        tag.setTag("entities", entities);
        return tag;
    }

    @Test public void pastesRoundTripWithFullNbt() {
        NBTTagCompound tag = paste();
        UUID player = UUID.randomUUID();
        WorldEditJob read = WorldEditJob.read(tag, player, 7);
        assertEquals(player, read.player);
        assertEquals(7, read.dimension);
        assertEquals(WorldEditJob.Kind.PASTE, read.kind);
        assertEquals(-5, read.x); assertEquals(70, read.y); assertEquals(9, read.z);
        assertEquals(4, read.volume);
        assertTrue(read.pasteWithoutUpdates);
        assertEquals(ReplaceBehavior.WITH_NON_AIR, read.replace);
        assertTrue(read.removesEntities());
        assertEquals(tag, read.write());
    }

    @Test public void extendedMetadataTravelsAsAHighByteArray() {
        NBTTagCompound tag = paste();
        tag.setByteArray("meta", new byte[] {60, 3, 7, 15});
        tag.setByteArray("metaHigh", new byte[] {1, 0, 0, 0});
        NBTTagCompound written = WorldEditJob.read(tag, UUID.randomUUID(), 0).write();
        assertEquals(tag, written);

        NBTTagCompound low = paste();
        assertTrue(!WorldEditJob.read(low, UUID.randomUUID(), 0).write().hasKey("metaHigh"));

        NBTTagCompound broken = paste();
        broken.setByteArray("metaHigh", new byte[] {1});
        assertThrows(IllegalArgumentException.class, () -> WorldEditJob.read(broken, UUID.randomUUID(), 0));
    }

    @Test public void fillsWithExtendedMetadataKeepIt() {
        WorldEditJob source = new WorldEditJob(UUID.randomUUID(), 0, WorldEditJob.Kind.REPLACE, 0, 1, 2, 3, 4, 5, first, 316, second, 6);
        NBTTagCompound tag = source.write();
        assertEquals(316, tag.getInteger("replacementMeta"));
        assertEquals(tag, WorldEditJob.read(tag, source.player, 0).write());
    }

    @Test public void fillsAndReplacesKeepTheirBlocks() {
        WorldEditJob source = new WorldEditJob(UUID.randomUUID(), 0, WorldEditJob.Kind.REPLACE, 0, 1, 2, 3, 4, 5, first, 2, second, 6);
        NBTTagCompound tag = source.write();
        assertEquals("test:remote_first", tag.getString("replacement"));
        assertEquals("test:remote_second", tag.getString("target"));
        WorldEditJob read = WorldEditJob.read(tag, source.player, 0);
        assertEquals(60, read.volume);
        assertEquals(tag, read.write());
    }

    @Test public void rejectsMalformedEdits() {
        UUID player = UUID.randomUUID();
        NBTTagCompound unknown = paste();
        NBTTagList palette = new NBTTagList();
        palette.appendTag(new NBTTagString("test:missing_block"));
        unknown.setTag("palette", palette);
        assertThrows(IllegalArgumentException.class, () -> WorldEditJob.read(unknown, player, 0));

        NBTTagCompound index = paste();
        index.setByteArray("cells", new byte[] {0, 0, 0, 1, 0, 2, 0, 0});
        assertThrows(IllegalArgumentException.class, () -> WorldEditJob.read(index, player, 0));

        NBTTagCompound cells = paste();
        cells.setByteArray("cells", new byte[6]);
        assertThrows(IllegalArgumentException.class, () -> WorldEditJob.read(cells, player, 0));

        NBTTagCompound box = paste();
        NBTTagList removal = new NBTTagList();
        NBTTagCompound outside = new NBTTagCompound();
        outside.setIntArray("box", new int[] {-6, 70, 9, -4, 70, 10});
        removal.appendTag(outside);
        box.setTag("removeEntities", removal);
        assertThrows(IllegalArgumentException.class, () -> WorldEditJob.read(box, player, 0));

        NBTTagCompound fill = new WorldEditJob(player, 0, WorldEditJob.Kind.FILL, 0, 0, 0, 1, 1, 1, first, 0, null, 0).write();
        fill.setTag("tiles", paste().getTagList("tiles", 10));
        assertThrows(IllegalArgumentException.class, () -> WorldEditJob.read(fill, player, 0));

        NBTTagCompound huge = paste();
        huge.setIntArray("bounds", new int[] {0, 0, 0, 32767, 255, 32767});
        assertThrows(IllegalArgumentException.class, () -> WorldEditJob.read(huge, player, 0));

        NBTTagCompound version = paste();
        version.setInteger("version", 2);
        assertThrows(IllegalArgumentException.class, () -> WorldEditJob.read(version, player, 0));
    }
}
