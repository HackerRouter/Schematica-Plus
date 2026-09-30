package com.github.lunatrius.schematica.tool;

import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WorldEditJobTest {
    @Test public void pasteOptionsDefaultOffAndDoNotAffectOtherTools() {
        WorldEditJob defaults = new WorldEditJob(UUID.randomUUID(), 0, WorldEditJob.Kind.PASTE, 0, 0, 0,
            1, 1, 1, null, 0, null, 0);
        assertFalse(defaults.pasteWithoutUpdates);
        assertFalse(defaults.pasteOnlyAir);
        WorldEditJob fill = job(WorldEditJob.Kind.FILL, true, true);
        assertFalse(fill.pasteWithoutUpdates);
        assertFalse(fill.pasteOnlyAir);
        assertTrue(fill.blockCommand(1, 2, 3, "minecraft:stone", 0).endsWith(" replace"));
    }

    @Test public void airOnlyCommandsRequireTheServerToRecheckTheTarget() {
        WorldEditJob job = job(WorldEditJob.Kind.PASTE, false, true);
        assertEquals("/setblock -1 64 2 minecraft:stone 3 keep", job.blockCommand(-1, 64, 2, "minecraft:stone", 3));
    }

    @Test public void silentPasteRejectsCommandFallbackBeforeSendingAnyCommands() {
        WorldEditJob job = job(WorldEditJob.Kind.PASTE, true, false);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, job::validateCommandFallback);
        assertTrue(error.getMessage().contains("cannot suppress updates"));
    }

    private static WorldEditJob job(WorldEditJob.Kind kind, boolean silent, boolean air) {
        return new WorldEditJob(UUID.randomUUID(), 0, kind, 0, 0, 0, 1, 1, 1, null, 0, null, 0, silent, air);
    }

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
