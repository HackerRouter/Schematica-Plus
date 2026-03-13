
package com.github.lunatrius.schematica.compat;

import java.util.Map;

/**
 * Block rename mappings: modern (1.13+) block names -> 1.7.10 block names.
 * Called by VanillaBlockMappings static initializer.
 */
final class VanillaBlockRenames {

    private VanillaBlockRenames() {}

    static void init() {
        final Map<String, String> m = VanillaBlockMappings.BLOCK_RENAMES;

        // Grass block
        m.put("minecraft:grass_block", "minecraft:grass");

        // Logs
        m.put("minecraft:oak_log", "minecraft:log");
        m.put("minecraft:spruce_log", "minecraft:log");
        m.put("minecraft:birch_log", "minecraft:log");
        m.put("minecraft:jungle_log", "minecraft:log");
        m.put("minecraft:acacia_log", "minecraft:log2");
        m.put("minecraft:dark_oak_log", "minecraft:log2");

        // Planks
        m.put("minecraft:oak_planks", "minecraft:planks");
        m.put("minecraft:spruce_planks", "minecraft:planks");
        m.put("minecraft:birch_planks", "minecraft:planks");
        m.put("minecraft:jungle_planks", "minecraft:planks");
        m.put("minecraft:acacia_planks", "minecraft:planks");
        m.put("minecraft:dark_oak_planks", "minecraft:planks");

        // Wooden slabs
        m.put("minecraft:oak_slab", "minecraft:wooden_slab");
        m.put("minecraft:spruce_slab", "minecraft:wooden_slab");
        m.put("minecraft:birch_slab", "minecraft:wooden_slab");
        m.put("minecraft:jungle_slab", "minecraft:wooden_slab");
        m.put("minecraft:acacia_slab", "minecraft:wooden_slab");
        m.put("minecraft:dark_oak_slab", "minecraft:wooden_slab");

        // Stone slabs
        m.put("minecraft:cobblestone_slab", "minecraft:stone_slab");
        m.put("minecraft:sandstone_slab", "minecraft:stone_slab");
        m.put("minecraft:brick_slab", "minecraft:stone_slab");
        m.put("minecraft:stone_brick_slab", "minecraft:stone_slab");
        m.put("minecraft:nether_brick_slab", "minecraft:stone_slab");
        m.put("minecraft:quartz_slab", "minecraft:stone_slab");
        m.put("minecraft:red_sandstone_slab", "minecraft:stone_slab2");
        m.put("minecraft:purpur_slab", "minecraft:stone_slab2");
        m.put("minecraft:prismarine_slab", "minecraft:stone_slab");
        m.put("minecraft:prismarine_brick_slab", "minecraft:stone_slab");
        m.put("minecraft:dark_prismarine_slab", "minecraft:stone_slab");
        m.put("minecraft:smooth_stone_slab", "minecraft:stone_slab");

        // Stairs rename
        m.put("minecraft:cobblestone_stairs", "minecraft:stone_stairs");

        // Fence / gate / door
        m.put("minecraft:oak_fence", "minecraft:fence");
        m.put("minecraft:spruce_fence", "minecraft:fence");
        m.put("minecraft:birch_fence", "minecraft:fence");
        m.put("minecraft:jungle_fence", "minecraft:fence");
        m.put("minecraft:acacia_fence", "minecraft:fence");
        m.put("minecraft:dark_oak_fence", "minecraft:fence");
        m.put("minecraft:oak_fence_gate", "minecraft:fence_gate");
        m.put("minecraft:spruce_fence_gate", "minecraft:fence_gate");
        m.put("minecraft:birch_fence_gate", "minecraft:fence_gate");
        m.put("minecraft:jungle_fence_gate", "minecraft:fence_gate");
        m.put("minecraft:acacia_fence_gate", "minecraft:fence_gate");
        m.put("minecraft:dark_oak_fence_gate", "minecraft:fence_gate");
        m.put("minecraft:oak_door", "minecraft:wooden_door");
        m.put("minecraft:spruce_door", "minecraft:wooden_door");
        m.put("minecraft:birch_door", "minecraft:wooden_door");
        m.put("minecraft:jungle_door", "minecraft:wooden_door");
        m.put("minecraft:acacia_door", "minecraft:wooden_door");
        m.put("minecraft:dark_oak_door", "minecraft:wooden_door");

        // Stone bricks
        m.put("minecraft:nether_bricks", "minecraft:nether_brick");
        m.put("minecraft:stone_bricks", "minecraft:stonebrick");
        m.put("minecraft:mossy_stone_bricks", "minecraft:stonebrick");
        m.put("minecraft:cracked_stone_bricks", "minecraft:stonebrick");
        m.put("minecraft:chiseled_stone_bricks", "minecraft:stonebrick");

        // Infested blocks
        m.put("minecraft:infested_stone", "minecraft:monster_egg");
        m.put("minecraft:infested_cobblestone", "minecraft:monster_egg");
        m.put("minecraft:infested_stone_bricks", "minecraft:monster_egg");
        m.put("minecraft:infested_mossy_stone_bricks", "minecraft:monster_egg");
        m.put("minecraft:infested_cracked_stone_bricks", "minecraft:monster_egg");
        m.put("minecraft:infested_chiseled_stone_bricks", "minecraft:monster_egg");

        // Leaves
        m.put("minecraft:oak_leaves", "minecraft:leaves");
        m.put("minecraft:spruce_leaves", "minecraft:leaves");
        m.put("minecraft:birch_leaves", "minecraft:leaves");
        m.put("minecraft:jungle_leaves", "minecraft:leaves");
        m.put("minecraft:acacia_leaves", "minecraft:leaves2");
        m.put("minecraft:dark_oak_leaves", "minecraft:leaves2");

        // Saplings
        m.put("minecraft:oak_sapling", "minecraft:sapling");
        m.put("minecraft:spruce_sapling", "minecraft:sapling");
        m.put("minecraft:birch_sapling", "minecraft:sapling");
        m.put("minecraft:jungle_sapling", "minecraft:sapling");
        m.put("minecraft:acacia_sapling", "minecraft:sapling");
        m.put("minecraft:dark_oak_sapling", "minecraft:sapling");

        // Double plants
        m.put("minecraft:rose_bush", "minecraft:double_plant");
        m.put("minecraft:peony", "minecraft:double_plant");
        m.put("minecraft:tall_grass", "minecraft:double_plant");
        m.put("minecraft:large_fern", "minecraft:double_plant");
        m.put("minecraft:sunflower", "minecraft:double_plant");
        m.put("minecraft:lilac", "minecraft:double_plant");

        // Flowers
        m.put("minecraft:poppy", "minecraft:red_flower");
        m.put("minecraft:blue_orchid", "minecraft:red_flower");
        m.put("minecraft:allium", "minecraft:red_flower");
        m.put("minecraft:azure_bluet", "minecraft:red_flower");
        m.put("minecraft:red_tulip", "minecraft:red_flower");
        m.put("minecraft:orange_tulip", "minecraft:red_flower");
        m.put("minecraft:white_tulip", "minecraft:red_flower");
        m.put("minecraft:pink_tulip", "minecraft:red_flower");
        m.put("minecraft:oxeye_daisy", "minecraft:red_flower");
        m.put("minecraft:dandelion", "minecraft:yellow_flower");

        // Cobblestone wall
        m.put("minecraft:mossy_cobblestone_wall", "minecraft:cobblestone_wall");

        // Terracotta
        m.put("minecraft:terracotta", "minecraft:hardened_clay");
        String[] colors = {"white", "orange", "magenta", "light_blue", "yellow", "lime",
            "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
        for (String c : colors) {
            m.put("minecraft:" + c + "_terracotta", "minecraft:stained_hardened_clay");
            m.put("minecraft:" + c + "_stained_glass", "minecraft:stained_glass");
            m.put("minecraft:" + c + "_stained_glass_pane", "minecraft:stained_glass_pane");
            m.put("minecraft:" + c + "_wool", "minecraft:wool");
            m.put("minecraft:" + c + "_carpet", "minecraft:carpet");
        }

        // Beds (all colors -> single bed in 1.7.10)
        for (String c : colors) {
            m.put("minecraft:" + c + "_bed", "minecraft:bed");
        }

        // Misc renames
        m.put("minecraft:bricks", "minecraft:brick_block");
        m.put("minecraft:jack_o_lantern", "minecraft:lit_pumpkin");
        m.put("minecraft:wet_sponge", "minecraft:sponge");
        m.put("minecraft:granite", "minecraft:stone");
        m.put("minecraft:polished_granite", "minecraft:stone");
        m.put("minecraft:diorite", "minecraft:stone");
        m.put("minecraft:polished_diorite", "minecraft:stone");
        m.put("minecraft:andesite", "minecraft:stone");
        m.put("minecraft:polished_andesite", "minecraft:stone");
        m.put("minecraft:coarse_dirt", "minecraft:dirt");
        m.put("minecraft:podzol", "minecraft:dirt");
        m.put("minecraft:red_sand", "minecraft:sand");
        m.put("minecraft:spawner", "minecraft:mob_spawner");
        m.put("minecraft:nether_portal", "minecraft:portal");
        m.put("minecraft:cobweb", "minecraft:web");
        m.put("minecraft:sugar_cane", "minecraft:reeds");
        m.put("minecraft:lily_pad", "minecraft:waterlily");
        m.put("minecraft:melon", "minecraft:melon_block");
        m.put("minecraft:comparator", "minecraft:unpowered_comparator");
        m.put("minecraft:repeater", "minecraft:unpowered_repeater");
        m.put("minecraft:chiseled_quartz_block", "minecraft:quartz_block");
        m.put("minecraft:quartz_pillar", "minecraft:quartz_block");
        m.put("minecraft:chiseled_sandstone", "minecraft:sandstone");
        m.put("minecraft:cut_sandstone", "minecraft:sandstone");
        m.put("minecraft:chiseled_red_sandstone", "minecraft:red_sandstone");
        m.put("minecraft:cut_red_sandstone", "minecraft:red_sandstone");
        m.put("minecraft:smooth_stone", "minecraft:stone");
        m.put("minecraft:carved_pumpkin", "minecraft:pumpkin");

        // Skulls / Heads
        m.put("minecraft:skeleton_skull", "minecraft:skull");
        m.put("minecraft:wither_skeleton_skull", "minecraft:skull");
        m.put("minecraft:zombie_head", "minecraft:skull");
        m.put("minecraft:player_head", "minecraft:skull");
        m.put("minecraft:creeper_head", "minecraft:skull");
        m.put("minecraft:dragon_head", "minecraft:skull");
        m.put("minecraft:piglin_head", "minecraft:skull");
        m.put("minecraft:skeleton_wall_skull", "minecraft:skull");
        m.put("minecraft:wither_skeleton_wall_skull", "minecraft:skull");
        m.put("minecraft:zombie_wall_head", "minecraft:skull");
        m.put("minecraft:player_wall_head", "minecraft:skull");
        m.put("minecraft:creeper_wall_head", "minecraft:skull");
        m.put("minecraft:dragon_wall_head", "minecraft:skull");
        m.put("minecraft:piglin_wall_head", "minecraft:skull");

        // Signs (all wood variants)
        String[] woodTypes = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "cherry", "bamboo", "pale_oak", "mangrove", "warped", "crimson"};
        for (String w : woodTypes) {
            m.put("minecraft:" + w + "_wall_sign", "minecraft:wall_sign");
            m.put("minecraft:" + w + "_sign", "minecraft:standing_sign");
            m.put("minecraft:" + w + "_wall_hanging_sign", "minecraft:wall_sign");
            m.put("minecraft:" + w + "_hanging_sign", "minecraft:standing_sign");
        }
        m.put("minecraft:sign", "minecraft:standing_sign");

        // Wall torches
        m.put("minecraft:wall_torch", "minecraft:torch");
        m.put("minecraft:redstone_wall_torch", "minecraft:redstone_torch");
        m.put("minecraft:soul_torch", "minecraft:torch");
        m.put("minecraft:soul_wall_torch", "minecraft:torch");
        m.put("minecraft:soul_lantern", "minecraft:torch");
        m.put("minecraft:soul_campfire", "minecraft:torch");
        m.put("minecraft:lantern", "minecraft:torch");
        m.put("minecraft:campfire", "minecraft:netherrack");

        // Command blocks
        m.put("minecraft:chain_command_block", "minecraft:command_block");
        m.put("minecraft:repeating_command_block", "minecraft:command_block");
        m.put("minecraft:lectern", "minecraft:bookshelf");

        // New wood types -> legacy equivalents
        String[] newWoods = {"cherry", "bamboo", "pale_oak", "mangrove", "warped", "crimson"};
        for (String w : newWoods) {
            m.put("minecraft:" + w + "_planks", "minecraft:planks");
            m.put("minecraft:" + w + "_slab", "minecraft:wooden_slab");
            m.put("minecraft:" + w + "_stairs", "minecraft:oak_stairs");
            m.put("minecraft:" + w + "_fence", "minecraft:fence");
            m.put("minecraft:" + w + "_fence_gate", "minecraft:fence_gate");
            m.put("minecraft:" + w + "_door", "minecraft:wooden_door");
            m.put("minecraft:" + w + "_trapdoor", "minecraft:trapdoor");
            m.put("minecraft:" + w + "_button", "minecraft:wooden_button");
            m.put("minecraft:" + w + "_pressure_plate", "minecraft:wooden_pressure_plate");
        }
        // Bamboo mosaic variants
        m.put("minecraft:bamboo_mosaic", "minecraft:planks");
        m.put("minecraft:bamboo_mosaic_slab", "minecraft:wooden_slab");
        m.put("minecraft:bamboo_mosaic_stairs", "minecraft:oak_stairs");

        // New wood type logs
        m.put("minecraft:cherry_log", "minecraft:log");
        m.put("minecraft:bamboo_block", "minecraft:log");
        m.put("minecraft:pale_oak_log", "minecraft:log");
        m.put("minecraft:mangrove_log", "minecraft:log");
        m.put("minecraft:warped_stem", "minecraft:log");
        m.put("minecraft:crimson_stem", "minecraft:log");

        // New wood type leaves
        m.put("minecraft:cherry_leaves", "minecraft:leaves");
        m.put("minecraft:pale_oak_leaves", "minecraft:leaves");
        m.put("minecraft:mangrove_leaves", "minecraft:leaves");
        m.put("minecraft:azalea_leaves", "minecraft:leaves");
        m.put("minecraft:flowering_azalea_leaves", "minecraft:leaves");

        // Original wood type trapdoors/buttons/pressure plates
        m.put("minecraft:oak_trapdoor", "minecraft:trapdoor");
        m.put("minecraft:spruce_trapdoor", "minecraft:trapdoor");
        m.put("minecraft:birch_trapdoor", "minecraft:trapdoor");
        m.put("minecraft:jungle_trapdoor", "minecraft:trapdoor");
        m.put("minecraft:acacia_trapdoor", "minecraft:trapdoor");
        m.put("minecraft:dark_oak_trapdoor", "minecraft:trapdoor");
        m.put("minecraft:oak_button", "minecraft:wooden_button");
        m.put("minecraft:spruce_button", "minecraft:wooden_button");
        m.put("minecraft:birch_button", "minecraft:wooden_button");
        m.put("minecraft:jungle_button", "minecraft:wooden_button");
        m.put("minecraft:acacia_button", "minecraft:wooden_button");
        m.put("minecraft:dark_oak_button", "minecraft:wooden_button");
        m.put("minecraft:oak_pressure_plate", "minecraft:wooden_pressure_plate");
        m.put("minecraft:spruce_pressure_plate", "minecraft:wooden_pressure_plate");
        m.put("minecraft:birch_pressure_plate", "minecraft:wooden_pressure_plate");
        m.put("minecraft:jungle_pressure_plate", "minecraft:wooden_pressure_plate");
        m.put("minecraft:acacia_pressure_plate", "minecraft:wooden_pressure_plate");
        m.put("minecraft:dark_oak_pressure_plate", "minecraft:wooden_pressure_plate");

        // Stripped logs
        m.put("minecraft:stripped_oak_log", "minecraft:log");
        m.put("minecraft:stripped_spruce_log", "minecraft:log");
        m.put("minecraft:stripped_birch_log", "minecraft:log");
        m.put("minecraft:stripped_jungle_log", "minecraft:log");
        m.put("minecraft:stripped_acacia_log", "minecraft:log2");
        m.put("minecraft:stripped_dark_oak_log", "minecraft:log2");
        m.put("minecraft:stripped_cherry_log", "minecraft:log");
        m.put("minecraft:stripped_pale_oak_log", "minecraft:log");
        m.put("minecraft:stripped_mangrove_log", "minecraft:log");
        m.put("minecraft:stripped_warped_stem", "minecraft:log");
        m.put("minecraft:stripped_crimson_stem", "minecraft:log");
        m.put("minecraft:stripped_bamboo_block", "minecraft:log");

        // Copper -> iron_block
        m.put("minecraft:copper_block", "minecraft:iron_block");
        m.put("minecraft:exposed_copper", "minecraft:iron_block");
        m.put("minecraft:weathered_copper", "minecraft:iron_block");
        m.put("minecraft:oxidized_copper", "minecraft:iron_block");
        m.put("minecraft:waxed_copper_block", "minecraft:iron_block");
        m.put("minecraft:cut_copper", "minecraft:iron_block");

        // Deepslate
        m.put("minecraft:deepslate", "minecraft:stone");
        m.put("minecraft:cobbled_deepslate", "minecraft:cobblestone");
        m.put("minecraft:polished_deepslate", "minecraft:stone");
        m.put("minecraft:deepslate_bricks", "minecraft:stonebrick");
        m.put("minecraft:deepslate_tiles", "minecraft:stonebrick");
        m.put("minecraft:chiseled_deepslate", "minecraft:stonebrick");

        // Tuff
        m.put("minecraft:tuff", "minecraft:stone");
        m.put("minecraft:tuff_bricks", "minecraft:stonebrick");
        m.put("minecraft:polished_tuff", "minecraft:stone");

        // Calcite/Dripstone
        m.put("minecraft:calcite", "minecraft:stone");
        m.put("minecraft:dripstone_block", "minecraft:stone");

        // Mud
        m.put("minecraft:mud", "minecraft:dirt");
        m.put("minecraft:packed_mud", "minecraft:dirt");
        m.put("minecraft:mud_bricks", "minecraft:brick_block");

        // Resin
        m.put("minecraft:resin_block", "minecraft:brick_block");
        m.put("minecraft:resin_bricks", "minecraft:brick_block");
        m.put("minecraft:chiseled_resin_bricks", "minecraft:brick_block");

        // Blackstone
        m.put("minecraft:blackstone", "minecraft:stonebrick");
        m.put("minecraft:polished_blackstone", "minecraft:stonebrick");
        m.put("minecraft:polished_blackstone_bricks", "minecraft:stonebrick");
        m.put("minecraft:chiseled_polished_blackstone", "minecraft:stonebrick");

        // Basalt
        m.put("minecraft:basalt", "minecraft:stone");
        m.put("minecraft:polished_basalt", "minecraft:stone");
        m.put("minecraft:smooth_basalt", "minecraft:stone");

        // Amethyst
        m.put("minecraft:amethyst_block", "minecraft:glass");
        m.put("minecraft:budding_amethyst", "minecraft:glass");

        // Sculk
        m.put("minecraft:sculk", "minecraft:wool");
        m.put("minecraft:sculk_catalyst", "minecraft:wool");
        m.put("minecraft:sculk_shrieker", "minecraft:wool");
        m.put("minecraft:sculk_sensor", "minecraft:wool");
        m.put("minecraft:calibrated_sculk_sensor", "minecraft:wool");

        // Potted plants -> flower_pot (TE data synthesized by SchematicLitematica)
        m.put("minecraft:potted_oak_sapling", "minecraft:flower_pot");
        m.put("minecraft:potted_spruce_sapling", "minecraft:flower_pot");
        m.put("minecraft:potted_birch_sapling", "minecraft:flower_pot");
        m.put("minecraft:potted_jungle_sapling", "minecraft:flower_pot");
        m.put("minecraft:potted_acacia_sapling", "minecraft:flower_pot");
        m.put("minecraft:potted_dark_oak_sapling", "minecraft:flower_pot");
        m.put("minecraft:potted_cherry_sapling", "minecraft:flower_pot");
        m.put("minecraft:potted_pale_oak_sapling", "minecraft:flower_pot");
        m.put("minecraft:potted_mangrove_propagule", "minecraft:flower_pot");
        m.put("minecraft:potted_fern", "minecraft:flower_pot");
        m.put("minecraft:potted_dandelion", "minecraft:flower_pot");
        m.put("minecraft:potted_poppy", "minecraft:flower_pot");
        m.put("minecraft:potted_blue_orchid", "minecraft:flower_pot");
        m.put("minecraft:potted_allium", "minecraft:flower_pot");
        m.put("minecraft:potted_azure_bluet", "minecraft:flower_pot");
        m.put("minecraft:potted_red_tulip", "minecraft:flower_pot");
        m.put("minecraft:potted_orange_tulip", "minecraft:flower_pot");
        m.put("minecraft:potted_white_tulip", "minecraft:flower_pot");
        m.put("minecraft:potted_pink_tulip", "minecraft:flower_pot");
        m.put("minecraft:potted_oxeye_daisy", "minecraft:flower_pot");
        m.put("minecraft:potted_red_mushroom", "minecraft:flower_pot");
        m.put("minecraft:potted_brown_mushroom", "minecraft:flower_pot");
        m.put("minecraft:potted_dead_bush", "minecraft:flower_pot");
        m.put("minecraft:potted_cactus", "minecraft:flower_pot");
        m.put("minecraft:potted_bamboo", "minecraft:flower_pot");
        m.put("minecraft:potted_crimson_fungus", "minecraft:flower_pot");
        m.put("minecraft:potted_warped_fungus", "minecraft:flower_pot");
        m.put("minecraft:potted_crimson_roots", "minecraft:flower_pot");
        m.put("minecraft:potted_warped_roots", "minecraft:flower_pot");
        m.put("minecraft:potted_azalea_bush", "minecraft:flower_pot");
        m.put("minecraft:potted_flowering_azalea_bush", "minecraft:flower_pot");
        m.put("minecraft:potted_torchflower", "minecraft:flower_pot");
        m.put("minecraft:potted_cornflower", "minecraft:flower_pot");
        m.put("minecraft:potted_lily_of_the_valley", "minecraft:flower_pot");
        m.put("minecraft:potted_wither_rose", "minecraft:flower_pot");

        // Shulker boxes -> chest (to preserve contained items)
        m.put("minecraft:shulker_box", "minecraft:chest");
        for (String c : colors) {
            m.put("minecraft:" + c + "_shulker_box", "minecraft:chest");
        }

        // Barrel -> chest
        m.put("minecraft:barrel", "minecraft:chest");
    }
}
