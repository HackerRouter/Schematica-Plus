package com.github.lunatrius.schematica.compat;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import com.github.lunatrius.schematica.reference.Reference;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry;
import cpw.mods.fml.common.registry.GameData;

/**
 * Singleton translator that converts modern BlockState strings to 1.7.10 (Block, metadata) pairs.
 * <p>
 * Resolution order:
 * 1. JSON override file (user-configurable, highest priority)
 * 2. DirectBlockMappings (direct Blocks.xxx references, bypasses registry)
 * 3. VanillaBlockMappings dictionary (property-to-metadata lookup via registry)
 * 4. Registry name fallback (modded blocks, metadata 0)
 *
 * @author HackerRouter
 */
public final class BlockStateTranslator {

    private static final BlockStateTranslator INSTANCE = new BlockStateTranslator();
    private static final FMLControlledNamespacedRegistry<Block> BLOCK_REGISTRY = GameData.getBlockRegistry();

    private final Map<String, BlockMapping> jsonOverrides = new HashMap<String, BlockMapping>();
    private final Map<String, BlockMapping> cache = new HashMap<String, BlockMapping>();

    /** Properties that don't exist in 1.7.10 and should be stripped before lookup */
    private static final Set<String> IGNORED_PROPERTIES = new HashSet<String>(Arrays.asList(
        "waterlogged", "snowy", "distance", "persistent", "powered",
        "signal_fire", "lit", "has_book", "hanging", "attached",
        "bottom", "drag", "conditional", "inverted", "short",
        "unstable", "locked", "triggered", "enabled", "has_bottle_0",
        "has_bottle_1", "has_bottle_2", "has_record", "eye",
        "up", "down", "north", "south", "east", "west",
        "in_wall", "disarmed", "suspended", "occupied"
    ));

    /**
     * Properties that should NOT be stripped even though they appear in IGNORED_PROPERTIES
     * for certain block types. Keyed by legacy block name.
     */
    private static final Map<String, Set<String>> KEEP_PROPERTIES = new HashMap<String, Set<String>>();

    static {
        // Vine needs north/south/east/west
        KEEP_PROPERTIES.put("minecraft:vine", new HashSet<String>(Arrays.asList("north", "south", "east", "west")));
        // Tripwire needs powered, suspended, attached, disarmed
        KEEP_PROPERTIES.put("minecraft:tripwire", new HashSet<String>(Arrays.asList("powered", "suspended", "attached", "disarmed")));
        // Tripwire hook needs attached, powered
        KEEP_PROPERTIES.put("minecraft:tripwire_hook", new HashSet<String>(Arrays.asList("attached", "powered")));
        // Fence gate: in_wall and powered don't affect 1.7.10 metadata (only facing+open matter)
        // "open" is NOT in IGNORED_PROPERTIES so it's always kept; no KEEP_PROPERTIES needed
        // Bed needs occupied
        KEEP_PROPERTIES.put("minecraft:bed", new HashSet<String>(Arrays.asList("occupied")));
        // End portal frame needs eye
        KEEP_PROPERTIES.put("minecraft:end_portal_frame", new HashSet<String>(Arrays.asList("eye")));
        // Brewing stand needs has_bottle_*
        KEEP_PROPERTIES.put("minecraft:brewing_stand", new HashSet<String>(Arrays.asList("has_bottle_0", "has_bottle_1", "has_bottle_2")));
        // Jukebox needs has_record
        KEEP_PROPERTIES.put("minecraft:jukebox", new HashSet<String>(Arrays.asList("has_record")));
        // TNT needs unstable (maps to explode)
        KEEP_PROPERTIES.put("minecraft:tnt", new HashSet<String>(Arrays.asList("unstable")));
        // Hopper needs enabled
        KEEP_PROPERTIES.put("minecraft:hopper", new HashSet<String>(Arrays.asList("enabled")));
        // Buttons need powered
        KEEP_PROPERTIES.put("minecraft:stone_button", new HashSet<String>(Arrays.asList("powered")));
        KEEP_PROPERTIES.put("minecraft:wooden_button", new HashSet<String>(Arrays.asList("powered")));
        // Lever needs powered
        KEEP_PROPERTIES.put("minecraft:lever", new HashSet<String>(Arrays.asList("powered")));
        // Pressure plates need powered
        KEEP_PROPERTIES.put("minecraft:stone_pressure_plate", new HashSet<String>(Arrays.asList("powered")));
        KEEP_PROPERTIES.put("minecraft:wooden_pressure_plate", new HashSet<String>(Arrays.asList("powered")));
        // Doors: handled specially in translateModernProperties (no KEEP_PROPERTIES needed)
        // Piston needs extended
        KEEP_PROPERTIES.put("minecraft:piston", new HashSet<String>(Arrays.asList("extended")));
        KEEP_PROPERTIES.put("minecraft:sticky_piston", new HashSet<String>(Arrays.asList("extended")));
        // Powered/detector/activator rails need powered
        KEEP_PROPERTIES.put("minecraft:golden_rail", new HashSet<String>(Arrays.asList("powered")));
        KEEP_PROPERTIES.put("minecraft:detector_rail", new HashSet<String>(Arrays.asList("powered")));
        KEEP_PROPERTIES.put("minecraft:activator_rail", new HashSet<String>(Arrays.asList("powered")));
        // Repeater: locked is runtime-only in 1.7.10, not stored in metadata - let IGNORED_PROPERTIES strip it
        // Comparator: powered state is handled by switching block name, no need to keep "powered" property
        // Daylight detector needs inverted (though it's a separate block in modern)
        KEEP_PROPERTIES.put("minecraft:daylight_detector", new HashSet<String>(Arrays.asList("inverted")));
        // Trapdoor needs open (not in IGNORED, but just to be safe)
        // Note: "open" is NOT in IGNORED_PROPERTIES, so it's already kept
    }

