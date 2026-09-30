package com.github.lunatrius.schematica.client.selection;

import java.io.File;

import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Area;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;

public final class AreaSelections {
    private static AreaSelectionLibrary library = new AreaSelectionLibrary();
    private static AreaSelectionStore store;
    private static boolean saveFailed;

    private AreaSelections() {}

    public static AreaSelectionLibrary library() { return library; }
    public static boolean saveFailed() { return saveFailed; }

    public static void capture() {
        Area area = library.selected();
        if (area != null) {
            if (area.selectedBox() != null) library.setPoints(area, ClientProxy.pointA, ClientProxy.pointB);
            library.setGuide(area, ClientProxy.isRenderingGuide);
        }
    }

    public static void apply() {
        Area area = library.selected();
        ClientProxy.pointA.set(area == null ? new Vector3i() : area.first());
        ClientProxy.pointB.set(area == null ? new Vector3i() : area.second());
        ClientProxy.isRenderingGuide = area != null && area.guide();
        ClientProxy.updatePoints();
    }

    public static void select(Area area) {
        capture();
        library.select(area);
        apply();
        saveCurrent();
    }

    public static void selectBox(AreaSelectionLibrary.Box box) {
        capture();
        library.selectBox(library.selected(), box);
        apply();
        saveCurrent();
    }

    public static void clear() {
        library = new AreaSelectionLibrary();
        store = null;
        saveFailed = false;
    }

    public static void restore(String key) {
        clear();
        if (key == null || key.isEmpty()) return;
        try {
            store = new AreaSelectionStore(new File(ConfigurationHandler.schematicDirectory, "AreaSelection.json"), key);
            library = store.library();
        } catch (Exception e) {
            saveFailed = true;
            Reference.logger.error("Could not restore area selections; preserving existing settings", e);
        }
        apply();
    }

    public static boolean saveCurrent() {
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

    public static boolean available(AreaSelectionLibrary expected) { return store != null && expected == library; }
}
