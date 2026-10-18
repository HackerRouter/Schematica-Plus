package com.github.lunatrius.schematica.handler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary;
import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.tool.PlacementDeletionMode;
import com.github.lunatrius.schematica.tool.ReplaceBehavior;
import com.github.lunatrius.schematica.tool.WorldEditJob;
import com.github.lunatrius.schematica.util.BlockGroups;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameData;

public class ConfigurationHandler {

    public static final ConfigurationHandler INSTANCE = new ConfigurationHandler();

    public static final String VERSION = "1";

    public static Configuration configuration;

    public static final double ALPHA_DEFAULT = 0.5;
    public static final double BLOCK_DELTA_DEFAULT = 0.005;
    public static final int PLACE_DELAY_DEFAULT = 1;
    public static final int TIMEOUT_DEFAULT = 10;
    public static final boolean[] SWAP_SLOTS_DEFAULT = new boolean[] { false, false, false, false, false, true, true,
        true, true };
    public static final String SCHEMATIC_DIRECTORY_STR = "schematics";
    public static final File SCHEMATIC_DIRECTORY_DEFAULT = new File(
        SchematicaPlus.proxy.getDataDirectory(),
        SCHEMATIC_DIRECTORY_STR);
    public static final String TOOL_ITEM_DEFAULT = "minecraft:stick";
    public static final int PLAYER_QUOTA_KILOBYTES_DEFAULT = 8192;

    public static boolean showDebugInfo = true;
    public static boolean useSchematicplusFormat = false;
    public static boolean enableAlpha = false;
    public static float alpha = (float) ALPHA_DEFAULT;
    public static boolean highlight = true;
    public static boolean highlightAir = true;
    public static float blockDelta = (float) BLOCK_DELTA_DEFAULT;
    public static boolean drawQuads = true;
    public static boolean drawLines = true;
    public static int placeDelay = PLACE_DELAY_DEFAULT;
    public static int timeout = TIMEOUT_DEFAULT;
    // Printer pass, after the behavior of litematica-printer and the Entropy5 Schematica fork
    public static double printerWorkRange;
    public static double printerMaxRange = PrinterRange.MAX;
    public static int printerIterationTimeLimit = 8, placeBlocksPerTick = 1, printerLagCheckMax = 20;
    public static String printerIteratorShape = "sphere", printerIteratorMode = "xzy", printSelectionType = "render_layers";
    public static String printerBuildOrder = "layers";
    public static boolean printerXAxisReverse, printerYAxisReverse, printerZAxisReverse, printerLagCheck = true;
    public static boolean placeInAir = true, printForcedSneak, printFallingBlockCheck = true, printerAutoDisable = true, printerPauseWhileMoving;
    public static String[] printSkipList = {};
    public static boolean containerVerifier = true, containerAutofill = true, containerLabels = true, worldFingerprinting = true;
    public static boolean printMissingMaterialHud = true, printAutoTool = true, printerPlacementSolver = true, destroyAssist, highlightNextBlock, printUseTools = true, easyPlacePrerequisites = true;
    public static int printAutoToolDurability = 10;
    public static boolean schematicPreview3D = true, schematicPreview3DReplacesImage;
    public static boolean printBreakWrongBlock, printBreakExtraBlock, printBreakWrongStateBlock, printHighlight, printHighlightThroughWalls;
    public static int printHighlightFade = 5;
    public static boolean destroyBlocks = false;
    public static boolean destroyInstantly = false;
    public static boolean[] swapSlots = SWAP_SLOTS_DEFAULT.clone();
    public static final Queue<Integer> swapSlotsQueue = new ArrayDeque<>();
    public static File schematicDirectory = SCHEMATIC_DIRECTORY_DEFAULT;
    public static String[] extraAirBlocks = {};
    public static String sortType = "";
    public static String toolItem = TOOL_ITEM_DEFAULT;
    public static boolean toolItemEnabled = true, executeRequireTool = true, pickBlockEnabled = true, cloneAtOriginalPosition, reverseOperationModeDirection;
    public static boolean easyPlaceMode, easyPlaceHoldEnabled = true, easyPlaceFirst = true, easyPlaceSwingHand = true, easyPlaceVanillaReach;
    public static boolean placementRestriction, signTextPaste = true, statusInfoHud, statusInfoHudAuto = true;
    public static int easyPlaceSwapInterval;
    /** MaLiLib MessageOutputType for Easy Place and Placement Restriction warnings: none, message or actionbar. */
    public static String placementRestrictionWarn = "actionbar";
    public static boolean pasteWithoutUpdates = false;
    public static ReplaceBehavior pasteReplaceBehavior = ReplaceBehavior.NONE;
    /** Area selection files per world (area_selections_per_world/<world>/area_selections) or shared. */
    public static boolean areaSelectionsPerWorld = true;
    public static boolean unhideSchematicVCS;
    public static boolean pasteRenderLayersOnly, pasteIgnoreInventories, pasteIgnoreEntities, pasteIgnoreBlockEntitiesEntirely, pasteUsingCommandsInSp;
    public static boolean layerModeFollowsPlayer, generateLowercaseNames, warnDisabledRendering = true;
    public static int commandLimitPerTick = 8, commandTaskInterval = 1;
    public static boolean easyPlacePostRewrite, easyPlaceClickAdjacent, pickBlockAvoidDamageable = true, pickBlockAvoidTools;
    public static String pickBlockableSlots = "1,2,3,4,5";
    public static boolean materialListContainerScan;
    public static boolean materialListIgnoreState, materialListRecipeDetails = true, renderMaterialListInGuis = true, highlightBlockInInventory;
    public static int materialListHudMaxLines = 10;
    public static double materialListHudScale = 1;
    public static PlacementDeletionMode schematicVcsDeleteMode = PlacementDeletionMode.MATCHING_BLOCK;
    public static boolean printerEnabled = true;
    public static boolean remoteEditsEnabled = true;
    public static boolean accuratePlacementEnabled = true;
    public static String easyPlaceProtocolVersion = "auto";
    public static boolean saveEnabled = true;
    public static boolean loadEnabled = true;
    public static int playerQuotaKilobytes = PLAYER_QUOTA_KILOBYTES_DEFAULT;
    public static boolean serversideSchematicsEnabled = true;