    private Block fallbackBlock = Blocks.stone;
    private int fallbackMeta = 0;

    private BlockStateTranslator() {}

    public static BlockStateTranslator instance() {
        return INSTANCE;
    }

    public BlockMapping translate(String blockStateString) {
        if (blockStateString == null || blockStateString.isEmpty()) {
            return new BlockMapping(Blocks.air, 0);
        }
        if ("minecraft:air".equals(blockStateString) || "air".equals(blockStateString)
            || "minecraft:cave_air".equals(blockStateString)
            || "minecraft:void_air".equals(blockStateString)) {
            return new BlockMapping(Blocks.air, 0);
        }

        BlockMapping cached = cache.get(blockStateString);
        if (cached != null) {
            return cached;
        }

        BlockMapping result = resolveInternal(blockStateString);
        cache.put(blockStateString, result);
        return result;
    }

    private BlockMapping resolveInternal(String blockStateString) {
        String blockName;
        String properties;
        int bracketStart = blockStateString.indexOf('[');
        if (bracketStart != -1) {
            blockName = blockStateString.substring(0, bracketStart);
            properties = blockStateString.substring(bracketStart + 1, blockStateString.length() - 1);
        } else {
            blockName = blockStateString;
            properties = "";
        }

        if (!blockName.contains(":")) {
            blockName = "minecraft:" + blockName;
        }

        // 1. JSON overrides (exact match on original input)
        String fullKey = properties.isEmpty() ? blockName : blockName + "[" + properties + "]";
        BlockMapping override = jsonOverrides.get(fullKey);
        if (override != null) {
            return override;
        }

        // 2. DirectBlockMappings for blocks without properties
        if (properties.isEmpty()) {
            BlockMapping direct = DirectBlockMappings.get(blockName);
            if (direct != null) {
                return direct;
            }
        }

        // 3. Color split blocks (wool, carpet, terracotta, etc.)
        int colorMeta = VanillaBlockMappings.getColorMetaFromModernName(blockName);
        if (colorMeta >= 0) {
            BlockMapping directColor = DirectBlockMappings.get(blockName);
            if (directColor != null) return directColor;
            String cLegacy = VanillaBlockMappings.getBlockName_1_7_10(blockName);
            Block cBlock = BLOCK_REGISTRY.getObject(cLegacy);
            if (cBlock != null && cBlock != Blocks.air) {
                if (!properties.isEmpty()) {
                    String tp = translateModernProperties(blockName, cLegacy, properties);
                    String sp = sortProperties(tp);
                    if (!sp.isEmpty()) {
                        int pm = VanillaBlockMappings.getMetadata(cLegacy, sp);
                        if (pm >= 0) return new BlockMapping(cBlock, pm);
                    }
                }
                return new BlockMapping(cBlock, colorMeta);
            }
        }

        // 4. Translate modern properties to 1.7.10 format before VanillaBlockMappings lookup
        String legacyName = VanillaBlockMappings.getBlockName_1_7_10(blockName);

        // Handle powered comparator/repeater: switch to powered variant block
        if (!properties.isEmpty() && properties.contains("powered=true")) {
            if ("minecraft:unpowered_comparator".equals(legacyName)) {
                legacyName = "minecraft:powered_comparator";
            } else if ("minecraft:unpowered_repeater".equals(legacyName)) {
                legacyName = "minecraft:powered_repeater";
            }
        }

        // Handle unlit redstone torch: lit=false means unlit_redstone_torch in 1.7.10
        if (!properties.isEmpty() && properties.contains("lit=false")) {
            if ("minecraft:redstone_torch".equals(legacyName)) {
                legacyName = "minecraft:unlit_redstone_torch";
            }
        }

        // Handle lit furnace: lit=true means lit_furnace in 1.7.10
        if (!properties.isEmpty() && properties.contains("lit=true")) {
            if ("minecraft:furnace".equals(legacyName)) {
                legacyName = "minecraft:lit_furnace";
            }
        }

        // Handle inverted daylight detector: inverted=true means daylight_detector_inverted in 1.7.10
        if (!properties.isEmpty() && properties.contains("inverted=true")) {
            if ("minecraft:daylight_detector".equals(legacyName)) {
                legacyName = "minecraft:daylight_detector_inverted";
            }
        }

        String translatedProps = translateModernProperties(blockName, legacyName, properties);
        String sortedProps = sortProperties(translatedProps);

        int meta = VanillaBlockMappings.getMetadata(legacyName, sortedProps);
        if (meta >= 0) {
            Block block = BLOCK_REGISTRY.getObject(legacyName);
            if (block != null && block != Blocks.air) {
                return new BlockMapping(block, meta);
            }
            BlockMapping directFallback = DirectBlockMappings.get(blockName);
            if (directFallback != null) {
                return new BlockMapping(directFallback.block, meta);
            }
        }

        // 5. Try without properties
        if (!properties.isEmpty()) {
            meta = VanillaBlockMappings.getMetadata(legacyName, "");
            if (meta >= 0) {
                Block block = BLOCK_REGISTRY.getObject(legacyName);
                if (block != null && block != Blocks.air) {
                    return new BlockMapping(block, meta);
                }
                BlockMapping directFallback = DirectBlockMappings.get(blockName);
                if (directFallback != null) {
                    return new BlockMapping(directFallback.block, meta);
                }
            }
        }

        // 6. Special dynamic cases (uses original properties for vine bitmask etc.)
        BlockMapping special = resolveSpecialCases(blockName, legacyName, properties);
        if (special != null) {
            return special;
        }

        // 7. Direct mapping with properties (for blocks that have properties but aren't in VanillaBlockMappings)
        BlockMapping directWithProps = DirectBlockMappings.get(blockName);
        if (directWithProps != null) {
            return directWithProps;
        }

        // 8. Registry lookup with legacy name
        Block block = BLOCK_REGISTRY.getObject(legacyName);
        if (block != null && block != Blocks.air) {
            return new BlockMapping(block, 0);
        }

        // 9. Registry lookup with original name (modded blocks)
        block = BLOCK_REGISTRY.getObject(blockName);
        if (block != null && block != Blocks.air) {
            return new BlockMapping(block, 0);
        }

        // 10. Fallback
        Reference.logger.warn("BlockStateTranslator: Unresolved blockstate '{}' (legacy: '{}'), using fallback {}",
            blockStateString, legacyName, Block.blockRegistry.getNameForObject(fallbackBlock));
        return new BlockMapping(fallbackBlock, fallbackMeta);
    }

