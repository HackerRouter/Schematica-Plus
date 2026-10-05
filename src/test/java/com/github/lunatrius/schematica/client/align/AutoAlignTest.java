package com.github.lunatrius.schematica.client.align;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.github.lunatrius.schematica.util.SchematicTransform;

import org.junit.Test;

import static org.junit.Assert.*;

public class AutoAlignTest {
    private static final String STONE = "stone", PLANKS = "planks", GLASS = "glass", GOLD = "gold", TORCH = "torch";

    /** A 7 x 5 x 4 house: stone floor, plank walls with glass, a gold block in one corner and a torch. */
    private static final class House implements AutoAlign.Source {
        @Override public int width() { return 7; }
        @Override public int height() { return 5; }
        @Override public int length() { return 4; }
        @Override public Object block(int x, int y, int z) {
            if (y == 0) return STONE;
            if (x == 0 && z == 0 && y == 1) return GOLD;
            if (x == 5 && z == 1 && y == 1) return TORCH;
            boolean wall = x == 0 || x == 6 || z == 0 || z == 3;
            if (!wall || y == 4) return null;
            return y == 2 && (x == 3 || z == 2) ? GLASS : PLANKS;
        }
    }

    @Test public void cellsFollowTheSchematicTransforms() {
        int w = 7, l = 4;
        for (int x = 0; x < w; x++) for (int z = 0; z < l; z++) {
            double[] rotated = SchematicTransform.point('Y', x, 0, z, w - 1, 0, l - 1);
            assertArrayEquals(new int[] {(int) rotated[0], (int) rotated[2], l, w}, AutoAlign.cell("Y", x, z, w, l));
            double[] mirrored = SchematicTransform.point('x', x, 0, z, w - 1, 0, l - 1);
            assertArrayEquals(new int[] {(int) mirrored[0], (int) mirrored[2], w, l}, AutoAlign.cell("x", x, z, w, l));
        }
        assertEquals(-3, AutoAlign.x(AutoAlign.pack(-3, 70, 1_000_000)));
        assertEquals(70, AutoAlign.y(AutoAlign.pack(-3, 70, 1_000_000)));
        assertEquals(1_000_000, AutoAlign.z(AutoAlign.pack(-3, 70, 1_000_000)));
    }

    @Test public void findsATurnedAndMirroredBuildAmongTerrain() {
        House house = new House();
        for (String steps : new String[] {"", "YY", "Yx", "YYYx"}) {
            Map<Long, Object> world = new HashMap<>();
            Random random = new Random(7);
            for (int x = -40; x < 40; x++) for (int z = -40; z < 40; z++) {
                world.put(AutoAlign.pack(x, 63, z), STONE);
                if (random.nextInt(50) == 0) world.put(AutoAlign.pack(x, 64, z), PLANKS);
            }
            int ox = 11, oy = 64, oz = -17;
            for (int y = 0; y < house.height(); y++) for (int z = 0; z < house.length(); z++) for (int x = 0; x < house.width(); x++) {
                Object block = house.block(x, y, z);
                int[] moved = AutoAlign.cell(steps, x, z, house.width(), house.length());
                long key = AutoAlign.pack(ox + moved[0], oy + y, oz + moved[1]);
                if (block == null) world.remove(key);
                else world.put(key, block);
            }
            AutoAlign.Target target = new AutoAlign.Target() {
                @Override public Object block(int x, int y, int z) { return world.get(AutoAlign.pack(x, y, z)); }
                @Override public boolean loaded(int x, int z) { return x >= -40 && x < 40 && z >= -40 && z < 40; }
            };

            AutoAlign.Plan plan = AutoAlign.plan(house, new HashSet<Object>(Collections.singleton(STONE)));
            // gold and the torch occur once each: the rarest kinds come first
            assertEquals(new HashSet<Object>(java.util.Arrays.asList(GOLD, TORCH)), new HashSet<>(plan.ids.subList(0, 2)));
            assertFalse(plan.ids.contains(STONE));
            List<long[]> hits = new ArrayList<>();
            for (Object id : plan.ids) {
                List<Long> found = new ArrayList<>();
                for (Map.Entry<Long, Object> entry : world.entrySet()) if (entry.getValue() == id) found.add(entry.getKey());
                long[] array = new long[found.size()];
                for (int i = 0; i < array.length; i++) array[i] = found.get(i);
                hits.add(array);
            }
            AutoAlign.Result best = null;
            for (AutoAlign.Candidate candidate : AutoAlign.vote(house.width(), house.length(), plan, hits)) {
                AutoAlign.Result result = AutoAlign.score(house, target, plan, candidate);
                if (result.better(best)) best = result;
            }
            assertNotNull(steps, best);
            assertTrue(steps, best.confident());
            assertEquals(steps, 100, best.percent());
            assertEquals(steps, steps, best.candidate.steps);
            assertEquals(ox, best.candidate.x);
            assertEquals(oy, best.candidate.y);
            assertEquals(oz, best.candidate.z);
            assertFalse(AutoAlign.score(house, target, plan, AutoAlign.current(0, 64, 0)).confident());
        }
    }
}
