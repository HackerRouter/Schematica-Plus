package com.github.lunatrius.schematica.world.schematic;

import net.minecraft.nbt.NBTTagCompound;
import com.github.lunatrius.schematica.api.SchematicOrigin;

final class SchematicOrigins {
    private static final String KEY = "SchematicaPlusOrigin";
    private SchematicOrigins() {}

    static SchematicOrigin read(NBTTagCompound tag) {
        if (!tag.hasKey(KEY) && !tag.hasKey(KEY + "Version")) return SchematicOrigin.ZERO;
        if (!tag.hasKey(KEY, 11) || !tag.hasKey(KEY + "Version", 3) || tag.getInteger(KEY + "Version") != 1) {
            throw new IllegalArgumentException("Unsupported schematic origin data");
        }
        int[] origin = tag.getIntArray(KEY);
        if (origin.length != 3) throw new IllegalArgumentException("Invalid schematic origin");
        return new SchematicOrigin(origin[0], origin[1], origin[2]);
    }

    static void write(NBTTagCompound tag, SchematicOrigin origin) {
        tag.removeTag(KEY); tag.removeTag(KEY + "Version");
        if (origin.isZero()) return;
        tag.setInteger(KEY + "Version", 1);
        tag.setIntArray(KEY, origin.coordinates());
    }
}
