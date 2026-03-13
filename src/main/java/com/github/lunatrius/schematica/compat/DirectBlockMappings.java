package com.github.lunatrius.schematica.compat;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

/**
 * Direct Block reference mappings for modern block names to 1.7.10 Block objects.
 * Uses Blocks.xxx static fields to completely bypass BLOCK_REGISTRY string lookups,
 * which can fail for blocks whose registry names differ from their expected names.
 *
 * This fixes issues like:
 * - minecraft:stone_bricks -> Blocks.stonebrick (registry lookup for "minecraft:stonebrick" fails)
 * - minecraft:farmland -> Blocks.farmland (registry lookup may fail)
 *
 * @author HackerRouter
 */
public final class DirectBlockMappings {

    private static Map<String, BlockMapping> MAP;

    private DirectBlockMappings() {}

    public static BlockMapping get(String modernName) {
        return getMap().get(modernName);
    }

    private static Map<String, BlockMapping> getMap() {
        if (MAP != null) return MAP;
        MAP = new HashMap<String, BlockMapping>(256);
        // Stone variants
        m("minecraft:stone", Blocks.stone, 0);
        m("minecraft:granite", Blocks.stone, 1);
        m("minecraft:polished_granite", Blocks.stone, 2);
        m("minecraft:diorite", Blocks.stone, 3);
        m("minecraft:polished_diorite", Blocks.stone, 4);
        m("minecraft:andesite", Blocks.stone, 5);
        m("minecraft:polished_andesite", Blocks.stone, 6);
        m("minecraft:smooth_stone", Blocks.stone, 0);
        // Stonebrick variants
        m("minecraft:stone_bricks", Blocks.stonebrick, 0);
        m("minecraft:mossy_stone_bricks", Blocks.stonebrick, 1);
        m("minecraft:cracked_stone_bricks", Blocks.stonebrick, 2);
        m("minecraft:chiseled_stone_bricks", Blocks.stonebrick, 3);
        // Dirt variants
        m("minecraft:dirt", Blocks.dirt, 0);
        m("minecraft:coarse_dirt", Blocks.dirt, 1);
        m("minecraft:podzol", Blocks.dirt, 2);
        // Sand
        m("minecraft:sand", Blocks.sand, 0);
        m("minecraft:red_sand", Blocks.sand, 1);
        // Sandstone
        m("minecraft:sandstone", Blocks.sandstone, 0);
        m("minecraft:chiseled_sandstone", Blocks.sandstone, 1);
        m("minecraft:cut_sandstone", Blocks.sandstone, 2);
        // red_sandstone was added in 1.8; map to regular sandstone in 1.7.10
        m("minecraft:red_sandstone", Blocks.sandstone, 0);
        m("minecraft:chiseled_red_sandstone", Blocks.sandstone, 1);
        m("minecraft:cut_red_sandstone", Blocks.sandstone, 2);
        // Quartz
        m("minecraft:quartz_block", Blocks.quartz_block, 0);
        m("minecraft:chiseled_quartz_block", Blocks.quartz_block, 1);
        m("minecraft:quartz_pillar", Blocks.quartz_block, 2);
        // Sponge
        m("minecraft:sponge", Blocks.sponge, 0);
        m("minecraft:wet_sponge", Blocks.sponge, 1);
        // Cobblestone wall
        m("minecraft:cobblestone_wall", Blocks.cobblestone_wall, 0);
        m("minecraft:mossy_cobblestone_wall", Blocks.cobblestone_wall, 1);
        // Monster egg
        m("minecraft:infested_stone", Blocks.monster_egg, 0);
        m("minecraft:infested_cobblestone", Blocks.monster_egg, 1);
        m("minecraft:infested_stone_bricks", Blocks.monster_egg, 2);
        m("minecraft:infested_mossy_stone_bricks", Blocks.monster_egg, 3);
        m("minecraft:infested_cracked_stone_bricks", Blocks.monster_egg, 4);
        m("minecraft:infested_chiseled_stone_bricks", Blocks.monster_egg, 5);
        // Prismarine was added in 1.8; map to stonebrick in 1.7.10
        m("minecraft:prismarine", Blocks.stonebrick, 0);
        m("minecraft:prismarine_bricks", Blocks.stonebrick, 0);
        m("minecraft:dark_prismarine", Blocks.stonebrick, 0);
        // Planks
        m("minecraft:oak_planks", Blocks.planks, 0);
        m("minecraft:spruce_planks", Blocks.planks, 1);
        m("minecraft:birch_planks", Blocks.planks, 2);
        m("minecraft:jungle_planks", Blocks.planks, 3);
        m("minecraft:acacia_planks", Blocks.planks, 4);
        m("minecraft:dark_oak_planks", Blocks.planks, 5);
        // Saplings
        m("minecraft:oak_sapling", Blocks.sapling, 0);
        m("minecraft:spruce_sapling", Blocks.sapling, 1);
        m("minecraft:birch_sapling", Blocks.sapling, 2);
        m("minecraft:jungle_sapling", Blocks.sapling, 3);
        m("minecraft:acacia_sapling", Blocks.sapling, 4);
        m("minecraft:dark_oak_sapling", Blocks.sapling, 5);
        // Leaves
        m("minecraft:oak_leaves", Blocks.leaves, 0);
        m("minecraft:spruce_leaves", Blocks.leaves, 1);
        m("minecraft:birch_leaves", Blocks.leaves, 2);
        m("minecraft:jungle_leaves", Blocks.leaves, 3);
        m("minecraft:acacia_leaves", Blocks.leaves2, 0);
        m("minecraft:dark_oak_leaves", Blocks.leaves2, 1);
        // Flowers
        m("minecraft:dandelion", Blocks.yellow_flower, 0);
        m("minecraft:poppy", Blocks.red_flower, 0);
        m("minecraft:blue_orchid", Blocks.red_flower, 1);
        m("minecraft:allium", Blocks.red_flower, 2);
        m("minecraft:azure_bluet", Blocks.red_flower, 3);
        m("minecraft:red_tulip", Blocks.red_flower, 4);
        m("minecraft:orange_tulip", Blocks.red_flower, 5);
        m("minecraft:white_tulip", Blocks.red_flower, 6);
        m("minecraft:pink_tulip", Blocks.red_flower, 7);
        m("minecraft:oxeye_daisy", Blocks.red_flower, 8);
        // Double plants
        m("minecraft:sunflower", Blocks.double_plant, 0);
        m("minecraft:lilac", Blocks.double_plant, 1);
        m("minecraft:tall_grass", Blocks.double_plant, 2);
        m("minecraft:large_fern", Blocks.double_plant, 3);
        m("minecraft:rose_bush", Blocks.double_plant, 4);
        m("minecraft:peony", Blocks.double_plant, 5);
        // Common renamed blocks (direct references to avoid registry failures)
        m("minecraft:grass_block", Blocks.grass, 0);
        m("minecraft:farmland", Blocks.farmland, 0);
        m("minecraft:cobblestone", Blocks.cobblestone, 0);
        m("minecraft:mossy_cobblestone", Blocks.mossy_cobblestone, 0);
        m("minecraft:nether_bricks", Blocks.nether_brick, 0);
        m("minecraft:bricks", Blocks.brick_block, 0);
        m("minecraft:terracotta", Blocks.hardened_clay, 0);
        m("minecraft:cobweb", Blocks.web, 0);
        m("minecraft:spawner", Blocks.mob_spawner, 0);
        m("minecraft:nether_portal", Blocks.portal, 0);
        m("minecraft:sugar_cane", Blocks.reeds, 0);
        m("minecraft:lily_pad", Blocks.waterlily, 0);
        m("minecraft:melon", Blocks.melon_block, 0);
        m("minecraft:jack_o_lantern", Blocks.lit_pumpkin, 0);
        m("minecraft:carved_pumpkin", Blocks.pumpkin, 0);
        m("minecraft:glass", Blocks.glass, 0);
        m("minecraft:glass_pane", Blocks.glass_pane, 0);
        m("minecraft:bookshelf", Blocks.bookshelf, 0);
        m("minecraft:obsidian", Blocks.obsidian, 0);
        m("minecraft:diamond_block", Blocks.diamond_block, 0);
        m("minecraft:gold_block", Blocks.gold_block, 0);
        m("minecraft:iron_block", Blocks.iron_block, 0);
        m("minecraft:emerald_block", Blocks.emerald_block, 0);
        m("minecraft:lapis_block", Blocks.lapis_block, 0);
        m("minecraft:coal_block", Blocks.coal_block, 0);
        m("minecraft:redstone_block", Blocks.redstone_block, 0);
        m("minecraft:clay", Blocks.clay, 0);
        m("minecraft:netherrack", Blocks.netherrack, 0);
        m("minecraft:soul_sand", Blocks.soul_sand, 0);
        m("minecraft:glowstone", Blocks.glowstone, 0);
        m("minecraft:end_stone", Blocks.end_stone, 0);
        m("minecraft:ice", Blocks.ice, 0);
        m("minecraft:packed_ice", Blocks.packed_ice, 0);
        m("minecraft:snow_block", Blocks.snow, 0);
        m("minecraft:bedrock", Blocks.bedrock, 0);
        m("minecraft:gravel", Blocks.gravel, 0);
        m("minecraft:iron_ore", Blocks.iron_ore, 0);
        m("minecraft:gold_ore", Blocks.gold_ore, 0);
        m("minecraft:diamond_ore", Blocks.diamond_ore, 0);
        m("minecraft:coal_ore", Blocks.coal_ore, 0);
        m("minecraft:lapis_ore", Blocks.lapis_ore, 0);
        m("minecraft:redstone_ore", Blocks.redstone_ore, 0);
        m("minecraft:emerald_ore", Blocks.emerald_ore, 0);
        m("minecraft:nether_quartz_ore", Blocks.quartz_ore, 0);
        m("minecraft:crafting_table", Blocks.crafting_table, 0);
        m("minecraft:enchanting_table", Blocks.enchanting_table, 0);
        m("minecraft:anvil", Blocks.anvil, 0);
        m("minecraft:iron_bars", Blocks.iron_bars, 0);
        m("minecraft:brewing_stand", Blocks.brewing_stand, 0);
        m("minecraft:cauldron", Blocks.cauldron, 0);
        m("minecraft:beacon", Blocks.beacon, 0);
        m("minecraft:hay_block", Blocks.hay_block, 0);
        m("minecraft:mycelium", Blocks.mycelium, 0);
        m("minecraft:nether_brick_fence", Blocks.nether_brick_fence, 0);
        m("minecraft:end_portal_frame", Blocks.end_portal_frame, 0);
        m("minecraft:torch", Blocks.torch, 0);
        m("minecraft:tnt", Blocks.tnt, 0);
        m("minecraft:pumpkin", Blocks.pumpkin, 0);
        m("minecraft:jukebox", Blocks.jukebox, 0);
        m("minecraft:note_block", Blocks.noteblock, 0);
        m("minecraft:cactus", Blocks.cactus, 0);
        // Wool (color split)
        m("minecraft:white_wool", Blocks.wool, 0);
        m("minecraft:orange_wool", Blocks.wool, 1);
        m("minecraft:magenta_wool", Blocks.wool, 2);
        m("minecraft:light_blue_wool", Blocks.wool, 3);
        m("minecraft:yellow_wool", Blocks.wool, 4);
        m("minecraft:lime_wool", Blocks.wool, 5);
        m("minecraft:pink_wool", Blocks.wool, 6);
        m("minecraft:gray_wool", Blocks.wool, 7);
        m("minecraft:light_gray_wool", Blocks.wool, 8);
        m("minecraft:cyan_wool", Blocks.wool, 9);
        m("minecraft:purple_wool", Blocks.wool, 10);
        m("minecraft:blue_wool", Blocks.wool, 11);
        m("minecraft:brown_wool", Blocks.wool, 12);
        m("minecraft:green_wool", Blocks.wool, 13);
        m("minecraft:red_wool", Blocks.wool, 14);
        m("minecraft:black_wool", Blocks.wool, 15);
        // Carpet
        m("minecraft:white_carpet", Blocks.carpet, 0);
        m("minecraft:orange_carpet", Blocks.carpet, 1);
        m("minecraft:magenta_carpet", Blocks.carpet, 2);
        m("minecraft:light_blue_carpet", Blocks.carpet, 3);
        m("minecraft:yellow_carpet", Blocks.carpet, 4);
        m("minecraft:lime_carpet", Blocks.carpet, 5);
        m("minecraft:pink_carpet", Blocks.carpet, 6);
        m("minecraft:gray_carpet", Blocks.carpet, 7);
        m("minecraft:light_gray_carpet", Blocks.carpet, 8);
        m("minecraft:cyan_carpet", Blocks.carpet, 9);
        m("minecraft:purple_carpet", Blocks.carpet, 10);
        m("minecraft:blue_carpet", Blocks.carpet, 11);
        m("minecraft:brown_carpet", Blocks.carpet, 12);
        m("minecraft:green_carpet", Blocks.carpet, 13);
        m("minecraft:red_carpet", Blocks.carpet, 14);
        m("minecraft:black_carpet", Blocks.carpet, 15);
        // Stained terracotta
        m("minecraft:white_terracotta", Blocks.stained_hardened_clay, 0);
        m("minecraft:orange_terracotta", Blocks.stained_hardened_clay, 1);
        m("minecraft:magenta_terracotta", Blocks.stained_hardened_clay, 2);
        m("minecraft:light_blue_terracotta", Blocks.stained_hardened_clay, 3);
        m("minecraft:yellow_terracotta", Blocks.stained_hardened_clay, 4);
        m("minecraft:lime_terracotta", Blocks.stained_hardened_clay, 5);
        m("minecraft:pink_terracotta", Blocks.stained_hardened_clay, 6);
        m("minecraft:gray_terracotta", Blocks.stained_hardened_clay, 7);
        m("minecraft:light_gray_terracotta", Blocks.stained_hardened_clay, 8);
        m("minecraft:cyan_terracotta", Blocks.stained_hardened_clay, 9);
        m("minecraft:purple_terracotta", Blocks.stained_hardened_clay, 10);
        m("minecraft:blue_terracotta", Blocks.stained_hardened_clay, 11);
        m("minecraft:brown_terracotta", Blocks.stained_hardened_clay, 12);
        m("minecraft:green_terracotta", Blocks.stained_hardened_clay, 13);
        m("minecraft:red_terracotta", Blocks.stained_hardened_clay, 14);
        m("minecraft:black_terracotta", Blocks.stained_hardened_clay, 15);
        // Stained glass
        m("minecraft:white_stained_glass", Blocks.stained_glass, 0);
        m("minecraft:orange_stained_glass", Blocks.stained_glass, 1);
        m("minecraft:magenta_stained_glass", Blocks.stained_glass, 2);
        m("minecraft:light_blue_stained_glass", Blocks.stained_glass, 3);
        m("minecraft:yellow_stained_glass", Blocks.stained_glass, 4);
        m("minecraft:lime_stained_glass", Blocks.stained_glass, 5);
        m("minecraft:pink_stained_glass", Blocks.stained_glass, 6);
        m("minecraft:gray_stained_glass", Blocks.stained_glass, 7);
        m("minecraft:light_gray_stained_glass", Blocks.stained_glass, 8);
        m("minecraft:cyan_stained_glass", Blocks.stained_glass, 9);
        m("minecraft:purple_stained_glass", Blocks.stained_glass, 10);
        m("minecraft:blue_stained_glass", Blocks.stained_glass, 11);
        m("minecraft:brown_stained_glass", Blocks.stained_glass, 12);
        m("minecraft:green_stained_glass", Blocks.stained_glass, 13);
        m("minecraft:red_stained_glass", Blocks.stained_glass, 14);
        m("minecraft:black_stained_glass", Blocks.stained_glass, 15);
        // Stained glass pane
        m("minecraft:white_stained_glass_pane", Blocks.stained_glass_pane, 0);
        m("minecraft:orange_stained_glass_pane", Blocks.stained_glass_pane, 1);
        m("minecraft:magenta_stained_glass_pane", Blocks.stained_glass_pane, 2);
        m("minecraft:light_blue_stained_glass_pane", Blocks.stained_glass_pane, 3);
        m("minecraft:yellow_stained_glass_pane", Blocks.stained_glass_pane, 4);
        m("minecraft:lime_stained_glass_pane", Blocks.stained_glass_pane, 5);
        m("minecraft:pink_stained_glass_pane", Blocks.stained_glass_pane, 6);
        m("minecraft:gray_stained_glass_pane", Blocks.stained_glass_pane, 7);
        m("minecraft:light_gray_stained_glass_pane", Blocks.stained_glass_pane, 8);
        m("minecraft:cyan_stained_glass_pane", Blocks.stained_glass_pane, 9);
        m("minecraft:purple_stained_glass_pane", Blocks.stained_glass_pane, 10);
        m("minecraft:blue_stained_glass_pane", Blocks.stained_glass_pane, 11);
        m("minecraft:brown_stained_glass_pane", Blocks.stained_glass_pane, 12);
        m("minecraft:green_stained_glass_pane", Blocks.stained_glass_pane, 13);
        m("minecraft:red_stained_glass_pane", Blocks.stained_glass_pane, 14);
        m("minecraft:black_stained_glass_pane", Blocks.stained_glass_pane, 15);
        // Modern blocks -> closest 1.7.10 approximation
        m("minecraft:deepslate", Blocks.stone, 0);
        m("minecraft:cobbled_deepslate", Blocks.cobblestone, 0);
        m("minecraft:polished_deepslate", Blocks.stone, 0);
        m("minecraft:deepslate_bricks", Blocks.stonebrick, 0);
        m("minecraft:deepslate_tiles", Blocks.stonebrick, 0);
        m("minecraft:chiseled_deepslate", Blocks.stonebrick, 3);
        m("minecraft:tuff", Blocks.stone, 0);
        m("minecraft:tuff_bricks", Blocks.stonebrick, 0);
        m("minecraft:polished_tuff", Blocks.stone, 0);
        m("minecraft:calcite", Blocks.stone, 0);
        m("minecraft:dripstone_block", Blocks.stone, 0);
        m("minecraft:mud", Blocks.dirt, 0);
        m("minecraft:packed_mud", Blocks.dirt, 0);
        m("minecraft:mud_bricks", Blocks.brick_block, 0);
        m("minecraft:resin_block", Blocks.brick_block, 0);
        m("minecraft:resin_bricks", Blocks.brick_block, 0);
        m("minecraft:chiseled_resin_bricks", Blocks.brick_block, 0);
        m("minecraft:blackstone", Blocks.stonebrick, 0);
        m("minecraft:polished_blackstone", Blocks.stonebrick, 0);
        m("minecraft:polished_blackstone_bricks", Blocks.stonebrick, 0);
        m("minecraft:chiseled_polished_blackstone", Blocks.stonebrick, 3);
        m("minecraft:basalt", Blocks.stone, 0);
        m("minecraft:polished_basalt", Blocks.stone, 0);
        m("minecraft:smooth_basalt", Blocks.stone, 0);
        m("minecraft:amethyst_block", Blocks.glass, 0);
        m("minecraft:budding_amethyst", Blocks.glass, 0);
        m("minecraft:copper_block", Blocks.iron_block, 0);
        m("minecraft:exposed_copper", Blocks.iron_block, 0);
        m("minecraft:weathered_copper", Blocks.iron_block, 0);
        m("minecraft:oxidized_copper", Blocks.iron_block, 0);
        m("minecraft:waxed_copper_block", Blocks.iron_block, 0);
        m("minecraft:cut_copper", Blocks.iron_block, 0);
        m("minecraft:blue_ice", Blocks.packed_ice, 0);
        m("minecraft:lectern", Blocks.bookshelf, 0);
        // New wood type planks -> oak planks
        m("minecraft:cherry_planks", Blocks.planks, 0);
        m("minecraft:bamboo_planks", Blocks.planks, 0);
        m("minecraft:bamboo_mosaic", Blocks.planks, 0);
        m("minecraft:pale_oak_planks", Blocks.planks, 0);
        m("minecraft:mangrove_planks", Blocks.planks, 0);
        m("minecraft:warped_planks", Blocks.planks, 0);
        m("minecraft:crimson_planks", Blocks.planks, 0);
        return MAP;
    }

    private static void m(String name, Block block, int meta) {
        MAP.put(name, new BlockMapping(block, meta));
    }
}
