// Named NBT references and cross-pack visual cache validation, by HackerRouter, 2026.
package com.github.lunatrius.schematica.world.schematic;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import com.github.lunatrius.schematica.util.SchematicLimits;

import cpw.mods.fml.common.registry.GameData;

/** Extra names for 1.7.10 NBT fields which remain numeric even in a named schematic format. */
final class PortableSchematicData {
    static final String BLOCK_MAPPING = "SchematicaPlusNbtBlockMapping";
    static final String REGISTRY = "SchematicaPlusRegistry";
    private static final String VISUAL = "SchematicaVisualState";

    private PortableSchematicData() {}

    static void write(NBTTagCompound root) {
        write(root, ItemIdMaps.CURRENT, registry());
    }

    static void write(NBTTagCompound root, ItemIdMaps.Ids ids, String registry) {
        ItemIdMaps.writeMapping(root, ids);
        NBTTagCompound names = new NBTTagCompound();
        walk(root, tag -> {
            String field = blockField(tag);
            if (field == null) return;
            String name = ids.blockName(number(tag, field));
            if (name != null) names.setInteger(name, number(tag, field));
        }, 0);
        if (names.hasNoTags()) root.removeTag(BLOCK_MAPPING);
        else root.setTag(BLOCK_MAPPING, names);
        root.setString(REGISTRY, registry);
    }

    static void read(NBTTagCompound root) {
        reportMissingFluids(root, FluidRegistry::isFluidRegistered);
        if (root.hasKey(ItemIdMaps.ITEM_MAPPING, NBT.TAG_COMPOUND)) ItemIdMaps.remap(root, ImportReport.items());
        readExtra(root, ItemIdMaps.CURRENT, root.hasKey(REGISTRY, NBT.TAG_STRING) ? registry() : "");
    }

    static void reportMissingFluids(NBTTagCompound root, Predicate<String> registered) {
        walk(root, tag -> {
            if (!tag.hasKey("FluidName", NBT.TAG_STRING) || !tag.hasKey("Amount", NBT.TAG_INT)
                || tag.getInteger("Amount") <= 0) return;
            String name = tag.getString("FluidName");
            if (!name.isEmpty() && !registered.test(name)) ImportReport.omittedData("unavailable fluid: " + name);
        }, 0);
    }

    static void readExtra(NBTTagCompound root, ItemIdMaps.Ids ids, String registry) {
        if (root.hasKey(BLOCK_MAPPING, NBT.TAG_COMPOUND)) {
            NBTTagCompound mapping = root.getCompoundTag(BLOCK_MAPPING);
            Map<Integer, String> names = new HashMap<>();
            for (Object key : mapping.func_150296_c()) names.put(mapping.getInteger((String) key), (String) key);
            NBTTagCompound updated = new NBTTagCompound();
            walk(root, tag -> {
                String field = blockField(tag);
                if (field == null) return;
                int old = number(tag, field);
                String name = names.get(old);
                int next = name == null ? -1 : ids.block(name);
                if (next < 0) {
                    if (old != 0) ImportReport.unknownBlock(name == null ? "block id " + old : name, "minecraft:air");
                    next = 0;
                } else updated.setInteger(name, next);
                if (tag.hasKey(field, NBT.TAG_SHORT)) tag.setShort(field, (short) next);
                else tag.setInteger(field, next);
            }, 0);
            root.setTag(BLOCK_MAPPING, updated);
        }
        if (root.hasKey(REGISTRY, NBT.TAG_STRING) && !registry.equals(root.getString(REGISTRY))) {
            walk(root, PortableSchematicData::discardOpaqueVisuals, 0);
            root.setString(REGISTRY, registry);
        }
    }

    private static String blockField(NBTTagCompound tag) {
        String type = tag.getString("id"), field;
        if ("Piston".equals(type) || "minecraft:piston".equals(type)) field = "blockId";
        else if ("FallingSand".equals(type) || "minecraft:falling_block".equals(type)) field = "TileID";
        else if ("Enderman".equals(type) || "minecraft:enderman".equals(type)) field = "carried";
        else if (type.startsWith("Minecart") || type.startsWith("minecraft:") && type.endsWith("minecart")) field = "DisplayTile";
        else return null;
        return tag.hasKey(field, NBT.TAG_SHORT) || tag.hasKey(field, NBT.TAG_INT) ? field : null;
    }