    public static Property propAlpha = null;
    public static Property propBlockDelta = null;
    public static Property propPlaceDelay = null;
    public static Property propTimeout = null;
    public static Property propSortType = null;

    private static final Set<Block> extraAirBlockList = new HashSet<>();

    /** Cached parsed tool item type from config (server-safe). */
    public static Item toolItemType = null;
    /** Cached parsed tool item meta from config. -1 means ignore meta. */
    public static int toolItemMeta = -1;

    public static void init(File configFile) {
        if (configuration == null) {
            configuration = new Configuration(configFile, VERSION);
            loadConfiguration();
        }
    }

    public static void loadConfiguration() {
        RenderColors.load(configuration);
        BlockInfoHudSettings.load(configuration);
        InfoHudSettings.load(configuration);
        VerifierOverlaySettings.load(configuration);
        VisualSettings.load(configuration);
        ConfigReader debug = new ConfigReader(configuration, Names.Config.Category.DEBUG, ConfigReader.LEGACY);
        ConfigReader render = debug.in(Names.Config.Category.RENDER, ConfigReader.LEGACY);
        ConfigReader printer = debug.in(Names.Config.Category.PRINTER, ConfigReader.LEGACY);
        ConfigReader general = debug.in(Names.Config.Category.GENERAL, ConfigReader.LEGACY);
        ConfigReader server = debug.in(Names.Config.Category.SERVER, ConfigReader.LEGACY);
        ConfigReader tool = debug.in(Names.Config.Category.TOOL, ConfigReader.GENERIC);
        ConfigReader legacyTool = debug.in(Names.Config.Category.TOOL, ConfigReader.LEGACY);
        ConfigReader overlays = debug.in(BlockInfoHudSettings.CATEGORY, ConfigReader.INFO_OVERLAYS);

        showDebugInfo = debug.flag(Names.Config.SHOW_DEBUG_INFO, true, Names.Config.SHOW_DEBUG_INFO_DESC);
        enableAlpha = render.flag(Names.Config.ALPHA_ENABLED, false, Names.Config.ALPHA_ENABLED_DESC);
        propAlpha = render.label(Names.Config.ALPHA,
            configuration.get(Names.Config.Category.RENDER, Names.Config.ALPHA, ALPHA_DEFAULT, Names.Config.ALPHA_DESC, 0.0, 1.0));
        alpha = (float) propAlpha.getDouble(ALPHA_DEFAULT);
        highlight = render.flag(Names.Config.HIGHLIGHT, true, Names.Config.HIGHLIGHT_DESC);
        highlightAir = render.flag(Names.Config.HIGHLIGHT_AIR, true, Names.Config.HIGHLIGHT_AIR_DESC);
        propBlockDelta = render.label(Names.Config.BLOCK_DELTA, configuration.get(Names.Config.Category.RENDER,
            Names.Config.BLOCK_DELTA, BLOCK_DELTA_DEFAULT, Names.Config.BLOCK_DELTA_DESC, 0.0, 0.2));
        blockDelta = (float) propBlockDelta.getDouble(BLOCK_DELTA_DEFAULT);
        drawQuads = render.flag(Names.Config.DRAW_QUADS, true, Names.Config.DRAW_QUADS_DESC);
        drawLines = render.flag(Names.Config.DRAW_LINES, true, Names.Config.DRAW_LINES_DESC);
        propPlaceDelay = printer.label(Names.Config.PLACE_DELAY, configuration.get(Names.Config.Category.PRINTER,
            Names.Config.PLACE_DELAY, PLACE_DELAY_DEFAULT, Names.Config.PLACE_DELAY_DESC, 0, 20));
        placeDelay = propPlaceDelay.getInt(PLACE_DELAY_DEFAULT);
        propTimeout = printer.label(Names.Config.TIMEOUT, configuration.get(Names.Config.Category.PRINTER,
            Names.Config.TIMEOUT, TIMEOUT_DEFAULT, Names.Config.TIMEOUT_DESC, 0, 100));
        timeout = propTimeout.getInt(TIMEOUT_DEFAULT);
        destroyBlocks = printer.flag(Names.Config.DESTROY_BLOCKS, false, Names.Config.DESTROY_BLOCKS_DESC);
        destroyInstantly = printer.flag(Names.Config.DESTROY_INSTANTLY, false, Names.Config.DESTROY_INSTANTLY_DESC);

        loadPrinter(printer);
        containerVerifier = legacyTool.flag("containerVerifier", true);
        containerAutofill = legacyTool.flag("containerAutofill", true);
        containerLabels = legacyTool.flag("containerLabels", true);
        worldFingerprinting = legacyTool.flag("worldFingerprinting", true);
        schematicPreview3D = legacyTool.flag("schematicPreview3D", true);
        schematicPreview3DReplacesImage = legacyTool.flag("schematicPreview3DReplacesImage", false);

        ConfigReader swap = debug.in(Names.Config.Category.PRINTER_SWAPSLOTS, ConfigReader.LEGACY);
        swapSlotsQueue.clear();
        for (int i = 0; i < SWAP_SLOTS_DEFAULT.length; i++) {
            swapSlots[i] = swap.flag(Names.Config.SWAP_SLOT + i, SWAP_SLOTS_DEFAULT[i], Names.Config.SWAP_SLOT_DESC);
            if (swapSlots[i]) swapSlotsQueue.offer(i);
        }

        loadSchematicDirectory(general);

        extraAirBlocks = general.label(Names.Config.EXTRA_AIR_BLOCKS, configuration.get(Names.Config.Category.GENERAL,
            Names.Config.EXTRA_AIR_BLOCKS, new String[0], Names.Config.EXTRA_AIR_BLOCKS_DESC)).getStringList();
        extraAirBlockList.clear();
        for (String name : extraAirBlocks) {
            final Block block = GameData.getBlockRegistry().getObject(name);
            if (block != Blocks.air) extraAirBlockList.add(block);
        }
        useSchematicplusFormat = general.flag(Names.Config.EXTENDED_ID_FORMAT, false, Names.Config.EXTENDED_ID_FORMAT_DESC);
        propSortType = configuration.get(Names.Config.Category.GENERAL, Names.Config.SORT_TYPE, "", Names.Config.SORT_TYPE_DESC);
        propSortType.setShowInGui(false);
        sortType = propSortType.getString();

        toolItem = legacyTool.label(Names.Config.TOOL_ITEM, configuration.get(Names.Config.Category.TOOL, Names.Config.TOOL_ITEM,
            TOOL_ITEM_DEFAULT, Names.Config.TOOL_ITEM_DESC)).getString();
        parseToolItem(toolItem);
        loadTool(tool);
        loadOverlays(overlays);
        pasteWithoutUpdates = legacyTool.flag(Names.Config.PASTE_WITHOUT_UPDATES, false, Names.Config.PASTE_WITHOUT_UPDATES_DESC);
        pasteReplaceBehavior = loadPasteReplaceBehavior(configuration);

        printerEnabled = server.flag(Names.Config.PRINTER_ENABLED, true, Names.Config.PRINTER_ENABLED_DESC);
        Property maxRange = server.label("printerMaxRange", configuration.get(Names.Config.Category.SERVER, "printerMaxRange", PrinterRange.MAX,
            "Maximum printer distance in blocks. 0 keeps normal reach; extended reach requires Plus on both sides."));
        maxRange.setMinValue(0.0).setMaxValue(PrinterRange.MAX);
        printerMaxRange = PrinterRange.clamp(maxRange.getDouble(PrinterRange.MAX));
        remoteEditsEnabled = server.flag(Names.Config.REMOTE_EDITS_ENABLED, true, Names.Config.REMOTE_EDITS_ENABLED_DESC);
        accuratePlacementEnabled = server.flag(Names.Config.ACCURATE_PLACEMENT_ENABLED, true, Names.Config.ACCURATE_PLACEMENT_ENABLED_DESC);
        saveEnabled = server.flag(Names.Config.SAVE_ENABLED, true, Names.Config.SAVE_ENABLED_DESC);
        loadEnabled = server.flag(Names.Config.LOAD_ENABLED, true, Names.Config.LOAD_ENABLED_DESC);
        playerQuotaKilobytes = server.label(Names.Config.PLAYER_QUOTA_KILOBYTES, configuration.get(Names.Config.Category.SERVER,
            Names.Config.PLAYER_QUOTA_KILOBYTES, PLAYER_QUOTA_KILOBYTES_DEFAULT, Names.Config.PLAYER_QUOTA_KILOBYTES_DESC))
            .getInt(PLAYER_QUOTA_KILOBYTES_DEFAULT);
        serversideSchematicsEnabled = server.flag(Names.Config.SERVERSIDE_SCHEMATICS_ENABLED, true, Names.Config.SERVERSIDE_SCHEMATICS_ENABLED_DESC);

        SchematicaPlus.proxy.createFolders();

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    private static void loadSchematicDirectory(ConfigReader general) {
        Property property = general.label(Names.Config.SCHEMATIC_DIRECTORY, configuration.get(Names.Config.Category.GENERAL,
            Names.Config.SCHEMATIC_DIRECTORY, SCHEMATIC_DIRECTORY_STR, Names.Config.SCHEMATIC_DIRECTORY_DESC));
        schematicDirectory = new File(property.getString());
        try {
            schematicDirectory = schematicDirectory.getCanonicalFile();
            final String schematicPath = schematicDirectory.getAbsolutePath();
            final String dataPath = SchematicaPlus.proxy.getDataDirectory().getAbsolutePath();
            if (schematicPath.contains(dataPath)) {
                property.set(schematicPath.substring(dataPath.length()).replace("\\", "/").replaceAll("^/+", ""));
            } else {
                property.set(schematicPath.replace("\\", "/"));
            }
        } catch (IOException e) {
            Reference.logger.warn("Could not canonize path!", e);
        }
    }

    private static void loadPrinter(ConfigReader printer) {
        Property range = printer.label("printerWorkRange", configuration.get(Names.Config.Category.PRINTER, "printerWorkRange", 0.0));
        range.setMinValue(0.0).setMaxValue(PrinterRange.MAX);
        printerWorkRange = PrinterRange.clamp(range.getDouble(0));
        printerIterationTimeLimit = printer.integer("printerIterationTimeLimit", 8, 0, 32);
        placeBlocksPerTick = printer.integer("placeBlocksPerTick", 1, 1, 64);
        printerBuildOrder = printer.choice("printerBuildOrder", "layers", "layers", "iterator");
        printerIteratorShape = printer.choice("printerIteratorShape", "sphere", "sphere", "octahedron", "cube");
        printerIteratorMode = printer.choice("printerIteratorMode", "xzy", "xzy", "xyz", "yxz", "yzx", "zxy", "zyx");
        printerXAxisReverse = printer.flag("printerXAxisReverse", false);
        printerYAxisReverse = printer.flag("printerYAxisReverse", false);
        printerZAxisReverse = printer.flag("printerZAxisReverse", false);
        printSelectionType = printer.choice("printSelectionType", "render_layers", "render_layers", "selection", "below_player", "above_player");
        printerLagCheck = printer.flag("printerLagCheck", true);
        printerLagCheckMax = printer.integer("printerLagCheckMax", 20, 20, 1200);
        placeInAir = printer.flag("placeInAir", true);
        printForcedSneak = printer.flag("printForcedSneak", false);
        printFallingBlockCheck = printer.flag("printFallingBlockCheck", true);
        printerPlacementSolver = printer.flag("printerPlacementSolver", true);
        destroyAssist = printer.flag("destroyAssist", false);
        printUseTools = printer.flag("printUseTools", true);
        easyPlacePrerequisites = printer.flag("easyPlacePrerequisites", true);
        highlightNextBlock = printer.flag("highlightNextBlock", false);
        printerAutoDisable = printer.flag("printerAutoDisable", true);
        printerPauseWhileMoving = printer.flag("printerPauseWhileMoving", false);
        printBreakWrongBlock = printer.flag("printBreakWrongBlock", false);
        printBreakExtraBlock = printer.flag("printBreakExtraBlock", false);
        printBreakWrongStateBlock = printer.flag("printBreakWrongStateBlock", false);
        printHighlight = printer.flag("printHighlight", false);
        printHighlightFade = printer.integer("printHighlightFade", 5, 1, 100);
        printHighlightThroughWalls = printer.flag("printHighlightThroughWalls", false);
        printMissingMaterialHud = printer.flag("printMissingMaterialHud", true);
        printAutoTool = printer.flag("printAutoTool", true);
        printAutoToolDurability = printer.integer("printAutoToolDurability", 10, 0, 1000);
        printSkipList = printer.list("printSkipList", new String[0]);
    }

    private static void loadTool(ConfigReader tool) {
        toolItemEnabled = tool.flag("toolItemEnabled", true);
        executeRequireTool = tool.flag("executeRequireHoldingTool", true);
        pickBlockEnabled = tool.flag("pickBlockEnabled", true);
        cloneAtOriginalPosition = tool.flag("cloneAtOriginalPosition", false);
        easyPlaceMode = tool.flag("easyPlaceMode", false);
        easyPlaceHoldEnabled = tool.flag("easyPlaceHoldEnabled", true);
        easyPlaceFirst = tool.flag("easyPlaceFirst", true);
        easyPlaceSwingHand = tool.flag("easyPlaceSwingHand", true);
        easyPlaceVanillaReach = tool.flag("easyPlaceVanillaReach", false);
        placementRestriction = tool.flag("placementRestriction", false);
        signTextPaste = tool.flag("signTextPaste", true);
        unhideSchematicVCS = tool.flag("unhideSchematicVCS", false);
        areaSelectionsPerWorld = tool.flag("areaSelectionsPerWorld", true);
        materialListIgnoreState = tool.flag("materialListIgnoreState", false);
        materialListContainerScan = tool.flag("materialListContainerScan", false);
        easyPlacePostRewrite = tool.flag("easyPlacePostRewrite", false);
        pasteIgnoreInventories = tool.flag("pasteIgnoreInventories", false);
        pasteIgnoreEntities = tool.flag("pasteIgnoreEntities", false);
        pasteIgnoreBlockEntitiesEntirely = tool.flag("pasteIgnoreBlockEntitiesEntirely", false);
        pasteUsingCommandsInSp = tool.flag("pasteUsingCommandsInSp", false);
        layerModeFollowsPlayer = tool.flag("layerModeFollowsPlayer", false);
        generateLowercaseNames = tool.flag("generateLowercaseNames", false);
        AreaSelectionLibrary.changeSelectedCornerOnMove = tool.flag("changeSelectedCornerOnMove", true);
        commandLimitPerTick = tool.integer("commandLimitPerTick", 8, 1, 256);
        commandTaskInterval = tool.integer("commandTaskInterval", 1, 1, 1000);
        String setblockName = tool.text("commandNameSetblock", "setblock").trim().replaceFirst("^/+", "");
        WorldEditJob.setblockCommand = setblockName.matches("[A-Za-z0-9_:.-]{1,64}") ? setblockName : "setblock";
        pasteRenderLayersOnly = "rendered_only".equals(tool.choice("pasteLayerBehavior", "all", "all", "rendered_only"));
        easyPlaceClickAdjacent = tool.flag("easyPlaceClickAdjacent", false);
        pickBlockAvoidDamageable = tool.flag("pickBlockAvoidDamageable", true);
        pickBlockAvoidTools = tool.flag("pickBlockAvoidTools", false);
        pickBlockableSlots = tool.text("pickBlockableSlots", "1,2,3,4,5");
        BlockGroups.enabled = tool.flag("enableDifferentBlocks", false);
        materialListRecipeDetails = tool.flag("materialListRecipeDetails", true);
        renderMaterialListInGuis = tool.flag("renderMaterialListInGuis", true);
        highlightBlockInInventory = tool.flag("highlightBlockInInventory", false);
        easyPlaceProtocolVersion = tool.choice("easyPlaceProtocolVersion", "auto", "auto", "v3", "v2", "slabs_only", "none");
        Property vcsDelete = tool.label("schematicVcsDeleteMode",
            configuration.get(Names.Config.Category.TOOL, "schematicVcsDeleteMode", "matching_block"));
        vcsDelete.setValidValues(PlacementDeletionMode.names());
        schematicVcsDeleteMode = PlacementDeletionMode.parse(vcsDelete.getString());
        vcsDelete.set(schematicVcsDeleteMode.value);
        easyPlaceSwapInterval = tool.integer("easyPlaceSwapInterval", 0, 0, 10000);
        placementRestrictionWarn = tool.choice("placementRestrictionWarn", "actionbar", "none", "message", "actionbar");
        reverseOperationModeDirection = tool.flag("reverseOperationModeDirection", false);
    }

    private static void loadOverlays(ConfigReader overlays) {
        AreaSelectionLibrary.defaultMode = "normal".equals(overlays.choice("defaultSelectionMode", "simple", "normal", "simple"))
            ? AreaSelectionLibrary.Mode.NORMAL
            : AreaSelectionLibrary.Mode.SIMPLE;
        warnDisabledRendering = overlays.flag("warnDisabledRendering", true);
        materialListHudMaxLines = overlays.integer("materialListHudMaxLines", 10, 1, 128);
        materialListHudScale = overlays.decimal("materialListHudScale", 1.0, 0.1, 4.0);
        statusInfoHud = overlays.flag("statusInfoHud", false);
        statusInfoHudAuto = overlays.flag("statusInfoHudAuto", true);
    }

    /** Litematica's pasteReplaceBehavior. */
    static ReplaceBehavior loadPasteReplaceBehavior(Configuration config) {
        Property property = config.get(Names.Config.Category.TOOL, "pasteReplaceBehavior", ReplaceBehavior.NONE.value);
        property.setLanguageKey("litematica.config.generic.name.pasteReplaceBehavior");
        property.setValidValues(ReplaceBehavior.names());
        ReplaceBehavior behavior = ReplaceBehavior.parse(property.getString());
        property.set(behavior.value);
        return behavior;
    }

    public static void setPasteReplaceBehavior(ReplaceBehavior behavior) {
        configuration.getCategory(Names.Config.Category.TOOL).get("pasteReplaceBehavior").set(behavior.value);
        loadConfiguration();
        configuration.save();
    }

    /**
     * Parses the tool item config string and caches the result.
     * Server-safe: does not reference any client-only classes.
     * Supports formats: "minecraft:stick", "minecraft:dye@4"
     */
    public static void parseToolItem(String itemStr) {
        toolItemType = null;
        toolItemMeta = -1;

        if (itemStr == null || itemStr.isEmpty()) {
            return;
        }

        String name = itemStr;
        int atIdx = itemStr.indexOf('@');
        if (atIdx > 0) {
            name = itemStr.substring(0, atIdx);
            try {
                toolItemMeta = Integer.parseInt(itemStr.substring(atIdx + 1));
            } catch (NumberFormatException e) {
                toolItemMeta = -1;
            }
        }

        Item item = (Item) GameData.getItemRegistry().getObject(name);
        if (item != null) {
            toolItemType = item;
            Reference.logger.debug("Tool item set to: {} (meta={})", name, toolItemMeta);
        } else {
            Reference.logger.warn("Tool item not found: {}, falling back to stick", name);
            toolItemType = (Item) GameData.getItemRegistry().getObject("minecraft:stick");
        }
    }

    private ConfigurationHandler() {}

    @SubscribeEvent
    public void onConfigurationChangedEvent(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.modID.equalsIgnoreCase(Reference.MODID)) {
            loadConfiguration();
        }
    }

    public static boolean isExtraAirBlock(final Block block) {
        return extraAirBlockList.contains(block);
    }
}
