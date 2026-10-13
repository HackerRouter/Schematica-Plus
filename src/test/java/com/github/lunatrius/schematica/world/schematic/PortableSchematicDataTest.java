package com.github.lunatrius.schematica.world.schematic;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.Test;

import static org.junit.Assert.*;

public class PortableSchematicDataTest {
    private static NBTTagCompound fluid(String name, int amount) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("FluidName", name);
        tag.setInteger("Amount", amount);
        return tag;
    }

    @Test public void unavailableFluidsInTanksAndNestedItemsAreReportedWithoutRewritingNbt() {
        NBTTagCompound root = new NBTTagCompound(), item = new NBTTagCompound();
        NBTTagList items = new NBTTagList();
        root.setTag("tank", fluid("missing.oil", 4000));
        item.setTag("tag", fluid("missing.oil", 1000));
        items.appendTag(item);
        root.setTag("Items", items);
        NBTTagCompound before = (NBTTagCompound) root.copy();
        ImportReport.begin();
        try { PortableSchematicData.reportMissingFluids(root, name -> false); }
        finally { ImportReport.end(); }
        assertEquals(before, root);
        assertEquals(java.util.Collections.singleton("unavailable fluid: missing.oil"), ImportReport.lastItems().omittedData);
    }

    @Test public void availableFluidKeepsAmountAndCustomTags() {
        NBTTagCompound root = fluid("water", 750), custom = new NBTTagCompound();
        custom.setString("label", "sample");
        root.setTag("Tag", custom);
        NBTTagCompound before = (NBTTagCompound) root.copy();
        ImportReport.begin();
        try { PortableSchematicData.reportMissingFluids(root, "water"::equals); }
        finally { ImportReport.end(); }
        assertEquals(before, root);
        assertTrue(ImportReport.lastItems().omittedData.isEmpty());
    }

    @Test public void emptyTanksAndUnrelatedFluidNamesDoNotWarn() {
        NBTTagCompound root = new NBTTagCompound(), settings = new NBTTagCompound();
        settings.setString("FluidName", "preferred.fluid");
        root.setTag("settings", settings);
        root.setTag("empty", fluid("missing.fluid", 0));
        root.setTag("unnamed", fluid("", 1000));
        ImportReport.begin();
        try { PortableSchematicData.reportMissingFluids(root, name -> false); }
        finally { ImportReport.end(); }
        assertTrue(ImportReport.lastItems().omittedData.isEmpty());
    }

    private static ItemIdMaps.Archive ids(int block, int item) {
        ItemIdMaps.Archive ids = new ItemIdMaps.Archive("fixture");
        ids.put("\u0001example:block", block);
        ids.put("\u0002example:item", item);
        ids.put("\u0001minecraft:air", 0);
        return ids;
    }

    private static NBTTagCompound reference(String type, String field, int id) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", type);
        tag.setInteger(field, id);
        return tag;
    }

    @Test public void blockReferencesUseTheirOwnRegistryAndRemainStableOnRepeatedLoad() {
        String[][] fields = {{"Piston", "blockId"}, {"minecraft:piston", "blockId"},
            {"FallingSand", "TileID"}, {"minecraft:falling_block", "TileID"},
            {"Enderman", "carried"}, {"minecraft:enderman", "carried"},
            {"MinecartChest", "DisplayTile"}, {"minecraft:chest_minecart", "DisplayTile"}};
        NBTTagCompound root = new NBTTagCompound();
        NBTTagList entities = new NBTTagList();
        for (String[] field : fields) entities.appendTag(reference(field[0], field[1], 3000));
        root.setTag("entities", entities);
        entities.getCompoundTagAt(4).setShort("carried", (short) 3000);
        PortableSchematicData.write(root, ids(3000, 3000), "source");
        PortableSchematicData.readExtra(root, ids(5000, 6000), "target");
        PortableSchematicData.readExtra(root, ids(5000, 6000), "target");
        for (int i = 0; i < fields.length; i++) assertEquals(5000, entities.getCompoundTagAt(i).getInteger(fields[i][1]));
        assertTrue(entities.getCompoundTagAt(4).hasKey("carried", 2));
    }

    @Test public void missingBlockReferencesBecomeAirWithAReport() {
        NBTTagCompound root = reference("Enderman", "carried", 3000);
        PortableSchematicData.write(root, ids(3000, 3000), "source");
        ImportReport.begin();
        try { PortableSchematicData.readExtra(root, ids(-1, 6000), "target"); }
        finally { ImportReport.end(); }
        assertEquals(0, root.getInteger("carried"));
        assertEquals("minecraft:air", ImportReport.last().get("example:block"));
    }

    @Test public void unrelatedModNumbersAreNotTreatedAsBlockIds() {
        NBTTagCompound root = reference("example:machine", "blockId", 3000);
        root.setInteger("mID", 3000);
        PortableSchematicData.write(root, ids(3000, 3000), "source");
        PortableSchematicData.readExtra(root, ids(5000, 6000), "target");
        assertEquals(3000, root.getInteger("blockId"));
        assertEquals(3000, root.getInteger("mID"));
    }

    private static NBTTagCompound visualRoot() {
        NBTTagCompound root = new NBTTagCompound(), visual = new NBTTagCompound(), adapters = new NBTTagCompound();
        root.setByteArray("Blocks", new byte[]{1, 2});
        root.setString(PortableSchematicData.REGISTRY, "source");
        root.setTag("SchematicaVisualState", visual);
        visual.setTag("Packet", new NBTTagCompound());
        visual.setTag("Adapters", adapters);
        NBTTagCompound binary = new NBTTagCompound(), named = new NBTTagCompound();
        binary.setByteArray("Data", new byte[]{1, 2});
        named.setString("Block", "example:block");
        adapters.setTag("binary", binary);
        adapters.setTag("named", named);
        return root;
    }

    @Test public void changedRegistriesDiscardOnlyUnsafeVisualState() {
        NBTTagCompound root = visualRoot();
        ImportReport.begin();
        try { PortableSchematicData.readExtra(root, ids(5000, 6000), "target"); }
        finally { ImportReport.end(); }
        NBTTagCompound visual = root.getCompoundTag("SchematicaVisualState");
        assertFalse(visual.hasKey("Packet"));
        assertFalse(visual.getCompoundTag("Adapters").hasKey("binary"));
        assertTrue(visual.getCompoundTag("Adapters").hasKey("named"));
        assertArrayEquals(new byte[]{1, 2}, root.getByteArray("Blocks"));
        assertEquals(2, ImportReport.lastItems().omittedData.size());
    }

    @Test public void matchingRegistriesRetainVisualState() {
        NBTTagCompound root = visualRoot(), before = (NBTTagCompound) root.copy();
        PortableSchematicData.readExtra(root, ids(5000, 6000), "source");
        assertEquals(before, root);
    }

    @Test public void nestedByteListsAreAlsoUnsafe() {
        NBTTagCompound root = visualRoot(), data = new NBTTagCompound();
        NBTTagList outer = new NBTTagList(), inner = new NBTTagList();
        inner.appendTag(new NBTTagByteArray(new byte[]{5}));
        outer.appendTag(inner);
        data.setTag("Data", outer);
        root.getCompoundTag("SchematicaVisualState").getCompoundTag("Adapters").setTag("nested", data);
        PortableSchematicData.readExtra(root, ids(5000, 6000), "target");
        assertFalse(root.getCompoundTag("SchematicaVisualState").getCompoundTag("Adapters").hasKey("nested"));
    }

    @Test public void fingerprintDetectsRenumberingButNotIterationOrder() {
        Map<String, Integer> a = new LinkedHashMap<>(), b = new LinkedHashMap<>();
        a.put("\u0001example:block", 3000); a.put("\u0002example:item", 6000);
        b.put("\u0002example:item", 6000); b.put("\u0001example:block", 3000);
        assertEquals(PortableSchematicData.fingerprint(a), PortableSchematicData.fingerprint(b));
        b.put("\u0002example:item", 6001);
        assertNotEquals(PortableSchematicData.fingerprint(a), PortableSchematicData.fingerprint(b));
    }
}
