package com.github.lunatrius.schematica.client.world;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.github.lunatrius.schematica.api.SchematicRegion;
import org.junit.Test;
import static org.junit.Assert.*;

public class SourceBlockPositionTest {
    private static final List<List<String>> GLOBALS = Arrays.asList(Collections.<String>emptyList(), Collections.singletonList("Y"),
        Arrays.asList("Y", "x"), Arrays.asList("z", "Y", "Y"), Arrays.asList("X", "z", "Y"));

    private static SubRegionPlacements model(SchematicOrigin pivot, SchematicRegion... regions) {
        ISchematic part = (ISchematic) Proxy.newProxyInstance(SourceBlockPositionTest.class.getClassLoader(), new Class<?>[] {ISchematic.class},
            (proxy, method, args) -> {
                if (method.getName().equals("getOrigin")) return pivot;
                throw new UnsupportedOperationException(method.getName());
            });
        ISchematic source = (ISchematic) Proxy.newProxyInstance(SourceBlockPositionTest.class.getClassLoader(), new Class<?>[] {ISchematic.class},
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "getWidth": return 20;
                    case "getHeight": return 6;
                    case "getLength": return 10;
                    case "getOrigin": return new SchematicOrigin(2, 1, -3);
                    case "getRegions": return Arrays.asList(regions);
                    case "getRegionSchematic": return part;
                    default: throw new UnsupportedOperationException(method.getName());
                }
            });
        return SubRegionPlacements.create(source);
    }

    @Test public void everyRegionCellRoundTripsThroughLocalAndGlobalTransforms() {
        SchematicOrigin origin = new SchematicOrigin(-37, 64, 1021);
        for (List<String> global : GLOBALS) for (int r = 0; r < 4; r++) for (int m = 0; m < 3; m++) {
            SubRegionPlacements base = model(new SchematicOrigin(1, -2, 3), new SchematicRegion("A", 0, 0, 0, 2, 3, 4), new SchematicRegion("B", 10, 0, 0, 12, 3, 4));
            SubRegionPlacements regions = base.replace(base.get("A").rotation(r).mirror(m).position(new SchematicOrigin(-5, 2, 7)));
            for (SubRegionPlacements.Region region : regions.regions()) {
                SchematicRegion box = region.box;
                for (int x = 0; x <= box.maxX - box.minX; x++) for (int y = 0; y <= box.maxY - box.minY; y++) for (int z = 0; z <= box.maxZ - box.minZ; z++) {
                    SchematicOrigin local = new SchematicOrigin(x, y, z);
                    SchematicOrigin world = SourceBlockPosition.world(region, local, origin, global);
                    SourceBlockPosition resolved = SourceBlockPosition.resolve(world, origin, global, regions);
                    assertNotNull(resolved);
                    assertEquals(region.name(), resolved.name());
                    assertArrayEquals(local.coordinates(), resolved.local.coordinates());
                }
            }
        }
    }

    @Test public void forwardMappingFillsExactlyTheTransformedRegionBounds() {
        SchematicOrigin origin = new SchematicOrigin(4, 70, -9);
        for (List<String> global : GLOBALS) for (int r = 0; r < 4; r++) {
            SubRegionPlacements base = model(new SchematicOrigin(2, 0, -1), new SchematicRegion("A", 0, 0, 0, 2, 3, 4));
            SubRegionPlacements.Region region = base.replace(base.get("A").rotation(r).mirror(r % 3)).get("A");
            SchematicRegion bounds = region.bounds();
            SchematicOrigin a = SubRegionPlacements.vector(new SchematicOrigin(bounds.minX, bounds.minY, bounds.minZ), global, false).atMinimum(origin.x, origin.y, origin.z);
            SchematicOrigin b = SubRegionPlacements.vector(new SchematicOrigin(bounds.maxX, bounds.maxY, bounds.maxZ), global, false).atMinimum(origin.x, origin.y, origin.z);
            SchematicRegion expected = new SchematicRegion("A", a.x, a.y, a.z, b.x, b.y, b.z);
            java.util.Set<List<Integer>> seen = new java.util.HashSet<>();
            for (int x = 0; x <= 2; x++) for (int y = 0; y <= 3; y++) for (int z = 0; z <= 4; z++) {
                SchematicOrigin world = SourceBlockPosition.world(region, new SchematicOrigin(x, y, z), origin, global);
                assertTrue(expected.contains(world.x, world.y, world.z));
                assertTrue(seen.add(Arrays.asList(world.x, world.y, world.z)));
            }
            assertEquals(3 * 4 * 5, seen.size());
        }
    }

    @Test public void laterEnabledRegionOwnsOverlapsEvenWhenItIsNotRendered() {
        SubRegionPlacements base = model(SchematicOrigin.ZERO, new SchematicRegion("A", 0, 0, 0, 3, 0, 0), new SchematicRegion("B", 10, 0, 0, 13, 0, 0));
        SubRegionPlacements overlapping = base.replace(base.get("B").position(base.get("A").position));
        SchematicOrigin origin = new SchematicOrigin(100, 50, 100);
        SchematicOrigin world = SourceBlockPosition.world(overlapping.get("A"), new SchematicOrigin(2, 0, 0), origin, Collections.<String>emptyList());
        assertEquals("B", SourceBlockPosition.resolve(world, origin, Collections.<String>emptyList(), overlapping).name());
        SubRegionPlacements hidden = overlapping.replace(overlapping.get("B").rendering(false));
        assertEquals("B", SourceBlockPosition.resolve(world, origin, Collections.<String>emptyList(), hidden).name());
        SubRegionPlacements disabled = overlapping.replace(overlapping.get("B").enabled(false));
        SourceBlockPosition owner = SourceBlockPosition.resolve(world, origin, Collections.<String>emptyList(), disabled);
        assertEquals("A", owner.name());
        assertArrayEquals(new int[] {2, 0, 0}, owner.local.coordinates());
        assertNull(SourceBlockPosition.resolve(new SchematicOrigin(0, 0, 0), origin, Collections.<String>emptyList(), disabled));
    }

    @Test public void inverseOperationsUndoEveryRotationAndMirrorSequence() {
        for (List<String> global : GLOBALS) {
            List<String> inverse = SourceBlockPosition.inverse(global);
            for (int x = -2; x <= 2; x++) for (int y = -2; y <= 2; y++) for (int z = -2; z <= 2; z++) {
                SchematicOrigin point = new SchematicOrigin(x, y, z);
                assertArrayEquals(point.coordinates(), SubRegionPlacements.vector(SubRegionPlacements.vector(point, global, false), inverse, false).coordinates());
            }
        }
        assertEquals(Arrays.asList("Y", "Y", "Y", "x"), SourceBlockPosition.inverse(Arrays.asList("x", "Y")));
    }
}
