
package com.github.lunatrius.schematica.compat;

import java.util.HashMap;
import java.util.Map;

/**
 * Complete vanilla BlockState property-to-metadata mapping dictionary.
 * Maps modern (1.12+) BlockState property strings to 1.7.10 metadata values.
 * Also handles block name renames between versions.
 *
 * @author HackerRouter
 */
public final class VanillaBlockMappings {

    private static final Map<String, String> BLOCK_RENAMES = new HashMap<>();
    private static final Map<String, Integer> PROPERTY_TO_META = new HashMap<>();

    private VanillaBlockMappings() {}

    public static String getBlockName_1_7_10(String modernName) {
        return BLOCK_RENAMES.getOrDefault(modernName, modernName);
    }

    public static int getMetadata(String blockName, String properties) {
        if (properties == null || properties.isEmpty()) {
            return PROPERTY_TO_META.getOrDefault(blockName, -1);
        }
        String key = blockName + "[" + properties + "]";
        return PROPERTY_TO_META.getOrDefault(key, -1);
    }

    private static void p(String block, String props, int meta) {
        PROPERTY_TO_META.put(block + "[" + props + "]", meta);
    }

    private static void registerColorMeta(String block) {
        p(block, "color=white", 0);
        p(block, "color=orange", 1);
        p(block, "color=magenta", 2);
        p(block, "color=light_blue", 3);
        p(block, "color=yellow", 4);
        p(block, "color=lime", 5);
        p(block, "color=pink", 6);
        p(block, "color=gray", 7);
        p(block, "color=silver", 8);
        p(block, "color=cyan", 9);
        p(block, "color=purple", 10);
        p(block, "color=blue", 11);
        p(block, "color=brown", 12);
        p(block, "color=green", 13);
        p(block, "color=red", 14);
        p(block, "color=black", 15);
    }

    /** Set of block suffixes that were split by color in 1.13+ (the Flattening) */
    private static final java.util.Set<String> COLOR_SPLIT_SUFFIXES = new java.util.HashSet<>(java.util.Arrays.asList(
        "wool", "carpet", "terracotta", "stained_glass", "stained_glass_pane",
        "concrete", "concrete_powder", "shulker_box", "glazed_terracotta", "bed",
        "banner", "wall_banner"
    ));

    /**
     * Returns the color metadata for blocks that were split into per-color blocks in 1.13+.
     * E.g. "minecraft:white_wool" -> 0, "minecraft:orange_wool" -> 1
     * Only matches blocks whose suffix is in COLOR_SPLIT_SUFFIXES to avoid false positives
     * like "minecraft:brown_mushroom_block" or "minecraft:red_sandstone".
     */
    public static int getColorMetaFromModernName(String modernName) {
        String name = modernName;
        if (name.startsWith("minecraft:")) {
            name = name.substring(10);
        }

        // Try each color prefix and verify the suffix is a known color-split block type
        String[][] colorPrefixes = {
            {"white_", "0"}, {"orange_", "1"}, {"magenta_", "2"}, {"light_blue_", "3"},
            {"yellow_", "4"}, {"lime_", "5"}, {"pink_", "6"}, {"gray_", "7"},
            {"light_gray_", "8"}, {"silver_", "8"}, {"cyan_", "9"}, {"purple_", "10"},
            {"blue_", "11"}, {"brown_", "12"}, {"green_", "13"}, {"red_", "14"},
            {"black_", "15"}
        };

        for (String[] entry : colorPrefixes) {
            if (name.startsWith(entry[0])) {
                String suffix = name.substring(entry[0].length());
                if (COLOR_SPLIT_SUFFIXES.contains(suffix)) {
                    return Integer.parseInt(entry[1]);
                }
            }
        }
        return -1;
    }