    /**
     * Translates modern (1.13+) BlockState properties to 1.7.10 property format.
     * <ul>
     *   <li>Strips properties that don't exist in 1.7.10 (waterlogged, snowy, etc.)</li>
     *   <li>Renames modern property names to 1.7.10 equivalents (type→half for slabs)</li>
     *   <li>Injects missing variant properties for logs, planks, etc. based on block name</li>
     * </ul>
     */
    private String translateModernProperties(String modernName, String legacyName, String properties) {
        if (properties == null || properties.isEmpty()) {
            return "";
        }

        Map<String, String> props = parseProperties(properties);
        Map<String, String> result = new TreeMap<String, String>();

        // Determine which properties to keep for this specific block
        Set<String> keepSet = KEEP_PROPERTIES.get(legacyName);

        // Special handling for buttons: convert modern face+facing to 1.7.10 facing
        if ("minecraft:stone_button".equals(legacyName) || "minecraft:wooden_button".equals(legacyName)) {
            String face = props.get("face");
            String facing = props.get("facing");
            String powered = props.get("powered");
            String legacyFacing;
            if ("floor".equals(face)) {
                legacyFacing = "up";
            } else if ("ceiling".equals(face)) {
                legacyFacing = "down";
            } else {
                legacyFacing = facing != null ? facing : "north";
            }
            result.put("facing", legacyFacing);
            if (powered != null) {
                result.put("powered", powered);
            }
            // Skip normal property processing for buttons
        }
        // Special handling for doors: upper half only stores hinge, lower half stores facing+open
        else if (isDoorBlock(legacyName)) {
            String half = props.get("half");
            if ("upper".equals(half)) {
                // Upper half: only half and hinge matter
                result.put("half", "upper");
                String hinge = props.get("hinge");
                result.put("hinge", hinge != null ? hinge : "left");
            } else {
                // Lower half: facing, half, open
                result.put("half", "lower");
                String facing = props.get("facing");
                if (facing != null) result.put("facing", facing);
                String open = props.get("open");
                result.put("open", open != null ? open : "false");
            }
            // Skip normal property processing for doors
        }
        // Special handling for lever: convert modern face+facing to 1.7.10 facing
        else if ("minecraft:lever".equals(legacyName)) {
            String face = props.get("face");
            String facing = props.get("facing");
            String powered = props.get("powered");
            String legacyFacing;
            if ("floor".equals(face)) {
                // up_x or up_z depending on facing
                if ("north".equals(facing) || "south".equals(facing)) {
                    legacyFacing = "up_z";
                } else {
                    legacyFacing = "up_x";
                }
            } else if ("ceiling".equals(face)) {
                if ("north".equals(facing) || "south".equals(facing)) {
                    legacyFacing = "down_z";
                } else {
                    legacyFacing = "down_x";
                }
            } else {
                legacyFacing = facing != null ? facing : "north";
            }
            result.put("facing", legacyFacing);
            if (powered != null) {
                result.put("powered", powered);
            }
            // Skip normal property processing for levers
        }
        else {
            for (Map.Entry<String, String> entry : props.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                // Skip properties not relevant to 1.7.10, unless block-specifically kept
                if (IGNORED_PROPERTIES.contains(key)) {
                    if (keepSet == null || !keepSet.contains(key)) {
                        continue;
                    }
                }

                // Skip "type" for non-slab/non-piston blocks (e.g. chest type=single/left/right)
                if ("type".equals(key) && !legacyName.contains("slab") && !legacyName.contains("piston")) {
                    continue;
                }

                // Skip "face" property (only used by buttons/levers which are handled above)
                if ("face".equals(key)) {
                    continue;
                }

                // Rename modern property names to 1.7.10 equivalents
                String translatedKey = translatePropertyName(key, legacyName);
                String translatedValue = translatePropertyValue(translatedKey, value, legacyName);

                result.put(translatedKey, translatedValue);
            }
        } // end else

        // Inject missing variant for logs based on modern block name
        if ("minecraft:log".equals(legacyName) || "minecraft:log2".equals(legacyName)) {
            if (!result.containsKey("variant")) {
                String variant = inferLogVariant(modernName, legacyName);
                if (variant != null) {
                    result.put("variant", variant);
                }
            }
        }

        // Inject missing variant for planks
        if ("minecraft:planks".equals(legacyName) && !result.containsKey("variant")) {
            String variant = inferPlanksVariant(modernName);
            if (variant != null) {
                result.put("variant", variant);
            }
        }

        // Inject missing variant for wooden slabs
        if ("minecraft:wooden_slab".equals(legacyName) && !result.containsKey("variant")) {
            String variant = inferWoodSlabVariant(modernName);
            if (variant != null) {
                result.put("variant", variant);
            }
        }

        // Inject missing variant for leaves
        if (("minecraft:leaves".equals(legacyName) || "minecraft:leaves2".equals(legacyName))
            && !result.containsKey("variant")) {
            String variant = inferLeavesVariant(modernName, legacyName);
            if (variant != null) {
                result.put("variant", variant);
            }
        }

        // Inject missing variant for saplings
        if ("minecraft:sapling".equals(legacyName) && !result.containsKey("type")) {
            String variant = inferSaplingVariant(modernName);
            if (variant != null) {
                result.put("type", variant);
            }
        }

        // Inject missing variant for stone slabs
        if ("minecraft:stone_slab".equals(legacyName) && !result.containsKey("variant")) {
            String variant = inferStoneSlabVariant(modernName);
            if (variant != null) {
                result.put("variant", variant);
            }
        }

        // Build result string
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : result.entrySet()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return sb.toString();
    }