    private static int number(NBTTagCompound tag, String field) {
        return tag.hasKey(field, NBT.TAG_SHORT) ? tag.getShort(field) & 0xFFFF : tag.getInteger(field);
    }

    private static void discardOpaqueVisuals(NBTTagCompound tag) {
        if (!tag.hasKey(VISUAL, NBT.TAG_COMPOUND)) return;
        NBTTagCompound visual = tag.getCompoundTag(VISUAL);
        if (visual.hasKey("Packet")) {
            visual.removeTag("Packet");
            ImportReport.omittedData("cross-pack tile update packet");
        }
        NBTTagCompound adapters = visual.getCompoundTag("Adapters");
        for (Object key : adapters.func_150296_c().toArray()) {
            if (containsBytes(adapters.getTag((String) key), 0)) {
                adapters.removeTag((String) key);
                ImportReport.omittedData("cross-pack visual data: " + key);
            }
        }
    }

    private static boolean containsBytes(NBTBase tag, int depth) {
        checkDepth(depth);
        if (tag.getId() == NBT.TAG_BYTE_ARRAY) return true;
        if (tag instanceof NBTTagCompound) {
            NBTTagCompound compound = (NBTTagCompound) tag;
            for (Object key : compound.func_150296_c()) if (containsBytes(compound.getTag((String) key), depth + 1)) return true;
        } else if (tag instanceof NBTTagList) {
            NBTTagList list = (NBTTagList) tag;
            if (list.func_150303_d() == NBT.TAG_BYTE_ARRAY) return list.tagCount() > 0;
            if (list.func_150303_d() == NBT.TAG_COMPOUND) {
                for (int i = 0; i < list.tagCount(); i++) if (containsBytes(list.getCompoundTagAt(i), depth + 1)) return true;
            } else if (list.func_150303_d() == NBT.TAG_LIST) {
                NBTTagList copy = (NBTTagList) list.copy();
                while (copy.tagCount() > 0) if (containsBytes(copy.removeTag(copy.tagCount() - 1), depth + 1)) return true;
            }
        }
        return false;
    }

    private static void walk(NBTTagCompound tag, Consumer<NBTTagCompound> visit, int depth) {
        checkDepth(depth);
        visit.accept(tag);
        for (Object key : tag.func_150296_c().toArray()) {
            NBTBase child = tag.getTag((String) key);
            if (child instanceof NBTTagCompound) walk((NBTTagCompound) child, visit, depth + 1);
            else if (child instanceof NBTTagList) {
                NBTTagList list = (NBTTagList) child;
                if (list.func_150303_d() == NBT.TAG_COMPOUND) {
                    for (int i = 0; i < list.tagCount(); i++) walk(list.getCompoundTagAt(i), visit, depth + 1);
                }
            }
        }
    }

    private static void checkDepth(int depth) {
        if (depth > SchematicLimits.MAX_NBT_DEPTH) throw new IllegalArgumentException("NBT nesting is too deep");
    }

    private static String registry() {
        Map<String, Integer> ids = new TreeMap<>(GameData.buildItemDataList().idMap);
        for (Map.Entry<String, Fluid> fluid : FluidRegistry.getRegisteredFluids().entrySet()) ids.put("\u0003" + fluid.getKey(), fluid.getValue().getID());
        return fingerprint(ids);
    }

    static String fingerprint(Map<String, Integer> ids) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Map.Entry<String, Integer> entry : new TreeMap<>(ids).entrySet()) {
                digest.update((entry.getKey() + "=" + entry.getValue() + "\n").getBytes(StandardCharsets.UTF_8));
            }
            StringBuilder hash = new StringBuilder();
            for (byte b : digest.digest()) hash.append(String.format("%02x", b & 255));
            return hash.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
