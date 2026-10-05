// Item ids of stored stacks across modpacks and worlds, by HackerRouter, 2026.
package com.github.lunatrius.schematica.world.schematic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.IntUnaryOperator;

import net.minecraft.item.Item;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants.NBT;

import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.PlusDataFiles;
import com.github.lunatrius.schematica.util.SchematicLimits;

import cpw.mods.fml.common.registry.GameData;

/**
 * 1.7.10 stacks store numeric item ids, which differ between modpacks and worlds. Alpha files therefore also store
 * the names of the item ids their stacks use, and loading turns the ids into this game's. Older files only name
 * their block ids; their item ids are looked up in an id map whose block ids match those names: one archived when
 * a world was joined, or the level.dat of the world the file was saved in. Stacks of ids that cannot be resolved
 * are removed instead of becoming whatever item has that id here. Vanilla items keep their fixed ids everywhere.
 */
public final class ItemIdMaps {
    public static final String ITEM_MAPPING = "SchematicaPlusItemMapping";
    static final String COVERS = "gt.covers";
    private static final int REMOVE = -1;
    private static final Map<File, Archive> CACHE = new HashMap<>();
    private static volatile int lastArchived;

    private ItemIdMaps() {}

    /** The ids of one game or world. */
    public interface Ids {
        /** The id of a block name, or -1. */
        int block(String name);
        /** The id of an item name, or -1. */
        int item(String name);
        /** The name of an item id, or null. */
        String itemName(int id);
    }

    /** An id map read from an FML ItemData list (a level.dat or an archive). */
    public static final class Archive implements Ids {
        public final String name;
        String stamp = "";
        final Map<String, Integer> blocks = new HashMap<>(), items = new HashMap<>();
        final Map<Integer, String> itemNames = new HashMap<>();

        Archive(String name) { this.name = name; }

        @Override public int block(String name) { Integer id = blocks.get(name); return id == null ? -1 : id; }
        @Override public int item(String name) { Integer id = items.get(name); return id == null ? -1 : id; }
        @Override public String itemName(int id) { return itemNames.get(id); }

        void put(String key, int id) {
            if (key.isEmpty()) return;
            if (key.charAt(0) == '\u0001') blocks.put(key.substring(1), id);
            else if (key.charAt(0) == '\u0002') { items.put(key.substring(1), id); itemNames.put(id, key.substring(1)); }
        }

