package com.github.lunatrius.schematica.world.storage;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.tileentity.TileEntity;

import com.github.lunatrius.schematica.api.ISchematic;
import org.junit.Test;
import static org.junit.Assert.*;

public class SchematicCopiesTest {
    private static final class Grid {
        final int[][][] values;
        final List<TileEntity> tiles = new ArrayList<>();
        final ISchematic schematic;

        Grid(int width, int height, int length) {
            values = new int[width][height][length];
            schematic = (ISchematic) Proxy.newProxyInstance(ISchematic.class.getClassLoader(), new Class<?>[] {ISchematic.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getWidth": return width;
                        case "getHeight": return height;
                        case "getLength": return length;
                        case "getBlock": return null;
                        case "getBlockMetadata": return values[(int) args[0]][(int) args[1]][(int) args[2]];
                        case "setBlock": values[(int) args[0]][(int) args[1]][(int) args[2]] = (int) args[4]; return true;
                        case "getTileEntities": return tiles;
                        case "getEntities": return Collections.emptyList();
                        default: throw new UnsupportedOperationException(method.getName());
                    }
                });
        }
    }

    @Test public void overlapCanBeSeparatedWithoutLosingEitherSourcesContents() {
        Grid a = new Grid(2, 1, 1), b = new Grid(2, 1, 1), merged = new Grid(2, 1, 1);
        a.values[0][0][0] = 3;
        b.values[1][0][0] = 7;
        SchematicCopies.overlay(a.schematic, merged.schematic, 0, 0, 0, true);
        assertEquals(3, merged.values[0][0][0]);
        SchematicCopies.overlay(b.schematic, merged.schematic, 0, 0, 0, true);
        assertEquals(0, merged.values[0][0][0]);
        assertEquals(7, merged.values[1][0][0]);
        Grid separated = new Grid(4, 2, 3);
        SchematicCopies.overlay(a.schematic, separated.schematic, 0, 0, 0, true);
        SchematicCopies.overlay(b.schematic, separated.schematic, 2, 1, 2, true);
        assertEquals(3, separated.values[0][0][0]);
        assertEquals(7, separated.values[3][1][2]);
        assertEquals(0, separated.values[2][1][2]);
        separated.values[0][0][0] = 9;
        assertEquals(3, a.values[0][0][0]);
        assertEquals(7, b.values[1][0][0]);
    }

    @Test public void replacingAnEmptyRegionClearsOnlyCoveredTileEntities() {
        Grid target = new Grid(4, 3, 4), empty = new Grid(2, 1, 2);
        TileEntity covered = new TileEntity(), outside = new TileEntity();
        covered.xCoord = 2; covered.yCoord = 1; covered.zCoord = 2;
        outside.xCoord = 3; outside.yCoord = 1; outside.zCoord = 2;
        target.tiles.add(covered); target.tiles.add(outside);
        SchematicCopies.overlay(empty.schematic, target.schematic, 1, 1, 1, false);
        assertEquals(Collections.singletonList(outside), target.tiles);
    }

    @Test public void invalidOverlayDoesNotPartiallyWriteTheTarget() {
        Grid source = new Grid(2, 1, 1), target = new Grid(2, 1, 1);
        source.values[0][0][0] = 8;
        for (int offset : new int[] {-1, 1, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> SchematicCopies.overlay(source.schematic, target.schematic, offset, 0, 0, true));
        }
        assertThrows(IllegalArgumentException.class, () -> SchematicCopies.overlay(source.schematic, source.schematic, 0, 0, 0, true));
        assertEquals(0, target.values[0][0][0]);
    }
}
