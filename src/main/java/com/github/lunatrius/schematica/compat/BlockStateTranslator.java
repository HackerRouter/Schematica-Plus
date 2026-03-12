package com.github.lunatrius.schematica.compat;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
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
 * Handles three resolution strategies in order:
 * 1. JSON override file (user-configurable, highest priority)
 * 2. VanillaBlockMappings dictionary (comprehensive vanilla coverage)
 * 3. Registry name fallback (modded blocks, metadata 0)
 * <p>
 * BlockState string format: "minecraft:stone[variant=granite]" or "minecraft:stone" (no properties)
 *
 * @author HackerRouter
 */
public final class BlockStateTranslator {

    private static final BlockStateTranslator INSTANCE = new BlockStateTranslator();
    private static final FMLControlledNamespacedRegistry<Block> BLOCK_REGISTRY = GameData.getBlockRegistry();

    /** User-defined overrides loaded from JSON: full blockstate string -> BlockMapping */
    private final Map<String, BlockMapping> jsonOverrides = new HashMap<>();

    /** Cache of previously resolved translations to avoid repeated lookups */
    private final Map<String, BlockMapping> cache = new HashMap<>();

    /** Fallback block for completely unresolvable states */
    private Block fallbackBlock = Blocks.stone;
    private int fallbackMeta = 0;

    private BlockStateTranslator() {}

    public static BlockStateTranslator instance() {
        return INSTANCE;
    }

    /**
     * Translates a full BlockState string to a 1.7.10 Block + metadata pair.
     *
     * @param blockStateString e.g. "minecraft:stone[variant=granite]" or "minecraft:oak_planks"
     * @return resolved BlockMapping, never null
     */
    public BlockMapping translate(String blockStateString) {
        if (blockStateString == null || blockStateString.isEmpty()) {
            return new BlockMapping(Blocks.air, 0);
        }

        // Check cache first
        BlockMapping cached = cache.get(blockStateString);
        if (cached != null) {
            return cached;
        }

        BlockMapping result = resolveInternal(blockStateString);
        cache.put(blockStateString, result);
        return result;
    }

    /**
     * Core resolution logic. Tries JSON overrides, then VanillaBlockMappings, then registry fallback.
     */
    private BlockMapping resolveInternal(String blockStateString) {
        // Parse the blockstate string into name and properties
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

        // Ensure namespace prefix
        if (!blockName.contains(":")) {
            blockName = "minecraft:" + blockName;
        }

        // 1. Check JSON overrides (exact match on full string)
        String fullKey = properties.isEmpty() ? blockName : blockName + "[" + properties + "]";
        BlockMapping override = jsonOverrides.get(fullKey);
        if (override != null) {
            return override;
        }

        // 2. Handle blocks that were split by color in 1.13+ (wool, carpet, terracotta, etc.)
        int colorMeta = VanillaBlockMappings.getColorMetaFromModernName(blockName);
        if (colorMeta >= 0) {
            String legacyName = VanillaBlockMappings.getBlockName_1_7_10(blockName);
            Block block = BLOCK_REGISTRY.getObject(legacyName);
            if (block != null && block != Blocks.air) {
                return new BlockMapping(block, colorMeta);
            }
        }

        // 3. Try VanillaBlockMappings with the legacy block name
        String legacyName = VanillaBlockMappings.getBlockName_1_7_10(blockName);

        // Sort properties alphabetically for consistent lookup keys
        String sortedProps = sortProperties(properties);

        int meta = VanillaBlockMappings.getMetadata(legacyName, sortedProps);
        if (meta >= 0) {
            Block block = BLOCK_REGISTRY.getObject(legacyName);
            if (block != null && block != Blocks.air) {
                return new BlockMapping(block, meta);
            }
        }

        // 4. Try without properties (some blocks just need the name mapping)
        if (!properties.isEmpty()) {
            meta = VanillaBlockMappings.getMetadata(legacyName, "");
            if (meta >= 0) {
                Block block = BLOCK_REGISTRY.getObject(legacyName);
                if (block != null && block != Blocks.air) {
                    return new BlockMapping(block, meta);
                }
            }
        }

        // 5. Handle special dynamic cases
        BlockMapping special = resolveSpecialCases(blockName, legacyName, properties);
        if (special != null) {
            return special;
        }

        // 6. Try direct registry lookup with legacy name (metadata 0)
        Block block = BLOCK_REGISTRY.getObject(legacyName);
        if (block != null && block != Blocks.air) {
            return new BlockMapping(block, 0);
        }

        // 7. Try direct registry lookup with original name (for modded blocks)
        block = BLOCK_REGISTRY.getObject(blockName);
        if (block != null && block != Blocks.air) {
            return new BlockMapping(block, 0);
        }

        // 8. Fallback
        Reference.logger.warn("BlockStateTranslator: Unresolved blockstate '{}', using fallback", blockStateString);
        return new BlockMapping(fallbackBlock, fallbackMeta);
    }