    /**
     * Translates a modern property name to its 1.7.10 equivalent.
     */
    private String translatePropertyName(String key, String legacyName) {
        // Slabs: modern "type" (bottom/top/double) → 1.7.10 "half"
        if ("type".equals(key) && (legacyName.contains("slab"))) {
            return "half";
        }
        // TNT: modern "unstable" → 1.7.10 "explode"
        if ("unstable".equals(key) && "minecraft:tnt".equals(legacyName)) {
            return "explode";
        }
        // Comparator: modern "mode" stays "mode"
        // Repeater: modern "delay" stays "delay"
        // Most properties keep the same name
        return key;
    }

    /**
     * Translates a modern property value to its 1.7.10 equivalent.
     */
    private String translatePropertyValue(String key, String value, String legacyName) {
        // Stairs shape: non-straight shapes are computed at runtime in 1.7.10
        // Force to "straight" so VanillaBlockMappings lookup succeeds
        if ("shape".equals(key) && legacyName.contains("stairs")) {
            if (!"straight".equals(value)) {
                return "straight";
            }
        }
        return value;
    }

    /** Infers the 1.7.10 log variant from the modern block name */
    private String inferLogVariant(String modernName, String legacyName) {
        if ("minecraft:log".equals(legacyName)) {
            if (modernName.contains("oak")) return "oak";
            if (modernName.contains("spruce")) return "spruce";
            if (modernName.contains("birch")) return "birch";
            if (modernName.contains("jungle")) return "jungle";
            return "oak"; // default for unknown wood types mapped to log
        }
        if ("minecraft:log2".equals(legacyName)) {
            if (modernName.contains("acacia")) return "acacia";
            if (modernName.contains("dark_oak")) return "dark_oak";
            return "acacia"; // default
        }
        return null;
    }

