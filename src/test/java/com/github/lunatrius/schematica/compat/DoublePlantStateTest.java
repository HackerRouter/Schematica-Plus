package com.github.lunatrius.schematica.compat;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class DoublePlantStateTest {
    private static final String[] PLANTS = {
        "sunflower", "lilac", "tall_grass", "large_fern", "rose_bush", "peony"
    };

    @Test public void lowerHalvesRetainEveryModernPlantSpecies() {
        for (int metadata = 0; metadata < PLANTS.length; metadata++) {
            String state = "minecraft:" + PLANTS[metadata] + "[half=lower]";
            assertEquals(state, metadata, BlockStateTranslator.instance().translate(state).metadata);
        }
    }

    @Test public void upperHalvesUseTheLegacyUpperHalfBit() {
        for (String plant : PLANTS) {
            String state = "minecraft:" + plant + "[half=upper]";
            assertEquals(state, 8, BlockStateTranslator.instance().translate(state).metadata);
        }
    }

    @Test public void propertylessPlantsAndIgnoredPropertiesKeepTheirSpecies() {
        for (int metadata = 0; metadata < PLANTS.length; metadata++) {
            String plant = PLANTS[metadata];
            assertEquals(plant, metadata, BlockStateTranslator.instance().translate(plant).metadata);
            String state = plant + "[waterlogged=false,half=lower]";
            assertEquals(state, metadata, BlockStateTranslator.instance().translate(state).metadata);
        }
    }
}
