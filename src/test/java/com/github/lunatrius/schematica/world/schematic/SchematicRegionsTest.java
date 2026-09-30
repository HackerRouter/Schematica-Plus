package com.github.lunatrius.schematica.world.schematic;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.world.storage.RegionMask;
import com.github.lunatrius.schematica.world.storage.SchematicRegion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class SchematicRegionsTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private ISchematic schematic(List<SchematicRegion> regions) {
        return (ISchematic) java.lang.reflect.Proxy.newProxyInstance(ISchematic.class.getClassLoader(), new Class<?>[] {ISchematic.class},
            (proxy, method, args) -> {
                if (method.getName().equals("getRegions")) return regions;
                if (method.getName().equals("getWidth")) return 5;
                if (method.getName().equals("getHeight") || method.getName().equals("getLength")) return 1;
                throw new UnsupportedOperationException(method.getName());
            });
    }

    @Test public void fileRoundTripKeepsSubregionNamesAndExcludesTheGap() throws Exception {
        ISchematic source = schematic(Arrays.asList(new SchematicRegion("Machines", 0, 0, 0, 1, 0, 0),
            new SchematicRegion("Pipes", 4, 0, 0, 4, 0, 0)));
        NBTTagCompound tag = new NBTTagCompound();
        assertTrue(SchematicRegions.requiresExtended(source));
        SchematicRegions.write(tag, source);
        SchematicBlockIds.write(tag, new byte[] {1, 0, 0, 0, 35}, new byte[5], SchematicRegions.requiresExtended(source));
        tag.setByteArray("Data", new byte[] {0, 0, 0, 0, 3});
        java.io.File requested = new java.io.File(temporary.getRoot(), "regions.schematic");
        java.io.File saved = SchematicFileWriter.write(requested, tag);
        assertEquals("regions.schemplus", saved.getName());
        assertFalse(requested.exists());
        NBTTagCompound decoded = SchematicFileSnapshot.read(saved).readNBT();
        List<SchematicRegion> regions = SchematicRegions.read(decoded, 5, 1, 1);
        assertEquals("Pipes", regions.get(1).name);
        BitSet mask = RegionMask.create(regions, 5, 1, 1);
        assertTrue(mask.get(1));
        assertFalse(mask.get(2));
        assertTrue(mask.get(4));
        assertEquals(35, SchematicBlockIds.read(decoded, 5)[4]);
        assertEquals(3, decoded.getByteArray("Data")[4]);
    }

    @Test public void singleFullBoxDoesNotForceAnExtendedFormat() {
        assertFalse(SchematicRegions.requiresExtended(schematic(java.util.Collections.singletonList(
            new SchematicRegion("Full", 0, 0, 0, 4, 0, 0)))));
        assertTrue(SchematicRegions.requiresExtended(schematic(java.util.Collections.singletonList(
            new SchematicRegion("Part", 1, 0, 0, 4, 0, 0)))));
        assertTrue(SchematicRegions.read(new NBTTagCompound(), 5, 1, 1).isEmpty());
    }

    @Test public void malformedRegionDataFailsInsteadOfBecomingADenseSchematic() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("SchematicaPlusRegions", "broken");
        assertThrows(IllegalArgumentException.class, () -> SchematicRegions.read(tag, 1, 1, 1));
        tag.setTag("SchematicaPlusRegions", new net.minecraft.nbt.NBTTagList());
        tag.setInteger("SchematicaPlusRegionsVersion", 1);
        assertThrows(IllegalArgumentException.class, () -> SchematicRegions.read(tag, 1, 1, 1));
    }
}
