// SPDX-License-Identifier: LGPL-3.0-only
// Litematica hotkey defaults, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.input;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Keyboard;

import com.github.lunatrius.schematica.handler.client.InputHandler;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.tool.ToolManager;

public final class Hotkeys {
    private static final Map<String, Hotkey> BY_ID = new LinkedHashMap<>();
    public static final List<Hotkey> ALL;
    private static HotkeyStore store;

    static {
        add("openGuiMainMenu", release(), Keyboard.KEY_M);
        add("openGuiSettings", normal(), Keyboard.KEY_M, Keyboard.KEY_C);
        add("openGuiMaterialList", normal(), Keyboard.KEY_M, Keyboard.KEY_L);
        add("openGuiSchematicPlacements", normal(), Keyboard.KEY_M, Keyboard.KEY_P);
        add("openGuiSchematicVerifier", normal(), Keyboard.KEY_M, Keyboard.KEY_V);
        add("openGuiSelectionManager", normal(), Keyboard.KEY_M, Keyboard.KEY_S);
        add("openGuiAreaSettings", normal(), Keyboard.KEY_MULTIPLY);
        add("openGuiPlacementSettings", normal(), Keyboard.KEY_SUBTRACT);
        add("openPlacementGridSettingsScreen", normal());
        add("openGuiLoadedSchematics", normal());
        add("saveAreaAsSchematicToFile", normal(), Keyboard.KEY_LCONTROL, Keyboard.KEY_LMENU, Keyboard.KEY_S);
        add("executeOperation", normal());
        add("operationModeChangeModifier", modifier(), Keyboard.KEY_LCONTROL);
        add("selectionNudgeModifier", modifier(), Keyboard.KEY_LMENU);
        add("selectionGrabModifier", modifier());
        add("selectionGrowModifier", modifier());
        add("toolPlaceCorner1", extra(), -100);
        add("toolPlaceCorner2", extra(), -99);
        add("toolSelectElements", extra(), -98);
        add("toolSelectModifierBlock1", modifier(), Keyboard.KEY_LMENU);
        add("toolSelectModifierBlock2", modifier(), Keyboard.KEY_LSHIFT);
        add("toolEnabledToggle", normal(), Keyboard.KEY_M, Keyboard.KEY_T);
        add("pickBlockFirst", extra(), -98);
        add("pickBlockLast", modifier());
        add("pickBlockToggle", normal(), Keyboard.KEY_M, -98);
        add("renderInfoOverlay", extra(), Keyboard.KEY_I);
        add("rerenderSchematic", normal(), Keyboard.KEY_F3, Keyboard.KEY_M);
        add("layerNext", normal(), Keyboard.KEY_PRIOR);
        add("layerPrevious", normal(), Keyboard.KEY_NEXT);
        add("layerModeNext", normal(), Keyboard.KEY_M, Keyboard.KEY_PRIOR);
        add("layerModePrevious", normal(), Keyboard.KEY_M, Keyboard.KEY_NEXT);
        add("layerSetHere", normal());
        add("selectionModeCycle", normal(), Keyboard.KEY_LCONTROL, Keyboard.KEY_M);
        add("addSelectionBox", normal(), Keyboard.KEY_M, Keyboard.KEY_A);
        for (String id : new String[] {"nudgeSelectionPositive", "nudgeSelectionNegative", "moveEntireSelection",
            "selectionGrow", "selectionShrink", "deleteSelectionBox", "setAreaOrigin", "setSelectionBoxPosition1",
            "setSelectionBoxPosition2", "unloadCurrentSchematic", "uiDemo"}) add(id, normal());
        for (String id : new String[] {"schematicEditBreakAllExcept", "schematicEditBreakPlaceAll", "schematicEditBreakPlaceDirection",
            "schematicEditReplaceAll", "schematicEditReplaceBlock", "schematicEditReplaceDirection"}) add(id, modifier());
        add("schematicEditReplaceSelection", modifier());
        add("toggleAllRendering", normal(), Keyboard.KEY_M, Keyboard.KEY_R);
        add("toggleSchematicRendering", normal(), Keyboard.KEY_M, Keyboard.KEY_G);
        for (String id : new String[] {"toggleAreaSelectionBoxesRendering", "toggleInfoOverlayRendering", "toggleOverlayRendering",
            "toggleOverlayOutlineRendering", "toggleOverlaySideRendering", "togglePlacementBoxesRendering", "toggleSchematicBlockRendering",
            "toggleTranslucentRendering", "toggleVerifierOverlayRendering", "invertGhostBlockRenderState", "invertOverlayRenderState"}) add(id, normal());
        add("renderOverlayThroughBlocks", extra(), Keyboard.KEY_RCONTROL);
        add("easyPlaceUseKey", extra(), -99);
        add("easyPlaceFirst", normal());
        add("easyPlaceToggle", normal());
        add("togglePlacementRestriction", normal());
        add("toggleSignTextPaste", normal());
        add("cloneSelection", normal());
        add("saveAreaAsInMemorySchematic", normal());
        add("schematicPlacementRotation", modifier());
        add("schematicPlacementMirror", modifier());
        add("openGuiSchematicProjects", normal());
        add("schematicVCSDeleteBlockByPlacement", normal());
        add("schematicVersionCycleModifier", modifier());
        add("schematicVersionCycleNext", normal());
        add("schematicVersionCyclePrevious", normal());
        add("workingSwitch", extra());
        add("refreshMaterialList", normal());
        for (String id : new String[] {"enableRendering", "enableSchematicRendering", "enableSchematicBlocksRendering",
            "enableSchematicFluidRendering", "enableSchematicOverlay", "enableSchematicOverlayCulling", "enableSchematicEntityHitboxes",
            "enableSchematicFakeLighting", "enableAreaSelectionBoxesRendering", "enablePlacementBoxesRendering", "overlayReducedInnerSides",
            "renderAOModernEnable", "renderBlocksAsTranslucent", "renderCollidingSchematicBlocks", "renderSchematicEntities",
            "renderSchematicTileEntities", "schematicOverlayEnableOutlines", "schematicOverlayEnableSides", "schematicOverlayModelOutline",
            "schematicOverlayModelSides", "schematicOverlayRenderThroughBlocks", "schematicOverlayTypeDiffBlock", "schematicOverlayTypeExtra",
            "schematicOverlayTypeMissing", "schematicOverlayTypeWrongBlock", "schematicOverlayTypeWrongState"})
            BY_ID.put(id, new Hotkey(id, "visuals", normal()));
        ALL = Collections.unmodifiableList(new ArrayList<>(BY_ID.values()));
    }

