// The printer and Easy Place use the material list's replacements (like Antideath's easy place block replacements), by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.printer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import com.github.lunatrius.schematica.client.gui.material.MaterialItemKey;
import com.github.lunatrius.schematica.client.gui.material.MaterialListModel;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A material counted as another item in a placement's material list is also built with that item: the printer and
 * Easy Place hold the replacement, and a block of the replacement where the schematic has the replaced material
 * counts as built for the printer.
 */
public final class MaterialReplacements {
    private static final Map<JsonObject, Map<MaterialItemKey, MaterialItemKey>> SAVED = new WeakHashMap<>();

    private MaterialReplacements() {}

    static Map<MaterialItemKey, MaterialItemKey> of(SchematicWorld placement) {
        if (placement.materialList != null) {
            Map<MaterialItemKey, MaterialItemKey> result = new HashMap<>();
            for (Map.Entry<MaterialItemKey, MaterialListModel.Replacement<MaterialItemKey>> entry : placement.materialList.model().replacements().entrySet()) {
                result.put(entry.getKey(), entry.getValue().key);
            }
            return result;
        }
        JsonObject data = placement.materialListData;
        if (data == null || !data.has("replacements")) return Collections.emptyMap();
        synchronized (SAVED) {
            Map<MaterialItemKey, MaterialItemKey> result = SAVED.get(data);
            if (result == null) {
                result = new HashMap<>();
                try {
                    for (Map.Entry<String, JsonElement> entry : data.getAsJsonObject("replacements").entrySet()) {
                        MaterialItemKey from = MaterialItemKey.decode(entry.getKey());
                        MaterialItemKey to = MaterialItemKey.decode(entry.getValue().getAsJsonObject().get("item").getAsString());
                        if (from != null && to != null) result.put(from, to);
                    }
                } catch (RuntimeException ignored) {
                    // a broken entry ends the list like MaterialList.fromJson
                }
                SAVED.put(data, result);
            }
            return result;
        }
    }

    /** The item to build with instead of the schematic's item, or null when the material is not replaced. */
    public static ItemStack replacement(SchematicWorld placement, ItemStack required) {
        if (required == null || required.getItem() == null) return null;
        Map<MaterialItemKey, MaterialItemKey> replacements = of(placement);
        if (replacements.isEmpty()) return null;
        MaterialItemKey key = replacements.get(new MaterialItemKey(required));
        return key == null ? null : key.stack();
    }

    /** Whether the world block is the block the replacement of the required item places. */
    public static boolean built(SchematicWorld placement, ItemStack required, Block real, int realMeta) {
        ItemStack replacement = replacement(placement, required);
        if (replacement == null || Block.getBlockFromItem(replacement.getItem()) != real) return false;
        return !replacement.getHasSubtypes() || real.damageDropped(realMeta) == replacement.getItemDamage();
    }
}
