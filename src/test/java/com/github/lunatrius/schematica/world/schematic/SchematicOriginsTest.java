package com.github.lunatrius.schematica.world.schematic;

import java.io.File;
import net.minecraft.nbt.NBTTagCompound;
import com.github.lunatrius.schematica.api.SchematicOrigin;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class SchematicOriginsTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void outsideOriginSurvivesFileUpgradeWithoutEnlargingBlockData() throws Exception {
        SchematicOrigin origin = new SchematicOrigin(-10, 20, 30);
        NBTTagCompound tag = new NBTTagCompound();
        SchematicOrigins.write(tag, origin);
        SchematicBlockIds.write(tag, new byte[] {1}, new byte[] {0}, !origin.isZero());
        tag.setByteArray("Data", new byte[] {0});
        File requested = new File(temporary.getRoot(), "origin.schematic");
        File saved = SchematicFileWriter.write(requested, tag);
        assertEquals("origin.schemplus", saved.getName());
        NBTTagCompound decoded = SchematicFileSnapshot.read(saved).readNBT();
        assertArrayEquals(origin.coordinates(), SchematicOrigins.read(decoded).coordinates());
        assertArrayEquals(new int[] {1}, SchematicBlockIds.read(decoded, 1));
        assertFalse(requested.exists());
        File again = new File(temporary.getRoot(), "again.schemplus");
        SchematicOrigins.write(decoded, SchematicOrigins.read(decoded));
        SchematicFileWriter.write(again, decoded);
        assertArrayEquals(origin.coordinates(), SchematicOrigins.read(SchematicFileSnapshot.read(again).readNBT()).coordinates());
    }

    @Test public void legacyAndClearedOriginsUseTheMinimumCorner() {
        NBTTagCompound tag = new NBTTagCompound();
        assertTrue(SchematicOrigins.read(tag).isZero());
        SchematicOrigins.write(tag, new SchematicOrigin(1, 2, 3));
        SchematicOrigins.write(tag, SchematicOrigin.ZERO);
        assertTrue(tag.hasNoTags());
        assertTrue(SchematicOrigins.read(tag).isZero());
    }

    @Test public void rejectsWrongTypesMissingDataAndUnsupportedVersions() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("SchematicaPlusOriginVersion", 1);
        assertThrows(IllegalArgumentException.class, () -> SchematicOrigins.read(tag));
        tag.setIntArray("SchematicaPlusOrigin", new int[2]);
        assertThrows(IllegalArgumentException.class, () -> SchematicOrigins.read(tag));
        tag.setIntArray("SchematicaPlusOrigin", new int[3]);
        tag.setInteger("SchematicaPlusOriginVersion", 2);
        assertThrows(IllegalArgumentException.class, () -> SchematicOrigins.read(tag));
        tag.setString("SchematicaPlusOriginVersion", "1");
        assertThrows(IllegalArgumentException.class, () -> SchematicOrigins.read(tag));
    }
}
