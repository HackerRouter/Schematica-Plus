package com.github.lunatrius.schematica.handler.client;

import java.util.UUID;

import com.github.lunatrius.schematica.task.TaskRegistry;
import com.github.lunatrius.schematica.tool.WorldEditJob;
import com.github.lunatrius.schematica.util.MessageException;
import cpw.mods.fml.common.gameevent.TickEvent;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

public class CommandEditQueueTest {
    private final CommandEditQueue queue = new CommandEditQueue();
    private final UUID owner = UUID.randomUUID();

    @After public void clear() { queue.cancel(); }

    private WorldEditJob job() {
        return new WorldEditJob(owner, 0, WorldEditJob.Kind.FILL, 0, 64, 0, 3, 1, 1, null, 0, null, 0);
    }

    @Test public void newCommandEditDoesNotOverwriteARunningTask() {
        queue.submit(job(), null);
        TaskRegistry.Task first = TaskRegistry.INSTANCE.tasks(owner, 0).get(0);
        MessageException error = assertThrows(MessageException.class, () -> queue.submit(job(), null));
        assertEquals("schematica.message.edit.busy", error.key());
        assertSame(first, TaskRegistry.INSTANCE.tasks(owner, 0).get(0));
        assertEquals(1, TaskRegistry.INSTANCE.tasks(owner, 0).size());
        assertEquals(TaskRegistry.Backend.COMMANDS, first.backend);
    }

    @Test public void removingAQueuedTaskStopsBeforeAccessingWorldOrSendingACommand() {
        queue.submit(job(), null);
        TaskRegistry.Task first = TaskRegistry.INSTANCE.tasks(owner, 0).get(0);
        assertTrue(first.cancel(owner));
        queue.onTick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
        assertTrue(first.progress().finished);
        assertTrue(TaskRegistry.INSTANCE.tasks(owner, 0).isEmpty());
        assertFalse(queue.cancel());
        queue.submit(job(), null);
        assertFalse(first.cancel(owner));
        assertTrue(TaskRegistry.INSTANCE.tasks(owner, 0).get(0).progress().cancellable);
    }

    @Test public void failedPreflightCreatesNoTaskAndClearingAllowsAnotherJob() {
        WorldEditJob silent = new WorldEditJob(owner, 0, WorldEditJob.Kind.PASTE, 0, 64, 0,
            1, 1, 1, null, 0, null, 0, true, false);
        assertThrows(MessageException.class, () -> queue.submit(silent, null));
        assertTrue(TaskRegistry.INSTANCE.tasks(owner, 0).isEmpty());
        queue.submit(job(), null);
        assertTrue(queue.cancel());
        assertTrue(TaskRegistry.INSTANCE.tasks(owner, 0).isEmpty());
        queue.submit(job(), null);
        assertEquals(1, TaskRegistry.INSTANCE.tasks(owner, 0).size());
    }
}
