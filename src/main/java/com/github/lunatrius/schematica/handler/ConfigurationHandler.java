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
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.tool.ReplaceBehavior;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameData;

public class ConfigurationHandler {

    public static final ConfigurationHandler INSTANCE = new ConfigurationHandler();

    public static final String VERSION = "1";

    public static Configuration configuration;

    public static final boolean SHOW_DEBUG_INFO_DEFAULT = true;
    public static final boolean EXTENDED_ID_FORMAT_DEFAULT = false;
    public static final boolean ENABLE_ALPHA_DEFAULT = false;
    public static final double ALPHA_DEFAULT = 1.0;
    public static final boolean HIGHLIGHT_DEFAULT = true;
    public static final boolean HIGHLIGHT_AIR_DEFAULT = true;
    public static final double BLOCK_DELTA_DEFAULT = 0.005;
    public static final boolean DRAW_QUADS_DEFAULT = true;
    public static final boolean DRAW_LINES_DEFAULT = true;
    public static final int PLACE_DELAY_DEFAULT = 1;
    public static final int TIMEOUT_DEFAULT = 10;
    public static final boolean DESTROY_BLOCKS_DEFAULT = false;
    public static final boolean DESTROY_INSTANTLY_DEFAULT = false;
    public static final boolean[] SWAP_SLOTS_DEFAULT = new boolean[] { false, false, false, false, false, true, true,
        true, true };
    public static final String SCHEMATIC_DIRECTORY_STR = "schematics";
    public static final File SCHEMATIC_DIRECTORY_DEFAULT = new File(
        SchematicaPlus.proxy.getDataDirectory(),
        SCHEMATIC_DIRECTORY_STR);
    public static final String[] EXTRA_AIR_BLOCKS_DEFAULT = {};
    public static final String SORT_TYPE_DEFAULT = "";
    public static final String TOOL_ITEM_DEFAULT = "minecraft:stick";
    public static final boolean PASTE_WITHOUT_UPDATES_DEFAULT = false;
    public static final boolean PRINTER_ENABLED_DEFAULT = true;
    public static final boolean SAVE_ENABLED_DEFAULT = true;
    public static final boolean LOAD_ENABLED_DEFAULT = true;
    public static final int PLAYER_QUOTA_KILOBYTES_DEFAULT = 8192;
    public static final boolean SERVERSIDE_SCHEMATICS_ENABLED_DEFAULT = true;

    public static boolean showDebugInfo = SHOW_DEBUG_INFO_DEFAULT;
    public static boolean useSchematicplusFormat = EXTENDED_ID_FORMAT_DEFAULT;
    public static boolean enableAlpha = ENABLE_ALPHA_DEFAULT;
    public static float alpha = (float) ALPHA_DEFAULT;
    public static boolean highlight = HIGHLIGHT_DEFAULT;
    public static boolean highlightAir = HIGHLIGHT_AIR_DEFAULT;
    public static float blockDelta = (float) BLOCK_DELTA_DEFAULT;
    public static boolean drawQuads = DRAW_QUADS_DEFAULT;
    public static boolean drawLines = DRAW_LINES_DEFAULT;
    public static int placeDelay = PLACE_DELAY_DEFAULT;
    public static int timeout = TIMEOUT_DEFAULT;
    // Printer pass, after the behavior of litematica-printer and the Entropy5 Schematica fork
    public static double printerWorkRange;
    public static int printerIterationTimeLimit = 8, placeBlocksPerTick = 1, printerLagCheckMax = 20;
    public static String printerIteratorShape = "sphere", printerIteratorMode = "xzy", printSelectionType = "render_layers";
    public static String printerBuildOrder = "layers";
    public static boolean printerXAxisReverse, printerYAxisReverse, printerZAxisReverse, printerLagCheck = true;
    public static boolean placeInAir = true, printForcedSneak, printFallingBlockCheck = true, printerAutoDisable = true, printerPauseWhileMoving;
    public static String[] printSkipList = {};
    public static boolean containerVerifier = true, containerAutofill = true, containerLabels = true;
    public static boolean printMissingMaterialHud = true, printAutoTool = true, printerPlacementSolver = true, destroyAssist, highlightNextBlock, printUseTools = true;
    public static int printAutoToolDurability = 10;
    public static boolean schematicPreview3D = true, schematicPreview3DReplacesImage;
    public static boolean printBreakWrongBlock, printBreakExtraBlock, printBreakWrongStateBlock, printHighlight, printHighlightThroughWalls;
    public static int printHighlightFade = 5;
    public static boolean destroyBlocks = DESTROY_BLOCKS_DEFAULT;
    public static boolean destroyInstantly = DESTROY_INSTANTLY_DEFAULT;
    public static boolean[] swapSlots = SWAP_SLOTS_DEFAULT.clone();
    public static final Queue<Integer> swapSlotsQueue = new ArrayDeque<>();
    public static File schematicDirectory = SCHEMATIC_DIRECTORY_DEFAULT;
    public static String[] extraAirBlocks = EXTRA_AIR_BLOCKS_DEFAULT;
    public static String sortType = SORT_TYPE_DEFAULT;
    public static String toolItem = TOOL_ITEM_DEFAULT;
    public static boolean toolItemEnabled = true, executeRequireTool = true, pickBlockEnabled = true, cloneAtOriginalPosition, reverseOperationModeDirection;
    public static boolean easyPlaceMode, easyPlaceHoldEnabled = true, easyPlaceFirst = true, easyPlaceSwingHand = true, easyPlaceVanillaReach;
    public static boolean placementRestriction, signTextPaste = true, statusInfoHud, statusInfoHudAuto = true;
    public static int easyPlaceSwapInterval;
    /** MaLiLib MessageOutputType for Easy Place and Placement Restriction warnings: none, message or actionbar. */
    public static String placementRestrictionWarn = "actionbar";
    public static boolean pasteWithoutUpdates = PASTE_WITHOUT_UPDATES_DEFAULT;
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
    public static com.github.lunatrius.schematica.tool.PlacementDeletionMode schematicVcsDeleteMode = com.github.lunatrius.schematica.tool.PlacementDeletionMode.MATCHING_BLOCK;
    public static boolean printerEnabled = PRINTER_ENABLED_DEFAULT;
    public static boolean remoteEditsEnabled = true;
    public static boolean accuratePlacementEnabled = true;
    public static String easyPlaceProtocolVersion = "auto";
    public static boolean saveEnabled = SAVE_ENABLED_DEFAULT;
    public static boolean loadEnabled = LOAD_ENABLED_DEFAULT;
    public static int playerQuotaKilobytes = PLAYER_QUOTA_KILOBYTES_DEFAULT;
    public static boolean serversideSchematicsEnabled = SERVERSIDE_SCHEMATICS_ENABLED_DEFAULT;

