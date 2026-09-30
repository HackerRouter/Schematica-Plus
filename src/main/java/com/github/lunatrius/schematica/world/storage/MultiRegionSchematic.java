package com.github.lunatrius.schematica.world.storage;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.item.ItemStack;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.api.SchematicRegion;

public final class MultiRegionSchematic extends Schematic {
    private final Map<String, ISchematic> contents = new LinkedHashMap<>();

    public MultiRegionSchematic(ItemStack icon, int width, int height, int length) {
        super(icon, width, height, length);
    }

    public void addRegion(SchematicRegion box, ISchematic region) {
        if (region.getWidth() != box.maxX - box.minX + 1 || region.getHeight() != box.maxY - box.minY + 1
            || region.getLength() != box.maxZ - box.minZ + 1 || contents.containsKey(box.name)) {
            throw new IllegalArgumentException("Invalid region contents");
        }
        SchematicCopies.overlay(region, this, box.minX, box.minY, box.minZ, true);
        contents.put(box.name, region);
    }

    @Override public ISchematic getRegionSchematic(String name) { return contents.get(name); }
}