    static {
        // =================================================================
        // Block Renames: modern name -> 1.7.10 name
        // =================================================================
        BLOCK_RENAMES.put("minecraft:grass_block", "minecraft:grass");
        BLOCK_RENAMES.put("minecraft:oak_log", "minecraft:log");
        BLOCK_RENAMES.put("minecraft:spruce_log", "minecraft:log");
        BLOCK_RENAMES.put("minecraft:birch_log", "minecraft:log");
        BLOCK_RENAMES.put("minecraft:jungle_log", "minecraft:log");
        BLOCK_RENAMES.put("minecraft:acacia_log", "minecraft:log2");
        BLOCK_RENAMES.put("minecraft:dark_oak_log", "minecraft:log2");
        BLOCK_RENAMES.put("minecraft:oak_planks", "minecraft:planks");
        BLOCK_RENAMES.put("minecraft:spruce_planks", "minecraft:planks");
        BLOCK_RENAMES.put("minecraft:birch_planks", "minecraft:planks");
        BLOCK_RENAMES.put("minecraft:jungle_planks", "minecraft:planks");
        BLOCK_RENAMES.put("minecraft:acacia_planks", "minecraft:planks");
        BLOCK_RENAMES.put("minecraft:dark_oak_planks", "minecraft:planks");
        BLOCK_RENAMES.put("minecraft:oak_slab", "minecraft:wooden_slab");
        BLOCK_RENAMES.put("minecraft:spruce_slab", "minecraft:wooden_slab");
        BLOCK_RENAMES.put("minecraft:birch_slab", "minecraft:wooden_slab");
        BLOCK_RENAMES.put("minecraft:jungle_slab", "minecraft:wooden_slab");
        BLOCK_RENAMES.put("minecraft:acacia_slab", "minecraft:wooden_slab");
        BLOCK_RENAMES.put("minecraft:dark_oak_slab", "minecraft:wooden_slab");
        BLOCK_RENAMES.put("minecraft:stone_slab", "minecraft:stone_slab");
        BLOCK_RENAMES.put("minecraft:cobblestone_stairs", "minecraft:stone_stairs");
        BLOCK_RENAMES.put("minecraft:oak_fence", "minecraft:fence");
        BLOCK_RENAMES.put("minecraft:oak_fence_gate", "minecraft:fence_gate");
        BLOCK_RENAMES.put("minecraft:oak_door", "minecraft:wooden_door");
        BLOCK_RENAMES.put("minecraft:nether_bricks", "minecraft:nether_brick");
        BLOCK_RENAMES.put("minecraft:stone_bricks", "minecraft:stonebrick");
        BLOCK_RENAMES.put("minecraft:mossy_stone_bricks", "minecraft:stonebrick");
        BLOCK_RENAMES.put("minecraft:cracked_stone_bricks", "minecraft:stonebrick");
        BLOCK_RENAMES.put("minecraft:chiseled_stone_bricks", "minecraft:stonebrick");
        BLOCK_RENAMES.put("minecraft:infested_stone", "minecraft:monster_egg");
        BLOCK_RENAMES.put("minecraft:infested_cobblestone", "minecraft:monster_egg");
        BLOCK_RENAMES.put("minecraft:infested_stone_bricks", "minecraft:monster_egg");
        BLOCK_RENAMES.put("minecraft:infested_mossy_stone_bricks", "minecraft:monster_egg");
        BLOCK_RENAMES.put("minecraft:infested_cracked_stone_bricks", "minecraft:monster_egg");
        BLOCK_RENAMES.put("minecraft:infested_chiseled_stone_bricks", "minecraft:monster_egg");
        BLOCK_RENAMES.put("minecraft:oak_leaves", "minecraft:leaves");
        BLOCK_RENAMES.put("minecraft:spruce_leaves", "minecraft:leaves");
        BLOCK_RENAMES.put("minecraft:birch_leaves", "minecraft:leaves");
        BLOCK_RENAMES.put("minecraft:jungle_leaves", "minecraft:leaves");
        BLOCK_RENAMES.put("minecraft:acacia_leaves", "minecraft:leaves2");
        BLOCK_RENAMES.put("minecraft:dark_oak_leaves", "minecraft:leaves2");
        BLOCK_RENAMES.put("minecraft:oak_sapling", "minecraft:sapling");
        BLOCK_RENAMES.put("minecraft:spruce_sapling", "minecraft:sapling");
        BLOCK_RENAMES.put("minecraft:birch_sapling", "minecraft:sapling");
        BLOCK_RENAMES.put("minecraft:jungle_sapling", "minecraft:sapling");
        BLOCK_RENAMES.put("minecraft:acacia_sapling", "minecraft:sapling");
        BLOCK_RENAMES.put("minecraft:dark_oak_sapling", "minecraft:sapling");
        BLOCK_RENAMES.put("minecraft:rose_bush", "minecraft:double_plant");
        BLOCK_RENAMES.put("minecraft:peony", "minecraft:double_plant");
        BLOCK_RENAMES.put("minecraft:tall_grass", "minecraft:double_plant");
        BLOCK_RENAMES.put("minecraft:large_fern", "minecraft:double_plant");
        BLOCK_RENAMES.put("minecraft:sunflower", "minecraft:double_plant");
        BLOCK_RENAMES.put("minecraft:lilac", "minecraft:double_plant");
        BLOCK_RENAMES.put("minecraft:poppy", "minecraft:red_flower");
        BLOCK_RENAMES.put("minecraft:blue_orchid", "minecraft:red_flower");
        BLOCK_RENAMES.put("minecraft:allium", "minecraft:red_flower");
        BLOCK_RENAMES.put("minecraft:azure_bluet", "minecraft:red_flower");
        BLOCK_RENAMES.put("minecraft:red_tulip", "minecraft:red_flower");
        BLOCK_RENAMES.put("minecraft:orange_tulip", "minecraft:red_flower");
        BLOCK_RENAMES.put("minecraft:white_tulip", "minecraft:red_flower");
        BLOCK_RENAMES.put("minecraft:pink_tulip", "minecraft:red_flower");
        BLOCK_RENAMES.put("minecraft:oxeye_daisy", "minecraft:red_flower");
        BLOCK_RENAMES.put("minecraft:dandelion", "minecraft:yellow_flower");
        BLOCK_RENAMES.put("minecraft:mossy_cobblestone_wall", "minecraft:cobblestone_wall");
        BLOCK_RENAMES.put("minecraft:terracotta", "minecraft:hardened_clay");
        BLOCK_RENAMES.put("minecraft:white_terracotta", "minecraft:stained_hardened_clay");
        BLOCK_RENAMES.put("minecraft:orange_terracotta", "minecraft:stained_hardened_clay");
        BLOCK_RENAMES.put("minecraft:magenta_terracotta", "minecraft:stained_hardened_clay");
        BLOCK_RENAMES.put("minecraft:light_blue_terracotta", "minecraft:stained_hardened_clay");
        BLOCK_RENAMES.put("minecraft:yellow_terracotta", "minecraft:stained_hardened_clay");
        BLOCK_RENAMES.put("minecraft:lime_terracotta", "minecraft:stained_hardened_clay");
        BLOCK_RENAMES.put("minecraft:pink_terracotta", "minecraft:stained_hardened_clay");
        BLOCK_RENAMES.put("minecraft:gray_terracotta", "minecraft:stained_hardened_clay");
        BLOCK_RENAMES.put("minecraft:light_gray_terracotta", "minecraft:stained_hardened_clay");
        BLOCK_RENAMES.put("minecraft:cyan_terracotta", "minecraft:stained_hardened_clay");
        BLOCK_RENAMES.put("minecraft:purple_terracotta", "minecraft:stained_hardened_clay");
        BLOCK_RENAMES.put("minecraft:blue_terracotta", "minecraft:stained_hardened_clay");
        BLOCK_RENAMES.put("minecraft:brown_terracotta", "minecraft:stained_hardened_clay");
        BLOCK_RENAMES.put("minecraft:green_terracotta", "minecraft:stained_hardened_clay");
        BLOCK_RENAMES.put("minecraft:red_terracotta", "minecraft:stained_hardened_clay");
        BLOCK_RENAMES.put("minecraft:black_terracotta", "minecraft:stained_hardened_clay");
        BLOCK_RENAMES.put("minecraft:white_stained_glass", "minecraft:stained_glass");
        BLOCK_RENAMES.put("minecraft:orange_stained_glass", "minecraft:stained_glass");
        BLOCK_RENAMES.put("minecraft:magenta_stained_glass", "minecraft:stained_glass");
        BLOCK_RENAMES.put("minecraft:light_blue_stained_glass", "minecraft:stained_glass");
        BLOCK_RENAMES.put("minecraft:yellow_stained_glass", "minecraft:stained_glass");
        BLOCK_RENAMES.put("minecraft:lime_stained_glass", "minecraft:stained_glass");
        BLOCK_RENAMES.put("minecraft:pink_stained_glass", "minecraft:stained_glass");
        BLOCK_RENAMES.put("minecraft:gray_stained_glass", "minecraft:stained_glass");
        BLOCK_RENAMES.put("minecraft:light_gray_stained_glass", "minecraft:stained_glass");
        BLOCK_RENAMES.put("minecraft:cyan_stained_glass", "minecraft:stained_glass");
        BLOCK_RENAMES.put("minecraft:purple_stained_glass", "minecraft:stained_glass");
        BLOCK_RENAMES.put("minecraft:blue_stained_glass", "minecraft:stained_glass");
        BLOCK_RENAMES.put("minecraft:brown_stained_glass", "minecraft:stained_glass");
        BLOCK_RENAMES.put("minecraft:green_stained_glass", "minecraft:stained_glass");
        BLOCK_RENAMES.put("minecraft:red_stained_glass", "minecraft:stained_glass");
        BLOCK_RENAMES.put("minecraft:black_stained_glass", "minecraft:stained_glass");
        BLOCK_RENAMES.put("minecraft:white_stained_glass_pane", "minecraft:stained_glass_pane");
        BLOCK_RENAMES.put("minecraft:orange_stained_glass_pane", "minecraft:stained_glass_pane");
        BLOCK_RENAMES.put("minecraft:magenta_stained_glass_pane", "minecraft:stained_glass_pane");
        BLOCK_RENAMES.put("minecraft:light_blue_stained_glass_pane", "minecraft:stained_glass_pane");
        BLOCK_RENAMES.put("minecraft:yellow_stained_glass_pane", "minecraft:stained_glass_pane");
        BLOCK_RENAMES.put("minecraft:lime_stained_glass_pane", "minecraft:stained_glass_pane");
        BLOCK_RENAMES.put("minecraft:pink_stained_glass_pane", "minecraft:stained_glass_pane");
        BLOCK_RENAMES.put("minecraft:gray_stained_glass_pane", "minecraft:stained_glass_pane");
        BLOCK_RENAMES.put("minecraft:light_gray_stained_glass_pane", "minecraft:stained_glass_pane");
        BLOCK_RENAMES.put("minecraft:cyan_stained_glass_pane", "minecraft:stained_glass_pane");
        BLOCK_RENAMES.put("minecraft:purple_stained_glass_pane", "minecraft:stained_glass_pane");
        BLOCK_RENAMES.put("minecraft:blue_stained_glass_pane", "minecraft:stained_glass_pane");
        BLOCK_RENAMES.put("minecraft:brown_stained_glass_pane", "minecraft:stained_glass_pane");
        BLOCK_RENAMES.put("minecraft:green_stained_glass_pane", "minecraft:stained_glass_pane");
        BLOCK_RENAMES.put("minecraft:red_stained_glass_pane", "minecraft:stained_glass_pane");
        BLOCK_RENAMES.put("minecraft:black_stained_glass_pane", "minecraft:stained_glass_pane");
        BLOCK_RENAMES.put("minecraft:white_wool", "minecraft:wool");
        BLOCK_RENAMES.put("minecraft:orange_wool", "minecraft:wool");
        BLOCK_RENAMES.put("minecraft:magenta_wool", "minecraft:wool");
        BLOCK_RENAMES.put("minecraft:light_blue_wool", "minecraft:wool");
        BLOCK_RENAMES.put("minecraft:yellow_wool", "minecraft:wool");
        BLOCK_RENAMES.put("minecraft:lime_wool", "minecraft:wool");
        BLOCK_RENAMES.put("minecraft:pink_wool", "minecraft:wool");
        BLOCK_RENAMES.put("minecraft:gray_wool", "minecraft:wool");
        BLOCK_RENAMES.put("minecraft:light_gray_wool", "minecraft:wool");
        BLOCK_RENAMES.put("minecraft:cyan_wool", "minecraft:wool");
        BLOCK_RENAMES.put("minecraft:purple_wool", "minecraft:wool");
        BLOCK_RENAMES.put("minecraft:blue_wool", "minecraft:wool");
        BLOCK_RENAMES.put("minecraft:brown_wool", "minecraft:wool");
        BLOCK_RENAMES.put("minecraft:green_wool", "minecraft:wool");
        BLOCK_RENAMES.put("minecraft:red_wool", "minecraft:wool");
        BLOCK_RENAMES.put("minecraft:black_wool", "minecraft:wool");
        BLOCK_RENAMES.put("minecraft:white_carpet", "minecraft:carpet");
        BLOCK_RENAMES.put("minecraft:orange_carpet", "minecraft:carpet");
        BLOCK_RENAMES.put("minecraft:magenta_carpet", "minecraft:carpet");
        BLOCK_RENAMES.put("minecraft:light_blue_carpet", "minecraft:carpet");
        BLOCK_RENAMES.put("minecraft:yellow_carpet", "minecraft:carpet");
        BLOCK_RENAMES.put("minecraft:lime_carpet", "minecraft:carpet");
        BLOCK_RENAMES.put("minecraft:pink_carpet", "minecraft:carpet");
        BLOCK_RENAMES.put("minecraft:gray_carpet", "minecraft:carpet");
        BLOCK_RENAMES.put("minecraft:light_gray_carpet", "minecraft:carpet");
        BLOCK_RENAMES.put("minecraft:cyan_carpet", "minecraft:carpet");
        BLOCK_RENAMES.put("minecraft:purple_carpet", "minecraft:carpet");
        BLOCK_RENAMES.put("minecraft:blue_carpet", "minecraft:carpet");
        BLOCK_RENAMES.put("minecraft:brown_carpet", "minecraft:carpet");
        BLOCK_RENAMES.put("minecraft:green_carpet", "minecraft:carpet");
        BLOCK_RENAMES.put("minecraft:red_carpet", "minecraft:carpet");
        BLOCK_RENAMES.put("minecraft:black_carpet", "minecraft:carpet");
        BLOCK_RENAMES.put("minecraft:bricks", "minecraft:brick_block");
        BLOCK_RENAMES.put("minecraft:jack_o_lantern", "minecraft:lit_pumpkin");
        BLOCK_RENAMES.put("minecraft:wet_sponge", "minecraft:sponge");
        BLOCK_RENAMES.put("minecraft:granite", "minecraft:stone");
        BLOCK_RENAMES.put("minecraft:polished_granite", "minecraft:stone");
        BLOCK_RENAMES.put("minecraft:diorite", "minecraft:stone");
        BLOCK_RENAMES.put("minecraft:polished_diorite", "minecraft:stone");
        BLOCK_RENAMES.put("minecraft:andesite", "minecraft:stone");
        BLOCK_RENAMES.put("minecraft:polished_andesite", "minecraft:stone");
        BLOCK_RENAMES.put("minecraft:coarse_dirt", "minecraft:dirt");
        BLOCK_RENAMES.put("minecraft:podzol", "minecraft:dirt");
        BLOCK_RENAMES.put("minecraft:red_sand", "minecraft:sand");
        BLOCK_RENAMES.put("minecraft:spawner", "minecraft:mob_spawner");
        BLOCK_RENAMES.put("minecraft:nether_portal", "minecraft:portal");
        BLOCK_RENAMES.put("minecraft:cobweb", "minecraft:web");
        BLOCK_RENAMES.put("minecraft:sugar_cane", "minecraft:reeds");
        BLOCK_RENAMES.put("minecraft:lily_pad", "minecraft:waterlily");
        BLOCK_RENAMES.put("minecraft:melon", "minecraft:melon_block");
        BLOCK_RENAMES.put("minecraft:comparator", "minecraft:unpowered_comparator");
        BLOCK_RENAMES.put("minecraft:repeater", "minecraft:unpowered_repeater");
        BLOCK_RENAMES.put("minecraft:chiseled_quartz_block", "minecraft:quartz_block");
        BLOCK_RENAMES.put("minecraft:quartz_pillar", "minecraft:quartz_block");
        BLOCK_RENAMES.put("minecraft:chiseled_sandstone", "minecraft:sandstone");
        BLOCK_RENAMES.put("minecraft:cut_sandstone", "minecraft:sandstone");
        BLOCK_RENAMES.put("minecraft:chiseled_red_sandstone", "minecraft:red_sandstone");
        BLOCK_RENAMES.put("minecraft:cut_red_sandstone", "minecraft:red_sandstone");
        BLOCK_RENAMES.put("minecraft:skeleton_skull", "minecraft:skull");
        BLOCK_RENAMES.put("minecraft:wither_skeleton_skull", "minecraft:skull");
        BLOCK_RENAMES.put("minecraft:zombie_head", "minecraft:skull");
        BLOCK_RENAMES.put("minecraft:player_head", "minecraft:skull");
        BLOCK_RENAMES.put("minecraft:creeper_head", "minecraft:skull");
        BLOCK_RENAMES.put("minecraft:dragon_head", "minecraft:skull");
        BLOCK_RENAMES.put("minecraft:redstone_lamp", "minecraft:redstone_lamp");
        BLOCK_RENAMES.put("minecraft:sign", "minecraft:standing_sign");

        // =================================================================
        // Property-to-Metadata Mappings
        // =================================================================

        // Stone variants
        p("minecraft:stone", "variant=stone", 0);
        p("minecraft:stone", "variant=granite", 1);
        p("minecraft:stone", "variant=smooth_granite", 2);
        p("minecraft:stone", "variant=diorite", 3);
        p("minecraft:stone", "variant=smooth_diorite", 4);
        p("minecraft:stone", "variant=andesite", 5);
        p("minecraft:stone", "variant=smooth_andesite", 6);

        // Dirt
        p("minecraft:dirt", "variant=dirt", 0);
        p("minecraft:dirt", "variant=coarse_dirt", 1);
        p("minecraft:dirt", "variant=podzol", 2);

        // Sand
        p("minecraft:sand", "variant=sand", 0);
        p("minecraft:sand", "variant=red_sand", 1);

        // Planks
        p("minecraft:planks", "variant=oak", 0);
        p("minecraft:planks", "variant=spruce", 1);
        p("minecraft:planks", "variant=birch", 2);
        p("minecraft:planks", "variant=jungle", 3);
        p("minecraft:planks", "variant=acacia", 4);
        p("minecraft:planks", "variant=dark_oak", 5);

        // Saplings
        p("minecraft:sapling", "stage=0,type=oak", 0);
        p("minecraft:sapling", "stage=0,type=spruce", 1);
        p("minecraft:sapling", "stage=0,type=birch", 2);
        p("minecraft:sapling", "stage=0,type=jungle", 3);
        p("minecraft:sapling", "stage=0,type=acacia", 4);
        p("minecraft:sapling", "stage=0,type=dark_oak", 5);
        p("minecraft:sapling", "stage=1,type=oak", 8);
        p("minecraft:sapling", "stage=1,type=spruce", 9);
        p("minecraft:sapling", "stage=1,type=birch", 10);
        p("minecraft:sapling", "stage=1,type=jungle", 11);
        p("minecraft:sapling", "stage=1,type=acacia", 12);
        p("minecraft:sapling", "stage=1,type=dark_oak", 13);

        // Logs
        p("minecraft:log", "axis=y,variant=oak", 0);
        p("minecraft:log", "axis=y,variant=spruce", 1);
        p("minecraft:log", "axis=y,variant=birch", 2);
        p("minecraft:log", "axis=y,variant=jungle", 3);
        p("minecraft:log", "axis=x,variant=oak", 4);
        p("minecraft:log", "axis=x,variant=spruce", 5);
        p("minecraft:log", "axis=x,variant=birch", 6);
        p("minecraft:log", "axis=x,variant=jungle", 7);
        p("minecraft:log", "axis=z,variant=oak", 8);
        p("minecraft:log", "axis=z,variant=spruce", 9);
        p("minecraft:log", "axis=z,variant=birch", 10);
        p("minecraft:log", "axis=z,variant=jungle", 11);
        p("minecraft:log", "axis=none,variant=oak", 12);
        p("minecraft:log", "axis=none,variant=spruce", 13);
        p("minecraft:log", "axis=none,variant=birch", 14);
        p("minecraft:log", "axis=none,variant=jungle", 15);
        p("minecraft:log2", "axis=y,variant=acacia", 0);
        p("minecraft:log2", "axis=y,variant=dark_oak", 1);
        p("minecraft:log2", "axis=x,variant=acacia", 4);
        p("minecraft:log2", "axis=x,variant=dark_oak", 5);
        p("minecraft:log2", "axis=z,variant=acacia", 8);
        p("minecraft:log2", "axis=z,variant=dark_oak", 9);
        p("minecraft:log2", "axis=none,variant=acacia", 12);
        p("minecraft:log2", "axis=none,variant=dark_oak", 13);

        // Leaves
        p("minecraft:leaves", "variant=oak", 0);
        p("minecraft:leaves", "variant=spruce", 1);
        p("minecraft:leaves", "variant=birch", 2);
        p("minecraft:leaves", "variant=jungle", 3);
        p("minecraft:leaves2", "variant=acacia", 0);
        p("minecraft:leaves2", "variant=dark_oak", 1);

        // Sandstone
        p("minecraft:sandstone", "type=sandstone", 0);
        p("minecraft:sandstone", "type=chiseled_sandstone", 1);
        p("minecraft:sandstone", "type=smooth_sandstone", 2);
        p("minecraft:red_sandstone", "type=red_sandstone", 0);
        p("minecraft:red_sandstone", "type=chiseled_red_sandstone", 1);
        p("minecraft:red_sandstone", "type=smooth_red_sandstone", 2);

        // Color-based blocks
        registerColorMeta("minecraft:wool");
        registerColorMeta("minecraft:stained_hardened_clay");
        registerColorMeta("minecraft:stained_glass");
        registerColorMeta("minecraft:stained_glass_pane");
        registerColorMeta("minecraft:carpet");

        // Sponge
        p("minecraft:sponge", "wet=false", 0);
        p("minecraft:sponge", "wet=true", 1);

        // Wooden slabs
        p("minecraft:wooden_slab", "half=bottom,variant=oak", 0);
        p("minecraft:wooden_slab", "half=bottom,variant=spruce", 1);
        p("minecraft:wooden_slab", "half=bottom,variant=birch", 2);
        p("minecraft:wooden_slab", "half=bottom,variant=jungle", 3);
        p("minecraft:wooden_slab", "half=bottom,variant=acacia", 4);
        p("minecraft:wooden_slab", "half=bottom,variant=dark_oak", 5);
        p("minecraft:wooden_slab", "half=top,variant=oak", 8);
        p("minecraft:wooden_slab", "half=top,variant=spruce", 9);
        p("minecraft:wooden_slab", "half=top,variant=birch", 10);
        p("minecraft:wooden_slab", "half=top,variant=jungle", 11);
        p("minecraft:wooden_slab", "half=top,variant=acacia", 12);
        p("minecraft:wooden_slab", "half=top,variant=dark_oak", 13);

        // Stone slabs
        p("minecraft:stone_slab", "half=bottom,variant=stone", 0);
        p("minecraft:stone_slab", "half=bottom,variant=sandstone", 1);
        p("minecraft:stone_slab", "half=bottom,variant=cobblestone", 3);
        p("minecraft:stone_slab", "half=bottom,variant=brick", 4);
        p("minecraft:stone_slab", "half=bottom,variant=stone_brick", 5);
        p("minecraft:stone_slab", "half=bottom,variant=nether_brick", 6);
        p("minecraft:stone_slab", "half=bottom,variant=quartz", 7);
        p("minecraft:stone_slab", "half=top,variant=stone", 8);
        p("minecraft:stone_slab", "half=top,variant=sandstone", 9);
        p("minecraft:stone_slab", "half=top,variant=cobblestone", 11);
        p("minecraft:stone_slab", "half=top,variant=brick", 12);
        p("minecraft:stone_slab", "half=top,variant=stone_brick", 13);
        p("minecraft:stone_slab", "half=top,variant=nether_brick", 14);
        p("minecraft:stone_slab", "half=top,variant=quartz", 15);

        // Stonebrick
        p("minecraft:stonebrick", "variant=stonebrick", 0);
        p("minecraft:stonebrick", "variant=mossy_stonebrick", 1);
        p("minecraft:stonebrick", "variant=cracked_stonebrick", 2);
        p("minecraft:stonebrick", "variant=chiseled_stonebrick", 3);

        // Monster egg (silverfish)
        p("minecraft:monster_egg", "variant=stone", 0);
        p("minecraft:monster_egg", "variant=cobblestone", 1);
        p("minecraft:monster_egg", "variant=stone_brick", 2);
        p("minecraft:monster_egg", "variant=mossy_brick", 3);
        p("minecraft:monster_egg", "variant=cracked_brick", 4);
        p("minecraft:monster_egg", "variant=chiseled_brick", 5);

        // Quartz block
        p("minecraft:quartz_block", "variant=default", 0);


        p("minecraft:quartz_block", "variant=chiseled", 1);
        p("minecraft:quartz_block", "variant=lines_y", 2);
        p("minecraft:quartz_block", "variant=lines_x", 3);
        p("minecraft:quartz_block", "variant=lines_z", 4);

        // Prismarine
        p("minecraft:prismarine", "variant=prismarine", 0);
        p("minecraft:prismarine", "variant=prismarine_bricks", 1);
        p("minecraft:prismarine", "variant=dark_prismarine", 2);

        // Cobblestone wall
        p("minecraft:cobblestone_wall", "variant=cobblestone", 0);
        p("minecraft:cobblestone_wall", "variant=mossy_cobblestone", 1);

        // Flowers
        p("minecraft:red_flower", "type=poppy", 0);
        p("minecraft:red_flower", "type=blue_orchid", 1);
        p("minecraft:red_flower", "type=allium", 2);
        p("minecraft:red_flower", "type=houstonia", 3);
        p("minecraft:red_flower", "type=red_tulip", 4);
        p("minecraft:red_flower", "type=orange_tulip", 5);
        p("minecraft:red_flower", "type=white_tulip", 6);
        p("minecraft:red_flower", "type=pink_tulip", 7);
        p("minecraft:red_flower", "type=oxeye_daisy", 8);

        // Double plants
        p("minecraft:double_plant", "half=lower,variant=sunflower", 0);
        p("minecraft:double_plant", "half=lower,variant=syringa", 1);
        p("minecraft:double_plant", "half=lower,variant=double_grass", 2);
        p("minecraft:double_plant", "half=lower,variant=double_fern", 3);
        p("minecraft:double_plant", "half=lower,variant=double_rose", 4);
        p("minecraft:double_plant", "half=lower,variant=paeonia", 5);
        p("minecraft:double_plant", "half=upper", 8);

        // Tallgrass
        p("minecraft:tallgrass", "type=dead_bush", 0);
        p("minecraft:tallgrass", "type=tall_grass", 1);
        p("minecraft:tallgrass", "type=fern", 2);

        // Anvil
        p("minecraft:anvil", "damage=0,facing=south", 0);
        p("minecraft:anvil", "damage=0,facing=west", 1);
        p("minecraft:anvil", "damage=0,facing=north", 2);
        p("minecraft:anvil", "damage=0,facing=east", 3);
        p("minecraft:anvil", "damage=1,facing=south", 4);
        p("minecraft:anvil", "damage=1,facing=west", 5);
        p("minecraft:anvil", "damage=1,facing=north", 6);
        p("minecraft:anvil", "damage=1,facing=east", 7);
        p("minecraft:anvil", "damage=2,facing=south", 8);
        p("minecraft:anvil", "damage=2,facing=west", 9);
        p("minecraft:anvil", "damage=2,facing=north", 10);
        p("minecraft:anvil", "damage=2,facing=east", 11);

        // Torch / Redstone torch (wall placement)
        p("minecraft:torch", "facing=up", 5);
        p("minecraft:torch", "facing=east", 1);
        p("minecraft:torch", "facing=west", 2);
        p("minecraft:torch", "facing=south", 3);
        p("minecraft:torch", "facing=north", 4);
        p("minecraft:redstone_torch", "facing=up", 5);
        p("minecraft:redstone_torch", "facing=east", 1);
        p("minecraft:redstone_torch", "facing=west", 2);
        p("minecraft:redstone_torch", "facing=south", 3);
        p("minecraft:redstone_torch", "facing=north", 4);

        // Stairs (all stair types share the same meta encoding)
        String[] stairBlocks = {
            "minecraft:oak_stairs", "minecraft:stone_stairs", "minecraft:brick_stairs",
            "minecraft:stone_brick_stairs", "minecraft:nether_brick_stairs", "minecraft:sandstone_stairs",
            "minecraft:spruce_stairs", "minecraft:birch_stairs", "minecraft:jungle_stairs",
            "minecraft:quartz_stairs", "minecraft:acacia_stairs", "minecraft:dark_oak_stairs",
            "minecraft:red_sandstone_stairs"
        };
        for (String stair : stairBlocks) {
            p(stair, "facing=east,half=bottom,shape=straight", 0);
            p(stair, "facing=west,half=bottom,shape=straight", 1);
            p(stair, "facing=south,half=bottom,shape=straight", 2);
            p(stair, "facing=north,half=bottom,shape=straight", 3);
            p(stair, "facing=east,half=top,shape=straight", 4);
            p(stair, "facing=west,half=top,shape=straight", 5);
            p(stair, "facing=south,half=top,shape=straight", 6);
            p(stair, "facing=north,half=top,shape=straight", 7);
        }

        // Facing blocks (furnace, dispenser, dropper, etc.)
        String[] facingBlocks = {
            "minecraft:furnace", "minecraft:lit_furnace", "minecraft:dispenser",
            "minecraft:dropper", "minecraft:chest", "minecraft:trapped_chest",
            "minecraft:ender_chest", "minecraft:ladder", "minecraft:wall_sign"
        };
        for (String fb : facingBlocks) {
            p(fb, "facing=north", 2);
            p(fb, "facing=south", 3);
            p(fb, "facing=west", 4);
            p(fb, "facing=east", 5);
        }

        // Piston / Sticky piston
        p("minecraft:piston", "extended=false,facing=down", 0);
        p("minecraft:piston", "extended=false,facing=up", 1);
        p("minecraft:piston", "extended=false,facing=north", 2);
        p("minecraft:piston", "extended=false,facing=south", 3);
        p("minecraft:piston", "extended=false,facing=west", 4);
        p("minecraft:piston", "extended=false,facing=east", 5);
        p("minecraft:piston", "extended=true,facing=down", 8);
        p("minecraft:piston", "extended=true,facing=up", 9);
        p("minecraft:piston", "extended=true,facing=north", 10);
        p("minecraft:piston", "extended=true,facing=south", 11);
        p("minecraft:piston", "extended=true,facing=west", 12);
        p("minecraft:piston", "extended=true,facing=east", 13);
        p("minecraft:sticky_piston", "extended=false,facing=down", 0);
        p("minecraft:sticky_piston", "extended=false,facing=up", 1);
        p("minecraft:sticky_piston", "extended=false,facing=north", 2);
        p("minecraft:sticky_piston", "extended=false,facing=south", 3);
        p("minecraft:sticky_piston", "extended=false,facing=west", 4);
        p("minecraft:sticky_piston", "extended=false,facing=east", 5);
        p("minecraft:sticky_piston", "extended=true,facing=down", 8);
        p("minecraft:sticky_piston", "extended=true,facing=up", 9);
        p("minecraft:sticky_piston", "extended=true,facing=north", 10);
        p("minecraft:sticky_piston", "extended=true,facing=south", 11);
        p("minecraft:sticky_piston", "extended=true,facing=west", 12);
        p("minecraft:sticky_piston", "extended=true,facing=east", 13);

        // Rails
        p("minecraft:rail", "shape=north_south", 0);
        p("minecraft:rail", "shape=east_west", 1);
        p("minecraft:rail", "shape=ascending_east", 2);
        p("minecraft:rail", "shape=ascending_west", 3);
        p("minecraft:rail", "shape=ascending_north", 4);
        p("minecraft:rail", "shape=ascending_south", 5);
        p("minecraft:rail", "shape=south_east", 6);
        p("minecraft:rail", "shape=south_west", 7);
        p("minecraft:rail", "shape=north_west", 8);
        p("minecraft:rail", "shape=north_east", 9);

        // Powered/Detector/Activator rails
        String[] poweredRails = {"minecraft:golden_rail", "minecraft:detector_rail", "minecraft:activator_rail"};
        for (String pr : poweredRails) {
            p(pr, "powered=false,shape=north_south", 0);
            p(pr, "powered=false,shape=east_west", 1);
            p(pr, "powered=false,shape=ascending_east", 2);
            p(pr, "powered=false,shape=ascending_west", 3);
            p(pr, "powered=false,shape=ascending_north", 4);
            p(pr, "powered=false,shape=ascending_south", 5);
            p(pr, "powered=true,shape=north_south", 8);
            p(pr, "powered=true,shape=east_west", 9);
            p(pr, "powered=true,shape=ascending_east", 10);
            p(pr, "powered=true,shape=ascending_west", 11);
            p(pr, "powered=true,shape=ascending_north", 12);
            p(pr, "powered=true,shape=ascending_south", 13);
        }

        // Lever
        p("minecraft:lever", "facing=down_x,powered=false", 0);
        p("minecraft:lever", "facing=east,powered=false", 1);
        p("minecraft:lever", "facing=west,powered=false", 2);
        p("minecraft:lever", "facing=south,powered=false", 3);
        p("minecraft:lever", "facing=north,powered=false", 4);
        p("minecraft:lever", "facing=up_z,powered=false", 5);
        p("minecraft:lever", "facing=up_x,powered=false", 6);
        p("minecraft:lever", "facing=down_z,powered=false", 7);
        p("minecraft:lever", "facing=down_x,powered=true", 8);
        p("minecraft:lever", "facing=east,powered=true", 9);
        p("minecraft:lever", "facing=west,powered=true", 10);
        p("minecraft:lever", "facing=south,powered=true", 11);
        p("minecraft:lever", "facing=north,powered=true", 12);
        p("minecraft:lever", "facing=up_z,powered=true", 13);
        p("minecraft:lever", "facing=up_x,powered=true", 14);
        p("minecraft:lever", "facing=down_z,powered=true", 15);

        // Buttons
        String[] buttons = {"minecraft:stone_button", "minecraft:wooden_button"};
        for (String btn : buttons) {
            p(btn, "facing=down,powered=false", 0);
            p(btn, "facing=east,powered=false", 1);
            p(btn, "facing=west,powered=false", 2);
            p(btn, "facing=south,powered=false", 3);
            p(btn, "facing=north,powered=false", 4);
            p(btn, "facing=up,powered=false", 5);
            p(btn, "facing=down,powered=true", 8);
            p(btn, "facing=east,powered=true", 9);
            p(btn, "facing=west,powered=true", 10);
            p(btn, "facing=south,powered=true", 11);
            p(btn, "facing=north,powered=true", 12);
            p(btn, "facing=up,powered=true", 13);
        }

        // Pressure plates
        p("minecraft:stone_pressure_plate", "powered=false", 0);
        p("minecraft:stone_pressure_plate", "powered=true", 1);
        p("minecraft:wooden_pressure_plate", "powered=false", 0);
        p("minecraft:wooden_pressure_plate", "powered=true", 1);

        // Doors (lower half stores hinge side, upper stores facing)
        String[] doors = {"minecraft:wooden_door", "minecraft:iron_door",
            "minecraft:spruce_door", "minecraft:birch_door", "minecraft:jungle_door",
            "minecraft:acacia_door", "minecraft:dark_oak_door"};
        for (String door : doors) {
            p(door, "facing=east,half=lower,hinge=right,open=false,powered=false", 0);
            p(door, "facing=south,half=lower,hinge=right,open=false,powered=false", 1);
            p(door, "facing=west,half=lower,hinge=right,open=false,powered=false", 2);
            p(door, "facing=north,half=lower,hinge=right,open=false,powered=false", 3);
            p(door, "facing=east,half=lower,hinge=right,open=true,powered=false", 4);
            p(door, "facing=south,half=lower,hinge=right,open=true,powered=false", 5);
            p(door, "facing=west,half=lower,hinge=right,open=true,powered=false", 6);
            p(door, "facing=north,half=lower,hinge=right,open=true,powered=false", 7);
            p(door, "half=upper,hinge=left", 8);
            p(door, "half=upper,hinge=right", 9);
        }

        // Trapdoors
        String[] trapdoors = {"minecraft:trapdoor", "minecraft:iron_trapdoor"};
        for (String td : trapdoors) {
            p(td, "facing=north,half=bottom,open=false", 0);
            p(td, "facing=south,half=bottom,open=false", 1);
            p(td, "facing=west,half=bottom,open=false", 2);
            p(td, "facing=east,half=bottom,open=false", 3);
            p(td, "facing=north,half=bottom,open=true", 4);
            p(td, "facing=south,half=bottom,open=true", 5);
            p(td, "facing=west,half=bottom,open=true", 6);
            p(td, "facing=east,half=bottom,open=true", 7);
            p(td, "facing=north,half=top,open=false", 8);
            p(td, "facing=south,half=top,open=false", 9);
            p(td, "facing=west,half=top,open=false", 10);
            p(td, "facing=east,half=top,open=false", 11);
            p(td, "facing=north,half=top,open=true", 12);
            p(td, "facing=south,half=top,open=true", 13);
            p(td, "facing=west,half=top,open=true", 14);
            p(td, "facing=east,half=top,open=true", 15);
        }

        // Fence gates
        String[] fenceGates = {"minecraft:fence_gate", "minecraft:spruce_fence_gate",
            "minecraft:birch_fence_gate", "minecraft:jungle_fence_gate",
            "minecraft:dark_oak_fence_gate", "minecraft:acacia_fence_gate"};
        for (String fg : fenceGates) {
            p(fg, "facing=south,in_wall=false,open=false,powered=false", 0);
            p(fg, "facing=west,in_wall=false,open=false,powered=false", 1);
            p(fg, "facing=north,in_wall=false,open=false,powered=false", 2);
            p(fg, "facing=east,in_wall=false,open=false,powered=false", 3);
            p(fg, "facing=south,in_wall=false,open=true,powered=false", 4);
            p(fg, "facing=west,in_wall=false,open=true,powered=false", 5);
            p(fg, "facing=north,in_wall=false,open=true,powered=false", 6);
            p(fg, "facing=east,in_wall=false,open=true,powered=false", 7);
        }

        // Bed
        p("minecraft:bed", "facing=south,occupied=false,part=foot", 0);
        p("minecraft:bed", "facing=west,occupied=false,part=foot", 1);
        p("minecraft:bed", "facing=north,occupied=false,part=foot", 2);
        p("minecraft:bed", "facing=east,occupied=false,part=foot", 3);
        p("minecraft:bed", "facing=south,occupied=false,part=head", 8);
        p("minecraft:bed", "facing=west,occupied=false,part=head", 9);
        p("minecraft:bed", "facing=north,occupied=false,part=head", 10);
        p("minecraft:bed", "facing=east,occupied=false,part=head", 11);

        // Redstone wire (just power level)
        for (int i = 0; i <= 15; i++) {
            p("minecraft:redstone_wire", "power=" + i, i);
        }

        // Crops (wheat, carrots, potatoes)
        String[] crops = {"minecraft:wheat", "minecraft:carrots", "minecraft:potatoes"};
        for (String crop : crops) {
            for (int i = 0; i <= 7; i++) {
                p(crop, "age=" + i, i);
            }
        }

        // Nether wart
        for (int i = 0; i <= 3; i++) {
            p("minecraft:nether_wart", "age=" + i, i);
        }

        // Cocoa
        p("minecraft:cocoa", "age=0,facing=south", 0);
        p("minecraft:cocoa", "age=0,facing=west", 1);
        p("minecraft:cocoa", "age=0,facing=north", 2);
        p("minecraft:cocoa", "age=0,facing=east", 3);
        p("minecraft:cocoa", "age=1,facing=south", 4);
        p("minecraft:cocoa", "age=1,facing=west", 5);
        p("minecraft:cocoa", "age=1,facing=north", 6);
        p("minecraft:cocoa", "age=1,facing=east", 7);
        p("minecraft:cocoa", "age=2,facing=south", 8);
        p("minecraft:cocoa", "age=2,facing=west", 9);
        p("minecraft:cocoa", "age=2,facing=north", 10);
        p("minecraft:cocoa", "age=2,facing=east", 11);

        // Cactus, sugar cane, snow layer (age/layers)
        for (int i = 0; i <= 15; i++) {
            p("minecraft:cactus", "age=" + i, i);
            p("minecraft:reeds", "age=" + i, i);
        }
        for (int i = 1; i <= 8; i++) {
            p("minecraft:snow_layer", "layers=" + i, i - 1);
        }

        // Cauldron
        for (int i = 0; i <= 3; i++) {
            p("minecraft:cauldron", "level=" + i, i);
        }

        // Cake
        for (int i = 0; i <= 6; i++) {
            p("minecraft:cake", "bites=" + i, i);
        }

        // Repeater
        for (int delay = 1; delay <= 4; delay++) {
            p("minecraft:unpowered_repeater", "delay=" + delay + ",facing=south,locked=false", (delay - 1) * 4);
            p("minecraft:unpowered_repeater", "delay=" + delay + ",facing=west,locked=false", (delay - 1) * 4 + 1);
            p("minecraft:unpowered_repeater", "delay=" + delay + ",facing=north,locked=false", (delay - 1) * 4 + 2);
            p("minecraft:unpowered_repeater", "delay=" + delay + ",facing=east,locked=false", (delay - 1) * 4 + 3);
        }

        // Comparator
        p("minecraft:unpowered_comparator", "facing=south,mode=compare,powered=false", 0);
        p("minecraft:unpowered_comparator", "facing=west,mode=compare,powered=false", 1);
        p("minecraft:unpowered_comparator", "facing=north,mode=compare,powered=false", 2);
        p("minecraft:unpowered_comparator", "facing=east,mode=compare,powered=false", 3);
        p("minecraft:unpowered_comparator", "facing=south,mode=subtract,powered=false", 4);
        p("minecraft:unpowered_comparator", "facing=west,mode=subtract,powered=false", 5);
        p("minecraft:unpowered_comparator", "facing=north,mode=subtract,powered=false", 6);
        p("minecraft:unpowered_comparator", "facing=east,mode=subtract,powered=false", 7);

        // Hopper
        p("minecraft:hopper", "enabled=true,facing=down", 0);
        p("minecraft:hopper", "enabled=true,facing=north", 2);
        p("minecraft:hopper", "enabled=true,facing=south", 3);
        p("minecraft:hopper", "enabled=true,facing=west", 4);
        p("minecraft:hopper", "enabled=true,facing=east", 5);
        p("minecraft:hopper", "enabled=false,facing=down", 8);
        p("minecraft:hopper", "enabled=false,facing=north", 10);
        p("minecraft:hopper", "enabled=false,facing=south", 11);
        p("minecraft:hopper", "enabled=false,facing=west", 12);
        p("minecraft:hopper", "enabled=false,facing=east", 13);

        // Pumpkin / Carved pumpkin / Jack o lantern
        p("minecraft:pumpkin", "facing=south", 0);
        p("minecraft:pumpkin", "facing=west", 1);
        p("minecraft:pumpkin", "facing=north", 2);
        p("minecraft:pumpkin", "facing=east", 3);
        p("minecraft:lit_pumpkin", "facing=south", 0);
        p("minecraft:lit_pumpkin", "facing=west", 1);
        p("minecraft:lit_pumpkin", "facing=north", 2);
        p("minecraft:lit_pumpkin", "facing=east", 3);

        // Vine (bitmask: south=1, west=2, north=4, east=8)
        // We handle this dynamically in BlockStateTranslator

        // Water / Lava levels
        for (int i = 0; i <= 15; i++) {
            p("minecraft:water", "level=" + i, i);
            p("minecraft:flowing_water", "level=" + i, i);
            p("minecraft:lava", "level=" + i, i);
            p("minecraft:flowing_lava", "level=" + i, i);
        }

        // End portal frame
        p("minecraft:end_portal_frame", "eye=false,facing=south", 0);
        p("minecraft:end_portal_frame", "eye=false,facing=west", 1);
        p("minecraft:end_portal_frame", "eye=false,facing=north", 2);
        p("minecraft:end_portal_frame", "eye=false,facing=east", 3);
        p("minecraft:end_portal_frame", "eye=true,facing=south", 4);
        p("minecraft:end_portal_frame", "eye=true,facing=west", 5);
        p("minecraft:end_portal_frame", "eye=true,facing=north", 6);
        p("minecraft:end_portal_frame", "eye=true,facing=east", 7);

        // Brewing stand
        p("minecraft:brewing_stand", "has_bottle_0=false,has_bottle_1=false,has_bottle_2=false", 0);
        p("minecraft:brewing_stand", "has_bottle_0=true,has_bottle_1=false,has_bottle_2=false", 1);
        p("minecraft:brewing_stand", "has_bottle_0=false,has_bottle_1=true,has_bottle_2=false", 2);
        p("minecraft:brewing_stand", "has_bottle_0=true,has_bottle_1=true,has_bottle_2=false", 3);
        p("minecraft:brewing_stand", "has_bottle_0=false,has_bottle_1=false,has_bottle_2=true", 4);
        p("minecraft:brewing_stand", "has_bottle_0=true,has_bottle_1=false,has_bottle_2=true", 5);
        p("minecraft:brewing_stand", "has_bottle_0=false,has_bottle_1=true,has_bottle_2=true", 6);
        p("minecraft:brewing_stand", "has_bottle_0=true,has_bottle_1=true,has_bottle_2=true", 7);

        // TNT
        p("minecraft:tnt", "explode=false", 0);
        p("minecraft:tnt", "explode=true", 1);

        // Jukebox
        p("minecraft:jukebox", "has_record=false", 0);
        p("minecraft:jukebox", "has_record=true", 1);

        // Skull (floor rotation handled by TileEntity, wall facing by meta)
        p("minecraft:skull", "facing=north", 2);
        p("minecraft:skull", "facing=south", 3);
        p("minecraft:skull", "facing=west", 4);
        p("minecraft:skull", "facing=east", 5);
        p("minecraft:skull", "facing=up,nodrop=false", 1);

        // Banner (wall)
        p("minecraft:wall_banner", "facing=north", 2);
        p("minecraft:wall_banner", "facing=south", 3);
        p("minecraft:wall_banner", "facing=west", 4);
        p("minecraft:wall_banner", "facing=east", 5);

        // Standing banner / sign (rotation 0-15)
        for (int i = 0; i <= 15; i++) {
            p("minecraft:standing_banner", "rotation=" + i, i);
            p("minecraft:standing_sign", "rotation=" + i, i);
        }

        // Hay bale / Bone block axis
        p("minecraft:hay_block", "axis=y", 0);
        p("minecraft:hay_block", "axis=x", 4);
        p("minecraft:hay_block", "axis=z", 8);

        // Farmland moisture
        for (int i = 0; i <= 7; i++) {
            p("minecraft:farmland", "moisture=" + i, i);
        }
    }
}