    /** Infers the 1.7.10 planks variant from the modern block name */
    private String inferPlanksVariant(String modernName) {
        if (modernName.contains("oak")) return "oak";
        if (modernName.contains("spruce")) return "spruce";
        if (modernName.contains("birch")) return "birch";
        if (modernName.contains("jungle")) return "jungle";
        if (modernName.contains("acacia")) return "acacia";
        if (modernName.contains("dark_oak")) return "dark_oak";
        return "oak";
    }

    /** Infers the 1.7.10 wooden slab variant from the modern block name */
    private String inferWoodSlabVariant(String modernName) {
        if (modernName.contains("oak")) return "oak";
        if (modernName.contains("spruce")) return "spruce";
        if (modernName.contains("birch")) return "birch";
        if (modernName.contains("jungle")) return "jungle";
        if (modernName.contains("acacia")) return "acacia";
        if (modernName.contains("dark_oak")) return "dark_oak";
        return "oak";
    }

    /** Infers the 1.7.10 leaves variant from the modern block name */
    private String inferLeavesVariant(String modernName, String legacyName) {
        if ("minecraft:leaves".equals(legacyName)) {
            if (modernName.contains("oak")) return "oak";
            if (modernName.contains("spruce")) return "spruce";
            if (modernName.contains("birch")) return "birch";
            if (modernName.contains("jungle")) return "jungle";
            return "oak";
        }
        if ("minecraft:leaves2".equals(legacyName)) {
            if (modernName.contains("acacia")) return "acacia";
            if (modernName.contains("dark_oak")) return "dark_oak";
            return "acacia";
        }
        return null;
    }

