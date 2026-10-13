package com.github.lunatrius.schematica.compat;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class VanillaMushroomStateTest {
    private static Map<String, String> faces(String... names) {
        Map<String, String> properties = new HashMap<>();
        for (String name : names) properties.put(name, "true");
        return properties;
    }

    @Test public void capSurfacePreservesTopEdgesAndCorners() {
        assertEquals(0, VanillaBlockMappings.mushroomMetadata(false, faces()));
        assertEquals(5, VanillaBlockMappings.mushroomMetadata(false, faces("up")));
        assertEquals(2, VanillaBlockMappings.mushroomMetadata(false, faces("up", "north")));
        assertEquals(9, VanillaBlockMappings.mushroomMetadata(false, faces("up", "south", "east")));
        assertEquals(14, VanillaBlockMappings.mushroomMetadata(false, faces("up", "down", "north", "south", "east", "west")));
    }

    @Test public void stemsKeepBarkOnTheSidesOrEveryFace() {
        assertEquals(10, VanillaBlockMappings.mushroomMetadata(true, faces("north", "south", "east", "west")));
        assertEquals(15, VanillaBlockMappings.mushroomMetadata(true, faces("up", "down", "north", "south", "east", "west")));
    }

    @Test public void unrepresentableFacesAreReported() {
        assertEquals(-1, VanillaBlockMappings.mushroomMetadata(false, faces("down")));
        assertEquals(-1, VanillaBlockMappings.mushroomMetadata(false, faces("up", "north", "south")));
        assertEquals(-1, VanillaBlockMappings.mushroomMetadata(true, faces("up")));
    }
}
