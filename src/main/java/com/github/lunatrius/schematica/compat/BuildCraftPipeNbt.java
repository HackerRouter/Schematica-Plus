// BuildCraft pipes turned with the schematic, by HackerRouter, 2026.
package com.github.lunatrius.schematica.compat;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.lunatrius.schematica.util.SchematicTransform;

/**
 * The side-indexed data a TileGenericPipe saves (BuildCraft 7.1 GTNH): pluggables (gates, facades, plugs, robot
 * stations), redstone inputs, the pipe's gates and their direction, fluid sections and transfer states, power
 * queries and wooden power sources; diamond pipes also keep a filter row of nine slots per side.
 */
final class BuildCraftPipeNbt {
    static final String[] SIDE_KEYS = {"pluggable", "redstoneInputSide", "Gate", "tank", "transferState", "powerQuery",
        "nextPowerQuery", "powerSources"};

    private BuildCraftPipeNbt() {}

    private static int turn(char operation, int side) {
        return SchematicTransform.direction(operation, ForgeDirection.getOrientation(side)).ordinal();
    }

    static void transform(NBTTagCompound tag, char operation, boolean diamond) {
        for (String key : SIDE_KEYS) {
            NBTBase[] values = new NBTBase[6];
            for (int side = 0; side < 6; side++) {
                values[side] = tag.hasKey(key + "[" + side + "]") ? tag.getTag(key + "[" + side + "]") : null;
                tag.removeTag(key + "[" + side + "]");
            }
            for (int side = 0; side < 6; side++) {
                if (values[side] != null) tag.setTag(key + "[" + turn(operation, side) + "]", values[side]);
            }
        }
        for (int side = 0; side < 6; side++) {
            NBTTagCompound gate = tag.getCompoundTag("Gate[" + side + "]");
            if (gate.hasKey("direction", NBT.TAG_INT)) {
                int direction = gate.getInteger("direction");
                if (direction >= 0 && direction < 6) gate.setInteger("direction", turn(operation, direction));
            }
        }
        if (!diamond) return;
        NBTTagList items = tag.getTagList("Items", NBT.TAG_COMPOUND);
        for (int i = 0; i < items.tagCount(); i++) {
            NBTTagCompound slot = items.getCompoundTagAt(i);
            int index = slot.getByte("Slot") & 255;
            if (index < 54) slot.setByte("Slot", (byte) (turn(operation, index / 9) * 9 + index % 9));
        }
        if (tag.hasKey("usedFilters", NBT.TAG_LONG)) {
            long used = tag.getLong("usedFilters"), turned = 0;
            for (int bit = 0; bit < 54; bit++) {
                if ((used >> bit & 1) != 0) turned |= 1L << turn(operation, bit / 9) * 9 + bit % 9;
            }
            tag.setLong("usedFilters", turned | used & ~((1L << 54) - 1));
        }
    }
}
