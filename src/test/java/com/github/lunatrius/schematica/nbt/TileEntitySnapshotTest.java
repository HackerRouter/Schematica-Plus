package com.github.lunatrius.schematica.nbt;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.junit.Test;
import static org.junit.Assert.*;

public class TileEntitySnapshotTest {
    @Test public void preservesUnsentInventoryAndAcceptsLaterOrientationEdits() {
        NBTTagCompound original = new NBTTagCompound();
        NBTTagList inventory = new NBTTagList();
        NBTTagCompound item = new NBTTagCompound();
        item.setInteger("Count", 64);
        inventory.appendTag(item);
        original.setTag("Items", inventory);
        NBTTagCompound machine = new NBTTagCompound();
        machine.setInteger("Energy", 10000);
        machine.setInteger("Facing", 2);
        original.setTag("Machine", machine);
        NBTTagCompound visual = new NBTTagCompound();
        visual.setBoolean("active", true);
        original.setTag(TileUpdateData.KEY, visual);
        TileEntitySnapshot snapshot = new TileEntitySnapshot(original);

        NBTTagCompound preview = new NBTTagCompound();
        preview.setTag("Items", new NBTTagList());
        NBTTagCompound previewMachine = new NBTTagCompound();
        previewMachine.setInteger("Energy", 0);
        previewMachine.setInteger("Facing", 2);
        preview.setTag("Machine", previewMachine);
        snapshot.initialize(preview);
        previewMachine.setInteger("Facing", 5);
        NBTTagCompound saved = snapshot.write(preview, 4, 3, 2);
        assertEquals(64, saved.getTagList("Items", 10).getCompoundTagAt(0).getInteger("Count"));
        assertEquals(10000, saved.getCompoundTag("Machine").getInteger("Energy"));
        assertEquals(5, saved.getCompoundTag("Machine").getInteger("Facing"));
        assertEquals(4, saved.getInteger("x"));
        assertTrue(saved.getCompoundTag(TileUpdateData.KEY).getBoolean("active"));
        assertEquals(2, original.getCompoundTag("Machine").getInteger("Facing"));
    }

    @Test public void preservesOriginalDataBeforePreviewAndAcrossRepeatedWrites() {
        NBTTagCompound original = new NBTTagCompound();
        original.setString("ServerOnly", "retained");
        TileEntitySnapshot snapshot = new TileEntitySnapshot(original);
        NBTTagCompound saved = snapshot.write(null, 1, 2, 3);
        assertEquals("retained", saved.getString("ServerOnly"));
        saved.setString("ServerOnly", "changed outside");
        assertEquals("retained", snapshot.write(null, 1, 2, 3).getString("ServerOnly"));
        assertFalse(original.hasKey("x"));
        assertFalse(snapshot.isInitialized());
    }

    @Test public void retainsExplicitRemovalsAndNewFields() {
        NBTTagCompound original = new NBTTagCompound();
        original.setBoolean("OldCover", true);
        TileEntitySnapshot snapshot = new TileEntitySnapshot(original);
        snapshot.initialize(original);
        NBTTagCompound edited = new NBTTagCompound();
        edited.setInteger("NewCover", 9);
        NBTTagCompound saved = snapshot.write(edited, 0, 0, 0);
        assertFalse(saved.hasKey("OldCover"));
        assertEquals(9, saved.getInteger("NewCover"));
    }
}
