package com.github.lunatrius.schematica.compat;

import java.util.Map;

/**
 * Property-to-metadata mappings for 1.7.10 blocks.
 * Called by VanillaBlockMappings static initializer.
 *
 * IMPORTANT: Property strings here must match what BlockStateTranslator produces
 * AFTER stripping IGNORED_PROPERTIES. For example, fence_gate entries use only
 * facing+open (not in_wall/powered, which are stripped).
 */
final class VanillaPropertyMeta {

    private VanillaPropertyMeta() {}

    static void init() {
        initVariants();
        initDirectional();
        initRedstone();
        initMisc();
    }

    private static void p(String block, String props, int meta) {
        VanillaBlockMappings.p(block, props, meta);
    }

    private static void initVariants() {
        final Map<String, Integer> m = VanillaBlockMappings.PROPERTY_TO_META;

        // Stone variants
        p("minecraft:stone", "variant=stone", 0);
        p("minecraft:stone", "variant=granite", 1);
        p("minecraft:stone", "variant=smooth_granite", 2);
        p("minecraft:stone", "variant=diorite", 3);
        p("minecraft:stone", "variant=smooth_diorite", 4);
        p("minecraft:stone", "variant=andesite", 5);
        p("minecraft:stone", "variant=smooth_andesite", 6);

        // Dirt / Sand
        p("minecraft:dirt", "variant=dirt", 0);
        p("minecraft:dirt", "variant=coarse_dirt", 1);
        p("minecraft:dirt", "variant=podzol", 2);
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
        String[] logVariants = {"oak", "spruce", "birch", "jungle"};
        String[] axes = {"y", "x", "z", "none"};
        int[] axisOffsets = {0, 4, 8, 12};
        for (int a = 0; a < axes.length; a++) {
            for (int v = 0; v < logVariants.length; v++) {
                p("minecraft:log", "axis=" + axes[a] + ",variant=" + logVariants[v], axisOffsets[a] + v);
            }
        }
        // Log2
        for (int a = 0; a < axes.length; a++) {
            p("minecraft:log2", "axis=" + axes[a] + ",variant=acacia", axisOffsets[a]);
            p("minecraft:log2", "axis=" + axes[a] + ",variant=dark_oak", axisOffsets[a] + 1);
        }

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

        // Color blocks
        VanillaBlockMappings.registerColorMeta("minecraft:wool");
        VanillaBlockMappings.registerColorMeta("minecraft:stained_hardened_clay");
        VanillaBlockMappings.registerColorMeta("minecraft:stained_glass");
        VanillaBlockMappings.registerColorMeta("minecraft:stained_glass_pane");
        VanillaBlockMappings.registerColorMeta("minecraft:carpet");

        // Sponge
        p("minecraft:sponge", "wet=false", 0);
        p("minecraft:sponge", "wet=true", 1);

        // Wooden slabs
        String[] woodVariants = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"};
        for (int i = 0; i < woodVariants.length; i++) {
            p("minecraft:wooden_slab", "half=bottom,variant=" + woodVariants[i], i);
            p("minecraft:wooden_slab", "half=top,variant=" + woodVariants[i], 8 + i);
        }

        // Stone slabs
        String[] stoneSlabVariants = {"stone", "sandstone", null, "cobblestone", "brick", "stone_brick", "nether_brick", "quartz"};
        for (int i = 0; i < stoneSlabVariants.length; i++) {
            if (stoneSlabVariants[i] == null) continue;
            p("minecraft:stone_slab", "half=bottom,variant=" + stoneSlabVariants[i], i);
            p("minecraft:stone_slab", "half=top,variant=" + stoneSlabVariants[i], 8 + i);
        }

        // Stonebrick
        m.put("minecraft:stonebrick", 0);
        p("minecraft:stonebrick", "variant=stonebrick", 0);
        p("minecraft:stonebrick", "variant=mossy_stonebrick", 1);
        p("minecraft:stonebrick", "variant=cracked_stonebrick", 2);
        p("minecraft:stonebrick", "variant=chiseled_stonebrick", 3);

        // Monster egg
        p("minecraft:monster_egg", "variant=stone", 0);
        p("minecraft:monster_egg", "variant=cobblestone", 1);
        p("minecraft:monster_egg", "variant=stone_brick", 2);
        p("minecraft:monster_egg", "variant=mossy_brick", 3);
        p("minecraft:monster_egg", "variant=cracked_brick", 4);
        p("minecraft:monster_egg", "variant=chiseled_brick", 5);

        // Quartz
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

        // Hay bale
        p("minecraft:hay_block", "axis=y", 0);
        p("minecraft:hay_block", "axis=x", 4);
        p("minecraft:hay_block", "axis=z", 8);

        // Default meta for common blocks
        m.put("minecraft:stone", 0);
        m.put("minecraft:dirt", 0);
        m.put("minecraft:sand", 0);
        m.put("minecraft:sandstone", 0);
        m.put("minecraft:red_sandstone", 0);
        m.put("minecraft:planks", 0);
        m.put("minecraft:log", 0);
        m.put("minecraft:log2", 0);
        m.put("minecraft:leaves", 0);
        m.put("minecraft:leaves2", 0);
        m.put("minecraft:sapling", 0);
        m.put("minecraft:quartz_block", 0);
        m.put("minecraft:prismarine", 0);
        m.put("minecraft:cobblestone_wall", 0);
        m.put("minecraft:monster_egg", 0);
        m.put("minecraft:red_flower", 0);
        m.put("minecraft:double_plant", 0);
        m.put("minecraft:sponge", 0);
        m.put("minecraft:nether_brick", 0);
        m.put("minecraft:brick_block", 0);
        m.put("minecraft:wool", 0);
        m.put("minecraft:carpet", 0);
        m.put("minecraft:stained_hardened_clay", 0);
        m.put("minecraft:stained_glass", 0);
        m.put("minecraft:stained_glass_pane", 0);
        m.put("minecraft:hardened_clay", 0);
        m.put("minecraft:farmland", 0);
    }

