package com.github.lunatrius.schematica.client.world;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.gui.material.MaterialLists;
import com.github.lunatrius.schematica.client.input.Hotkey;
import com.github.lunatrius.schematica.client.input.Hotkeys;
import com.github.lunatrius.schematica.client.printer.SchematicPrinter;
import com.github.lunatrius.schematica.client.renderer.RendererSchematicGlobal;
import com.github.lunatrius.schematica.client.renderer.hud.StatusInfoHud;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.VisualSettings;
import com.github.lunatrius.schematica.handler.client.WorldHandler;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.world.schematic.ItemIdMaps;
import com.github.lunatrius.schematica.world.schematic.SchematicFileSnapshot;

import org.lwjgl.input.Keyboard;

public final class Placements {
    public static final SchematicLibrary<SchematicSourceData, SchematicWorld> LIBRARY = new SchematicLibrary<>(SchematicSourceData::read);
    public static final List<SchematicWorld> loaded = LIBRARY.placements();
    /** The currently active/selected schematic (for tools, printer, control GUI). */
    public static SchematicWorld selected;

    private static final ExecutorService SOURCE_READER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Schematica Plus schematic reader");
        thread.setDaemon(true);
        return thread;
    });
    private static final Set<File> READING = Collections.synchronizedSet(new HashSet<>());

    private Placements() {}

    /** The placements and their grid copies, for rendering and targeting (the visible placements upstream). */
    public static List<SchematicWorld> visible() {
        List<SchematicWorld> copies = GridPlacements.INSTANCE.copies();
        boolean hidden = false;
        for (SchematicWorld world : loaded) hidden |= world.otherWorld;
        if (copies.isEmpty() && !hidden) return loaded;
        List<SchematicWorld> all = new ArrayList<>(loaded.size() + copies.size());
        // worldFingerprinting: placements put in another world of the same server are left out
        for (SchematicWorld world : loaded) if (!world.otherWorld) all.add(world);
        for (SchematicWorld copy : copies) if (copy.gridBase == null || !copy.gridBase.otherWorld) all.add(copy);
        return all;
    }

    public static void moveToPlayer(SchematicWorld schematic) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (schematic != null && player != null) schematic.moveOriginTo((int) Math.floor(player.posX),
            (int) Math.floor(player.posY - player.yOffset), (int) Math.floor(player.posZ));
    }

    public static void moveMinimumTo(SchematicWorld schematic, Integer x, Integer y, Integer z) {
        if (schematic != null) {
            schematic.moveMinimumTo(x, y, z);
        }
    }

    public static void remove(SchematicWorld world) {
        if (!LIBRARY.remove(world)) return;
        RendererSchematicGlobal.INSTANCE.removeRendererSchematicChunks(world);
        if (selected == world) select(loaded.isEmpty() ? null : loaded.get(0));
        WorldHandler.INSTANCE.saveSession();
    }

    public static void unloadSource(SchematicLibrary.Source<SchematicSourceData> source) {
        for (SchematicWorld world : LIBRARY.unload(source)) {
            RendererSchematicGlobal.INSTANCE.removeRendererSchematicChunks(world);
        }
        if (selected != null && !loaded.contains(selected)) {
            select(loaded.isEmpty() ? null : loaded.get(0));
        }
        WorldHandler.INSTANCE.saveSession();
    }

    public static void clear() {
        selected = null;
        LIBRARY.clear();
        RendererSchematicGlobal.INSTANCE.destroyRendererSchematicChunks();
        SchematicPrinter.INSTANCE.setSchematic(null);
    }

    public static SchematicLibrary.Source<SchematicSourceData> loadSource(File file) throws IOException {
        SchematicLibrary.Source<SchematicSourceData> source = LIBRARY.load(file);
        reportUnknownBlocks(source);
        return source;
    }

    /**
     * Reads and parses a file on a worker thread so that the game keeps running, then decodes and adds it on the client
     * thread and passes it on; false when the file is already being read. A file loaded before is passed on right away.
     */
    public static boolean loadSourceAsync(File file, Consumer<SchematicLibrary.Source<SchematicSourceData>> onLoaded,
        Consumer<Exception> failed) throws IOException {
        File canonical = file.getCanonicalFile();
        SchematicLibrary.Source<SchematicSourceData> existing = LIBRARY.find(canonical);
        if (existing != null) {
            onLoaded.accept(existing);
            return true;
        }
        if (!READING.add(canonical)) return false;
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.theWorld;
        SOURCE_READER.execute(() -> {
            SchematicSourceData.Prepared data = null;
            Exception error = null;
            try {
                data = SchematicSourceData.prepare(canonical);
            } catch (IOException | RuntimeException e) {
                error = e;
            }
            SchematicSourceData.Prepared prepared = data;
            Exception problem = error;
            mc.func_152344_a(() -> {
                READING.remove(canonical);
                if (problem != null) { failed.accept(problem); return; }
                if (mc.theWorld != world) return;
                try {
                    SchematicLibrary.Source<SchematicSourceData> source = LIBRARY.add(canonical, SchematicSourceData.of(prepared));
                    reportUnknownBlocks(source);
                    onLoaded.accept(source);
                } catch (IOException | RuntimeException e) {
                    failed.accept(e);
                }
            });
        });
        return true;
    }

    /**
     * Lists in chat the blocks of a newly read file that this game does not have and what replaced them, and the
     * stored item stacks whose ids were translated or removed.
     */
    private static void reportUnknownBlocks(SchematicLibrary.Source<SchematicSourceData> source) {
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return;
        Map<String, String> unknown = source.data().takeUnknownBlocks();
        ItemIdMaps.Result items = source.data().takeItemIds();
        if (unknown.isEmpty() && (items == null || items.removed == 0 && items.translatedWith == null && items.omittedData.isEmpty())) return;
        Consumer<String> chat = line -> player.addChatMessage(new ChatComponentText(
            EnumChatFormatting.GOLD + line));
        if (items != null) reportItemIds(source.file().getName(), items, chat);
        if (unknown.isEmpty()) return;
        Reference.logger.warn("{}: unknown blocks replaced: {}", source.file().getName(), unknown);
        chat.accept(UiTranslations.format("schematica.message.import.unknown_blocks",
            source.file().getName(), unknown.size()));
        int shown = 0;
        for (Map.Entry<String, String> entry : unknown.entrySet()) {
            if (shown++ == 8) {
                chat.accept(UiTranslations.format("schematica.message.import.unknown_more", unknown.size() - 8));
                break;
            }
            chat.accept(UiTranslations.format("schematica.message.import.unknown_block", entry.getKey(), entry.getValue()));
        }
    }

    private static void reportItemIds(String file, ItemIdMaps.Result items,
        Consumer<String> chat) {
        if (!items.omittedData.isEmpty()) {
            Reference.logger.warn("{}: unsupported imported data omitted: {}", file, items.omittedData);
            chat.accept(UiTranslations.format("schematica.message.import.omitted_data", file,
                String.join(", ", new ArrayList<>(items.omittedData).subList(0, Math.min(8, items.omittedData.size())))));
        }
        if (items.translatedWith != null) {
            Reference.logger.info("{}: item ids translated with the id map {}", file, items.translatedWith);
            chat.accept(UiTranslations.format("schematica.message.import.items_translated", file, items.translatedWith));
        }
        if (items.removed == 0) return;
        Reference.logger.warn("{}: {} stored item stacks removed; unknown items: {}", file, items.removed, items.unknownItems);
        if (items.translatedWith == null && items.unknownItems.isEmpty()) {
            chat.accept(UiTranslations.format("schematica.message.import.items_removed", file, items.removed,
                ItemIdMaps.directory().getPath()));
        } else {
            chat.accept(UiTranslations.format("schematica.message.import.unknown_items", file, items.removed,
                String.join(", ", new ArrayList<>(items.unknownItems).subList(0, Math.min(8, items.unknownItems.size())))));
        }
    }

    /** Registers a captured schematic that has no file; it is not restored in later sessions. */
    public static SchematicLibrary.Source<SchematicSourceData> addMemorySource(String name,
        SchematicFileSnapshot snapshot) throws IOException {
        String safe = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return LIBRARY.addMemory(new File("<memory>", (safe.isEmpty() ? "schematic" : safe) + ".schemplus"), SchematicSourceData.of(snapshot));
    }

    static SchematicWorld instantiate(SchematicLibrary.Source<SchematicSourceData> source, SchematicSourceData data) throws IOException {
        SchematicWorld world = new SchematicWorld(data.instantiate(), source.file().getName());
        world.setPlacementSource(data);
        world.sourceDirectory = source.file().getParentFile();
        world.sourceFilename = source.file().getName();
        world.isRendering = true;
        return world;
    }

    public static SchematicWorld create(SchematicLibrary.Source<SchematicSourceData> source) throws IOException {
        SchematicWorld world = LIBRARY.create(source, (data, previous) -> instantiate(source, data));
        String base = world.name;
        int suffix = 2;
        Set<String> names = new HashSet<>();
        for (SchematicWorld other : loaded) if (other != world) names.add(other.name);
        while (names.contains(world.name)) world.name = base + " #" + suffix++;
        StatusInfoHud.startOverride();
        warnDisabledRendering();
        return world;
    }

    /** warnDisabledRendering: the layer mode and disabled renderers are pointed out when a placement is created. */
    private static void warnDisabledRendering() {
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        if (!ConfigurationHandler.warnDisabledRendering || player == null) return;
        RenderLayerRange range = RenderLayerSettings.RANGE;
        if (range.mode() != RenderLayerRange.Mode.ALL) {
            player.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + UiTranslations.format(
                "litematica.message.warn.layer_mode_currently_at", UiTranslations.format(range.mode().translationKey()))));
        }
        warnDisabled(player, VisualSettings.rendering, "litematica.message.warn.main_rendering_disabled", "enableRendering", "toggleAllRendering");
        warnDisabled(player, VisualSettings.schematic, "litematica.message.warn.schematic_rendering_disabled", "enableSchematicRendering", "toggleSchematicRendering");
        warnDisabled(player, VisualSettings.blocks, "litematica.message.warn.schematic_blocks_rendering_disabled", "enableSchematicBlocksRendering", "toggleSchematicBlockRendering");
    }

    private static void warnDisabled(EntityClientPlayerMP player, boolean enabled, String key, String config, String hotkey) {
        if (enabled) return;
        Hotkey binding = Hotkeys.get(hotkey);
        StringBuilder keys = new StringBuilder();
        if (binding != null) for (int code : binding.keys()) {
            if (keys.length() > 0) keys.append(" + ");
            keys.append(Keyboard.getKeyName(code));
        }
        for (String line : UiTranslations.format(key, config, hotkey, keys).split("\n")) {
            player.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + line));
        }
    }

    public static void reloadSource(SchematicLibrary.Source<SchematicSourceData> source) throws IOException {
        replaceSource(source, false);
    }

    /** Recomposes all placements of a source from its edited in-memory data. */
    public static void refreshSource(SchematicLibrary.Source<SchematicSourceData> source) throws IOException {
        replaceSource(source, true);
    }

    private static void replaceSource(SchematicLibrary.Source<SchematicSourceData> source, boolean memory) throws IOException {
        List<SchematicWorld> prepared = new ArrayList<>();
        Map<SchematicWorld, SchematicWorld> changes;
        try {
            SchematicLibrary.Factory<SchematicSourceData, SchematicWorld> factory = (data, previous) -> {
                SchematicWorld next = instantiate(source, data);
                PlacementState.copy(previous, next);
                prepared.add(next);
                RendererSchematicGlobal.INSTANCE.createRendererSchematicChunks(next);
                return next;
            };
            changes = memory ? LIBRARY.refresh(source, factory) : LIBRARY.reload(source, factory);
        } catch (IOException | RuntimeException e) {
            for (SchematicWorld world : prepared) RendererSchematicGlobal.INSTANCE.removeRendererSchematicChunks(world);
            throw e;
        }
        SchematicWorld next = changes.containsKey(selected) ? changes.get(selected) : selected;
        for (Map.Entry<SchematicWorld, SchematicWorld> entry : changes.entrySet()) {
            RendererSchematicGlobal.INSTANCE.removeRendererSchematicChunks(entry.getKey());
        }
        if (next != selected) setSelected(next);
        if (!memory) reportUnknownBlocks(source);
        WorldHandler.INSTANCE.saveSession();
    }

    public static void sourceRenamed(File previous, File next) throws IOException {
        LIBRARY.renamed(previous, next);
        for (SchematicWorld world : loaded) {
            File file = LIBRARY.sourceOf(world).file();
            world.sourceDirectory = file.getParentFile();
            world.sourceFilename = file.getName();
        }
    }

    /** Selects a schematic as the active one for tools/printer/control. */
    public static void select(SchematicWorld world) {
        // Forget the last viewed material list when changing the placement selection
        if (world != selected) MaterialLists.clear();
        setSelected(world);
    }

    private static void setSelected(SchematicWorld world) {
        selected = world;
        RendererSchematicGlobal.INSTANCE.selectSchematic(world);
        SchematicPrinter.INSTANCE.setSchematic(world);
    }
}
