package com.github.lunatrius.schematica.client.selection;

import java.io.File;
import java.util.Locale;
import java.util.function.BooleanSupplier;

import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Area;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.PlusDataFiles;

public final class AreaSelections {
    private static AreaSelectionLibrary library = new AreaSelectionLibrary();
    private static AreaSelectionStore store;
    private static boolean saveFailed;
    private static AreaSelectionLibrary override;
    private static BooleanSupplier overrideSave;
    private static final Vector3i pointA = new Vector3i();
    private static final Vector3i pointB = new Vector3i();
    public static boolean isRenderingGuide = false;

    private AreaSelections() {}

    /** The selections tools and screens edit: an open schematic project's own, else the world's. */
    public static AreaSelectionLibrary library() { return override != null ? override : library; }

    /** Swaps in a schematic project's selections (null restores the world's), keeping the edit points consistent. */
    public static void setOverride(AreaSelectionLibrary selections, BooleanSupplier save) {
        if (selections == override) return;
        capture();
        override = selections;
        overrideSave = selections == null ? null : save;
        apply();
    }

    public static boolean overridden() { return override != null; }
    public static boolean saveFailed() { return saveFailed; }

    public static void switchMode() {
        capture();
        AreaSelectionLibrary library = library();
        library.setMode(library.mode() == AreaSelectionLibrary.Mode.NORMAL ? AreaSelectionLibrary.Mode.SIMPLE : AreaSelectionLibrary.Mode.NORMAL);
        apply();
        saveCurrent();
    }

    public static String modeKey() { return "litematica.gui.label.area_selection.mode." + library().mode().name().toLowerCase(Locale.ROOT); }
    public static String cornerModeKey() { return "litematica.hud.area_selection.mode." + library().cornerMode().name().toLowerCase(Locale.ROOT); }

    public static void capture() {
        AreaSelectionLibrary library = library();
        Area area = library.selected();
        if (area != null) {
            if (area.selectedBox() != null) library.setPoints(area, pointA, pointB);
            library.setGuide(area, isRenderingGuide);
        }
    }

    public static void apply() {
        Area area = library().selected();
        pointA.set(area == null ? new Vector3i() : area.first());
        pointB.set(area == null ? new Vector3i() : area.second());
        isRenderingGuide = area != null && area.guide();
    }

    public static void select(Area area) {
        capture();
        library().select(area);
        apply();
        saveCurrent();
    }

    public static void selectBox(AreaSelectionLibrary.Box box) {
        capture();
        library().selectBox(library().selected(), box);
        apply();
        saveCurrent();
    }

    public static void clear() {
        library = new AreaSelectionLibrary();
        override = null;
        overrideSave = null;
        store = null;
        saveFailed = false;
        pointA.set(0, 0, 0);
        pointB.set(0, 0, 0);
        isRenderingGuide = false;
    }

    public static void restore(String key) {
        clear();
        if (key == null || key.isEmpty()) return;
        try {
            store = new AreaSelectionStore(PlusDataFiles.file("AreaSelection.json"), directory(key), key);
            library = store.library();
        } catch (Exception e) {
            saveFailed = true;
            Reference.logger.error("Could not restore area selections; preserving existing settings", e);
        }
        apply();
    }

    /** DataManager.getAreaSelectionsBaseDirectory: per world or server (without the dimension), or shared. */
    static File directory(String key) {
        if (!ConfigurationHandler.areaSelectionsPerWorld) return PlusDataFiles.file("area_selections");
        String world = key.contains("|") ? key.substring(0, key.lastIndexOf('|')) : key;
        if (world.startsWith("save:")) world = world.substring(5);
        else if (world.startsWith("server:")) world = world.substring(7);
        String name = AreaSelectionLibrary.safeFileName(world);
        return new File(new File(PlusDataFiles.file("area_selections_per_world"),
            name.isEmpty() ? "_" : name), "area_selections");
    }

    /** The selection files' directory of the current world, or null without a world. */
    public static File directory() { return store == null ? null : store.directory(); }

    public static boolean saveCurrent() {
        if (override != null) {
            capture();
            saveFailed = !overrideSave.getAsBoolean();
            return !saveFailed;
        }
        if (store == null) return false;
        try {
            capture();
            store.save();
            saveFailed = false;
            return true;
        } catch (Exception e) {
            saveFailed = true;
            Reference.logger.error("Could not save area selections; preserving existing settings", e);
            return false;
        }
    }

    public static void save(String key) {
        if (store != null && store.key().equals(key)) saveCurrent();
    }

    public static boolean available(AreaSelectionLibrary expected) { return (store != null || override != null) && expected == library(); }
}