    public static Property propShowDebugInfo = null;
    public static Property propUseSchematicplusFormat = null;
    public static Property propEnableAlpha = null;
    public static Property propAlpha = null;
    public static Property propHighlight = null;
    public static Property propHighlightAir = null;
    public static Property propBlockDelta = null;
    public static Property propDrawQuads = null;
    public static Property propDrawLines = null;
    public static Property propPlaceDelay = null;
    public static Property propTimeout = null;
    public static Property propDestroyBlocks = null;
    public static Property propDestroyInstantly = null;
    public static Property[] propSwapSlots = new Property[SWAP_SLOTS_DEFAULT.length];
    public static Property propSchematicDirectory = null;
    public static Property propExtraAirBlocks = null;
    public static Property propSortType = null;
    public static Property propToolItem = null;
    public static Property propPasteWithoutUpdates = null;
    public static Property propPrinterEnabled = null;
    public static Property propSaveEnabled = null;
    public static Property propLoadEnabled = null;
    public static Property propPlayerQuotaKilobytes = null;
    public static Property propServersideSchematicsEnabled = null;

    private static final Set<Block> extraAirBlockList = new HashSet<>();

    /** Cached parsed tool item type from config (server-safe). */
    public static Item toolItemType = null;
    /** Cached parsed tool item meta from config. -1 means ignore meta. */
    public static int toolItemMeta = -1;

