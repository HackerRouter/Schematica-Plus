package com.github.lunatrius.schematica.tool;

import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class WorldEditJobTest {
    @Test public void rejectsOutOfWorldPasteBeforeCapturingData() {
        UUID player = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> new WorldEditJob(player, 0,
            WorldEditJob.Kind.PASTE, 0, 255, 0, 2, 2, 2, null, 0, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new WorldEditJob(player, 0,
            WorldEditJob.Kind.PASTE, Integer.MAX_VALUE, 0, 0, 2, 2, 2, null, 0, null, 0));
        WorldEditJob job = new WorldEditJob(player, 0, WorldEditJob.Kind.PASTE, -1, 254, -1,
            2, 2, 2, null, 0, null, 0);
        assertEquals(8, job.volume);
        assertEquals(-1, job.x);
    }
}
