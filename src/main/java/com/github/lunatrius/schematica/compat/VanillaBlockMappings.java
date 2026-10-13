package com.github.lunatrius.schematica.compat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class VanillaBlockMappings {

    static final Map<String, String> BLOCK_RENAMES = new HashMap<String, String>();
    static final Map<String, Integer> PROPERTY_TO_META = new HashMap<String, Integer>();

    private static final Set<String> COLOR_SPLIT_SUFFIXES = new HashSet<String>(Arrays.asList(
        "wool", "carpet", "terracotta", "stained_glass", "stained_glass_pane",
        "concrete", "concrete_powder", "shulker_box", "glazed_terracotta", "bed",
        "banner", "wall_banner"
    ));

    static {
        VanillaBlockRenames.init();
        VanillaPropertyMeta.init();
    }

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

    static void p(String block, String props, int meta) {
        PROPERTY_TO_META.put(block + "[" + props + "]", meta);
    }

    /** Returns -1 for face combinations that 1.7.10 cannot represent. */
    static int mushroomMetadata(boolean stem, Map<String, String> properties) {
        String[] faces = {"up", "down", "north", "south", "west", "east"};
        int mask = 0;
        for (int i = 0; i < faces.length; i++) if ("true".equals(properties.get(faces[i]))) mask |= 1 << i;
        if (mask == 0) return 0;
        if (stem) return mask == 60 ? 10 : mask == 63 ? 15 : -1;
        if (mask == 63) return 14;
        int[] caps = {0, 21, 5, 37, 17, 1, 33, 25, 9, 41};
        for (int meta = 1; meta <= 9; meta++) if (mask == caps[meta]) return meta;
        return -1;
    }

    static void registerColorMeta(String block) {
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

    public static int getColorMetaFromModernName(String modernName) {
        String name = modernName;
        if (name.startsWith("minecraft:")) {
            name = name.substring(10);
        }
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
}