    /** Infers the 1.7.10 sapling type from the modern block name */
    private String inferSaplingVariant(String modernName) {
        if (modernName.contains("oak")) return "oak";
        if (modernName.contains("spruce")) return "spruce";
        if (modernName.contains("birch")) return "birch";
        if (modernName.contains("jungle")) return "jungle";
        if (modernName.contains("acacia")) return "acacia";
        if (modernName.contains("dark_oak")) return "dark_oak";
        return "oak";
    }

    /** Infers the 1.7.10 stone slab variant from the modern block name */
    private String inferStoneSlabVariant(String modernName) {
        // minecraft:stone_slab -> variant=stone
        if ("minecraft:stone_slab".equals(modernName) || modernName.contains("smooth_stone")) return "stone";
        if (modernName.contains("cobblestone")) return "cobblestone";
        if (modernName.contains("sandstone") && !modernName.contains("red_sandstone")) return "sandstone";
        if (modernName.contains("stone_brick")) return "stone_brick";
        if (modernName.contains("brick") && !modernName.contains("nether") && !modernName.contains("stone")) return "brick";
        if (modernName.contains("nether_brick")) return "nether_brick";
        if (modernName.contains("quartz")) return "quartz";
        if (modernName.contains("prismarine")) return "stone"; // best approximation
        return "stone";
    }

    /** Checks if a legacy block name is a door block */
    private boolean isDoorBlock(String legacyName) {
        return "minecraft:wooden_door".equals(legacyName)
            || "minecraft:iron_door".equals(legacyName);
    }

