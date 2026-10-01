package com.github.lunatrius.schematica.client.selection;

import org.junit.Test;

import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Area;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;

import static org.junit.Assert.*;

public class SelectionMovementTest {
    @Test public void movingAllBoxesPreservesSelectedCornerAndManualOriginAtomically() {
        AreaSelectionLibrary library = AreaSelectionLibrary.fromJson(null); Area area = library.selected();
        library.setPoints(area, new Vector3i(-20, 60, -8), new Vector3i(-12, 70, -4));
        AreaSelectionLibrary.Box box = library.addBox(area, "Other", new Vector3i(5, 245, 8), new Vector3i(7, 250, 12));
        library.setOrigin(area, new Vector3i(-21, 59, -9)); library.selectCorner(area, box, AreaSelectionLibrary.Corner.SECOND);
        String before = library.toJson().toString();
        assertThrows(IllegalArgumentException.class, () -> library.moveEntire(area, 1, 20, 1));
        assertEquals(before, library.toJson().toString());
        library.moveEntire(area, -2, 3, -4);
        assertEquals(new Vector3i(3, 248, 4), box.first());
        assertEquals(new Vector3i(-23, 62, -13), area.manualOrigin());
        assertSame(box, area.selectedBox()); assertEquals(AreaSelectionLibrary.Corner.SECOND, area.selectedCorner());
    }
    @Test public void resizePreservesCornerOrientationAndDoesNotInvertOneBlockBoxes() {
        AreaSelectionLibrary library = AreaSelectionLibrary.fromJson(null); Area area = library.selected();
        library.setPoints(area, new Vector3i(7, 70, 6), new Vector3i(5, 64, 2));
        library.growSelected(area, 1);
        assertEquals(new Vector3i(8, 71, 7), area.first()); assertEquals(new Vector3i(4, 63, 1), area.second());
        library.growSelected(area, -10);
        assertEquals(new Vector3i(6, 67, 4), area.first()); assertEquals(area.first(), area.second());
    }
}