    public static void init(File configFile) {
        if (configuration == null) {
            try {
                com.github.lunatrius.schematica.util.FileUtils.migrateLegacyConfiguration(configFile);
            } catch (IOException e) {
                throw new cpw.mods.fml.common.LoaderException("Cannot migrate the Schematica configuration to " + configFile + ": " + e);
            }
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
        propShowDebugInfo = configuration.get(
            Names.Config.Category.DEBUG,
            Names.Config.SHOW_DEBUG_INFO,
            SHOW_DEBUG_INFO_DEFAULT,
            Names.Config.SHOW_DEBUG_INFO_DESC);
        propShowDebugInfo.setLanguageKey(Names.Config.LANG_PREFIX + "." + Names.Config.SHOW_DEBUG_INFO);
        showDebugInfo = propShowDebugInfo.getBoolean(SHOW_DEBUG_INFO_DEFAULT);

        propEnableAlpha = configuration.get(
            Names.Config.Category.RENDER,
            Names.Config.ALPHA_ENABLED,
            ENABLE_ALPHA_DEFAULT,
            Names.Config.ALPHA_ENABLED_DESC);
        propEnableAlpha.setLanguageKey(Names.Config.LANG_PREFIX + "." + Names.Config.ALPHA_ENABLED);
        enableAlpha = propEnableAlpha.getBoolean(ENABLE_ALPHA_DEFAULT);

        propAlpha = configuration
            .get(Names.Config.Category.RENDER, Names.Config.ALPHA, ALPHA_DEFAULT, Names.Config.ALPHA_DESC, 0.0, 1.0);
        propAlpha.setLanguageKey(Names.Config.LANG_PREFIX + "." + Names.Config.ALPHA);
        alpha = (float) propAlpha.getDouble(ALPHA_DEFAULT);

        propHighlight = configuration
            .get(Names.Config.Category.RENDER, Names.Config.HIGHLIGHT, HIGHLIGHT_DEFAULT, Names.Config.HIGHLIGHT_DESC);
        propHighlight.setLanguageKey(Names.Config.LANG_PREFIX + "." + Names.Config.HIGHLIGHT);
        highlight = propHighlight.getBoolean(HIGHLIGHT_DEFAULT);

        propHighlightAir = configuration.get(
            Names.Config.Category.RENDER,
            Names.Config.HIGHLIGHT_AIR,
            HIGHLIGHT_AIR_DEFAULT,
            Names.Config.HIGHLIGHT_AIR_DESC);
        propHighlightAir.setLanguageKey(Names.Config.LANG_PREFIX + "." + Names.Config.HIGHLIGHT_AIR);
        highlightAir = propHighlightAir.getBoolean(HIGHLIGHT_AIR_DEFAULT);

        propBlockDelta = configuration.get(
            Names.Config.Category.RENDER,
            Names.Config.BLOCK_DELTA,
            BLOCK_DELTA_DEFAULT,
            Names.Config.BLOCK_DELTA_DESC,
            0.0,
            0.2);
        propBlockDelta.setLanguageKey(Names.Config.LANG_PREFIX + "." + Names.Config.BLOCK_DELTA);
        blockDelta = (float) propBlockDelta.getDouble(BLOCK_DELTA_DEFAULT);

        propDrawQuads = configuration.get(
            Names.Config.Category.RENDER,
            Names.Config.DRAW_QUADS,
            DRAW_QUADS_DEFAULT,
            Names.Config.DRAW_QUADS_DESC);
        propDrawQuads.setLanguageKey(Names.Config.LANG_PREFIX + "." + Names.Config.DRAW_QUADS);
        drawQuads = propDrawQuads.getBoolean(DRAW_QUADS_DEFAULT);

        propDrawLines = configuration.get(
            Names.Config.Category.RENDER,
            Names.Config.DRAW_LINES,
            DRAW_LINES_DEFAULT,
            Names.Config.DRAW_LINES_DESC);
        propDrawLines.setLanguageKey(Names.Config.LANG_PREFIX + "." + Names.Config.DRAW_LINES);
        drawLines = propDrawLines.getBoolean(DRAW_LINES_DEFAULT);

        propPlaceDelay = configuration.get(
            Names.Config.Category.PRINTER,
            Names.Config.PLACE_DELAY,
            PLACE_DELAY_DEFAULT,
            Names.Config.PLACE_DELAY_DESC,
            0,
            20);
        propPlaceDelay.setLanguageKey(Names.Config.LANG_PREFIX + "." + Names.Config.PLACE_DELAY);
        placeDelay = propPlaceDelay.getInt(PLACE_DELAY_DEFAULT);

        propTimeout = configuration.get(
            Names.Config.Category.PRINTER,
            Names.Config.TIMEOUT,
            TIMEOUT_DEFAULT,
            Names.Config.TIMEOUT_DESC,
            0,
            100);
        propTimeout.setLanguageKey(Names.Config.LANG_PREFIX + "." + Names.Config.TIMEOUT);
        timeout = propTimeout.getInt(TIMEOUT_DEFAULT);

        propDestroyBlocks = configuration.get(
            Names.Config.Category.PRINTER,
            Names.Config.DESTROY_BLOCKS,
            DESTROY_BLOCKS_DEFAULT,
            Names.Config.DESTROY_BLOCKS_DESC);
        propDestroyBlocks.setLanguageKey(Names.Config.LANG_PREFIX + "." + Names.Config.DESTROY_BLOCKS);
        destroyBlocks = propDestroyBlocks.getBoolean(DESTROY_BLOCKS_DEFAULT);

        propDestroyInstantly = configuration.get(
            Names.Config.Category.PRINTER,
            Names.Config.DESTROY_INSTANTLY,
            DESTROY_INSTANTLY_DEFAULT,
            Names.Config.DESTROY_INSTANTLY_DESC);
        propDestroyInstantly.setLanguageKey(Names.Config.LANG_PREFIX + "." + Names.Config.DESTROY_INSTANTLY);
        destroyInstantly = propDestroyInstantly.getBoolean(DESTROY_INSTANTLY_DEFAULT);

        loadPrinter();
        containerVerifier = printerProperty(configuration.get(Names.Config.Category.TOOL, "containerVerifier", true), "containerVerifier").getBoolean(true);
        containerAutofill = printerProperty(configuration.get(Names.Config.Category.TOOL, "containerAutofill", true), "containerAutofill").getBoolean(true);
        containerLabels = printerProperty(configuration.get(Names.Config.Category.TOOL, "containerLabels", true), "containerLabels").getBoolean(true);
        schematicPreview3D = printerProperty(configuration.get(Names.Config.Category.TOOL, "schematicPreview3D", true), "schematicPreview3D").getBoolean(true);
        schematicPreview3DReplacesImage = printerProperty(configuration.get(Names.Config.Category.TOOL, "schematicPreview3DReplacesImage", false), "schematicPreview3DReplacesImage").getBoolean(false);

        swapSlotsQueue.clear();
        for (int i = 0; i < SWAP_SLOTS_DEFAULT.length; i++) {
            propSwapSlots[i] = configuration.get(
                Names.Config.Category.PRINTER_SWAPSLOTS,
                Names.Config.SWAP_SLOT + i,
                SWAP_SLOTS_DEFAULT[i],
                Names.Config.SWAP_SLOT_DESC);
            propSwapSlots[i].setLanguageKey(Names.Config.LANG_PREFIX + "." + Names.Config.SWAP_SLOT + i);
            swapSlots[i] = propSwapSlots[i].getBoolean(SWAP_SLOTS_DEFAULT[i]);

            if (swapSlots[i]) {
                swapSlotsQueue.offer(i);
            }
        }

        propSchematicDirectory = configuration.get(
            Names.Config.Category.GENERAL,
            Names.Config.SCHEMATIC_DIRECTORY,
            SCHEMATIC_DIRECTORY_STR,
            Names.Config.SCHEMATIC_DIRECTORY_DESC);
        propSchematicDirectory.setLanguageKey(Names.Config.LANG_PREFIX + "." + Names.Config.SCHEMATIC_DIRECTORY);
        schematicDirectory = new File(propSchematicDirectory.getString());

        try {
            schematicDirectory = schematicDirectory.getCanonicalFile();
            final String schematicPath = schematicDirectory.getAbsolutePath();
            final String dataPath = SchematicaPlus.proxy.getDataDirectory()
                .getAbsolutePath();
            if (schematicPath.contains(dataPath)) {
                propSchematicDirectory.set(
                    schematicPath.substring(dataPath.length())
                        .replace("\\", "/")
                        .replaceAll("^/+", ""));
            } else {
                propSchematicDirectory.set(schematicPath.replace("\\", "/"));
            }
        } catch (IOException e) {
            Reference.logger.warn("Could not canonize path!", e);
        }

        propExtraAirBlocks = configuration.get(
            Names.Config.Category.GENERAL,
            Names.Config.EXTRA_AIR_BLOCKS,
            EXTRA_AIR_BLOCKS_DEFAULT,
            Names.Config.EXTRA_AIR_BLOCKS_DESC);
        propExtraAirBlocks.setLanguageKey(Names.Config.LANG_PREFIX + "." + Names.Config.EXTRA_AIR_BLOCKS);
        extraAirBlocks = propExtraAirBlocks.getStringList();

        extraAirBlockList.clear();
        for (String name : extraAirBlocks) {
            final Block block = GameData.getBlockRegistry()
                .getObject(name);
            if (block != Blocks.air) {
                extraAirBlockList.add(block);
            }
        }

        propUseSchematicplusFormat = configuration.get(
            Names.Config.Category.GENERAL,
            Names.Config.EXTENDED_ID_FORMAT,
            EXTENDED_ID_FORMAT_DEFAULT,
            Names.Config.EXTENDED_ID_FORMAT_DESC);
        propUseSchematicplusFormat.setLanguageKey(Names.Config.LANG_PREFIX + "." + Names.Config.EXTENDED_ID_FORMAT);
        useSchematicplusFormat = propUseSchematicplusFormat.getBoolean(EXTENDED_ID_FORMAT_DEFAULT);

        propSortType = configuration
            .get(Names.Config.Category.GENERAL, Names.Config.SORT_TYPE, SORT_TYPE_DEFAULT, Names.Config.SORT_TYPE_DESC);
        propSortType.setShowInGui(false);
        sortType = propSortType.getString();

        propToolItem = configuration.get(
            Names.Config.Category.TOOL,
            Names.Config.TOOL_ITEM,
            TOOL_ITEM_DEFAULT,
            Names.Config.TOOL_ITEM_DESC);
        propToolItem.setLanguageKey(Names.Config.LANG_PREFIX + "." + Names.Config.TOOL_ITEM);
        toolItem = propToolItem.getString();
        parseToolItem(toolItem);
        toolItemEnabled = toolBoolean("toolItemEnabled");
        executeRequireTool = toolBoolean("executeRequireHoldingTool");
        pickBlockEnabled = toolBoolean("pickBlockEnabled");
        Property cloneAtOrigin = configuration.get(Names.Config.Category.TOOL, "cloneAtOriginalPosition", false);
        cloneAtOrigin.setLanguageKey("litematica.config.generic.name.cloneAtOriginalPosition");
        cloneAtOriginalPosition = cloneAtOrigin.getBoolean(false);
        easyPlaceMode = toolFlag("easyPlaceMode", false);
        easyPlaceHoldEnabled = toolFlag("easyPlaceHoldEnabled", true);
        easyPlaceFirst = toolFlag("easyPlaceFirst", true);
        easyPlaceSwingHand = toolFlag("easyPlaceSwingHand", true);
        easyPlaceVanillaReach = toolFlag("easyPlaceVanillaReach", false);
        placementRestriction = toolFlag("placementRestriction", false);
        signTextPaste = toolFlag("signTextPaste", true);
        unhideSchematicVCS = toolFlag("unhideSchematicVCS", false);
        areaSelectionsPerWorld = toolFlag("areaSelectionsPerWorld", true);
        materialListIgnoreState = toolFlag("materialListIgnoreState", false);
        materialListContainerScan = toolFlag("materialListContainerScan", false);
        easyPlacePostRewrite = toolFlag("easyPlacePostRewrite", false);
        pasteIgnoreInventories = toolFlag("pasteIgnoreInventories", false);
        pasteIgnoreEntities = toolFlag("pasteIgnoreEntities", false);
        pasteIgnoreBlockEntitiesEntirely = toolFlag("pasteIgnoreBlockEntitiesEntirely", false);
        pasteUsingCommandsInSp = toolFlag("pasteUsingCommandsInSp", false);
        layerModeFollowsPlayer = toolFlag("layerModeFollowsPlayer", false);
        generateLowercaseNames = toolFlag("generateLowercaseNames", false);
        com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.changeSelectedCornerOnMove = toolFlag("changeSelectedCornerOnMove", true);
        commandLimitPerTick = toolInt("commandLimitPerTick", 8, 1, 256);
        commandTaskInterval = toolInt("commandTaskInterval", 1, 1, 1000);
        Property setblock = configuration.get(Names.Config.Category.TOOL, "commandNameSetblock", "setblock");
        setblock.setLanguageKey("litematica.config.generic.name.commandNameSetblock");
        String setblockName = setblock.getString().trim().replaceFirst("^/+", "");
        com.github.lunatrius.schematica.tool.WorldEditJob.setblockCommand = setblockName.matches("[A-Za-z0-9_:.-]{1,64}") ? setblockName : "setblock";
        Property selectionMode = configuration.get(BlockInfoHudSettings.CATEGORY, "defaultSelectionMode", "simple");
        selectionMode.setLanguageKey("litematica.config.info_overlays.name.defaultSelectionMode");
        selectionMode.setValidValues(new String[] {"normal", "simple"});
        boolean normal = "normal".equals(selectionMode.getString());
        selectionMode.set(normal ? "normal" : "simple");
        com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.defaultMode = normal
            ? com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Mode.NORMAL
            : com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Mode.SIMPLE;
        Property warnRendering = configuration.get(BlockInfoHudSettings.CATEGORY, "warnDisabledRendering", true);
        warnRendering.setLanguageKey("litematica.config.info_overlays.name.warnDisabledRendering");
        warnDisabledRendering = warnRendering.getBoolean(true);
        Property layers = configuration.get(Names.Config.Category.TOOL, "pasteLayerBehavior", "all");
        layers.setLanguageKey("litematica.config.generic.name.pasteLayerBehavior");
        layers.setValidValues(new String[] {"all", "rendered_only"});
        pasteRenderLayersOnly = "rendered_only".equals(layers.getString());
        layers.set(pasteRenderLayersOnly ? "rendered_only" : "all");
        easyPlaceClickAdjacent = toolFlag("easyPlaceClickAdjacent", false);
        pickBlockAvoidDamageable = toolFlag("pickBlockAvoidDamageable", true);
        pickBlockAvoidTools = toolFlag("pickBlockAvoidTools", false);
        Property pickSlots = configuration.get(Names.Config.Category.TOOL, "pickBlockableSlots", "1,2,3,4,5");
        pickSlots.setLanguageKey("litematica.config.generic.name.pickBlockableSlots");
        pickBlockableSlots = pickSlots.getString();
        com.github.lunatrius.schematica.util.BlockGroups.enabled = toolFlag("enableDifferentBlocks", false);
        materialListRecipeDetails = toolFlag("materialListRecipeDetails", true);
        renderMaterialListInGuis = toolFlag("renderMaterialListInGuis", true);
        highlightBlockInInventory = toolFlag("highlightBlockInInventory", false);
        Property hudLines = configuration.get(BlockInfoHudSettings.CATEGORY, "materialListHudMaxLines", 10);
        hudLines.setLanguageKey("litematica.config.info_overlays.name.materialListHudMaxLines");
        hudLines.setMinValue(1).setMaxValue(128);
        materialListHudMaxLines = Math.max(1, Math.min(128, hudLines.getInt(10)));
        Property hudScale = configuration.get(BlockInfoHudSettings.CATEGORY, "materialListHudScale", 1.0);
        hudScale.setLanguageKey("litematica.config.info_overlays.name.materialListHudScale");
        hudScale.setMinValue(0.1).setMaxValue(4.0);
        materialListHudScale = hudScale.getDouble(1.0);
        if (!Double.isFinite(materialListHudScale) || materialListHudScale < 0.1 || materialListHudScale > 4) materialListHudScale = 1;
        Property protocol = configuration.get(Names.Config.Category.TOOL, "easyPlaceProtocolVersion", "auto");
        protocol.setLanguageKey("litematica.config.generic.name.easyPlaceProtocolVersion");
        protocol.setValidValues(new String[] {"auto", "v3", "v2", "slabs_only", "none"});
        easyPlaceProtocolVersion = java.util.Arrays.asList(protocol.getValidValues()).contains(protocol.getString()) ? protocol.getString() : "auto";
        protocol.set(easyPlaceProtocolVersion);
        Property vcsDelete = configuration.get(Names.Config.Category.TOOL, "schematicVcsDeleteMode", "matching_block");
        vcsDelete.setLanguageKey("litematica.config.generic.name.schematicVcsDeleteMode");
        vcsDelete.setValidValues(com.github.lunatrius.schematica.tool.PlacementDeletionMode.names());
        schematicVcsDeleteMode = com.github.lunatrius.schematica.tool.PlacementDeletionMode.parse(vcsDelete.getString());
        vcsDelete.set(schematicVcsDeleteMode.value);
        Property swapInterval = configuration.get(Names.Config.Category.TOOL, "easyPlaceSwapInterval", 0);
        swapInterval.setLanguageKey("litematica.config.generic.name.easyPlaceSwapInterval");
        swapInterval.setMinValue(0).setMaxValue(10000);
        easyPlaceSwapInterval = Math.max(0, Math.min(10000, swapInterval.getInt(0)));
        Property warn = configuration.get(Names.Config.Category.TOOL, "placementRestrictionWarn", "actionbar");
        warn.setLanguageKey("litematica.config.generic.name.placementRestrictionWarn");
        warn.setValidValues(new String[] {"none", "message", "actionbar"});
        placementRestrictionWarn = java.util.Arrays.asList("none", "message", "actionbar").contains(warn.getString()) ? warn.getString() : "actionbar";
        warn.set(placementRestrictionWarn);
        Property status = configuration.get(BlockInfoHudSettings.CATEGORY, "statusInfoHud", false);
        status.setLanguageKey("litematica.config.info_overlays.name.statusInfoHud");
        statusInfoHud = status.getBoolean(false);
        Property statusAuto = configuration.get(BlockInfoHudSettings.CATEGORY, "statusInfoHudAuto", true);
        statusAuto.setLanguageKey("litematica.config.info_overlays.name.statusInfoHudAuto");
        statusInfoHudAuto = statusAuto.getBoolean(true);
        Property reverseModes = configuration.get(Names.Config.Category.TOOL, "reverseOperationModeDirection", false);
        reverseModes.setLanguageKey("litematica.config.generic.name.reverseOperationModeDirection");
        reverseOperationModeDirection = reverseModes.getBoolean(false);

        propPasteWithoutUpdates = configuration.get(Names.Config.Category.TOOL, Names.Config.PASTE_WITHOUT_UPDATES,
            PASTE_WITHOUT_UPDATES_DEFAULT, Names.Config.PASTE_WITHOUT_UPDATES_DESC);
        propPasteWithoutUpdates.setLanguageKey(Names.Config.LANG_PREFIX + "." + Names.Config.PASTE_WITHOUT_UPDATES);
        pasteWithoutUpdates = propPasteWithoutUpdates.getBoolean(PASTE_WITHOUT_UPDATES_DEFAULT);

        pasteReplaceBehavior = loadPasteReplaceBehavior(configuration);

        propPrinterEnabled = configuration.get(
            Names.Config.Category.SERVER,
            Names.Config.PRINTER_ENABLED,
            PRINTER_ENABLED_DEFAULT,
            Names.Config.PRINTER_ENABLED_DESC);
        propPrinterEnabled.setLanguageKey(Names.Config.LANG_PREFIX + "." + Names.Config.PRINTER_ENABLED);
        printerEnabled = propPrinterEnabled.getBoolean(PRINTER_ENABLED_DEFAULT);
        remoteEditsEnabled = serverFlag(Names.Config.REMOTE_EDITS_ENABLED, Names.Config.REMOTE_EDITS_ENABLED_DESC);
        accuratePlacementEnabled = serverFlag(Names.Config.ACCURATE_PLACEMENT_ENABLED, Names.Config.ACCURATE_PLACEMENT_ENABLED_DESC);

        propSaveEnabled = configuration.get(
            Names.Config.Category.SERVER,
            Names.Config.SAVE_ENABLED,
            SAVE_ENABLED_DEFAULT,
            Names.Config.SAVE_ENABLED_DESC);
        propSaveEnabled.setLanguageKey(Names.Config.LANG_PREFIX + "." + Names.Config.SAVE_ENABLED);
        saveEnabled = propSaveEnabled.getBoolean(SAVE_ENABLED_DEFAULT);

        propLoadEnabled = configuration.get(
            Names.Config.Category.SERVER,
            Names.Config.LOAD_ENABLED,
            LOAD_ENABLED_DEFAULT,
            Names.Config.LOAD_ENABLED_DESC);
        propLoadEnabled.setLanguageKey(Names.Config.LANG_PREFIX + "." + Names.Config.LOAD_ENABLED);
        loadEnabled = propLoadEnabled.getBoolean(LOAD_ENABLED_DEFAULT);

        propPlayerQuotaKilobytes = configuration.get(
            Names.Config.Category.SERVER,
            Names.Config.PLAYER_QUOTA_KILOBYTES,
            PLAYER_QUOTA_KILOBYTES_DEFAULT,
            Names.Config.PLAYER_QUOTA_KILOBYTES_DESC);
        propPlayerQuotaKilobytes.setLanguageKey(Names.Config.LANG_PREFIX + "." + Names.Config.PLAYER_QUOTA_KILOBYTES);
        playerQuotaKilobytes = propPlayerQuotaKilobytes.getInt(PLAYER_QUOTA_KILOBYTES_DEFAULT);

        propServersideSchematicsEnabled = configuration.get(
            Names.Config.Category.SERVER,
            Names.Config.SERVERSIDE_SCHEMATICS_ENABLED,
            SERVERSIDE_SCHEMATICS_ENABLED_DEFAULT,
            Names.Config.SERVERSIDE_SCHEMATICS_ENABLED_DESC);
        propServersideSchematicsEnabled
            .setLanguageKey(Names.Config.LANG_PREFIX + "." + Names.Config.SERVERSIDE_SCHEMATICS_ENABLED);
        serversideSchematicsEnabled = propServersideSchematicsEnabled.getBoolean(SERVERSIDE_SCHEMATICS_ENABLED_DEFAULT);

        SchematicaPlus.proxy.createFolders();

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    private static void loadPrinter() {
        ConfigCategory printer = configuration.getCategory(Names.Config.Category.PRINTER);
        // Replaced by placeBlocksPerTick and placeInAir
        printer.remove("placeInstantly");
        printer.remove("placeAdjacent");
        Property range = printerProperty(configuration.get(Names.Config.Category.PRINTER, "printerWorkRange", 0.0), "printerWorkRange");
        range.setMinValue(0.0).setMaxValue(6.0);
        printerWorkRange = Math.max(0, Math.min(6, range.getDouble(0)));
        printerIterationTimeLimit = printerInt("printerIterationTimeLimit", 8, 0, 32);
        placeBlocksPerTick = printerInt("placeBlocksPerTick", 1, 1, 64);
        printerBuildOrder = printerChoice("printerBuildOrder", "layers", "layers", "iterator");
        printerIteratorShape = printerChoice("printerIteratorShape", "sphere", "sphere", "octahedron", "cube");
        printerIteratorMode = printerChoice("printerIteratorMode", "xzy", "xzy", "xyz", "yxz", "yzx", "zxy", "zyx");
        printerXAxisReverse = printerFlag("printerXAxisReverse", false);
        printerYAxisReverse = printerFlag("printerYAxisReverse", false);
        printerZAxisReverse = printerFlag("printerZAxisReverse", false);
        printSelectionType = printerChoice("printSelectionType", "render_layers", "render_layers", "selection", "below_player", "above_player");
        printerLagCheck = printerFlag("printerLagCheck", true);
        printerLagCheckMax = printerInt("printerLagCheckMax", 20, 20, 1200);
        placeInAir = printerFlag("placeInAir", true);
        printForcedSneak = printerFlag("printForcedSneak", false);
        printFallingBlockCheck = printerFlag("printFallingBlockCheck", true);
        printerPlacementSolver = printerFlag("printerPlacementSolver", true);
        destroyAssist = printerFlag("destroyAssist", false);
        printUseTools = printerFlag("printUseTools", true);
        highlightNextBlock = printerFlag("highlightNextBlock", false);
        printerAutoDisable = printerFlag("printerAutoDisable", true);
        printerPauseWhileMoving = printerFlag("printerPauseWhileMoving", false);
        printBreakWrongBlock = printerFlag("printBreakWrongBlock", false);
        printBreakExtraBlock = printerFlag("printBreakExtraBlock", false);
        printBreakWrongStateBlock = printerFlag("printBreakWrongStateBlock", false);
        printHighlight = printerFlag("printHighlight", false);
        printHighlightFade = printerInt("printHighlightFade", 5, 1, 100);
        printHighlightThroughWalls = printerFlag("printHighlightThroughWalls", false);
        printMissingMaterialHud = printerFlag("printMissingMaterialHud", true);
        printAutoTool = printerFlag("printAutoTool", true);
        printAutoToolDurability = printerInt("printAutoToolDurability", 10, 0, 1000);
        printSkipList = printerProperty(configuration.get(Names.Config.Category.PRINTER, "printSkipList", new String[0]), "printSkipList").getStringList();
    }

    private static Property printerProperty(Property property, String name) {
        property.setLanguageKey(Names.Config.LANG_PREFIX + "." + name);
        return property;
    }

    private static int printerInt(String name, int fallback, int min, int max) {
        Property property = printerProperty(configuration.get(Names.Config.Category.PRINTER, name, fallback), name);
        property.setMinValue(min).setMaxValue(max);
        return Math.max(min, Math.min(max, property.getInt(fallback)));
    }

    private static boolean printerFlag(String name, boolean fallback) {
        return printerProperty(configuration.get(Names.Config.Category.PRINTER, name, fallback), name).getBoolean(fallback);
    }

    private static String printerChoice(String name, String fallback, String... values) {
        Property property = printerProperty(configuration.get(Names.Config.Category.PRINTER, name, fallback), name);
        property.setValidValues(values);
        String value = property.getString();
        for (String candidate : values) if (candidate.equals(value)) return value;
        property.set(fallback);
        return fallback;
    }

    private static boolean serverFlag(String name, String comment) {
        Property property = configuration.get(Names.Config.Category.SERVER, name, true, comment);
        property.setLanguageKey(Names.Config.LANG_PREFIX + "." + name);
        return property.getBoolean(true);
    }

    private static int toolInt(String name, int fallback, int min, int max) {
        Property property = configuration.get(Names.Config.Category.TOOL, name, fallback);
        property.setLanguageKey("litematica.config.generic.name." + name);
        property.setMinValue(min).setMaxValue(max);
        return Math.max(min, Math.min(max, property.getInt(fallback)));
    }

    private static boolean toolFlag(String name, boolean fallback) {
        Property property = configuration.get(Names.Config.Category.TOOL, name, fallback);
        property.setLanguageKey("litematica.config.generic.name." + name);
        return property.getBoolean(fallback);
    }

    private static boolean toolBoolean(String name) {
        Property property = configuration.get(Names.Config.Category.TOOL, name, true);
        property.setLanguageKey("litematica.config.generic.name." + name);
        return property.getBoolean(true);
    }

    /**
     * Litematica's pasteReplaceBehavior. A legacy pasteOnlyAir setting is converted once: only-air becomes None and
     * the old default, which pasted non-air blocks over anything, becomes With non-air.
     */
    static ReplaceBehavior loadPasteReplaceBehavior(Configuration config) {
        net.minecraftforge.common.config.ConfigCategory tool = config.getCategory(Names.Config.Category.TOOL);
        Property legacy = tool.get("pasteOnlyAir");
        boolean migrate = legacy != null && !tool.containsKey("pasteReplaceBehavior");
        Property property = config.get(Names.Config.Category.TOOL, "pasteReplaceBehavior", ReplaceBehavior.NONE.value);
        property.setLanguageKey("litematica.config.generic.name.pasteReplaceBehavior");
        property.setValidValues(ReplaceBehavior.names());
        if (migrate) property.set((legacy.getBoolean(false) ? ReplaceBehavior.NONE : ReplaceBehavior.WITH_NON_AIR).value);
        if (legacy != null) tool.remove("pasteOnlyAir");
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