    private BlockMapping resolveSpecialCases(String modernName, String legacyName, String properties) {
        if (properties.isEmpty()) {
            return null;
        }

        Map<String, String> props = parseProperties(properties);

        // Skull / Head blocks: floor skulls have rotation property, wall skulls have facing property
        // In 1.7.10: meta 1 = floor, meta 2-5 = wall (north/south/west/east)
        // The "powered" property (modern only) should be ignored
        if ("minecraft:skull".equals(legacyName)) {
            Block block = BLOCK_REGISTRY.getObject("minecraft:skull");
            if (block == null) block = Blocks.skull;
            if (props.containsKey("rotation")) {
                // Floor skull: metadata = 1, rotation is stored in TileEntity Rot tag
                return new BlockMapping(block, 1);
            }
            if (props.containsKey("facing")) {
                String facing = props.get("facing");
                switch (facing) {
                    case "north": return new BlockMapping(block, 2);
                    case "south": return new BlockMapping(block, 3);
                    case "west":  return new BlockMapping(block, 4);
                    case "east":  return new BlockMapping(block, 5);
                    case "up":    return new BlockMapping(block, 1);
                    default:      return new BlockMapping(block, 1);
                }
            }
            // Default: floor skull
            return new BlockMapping(block, 1);
        }

        // Vines: bitmask south=1, west=2, north=4, east=8
        if ("minecraft:vine".equals(legacyName)) {
            int meta = 0;
            if ("true".equals(props.get("south"))) meta |= 1;
            if ("true".equals(props.get("west"))) meta |= 2;
            if ("true".equals(props.get("north"))) meta |= 4;
            if ("true".equals(props.get("east"))) meta |= 8;
            Block block = BLOCK_REGISTRY.getObject("minecraft:vine");
            if (block != null) return new BlockMapping(block, meta);
        }

        // Fire: age only
        if ("minecraft:fire".equals(legacyName)) {
            int age = parseInt(props.get("age"), 0);
            Block block = BLOCK_REGISTRY.getObject("minecraft:fire");
            if (block != null) return new BlockMapping(block, age);
        }

        // Tripwire: bitmask
        if ("minecraft:tripwire".equals(legacyName)) {
            int meta = 0;
            if ("true".equals(props.get("powered"))) meta |= 1;
            if ("true".equals(props.get("suspended"))) meta |= 2;
            if ("true".equals(props.get("attached"))) meta |= 4;
            if ("true".equals(props.get("disarmed"))) meta |= 8;
            Block block = BLOCK_REGISTRY.getObject("minecraft:tripwire");
            if (block != null) return new BlockMapping(block, meta);
        }

        // Tripwire hook
        if ("minecraft:tripwire_hook".equals(legacyName)) {
            int meta = 0;
            String facing = props.get("facing");
            if ("south".equals(facing)) meta = 0;
            else if ("west".equals(facing)) meta = 1;
            else if ("north".equals(facing)) meta = 2;
            else if ("east".equals(facing)) meta = 3;
            if ("true".equals(props.get("attached"))) meta |= 4;
            if ("true".equals(props.get("powered"))) meta |= 8;
            Block block = BLOCK_REGISTRY.getObject("minecraft:tripwire_hook");
            if (block != null) return new BlockMapping(block, meta);
        }

        // Mushroom blocks
        if ("minecraft:brown_mushroom_block".equals(legacyName) || "minecraft:red_mushroom_block".equals(legacyName)) {
            Block block = BLOCK_REGISTRY.getObject(legacyName);
            if (block != null) return new BlockMapping(block, 0);
        }

        // Flower pot
        if ("minecraft:flower_pot".equals(legacyName)) {
            Block block = BLOCK_REGISTRY.getObject("minecraft:flower_pot");
            if (block != null) return new BlockMapping(block, 0);
        }

        return null;
    }

    private String sortProperties(String properties) {
        if (properties == null || properties.isEmpty()) {
            return "";
        }
        String[] pairs = properties.split(",");
        if (pairs.length <= 1) {
            return properties;
        }
        TreeMap<String, String> sorted = new TreeMap<String, String>();
        for (String pair : pairs) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                sorted.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
            }
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return sb.toString();
    }

    private Map<String, String> parseProperties(String properties) {
        Map<String, String> map = new HashMap<String, String>();
        if (properties == null || properties.isEmpty()) return map;
        for (String pair : properties.split(",")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                map.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
            }
        }
        return map;
    }

    private int parseInt(String s, int def) {
        if (s == null) return def;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }

    public void loadOverrides(File jsonFile) {
        if (jsonFile == null || !jsonFile.exists()) {
            return;
        }
        try {
            FileReader reader = new FileReader(jsonFile);
            try {
                Gson gson = new Gson();
                Map<String, JsonBlockEntry> entries = gson.fromJson(
                    reader, new TypeToken<Map<String, JsonBlockEntry>>(){}.getType()
                );
                if (entries != null) {
                    for (Map.Entry<String, JsonBlockEntry> entry : entries.entrySet()) {
                        Block block = BLOCK_REGISTRY.getObject(entry.getValue().block);
                        if (block != null && block != Blocks.air) {
                            jsonOverrides.put(entry.getKey(), new BlockMapping(block, entry.getValue().meta));
                        }
                    }
                    Reference.logger.info("BlockStateTranslator: Loaded {} JSON overrides from {}", entries.size(), jsonFile.getName());
                }
            } finally {
                reader.close();
            }
        } catch (Exception e) {
            Reference.logger.error("BlockStateTranslator: Failed to load overrides from " + jsonFile.getName(), e);
        }
    }

    public void setFallback(Block block, int meta) {
        this.fallbackBlock = block != null ? block : Blocks.stone;
        this.fallbackMeta = meta;
    }

    public void clearCache() {
        cache.clear();
    }

    private static class JsonBlockEntry {
        String block;
        int meta;
    }
}