    /**
     * Handles special blocks that need dynamic property computation rather than table lookup.
     */
    private BlockMapping resolveSpecialCases(String modernName, String legacyName, String properties) {
        if (properties.isEmpty()) {
            return null;
        }

        Map<String, String> props = parseProperties(properties);

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

        // Fire: age only (connections are computed, not stored in 1.7.10)
        if ("minecraft:fire".equals(legacyName)) {
            int age = parseInt(props.get("age"), 0);
            Block block = BLOCK_REGISTRY.getObject("minecraft:fire");
            if (block != null) return new BlockMapping(block, age);
        }

        // Tripwire: bitmask
        if ("minecraft:tripwire".equals(legacyName)) {
            int meta = 0;
            if ("true".equals(props.get("powered"))) meta |= 1;
            if ("true".equals(props.get("suspended"))) meta |= 2; // 1.7.10 specific
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

        // Mushroom blocks (huge_mushroom): complex bitmask
        if ("minecraft:brown_mushroom_block".equals(legacyName) || "minecraft:red_mushroom_block".equals(legacyName)) {
            // In 1.7.10, metadata encodes which faces show the cap texture
            // Modern versions use individual boolean properties
            // Default to meta 0 (all pores) if we can't resolve
            Block block = BLOCK_REGISTRY.getObject(legacyName);
            if (block != null) return new BlockMapping(block, 0);
        }

        // Flower pot: contents stored in TileEntity in 1.7.10, meta 0
        if ("minecraft:flower_pot".equals(legacyName)) {
            Block block = BLOCK_REGISTRY.getObject("minecraft:flower_pot");
            if (block != null) return new BlockMapping(block, 0);
        }

        return null;
    }

    /**
     * Sorts comma-separated property pairs alphabetically for consistent map keys.
     * "facing=east,half=bottom,shape=straight" stays the same since it's already sorted.
     * "half=bottom,facing=east" becomes "facing=east,half=bottom".
     */
    private String sortProperties(String properties) {
        if (properties == null || properties.isEmpty()) {
            return "";
        }
        String[] pairs = properties.split(",");
        if (pairs.length <= 1) {
            return properties;
        }
        TreeMap<String, String> sorted = new TreeMap<>();
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

    /**
     * Parses "key1=val1,key2=val2" into a map.
     */
    private Map<String, String> parseProperties(String properties) {
        Map<String, String> map = new HashMap<>();
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

    /**
     * Loads user-defined JSON override mappings from a file.
     * Format: { "minecraft:some_block[prop=val]": { "block": "minecraft:stone", "meta": 1 } }
     */
    public void loadOverrides(File jsonFile) {
        if (jsonFile == null || !jsonFile.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(jsonFile)) {
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
        } catch (Exception e) {
            Reference.logger.error("BlockStateTranslator: Failed to load overrides from " + jsonFile.getName(), e);
        }
    }

    /**
     * Sets the fallback block used when a blockstate cannot be resolved at all.
     */
    public void setFallback(Block block, int meta) {
        this.fallbackBlock = block != null ? block : Blocks.stone;
        this.fallbackMeta = meta;
    }

    /**
     * Clears the translation cache. Call after loading new overrides.
     */
    public void clearCache() {
        cache.clear();
    }

    /** JSON deserialization helper */
    private static class JsonBlockEntry {
        String block;
        int meta;
    }
}
