package com.github.lunatrius.schematica.world.schematic;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.world.storage.RegionMask;
import com.github.lunatrius.schematica.world.storage.SchematicRegion;

final class SchematicRegions {
    private static final String KEY = "SchematicaPlusRegions";
    private SchematicRegions() {}

    static List<SchematicRegion> read(NBTTagCompound tag, int width, int height, int length) {
        if (!tag.hasKey(KEY)) return java.util.Collections.emptyList();
        if (!tag.hasKey(KEY, 9) || !tag.hasKey("SchematicaPlusRegionsVersion", 3)
            || tag.getInteger("SchematicaPlusRegionsVersion") != 1) throw new IllegalArgumentException("Unsupported subregion data");
        NBTTagList entries = tag.getTagList(KEY, 10);
        if (entries.tagCount() == 0 || entries.tagCount() > 256) throw new IllegalArgumentException("Invalid subregion count");
        List<SchematicRegion> regions = new ArrayList<>();
        java.util.Set<String> names = new java.util.HashSet<>();
        for (int i = 0; i < entries.tagCount(); i++) {
            NBTTagCompound entry = entries.getCompoundTagAt(i);
            if (!entry.hasKey("Name", 8) || !entry.hasKey("Bounds", 11)) throw new IllegalArgumentException("Invalid subregion");
            int[] bounds = entry.getIntArray("Bounds");
            String name = entry.getString("Name");
            if (bounds.length != 6 || !names.add(name.toLowerCase(java.util.Locale.ROOT))) throw new IllegalArgumentException("Invalid subregion bounds or name");
            regions.add(new SchematicRegion(name, bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]));
        }
        RegionMask.create(regions, width, height, length);
        return regions;
    }

    static boolean requiresExtended(ISchematic schematic) {
        List<SchematicRegion> regions = schematic.getRegions();
        if (regions.isEmpty()) return false;
        if (regions.size() != 1) return true;
        SchematicRegion region = regions.get(0);
        return region.minX != 0 || region.minY != 0 || region.minZ != 0 || region.maxX != schematic.getWidth() - 1
            || region.maxY != schematic.getHeight() - 1 || region.maxZ != schematic.getLength() - 1;
    }

    static void write(NBTTagCompound tag, ISchematic schematic) {
        if (schematic.getRegions().isEmpty()) return;
        NBTTagList entries = new NBTTagList();
        for (SchematicRegion region : schematic.getRegions()) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setString("Name", region.name);
            entry.setIntArray("Bounds", new int[] {region.minX, region.minY, region.minZ, region.maxX, region.maxY, region.maxZ});
            entries.appendTag(entry);
        }
        tag.setInteger("SchematicaPlusRegionsVersion", 1);
        tag.setTag(KEY, entries);
    }
}