    private static void initDirectional() {
        // Torch
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
        p("minecraft:unlit_redstone_torch", "facing=up", 5);
        p("minecraft:unlit_redstone_torch", "facing=east", 1);
        p("minecraft:unlit_redstone_torch", "facing=west", 2);
        p("minecraft:unlit_redstone_torch", "facing=south", 3);
        p("minecraft:unlit_redstone_torch", "facing=north", 4);

        // Stairs
        String[] stairs = {
            "minecraft:oak_stairs", "minecraft:stone_stairs", "minecraft:brick_stairs",
            "minecraft:stone_brick_stairs", "minecraft:nether_brick_stairs", "minecraft:sandstone_stairs",
            "minecraft:spruce_stairs", "minecraft:birch_stairs", "minecraft:jungle_stairs",
            "minecraft:quartz_stairs", "minecraft:acacia_stairs", "minecraft:dark_oak_stairs",
            "minecraft:red_sandstone_stairs"
        };
        for (String s : stairs) {
            p(s, "facing=east,half=bottom,shape=straight", 0);
            p(s, "facing=west,half=bottom,shape=straight", 1);
            p(s, "facing=south,half=bottom,shape=straight", 2);
            p(s, "facing=north,half=bottom,shape=straight", 3);
            p(s, "facing=east,half=top,shape=straight", 4);
            p(s, "facing=west,half=top,shape=straight", 5);
            p(s, "facing=south,half=top,shape=straight", 6);
            p(s, "facing=north,half=top,shape=straight", 7);
        }

        // Facing blocks (furnace, chest, etc.) - horizontal only
        String[] horizontalFacingBlocks = {
            "minecraft:furnace", "minecraft:lit_furnace",
            "minecraft:chest", "minecraft:trapped_chest",
            "minecraft:ender_chest", "minecraft:ladder", "minecraft:wall_sign"
        };
        for (String fb : horizontalFacingBlocks) {
            p(fb, "facing=north", 2);
            p(fb, "facing=south", 3);
            p(fb, "facing=west", 4);
            p(fb, "facing=east", 5);
        }

        // Dispenser / Dropper: support all 6 facings (down=0, up=1, north=2, south=3, west=4, east=5)
        String[] sixFacingBlocks = {"minecraft:dispenser", "minecraft:dropper"};
        for (String fb : sixFacingBlocks) {
            p(fb, "facing=down", 0);
            p(fb, "facing=up", 1);
            p(fb, "facing=north", 2);
            p(fb, "facing=south", 3);
            p(fb, "facing=west", 4);
            p(fb, "facing=east", 5);
        }

        // Piston
        String[] pistons = {"minecraft:piston", "minecraft:sticky_piston"};
        String[] pFacings = {"down", "up", "north", "south", "west", "east"};
        for (String piston : pistons) {
            for (int i = 0; i < pFacings.length; i++) {
                p(piston, "extended=false,facing=" + pFacings[i], i);
                p(piston, "extended=true,facing=" + pFacings[i], 8 + i);
            }
        }

        // Piston head: facing + type (normal=0, sticky=8)
        for (int i = 0; i < pFacings.length; i++) {
            p("minecraft:piston_head", "facing=" + pFacings[i] + ",type=normal", i);
            p("minecraft:piston_head", "facing=" + pFacings[i] + ",type=sticky", 8 + i);
        }

        // Doors (only 1.7.10 door types; modern variants are renamed to wooden_door by VanillaBlockRenames)
        String[] doors = {"minecraft:wooden_door", "minecraft:iron_door"};
        for (String door : doors) {
            p(door, "facing=east,half=lower,open=false", 0);
            p(door, "facing=south,half=lower,open=false", 1);
            p(door, "facing=west,half=lower,open=false", 2);
            p(door, "facing=north,half=lower,open=false", 3);
            p(door, "facing=east,half=lower,open=true", 4);
            p(door, "facing=south,half=lower,open=true", 5);
            p(door, "facing=west,half=lower,open=true", 6);
            p(door, "facing=north,half=lower,open=true", 7);
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

        // Fence gates - ONLY facing+open (in_wall and powered are stripped by IGNORED_PROPERTIES)
        // Only 1.7.10 fence_gate; modern variants are renamed by VanillaBlockRenames
        String[] fenceGates = {"minecraft:fence_gate"};
        for (String fg : fenceGates) {
            p(fg, "facing=south,open=false", 0);
            p(fg, "facing=west,open=false", 1);
            p(fg, "facing=north,open=false", 2);
            p(fg, "facing=east,open=false", 3);
            p(fg, "facing=south,open=true", 4);
            p(fg, "facing=west,open=true", 5);
            p(fg, "facing=north,open=true", 6);
            p(fg, "facing=east,open=true", 7);
        }

        // Bed (occupied is kept via KEEP_PROPERTIES)
        // In 1.7.10: bits 0-1 = facing, bit 2 = occupied, bit 3 = part(head)
        p("minecraft:bed", "facing=south,occupied=false,part=foot", 0);
        p("minecraft:bed", "facing=west,occupied=false,part=foot", 1);
        p("minecraft:bed", "facing=north,occupied=false,part=foot", 2);
        p("minecraft:bed", "facing=east,occupied=false,part=foot", 3);
        p("minecraft:bed", "facing=south,occupied=true,part=foot", 4);
        p("minecraft:bed", "facing=west,occupied=true,part=foot", 5);
        p("minecraft:bed", "facing=north,occupied=true,part=foot", 6);
        p("minecraft:bed", "facing=east,occupied=true,part=foot", 7);
        p("minecraft:bed", "facing=south,occupied=false,part=head", 8);
        p("minecraft:bed", "facing=west,occupied=false,part=head", 9);
        p("minecraft:bed", "facing=north,occupied=false,part=head", 10);
        p("minecraft:bed", "facing=east,occupied=false,part=head", 11);
        p("minecraft:bed", "facing=south,occupied=true,part=head", 12);
        p("minecraft:bed", "facing=west,occupied=true,part=head", 13);
        p("minecraft:bed", "facing=north,occupied=true,part=head", 14);
        p("minecraft:bed", "facing=east,occupied=true,part=head", 15);

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

        // Pumpkin / Jack o lantern
        p("minecraft:pumpkin", "facing=south", 0);
        p("minecraft:pumpkin", "facing=west", 1);
        p("minecraft:pumpkin", "facing=north", 2);
        p("minecraft:pumpkin", "facing=east", 3);
        p("minecraft:lit_pumpkin", "facing=south", 0);
        p("minecraft:lit_pumpkin", "facing=west", 1);
        p("minecraft:lit_pumpkin", "facing=north", 2);
        p("minecraft:lit_pumpkin", "facing=east", 3);

        // Skull
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
    }

    private static void initRedstone() {
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

        // Redstone wire
        for (int i = 0; i <= 15; i++) {
            p("minecraft:redstone_wire", "power=" + i, i);
        }

        // Repeater
        for (int delay = 1; delay <= 4; delay++) {
            p("minecraft:unpowered_repeater", "delay=" + delay + ",facing=south", (delay - 1) * 4);
            p("minecraft:unpowered_repeater", "delay=" + delay + ",facing=west", (delay - 1) * 4 + 1);
            p("minecraft:unpowered_repeater", "delay=" + delay + ",facing=north", (delay - 1) * 4 + 2);
            p("minecraft:unpowered_repeater", "delay=" + delay + ",facing=east", (delay - 1) * 4 + 3);
            p("minecraft:powered_repeater", "delay=" + delay + ",facing=south", (delay - 1) * 4);
            p("minecraft:powered_repeater", "delay=" + delay + ",facing=west", (delay - 1) * 4 + 1);
            p("minecraft:powered_repeater", "delay=" + delay + ",facing=north", (delay - 1) * 4 + 2);
            p("minecraft:powered_repeater", "delay=" + delay + ",facing=east", (delay - 1) * 4 + 3);
        }

        // Comparator
        p("minecraft:unpowered_comparator", "facing=south,mode=compare", 0);
        p("minecraft:unpowered_comparator", "facing=west,mode=compare", 1);
        p("minecraft:unpowered_comparator", "facing=north,mode=compare", 2);
        p("minecraft:unpowered_comparator", "facing=east,mode=compare", 3);
        p("minecraft:unpowered_comparator", "facing=south,mode=subtract", 4);
        p("minecraft:unpowered_comparator", "facing=west,mode=subtract", 5);
        p("minecraft:unpowered_comparator", "facing=north,mode=subtract", 6);
        p("minecraft:unpowered_comparator", "facing=east,mode=subtract", 7);
        p("minecraft:powered_comparator", "facing=south,mode=compare", 0);
        p("minecraft:powered_comparator", "facing=west,mode=compare", 1);
        p("minecraft:powered_comparator", "facing=north,mode=compare", 2);
        p("minecraft:powered_comparator", "facing=east,mode=compare", 3);
        p("minecraft:powered_comparator", "facing=south,mode=subtract", 4);
        p("minecraft:powered_comparator", "facing=west,mode=subtract", 5);
        p("minecraft:powered_comparator", "facing=north,mode=subtract", 6);
        p("minecraft:powered_comparator", "facing=east,mode=subtract", 7);

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
    }

    private static void initMisc() {
        // Daylight detector (power 0-15)
        for (int i = 0; i <= 15; i++) {
            p("minecraft:daylight_detector", "power=" + i, i);
            p("minecraft:daylight_detector_inverted", "power=" + i, i);
        }

        // Crops
        String[] crops = {"minecraft:wheat", "minecraft:carrots", "minecraft:potatoes"};
        for (String crop : crops) {
            for (int i = 0; i <= 7; i++) {
                p(crop, "age=" + i, i);
            }
        }
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

        // Cactus, sugar cane
        for (int i = 0; i <= 15; i++) {
            p("minecraft:cactus", "age=" + i, i);
            p("minecraft:reeds", "age=" + i, i);
        }
        // Snow layer
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
        // Farmland
        for (int i = 0; i <= 7; i++) {
            p("minecraft:farmland", "moisture=" + i, i);
        }

        // Water / Lava
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
    }
}
