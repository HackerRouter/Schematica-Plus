package com.github.lunatrius.schematica.handler;

import java.util.UUID;

import com.github.lunatrius.schematica.task.TaskRegistry;
import com.github.lunatrius.schematica.tool.WorldEditJob;
import cpw.mods.fml.common.gameevent.TickEvent;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

public class WorldEditQueueTest {
    private final WorldEditQueue queue = new WorldEditQueue();
    private final UUID owner = UUID.randomUUID();

    @After public void clear() { queue.clear(); }

    private WorldEditJob job() {
        return new WorldEditJob(owner, 0, WorldEditJob.Kind.FILL, 0, 64, 0, 3, 1, 1, null, 0, null, 0);
    }

    @Test public void cancellationKeepsTheQueueBusyUntilCleanupAndClearReleasesTasks() {
        WorldEditJob job = job();
        assertTrue(queue.submit(null, job));
        TaskRegistry.Task task = TaskRegistry.INSTANCE.tasks(owner, 0).get(0);
        assertFalse(queue.cancel(UUID.randomUUID()));
        assertTrue(queue.cancel(owner));
        assertTrue(job.cancelled);
        assertTrue(task.progress().cancelling);
        assertTrue(queue.cancel(owner));
        assertFalse(queue.submit(null, job()));
        queue.clear();
        assertTrue(task.progress().finished);
        assertTrue(TaskRegistry.INSTANCE.tasks(owner, 0).isEmpty());
        assertTrue(queue.submit(null, job()));
        assertFalse(task.cancel(owner));
        assertEquals(1, TaskRegistry.INSTANCE.tasks(owner, 0).size());
    }

    @Test public void lossOfOwningServerRemovesTheTask() {
        assertTrue(queue.submit(null, job()));
        TaskRegistry.Task task = TaskRegistry.INSTANCE.tasks(owner, 0).get(0);
        queue.onTick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
        assertTrue(task.progress().finished);
        assertTrue(TaskRegistry.INSTANCE.tasks(owner, 0).isEmpty());
    }
}