    private Hotkeys() {}
    public static Hotkey get(String id) { return BY_ID.get(id); }
    public static boolean held(String id) { return HotkeyHooks.held(get(id)); }
    /** Whether the hotkey is bound to exactly this one key, as KeybindMulti.hotkeyMatchesKeybind for a single vanilla key. */
    public static boolean boundOnlyTo(String id, int code) { return get(id).keys().size() == 1 && get(id).keys().get(0) == code; }
    private static Hotkey.Settings normal() { return new Hotkey.Settings(); }
    private static Hotkey.Settings extra() { Hotkey.Settings s = normal(); s.allowExtra = true; return s; }
    private static Hotkey.Settings modifier() { Hotkey.Settings s = extra(); s.ordered = false; s.cancel = false; return s; }
    private static Hotkey.Settings release() { Hotkey.Settings s = normal(); s.action = Hotkey.Action.RELEASE; s.exclusive = true; return s; }
    private static void add(String id, Hotkey.Settings settings, int... codes) { BY_ID.put(id, new Hotkey(id, settings, codes)); }

    public static void initialize(Path configDirectory, Path gameDirectory) {
        store = new HotkeyStore(configDirectory.resolve("schematica_plus_hotkeys.json"));
        try {
            if (!store.load(ALL)) { migrate(gameDirectory.resolve("options.txt")); store.save(ALL); }
        } catch (IOException error) { Reference.logger.error("Could not load hotkeys; keeping the original configuration file", error); }
        HotkeyHooks.initialize(ALL, InputHandler.INSTANCE::onHotkey, ToolManager::scroll);
        HotkeyHooks.click(com.github.lunatrius.schematica.tool.SchematicRebuild::click);
    }
    public static void save() {
        if (store == null) return;
        try { store.save(ALL); }
        catch (IOException error) { Reference.logger.error("Could not save hotkeys", error); }
    }

    private static void migrate(Path options) throws IOException {
        if (!Files.isRegularFile(options)) return;
        Map<String, String> old = new LinkedHashMap<>();
        old.put("save", "saveAreaAsSchematicToFile"); old.put("control", "openGuiMainMenu");
        old.put("layerInc", "layerNext"); old.put("layerDec", "layerPrevious"); old.put("execute", "executeOperation");
        old.put("load", "openGuiLoadedSchematics"); old.put("manipulate", "openGuiPlacementSettings"); old.put("uiDemo", "uiDemo");
        for (String line : Files.readAllLines(options, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> entry : old.entrySet()) {
                String prefix = "key_schematica.key." + entry.getKey() + ":";
                if (!line.startsWith(prefix)) continue;
                try {
                    int code = Integer.parseInt(line.substring(prefix.length()));
                    int previousDefault = entry.getKey().equals("save") ? Keyboard.KEY_N : entry.getKey().equals("control")
                        ? Keyboard.KEY_M : entry.getKey().equals("execute") ? Keyboard.KEY_RETURN : 0;
                    if (code != previousDefault) get(entry.getValue()).setKeys(code == 0 ? Collections.emptyList() : Collections.singletonList(code));
                } catch (IllegalArgumentException error) { Reference.logger.warn("Ignoring invalid legacy hotkey: " + entry.getKey()); }
            }
        }
    }
}