        /** The ItemData list of a level.dat ({FML: {ItemData: [{K, V}]}}), or null when it has none. */
        public static Archive read(NBTTagCompound root, String name) {
            NBTTagList list = root.getCompoundTag("FML").getTagList("ItemData", NBT.TAG_COMPOUND);
            if (list.tagCount() == 0) return null;
            Archive archive = new Archive(name);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound entry = list.getCompoundTagAt(i);
                archive.put(entry.getString("K"), entry.getInteger("V"));
            }
            return archive.items.isEmpty() ? null : archive;
        }
    }

    /** What loading did with the item ids of a file. */
    public static final class Result {
        public int removed;
        public final Set<String> unknownItems = new LinkedHashSet<>();
        public String translatedWith;
    }

    /** This game's registries. */
    public static final Ids CURRENT = new Ids() {
        @Override public int block(String name) { return GameData.getBlockRegistry().containsKey(name) ? GameData.getBlockRegistry().getId(name) : -1; }
        @Override public int item(String name) { return GameData.getItemRegistry().containsKey(name) ? GameData.getItemRegistry().getId(name) : -1; }
        @Override public String itemName(int id) {
            Item item = Item.getItemById(id);
            return item == null ? null : GameData.getItemRegistry().getNameForObject(item);
        }
    };

    static boolean vanilla(Ids ids, int id) {
        String name = ids.itemName(id);
        return name != null && name.startsWith("minecraft:");
    }

    /** Writes the names of the item ids the tile entities, entities and icon of an Alpha root use. */
    public static void writeMapping(NBTTagCompound root, Ids ids) {
        NBTTagCompound mapping = new NBTTagCompound();
        visitRoot(root, id -> {
            String name = ids.itemName(id);
            if (name != null) mapping.setInteger(name, id);
            return id;
        });
        if (mapping.hasNoTags()) root.removeTag(ITEM_MAPPING);
        else root.setTag(ITEM_MAPPING, mapping);
    }

    /** Turns the item ids of an Alpha root into this game's, then names them so a second load leaves them alone. */
    public static void remap(NBTTagCompound root, Result result) {
        remap(root, CURRENT, ItemIdMaps::archives, result);
    }

    static void remap(NBTTagCompound root, Ids current, java.util.function.Supplier<List<Archive>> archives, Result result) {
        IntUnaryOperator translation = translation(root, current, archives, result);
        if (translation == null) return;
        visitRoot(root, id -> {
            int next = translation.applyAsInt(id);
            if (next == REMOVE) result.removed++;
            return next;
        });
        writeMapping(root, current);
    }

    private static IntUnaryOperator translation(NBTTagCompound root, Ids current, java.util.function.Supplier<List<Archive>> archives, Result result) {
        if (root.hasKey(ITEM_MAPPING, NBT.TAG_COMPOUND)) {
            NBTTagCompound mapping = root.getCompoundTag(ITEM_MAPPING);
            Map<Integer, String> names = new HashMap<>();
            for (Object key : mapping.func_150296_c()) names.put(mapping.getInteger((String) key), (String) key);
            boolean identity = true;
            for (Map.Entry<Integer, String> entry : names.entrySet()) identity &= current.item(entry.getValue()) == entry.getKey();
            if (identity) return null;
            return id -> {
                String name = names.get(id);
                if (name == null) return vanilla(current, id) ? id : REMOVE;
                int next = current.item(name);
                if (next < 0) result.unknownItems.add(name);
                return next < 0 ? REMOVE : next;
            };
        }
        Set<Integer> used = new LinkedHashSet<>();
        visitRoot(root, id -> { used.add(id); return id; });
        boolean modded = false;
        for (int id : used) modded |= !vanilla(current, id);
        if (!modded || !root.hasKey(Names.NBT.MAPPING_SCHEMATICA, NBT.TAG_COMPOUND)) return null;
        NBTTagCompound blocks = root.getCompoundTag(Names.NBT.MAPPING_SCHEMATICA);
        int informative = 0;
        for (Object key : blocks.func_150296_c()) if (!((String) key).startsWith("minecraft:")) informative++;
        if (informative > 0) {
            if (matches(blocks, current)) return null;
            for (Archive archive : archives.get()) {
                if (!matches(blocks, archive)) continue;
                result.translatedWith = archive.name;
                return id -> {
                    if (vanilla(current, id)) return id;
                    String name = archive.itemName(id);
                    int next = name == null ? -1 : current.item(name);
                    if (name != null && next < 0) result.unknownItems.add(name);
                    return next < 0 ? REMOVE : next;
                };
            }
        }
        return id -> vanilla(current, id) ? id : REMOVE;
    }

    static boolean matches(NBTTagCompound blocks, Ids ids) {
        for (Object key : blocks.func_150296_c()) {
            if (ids.block((String) key) != (blocks.getShort((String) key) & 65535)) return false;
        }
        return true;
    }

    /** Every stored stack of the tile entities, entities and icon; the operator gives the new id, or -1 to remove. */
    static void visitRoot(NBTTagCompound root, IntUnaryOperator ids) {
        visitList(root.getTagList(Names.NBT.TILE_ENTITIES, NBT.TAG_COMPOUND), ids, 0, false);
        visitList(root.getTagList(Names.NBT.ENTITIES, NBT.TAG_COMPOUND), ids, 0, false);
        if (root.hasKey(Names.NBT.ICON, NBT.TAG_COMPOUND) && stack(root.getCompoundTag(Names.NBT.ICON))
            && !apply(root.getCompoundTag(Names.NBT.ICON), "id", ids)) root.removeTag(Names.NBT.ICON);
    }

    private static boolean numeric(NBTTagCompound tag, String key) {
        return tag.hasKey(key, NBT.TAG_SHORT) || tag.hasKey(key, NBT.TAG_INT);
    }

    /** A vanilla-shaped stack: a numeric id with a count. */
    static boolean stack(NBTTagCompound tag) {
        return numeric(tag, "id") && (tag.hasKey("Count", NBT.TAG_BYTE) || numeric(tag, "Count"));
    }

    /** Sets the new id of a numeric field, keeping its type; false when the stack is to be removed. */
    private static boolean apply(NBTTagCompound tag, String key, IntUnaryOperator ids) {
        boolean wide = tag.hasKey(key, NBT.TAG_INT);
        int next = ids.applyAsInt(wide ? tag.getInteger(key) : tag.getShort(key));
        if (next == REMOVE) return false;
        if (wide) tag.setInteger(key, next);
        else tag.setShort(key, (short) next);
        return true;
    }

    private static void visit(NBTTagCompound tag, IntUnaryOperator ids, int depth) {
        if (depth > SchematicLimits.MAX_NBT_DEPTH) throw new IllegalArgumentException("NBT nesting is too deep");
        String type = tag.getString("id");
        if ("FlowerPot".equals(type) && tag.hasKey("Item", NBT.TAG_INT) && tag.getInteger("Item") != 0 && !apply(tag, "Item", ids)) {
            tag.setInteger("Item", 0);
            tag.setInteger("Data", 0);
        }
        if ("RecordPlayer".equals(type) && tag.hasKey("Record", NBT.TAG_INT) && tag.getInteger("Record") != 0 && !apply(tag, "Record", ids)) {
            tag.setInteger("Record", 0);
        }
        // older GregTech covers: mCoverSides, one item id | damage << 16 per side
        if (tag.hasKey("mCoverSides", NBT.TAG_INT_ARRAY)) {
            int[] covers = tag.getIntArray("mCoverSides");
            for (int i = 0; i < covers.length; i++) {
                if (covers[i] == 0) continue;
                int next = ids.applyAsInt(covers[i] & 0xFFFF);
                covers[i] = next == REMOVE ? 0 : next | covers[i] & 0xFFFF0000;
            }
            tag.setIntArray("mCoverSides", covers);
        }
        // BuildCraft pipes: the pipe item
        if (tag.hasKey("pipeId", NBT.TAG_INT) && tag.getInteger("pipeId") != 0 && !apply(tag, "pipeId", ids)) tag.removeTag("pipeId");
        // StorageDrawers slots: {Item, Meta, Count}
        if (numeric(tag, "Item") && tag.hasKey("Meta") && !apply(tag, "Item", ids)) {
            tag.removeTag("Item");
            tag.setInteger("Count", 0);
        }
        for (Object key : tag.func_150296_c().toArray()) {
            NBTBase child = tag.getTag((String) key);
            if (child instanceof NBTTagCompound) {
                NBTTagCompound compound = (NBTTagCompound) child;
                if (stack(compound) && !apply(compound, "id", ids)) tag.removeTag((String) key);
                else visit(compound, ids, depth + 1);
            } else if (child instanceof NBTTagList && ((NBTTagList) child).func_150303_d() == NBT.TAG_COMPOUND) {
                visitList((NBTTagList) child, ids, depth + 1, COVERS.equals(key));
            }
        }
    }

    private static void visitList(NBTTagList list, IntUnaryOperator ids, int depth, boolean covers) {
        for (int i = list.tagCount() - 1; i >= 0; i--) {
            NBTTagCompound element = list.getCompoundTagAt(i);
            if (covers && element.hasKey("id", NBT.TAG_INT)) {
                // GregTech covers: {s: side, id: item id | damage << 16}
                int cover = element.getInteger("id");
                int next = ids.applyAsInt(cover & 0xFFFF);
                if (next == REMOVE) list.removeTag(i);
                else element.setInteger("id", next | cover & 0xFFFF0000);
            } else if (stack(element)) {
                if (!apply(element, "id", ids)) list.removeTag(i);
                else visit(element, ids, depth + 1);
            } else {
                visit(element, ids, depth + 1);
            }
        }
    }

    public static File directory() { return new File(PlusDataFiles.directory(), "id_maps"); }

    /** The archived id maps and level.dat files in the id map folder, newest first. */
    static List<Archive> archives() {
        File[] files = directory().listFiles((dir, name) -> name.endsWith(".dat") || name.endsWith(".dat_old"));
        List<Archive> result = new ArrayList<>();
        if (files == null) return result;
        Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        synchronized (CACHE) {
            CACHE.keySet().retainAll(Arrays.asList(files));
            for (File file : files) {
                Archive archive = CACHE.get(file);
                String stamp = file.lastModified() + ":" + file.length();
                if (archive == null || !archive.stamp.equals(stamp)) {
                    archive = load(file);
                    if (archive == null) continue;
                    archive.stamp = stamp;
                    CACHE.put(file, archive);
                }
                result.add(archive);
            }
        }
        return result;
    }

    private static Archive load(File file) {
        if (file.length() > 64L << 20) return null;
        try (InputStream input = new FileInputStream(file)) {
            return Archive.read(CompressedStreamTools.readCompressed(input), file.getName());
        } catch (IOException | RuntimeException e) {
            Reference.logger.warn("Could not read the id map {}", file, e);
            return null;
        }
    }

    /** Archives the ids of the world just joined, once per distinct id map. */
    public static void archiveCurrent() {
        Map<String, Integer> ids = new TreeMap<>(GameData.buildItemDataList().idMap);
        if (ids.hashCode() == lastArchived) return;
        lastArchived = ids.hashCode();
        Thread thread = new Thread(() -> {
            try {
                write(ids, directory());
            } catch (IOException | RuntimeException e) {
                Reference.logger.warn("Could not archive the item ids of this world", e);
            }
        }, "Schematica Plus id map");
        thread.setDaemon(true);
        thread.start();
    }

    static File write(Map<String, Integer> ids, File directory) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
        NBTTagList list = new NBTTagList();
        for (Map.Entry<String, Integer> entry : new TreeMap<>(ids).entrySet()) {
            digest.update((entry.getKey() + "=" + entry.getValue() + "\n").getBytes(StandardCharsets.UTF_8));
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("K", entry.getKey());
            tag.setInteger("V", entry.getValue());
            list.appendTag(tag);
        }
        StringBuilder hash = new StringBuilder();
        for (byte b : Arrays.copyOf(digest.digest(), 8)) hash.append(String.format("%02x", b & 255));
        File file = new File(directory, "world-" + hash + ".dat");
        if (file.isFile()) return file;
        Files.createDirectories(directory.toPath());
        NBTTagCompound fml = new NBTTagCompound();
        fml.setTag("ItemData", list);
        NBTTagCompound root = new NBTTagCompound();
        root.setTag("FML", fml);
        File temporary = new File(directory, file.getName() + ".tmp");
        try (OutputStream output = new FileOutputStream(temporary)) {
            CompressedStreamTools.writeCompressed(root, output);
        }
        Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return file;
    }
}
