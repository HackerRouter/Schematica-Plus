package com.github.lunatrius.schematica.api;

import com.github.lunatrius.schematica.client.gui.placement.PlacementTransform;
import org.junit.Test;
import static org.junit.Assert.*;

public class SchematicOriginTest {
    @Test public void everyTransformKeepsWorldOriginAndRotatesBlocksAroundIt() {
        SchematicOrigin local = new SchematicOrigin(-4, 8, 12);
        SchematicOrigin world = new SchematicOrigin(-100, 70, 15);
        for (String steps : new String[] {"X", "Y", "Z", "x", "y", "z", "YxzYYz", "XYZxyzXYZ"}) {
            SchematicOrigin next = PlacementTransform.transformOrigin(local, 3, 5, 7, steps);
            SchematicOrigin min = next.minimumAt(world);
            assertArrayEquals(world.coordinates(), next.atMinimum(min.x, min.y, min.z).coordinates());
            SchematicOrigin block = PlacementTransform.transformOrigin(new SchematicOrigin(1, 2, 3), 3, 5, 7, steps);
            double[] vector = {1 - local.x, 2 - local.y, 3 - local.z};
            for (char op : steps.toCharArray()) vector = com.github.lunatrius.schematica.util.SchematicTransform.point(op,
                vector[0], vector[1], vector[2], 0, 0, 0);
            assertArrayEquals(new int[] {world.x + (int) vector[0], world.y + (int) vector[1], world.z + (int) vector[2]},
                block.atMinimum(min.x, min.y, min.z).coordinates());
        }
    }

    @Test public void reloadAndLegacyRestoreUseTheCorrectCoordinateMeaning() {
        SchematicOrigin previous = new SchematicOrigin(2, 1, -4);
        SchematicOrigin next = new SchematicOrigin(10, 3, -7);
        SchematicOrigin world = previous.atMinimum(100, 60, -200);
        assertArrayEquals(new int[] {92, 58, -197}, next.restoredMinimum(100, 60, -200, world.coordinates()).coordinates());
        assertArrayEquals(new int[] {100, 60, -200}, next.restoredMinimum(100, 60, -200, null).coordinates());
        assertThrows(IllegalArgumentException.class, () -> next.restoredMinimum(0, 0, 0, new int[2]));
    }

    @Test public void completeTurnsAndMirrorsRestoreOutsideOriginsWithoutMutation() {
        SchematicOrigin origin = new SchematicOrigin(-50, 60, 40);
        for (String sequence : new String[] {"XXXX", "YYYY", "ZZZZ", "xx", "yy", "zz"}) {
            assertArrayEquals(origin.coordinates(), PlacementTransform.transformOrigin(origin, 3, 5, 7, sequence).coordinates());
        }
        int[] copy = origin.coordinates(); copy[0] = 0;
        assertEquals(-50, origin.x);
        assertThrows(ArithmeticException.class, () -> new SchematicOrigin(Integer.MIN_VALUE, 0, 0).transform('x', 3, 5, 7));
        assertThrows(ArithmeticException.class, () -> origin.atMinimum(Integer.MIN_VALUE, 0, 0));
    }
}
