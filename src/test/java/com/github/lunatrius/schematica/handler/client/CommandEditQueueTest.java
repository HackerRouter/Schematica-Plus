package com.github.lunatrius.schematica.handler.client;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.proxy.CommonProxy;
import com.github.lunatrius.schematica.proxy.ServerProxy;
import com.github.lunatrius.schematica.task.TaskRegistry;
import com.github.lunatrius.schematica.tool.ReplaceBehavior;
import com.github.lunatrius.schematica.tool.WorldEditJob;
import com.github.lunatrius.schematica.tool.WorldEditTask;
import com.github.lunatrius.schematica.util.MessageException;
import com.github.lunatrius.schematica.util.PlayerMessage;
import com.github.lunatrius.schematica.world.storage.Schematic;

import cpw.mods.fml.common.gameevent.TickEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

public class CommandEditQueueTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private CommonProxy previousProxy;
    private CommandEditQueue queue = new CommandEditQueue();
    private final UUID owner = UUID.randomUUID();

    @Before public void proxy() {
        previousProxy = SchematicaPlus.proxy;
        if (previousProxy == null) SchematicaPlus.proxy = new ServerProxy() {
            @Override public File getDataDirectory() { return temporary.getRoot(); }
        };
    }

    @After public void clear() {
        queue.cancel();
        SchematicaPlus.proxy = previousProxy;
    }

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
            1, 1, 1, null, 0, null, 0, true, ReplaceBehavior.ALL);
        assertThrows(MessageException.class, () -> queue.submit(silent, null));
        assertTrue(TaskRegistry.INSTANCE.tasks(owner, 0).isEmpty());
        queue.submit(job(), null);
        assertTrue(queue.cancel());
        assertTrue(TaskRegistry.INSTANCE.tasks(owner, 0).isEmpty());
        queue.submit(job(), null);
        assertEquals(1, TaskRegistry.INSTANCE.tasks(owner, 0).size());
    }

    @Test public void permissionFailureWaitsForFirstReplyWithoutSendingABurstOrReportingSuccess() {
        FakeClient client = start(3);
        tick();
        for (int i = 0; i < 20; i++) tick();
        assertEquals(1, client.commands.size());
        assertTrue(client.completed.isEmpty());
        chat(new ChatComponentTranslation("commands.generic.permission"));
        tick();
        assertEquals(Collections.singletonList(false), client.completed);
        assertEquals(1, client.commands.size());
        assertFalse(queue.busy());
    }

    @Test public void finalCommandRequiresItsOwnReplyBeforeCompletingOrCallingTheNextProjectStep() {
        FakeClient client = start(3);
        tick();
        chat(new ChatComponentTranslation("commands.setblock.success"));
        tick();
        assertEquals(3, client.commands.size());
        assertTrue(queue.busy());
        chat(new ChatComponentTranslation("commands.setblock.success"));
        tick();
        assertTrue(client.completed.isEmpty());
        chat(new ChatComponentTranslation("commands.setblock.success"));
        tick();
        assertEquals(Collections.singletonList(true), client.completed);
        assertFalse(queue.busy());
    }

    @Test public void aSingleCommandDeniedAfterSendingCannotCompleteEarly() {
        FakeClient client = start(1);
        tick();
        assertTrue(client.completed.isEmpty());
        chat(new ChatComponentTranslation("commands.setblock.outOfWorld"));
        assertEquals(Collections.singletonList(false), client.completed);
    }

    @Test public void missingOrUnrelatedRepliesStopWithoutClaimingCompletion() {
        FakeClient client = start(3);
        tick();
        chat(new ChatComponentTranslation("chat.type.admin", "Other player",
            new ChatComponentTranslation("commands.setblock.success")));
        chat(new ChatComponentTranslation("commands.time.set", 6000));
        for (int i = 0; i < 200; i++) tick();
        assertEquals(1, client.commands.size());
        assertEquals(Collections.singletonList(false), client.completed);
        assertEquals("schematica.message.edit.commands_unconfirmed",
            client.messages.get(0).key);
    }

    @Test public void wrappedCommandErrorsAndNoChangeDoNotCountAsAcknowledgedWrites() {
        for (String key : new String[] {"commands.generic.permission", "commands.generic.usage", "commands.setblock.noChange"}) {
            FakeClient client = start(1);
            tick();
            chat(new ChatComponentText("").appendSibling(new ChatComponentTranslation(key)));
            assertEquals(Collections.singletonList(false), client.completed);
        }
    }

    @Test public void lostContextAndTaskCancellationStopWhileWaitingForReplies() {
        for (boolean cancelTask : new boolean[] {false, true}) {
            FakeClient client = start(3);
            tick();
            if (cancelTask) TaskRegistry.INSTANCE.tasks(owner, 0).get(0).cancel(owner);
            else client.valid = false;
            tick();
            chat(new ChatComponentTranslation("commands.setblock.success"));
            assertEquals(1, client.commands.size());
            assertEquals(Collections.singletonList(false), client.completed);
        }
    }

    private FakeClient start(int size) {
        return start(size, WorldEditJob.Kind.FILL);
    }

    private FakeClient start(int size, WorldEditJob.Kind kind) {
        queue.cancel();
        FakeClient client = new FakeClient();
        queue = new CommandEditQueue(client);
        WorldEditJob job = new WorldEditJob(owner, 0, kind, 0, 64, 0, size, 1, 1, null, 0, null, 0);
        if (kind == WorldEditJob.Kind.PASTE) job.capture(new Schematic(null, size, 1, 1), false, false);
        client.job = job;
        job.completion = outcome -> client.completed.add(outcome == WorldEditTask.Outcome.FINISHED);
        queue.submit(job, null);
        return client;
    }

    @Test public void skippedCommandsReportNoChangesButAllowTheNextProjectStep() {
        FakeClient client = start(3);
        client.skip = true;
        tick();
        assertTrue(client.commands.isEmpty());
        assertEquals(Collections.singletonList(true), client.completed);
        assertEquals("schematica.message.edit.no_changes",
            client.messages.get(0).key);
    }

    @Test public void pasteCommandErrorsContinueAndReportOnlyAcknowledgedWrites() {
        for (String key : new String[] {"commands.setblock.noChange", "commands.setblock.outOfWorld", "commands.generic.exception", "commands.give.notFound"}) {
            FakeClient client = start(3, WorldEditJob.Kind.PASTE);
            tick();
            chat(new ChatComponentTranslation(key));
            tick();
            assertEquals(3, client.commands.size());
            assertTrue(client.completed.isEmpty());
            chat(new ChatComponentTranslation("commands.setblock.success"));
            chat(new ChatComponentTranslation("commands.setblock.success"));
            tick();
            assertEquals(Collections.singletonList(false), client.completed);
            assertEquals(2, client.job.blockCount);
            assertEquals(1, client.job.failedBlockCount());
            assertTrue(client.job.completedTraversal);
            assertEquals("schematica.message.edit.paste_errors",
                client.messages.get(0).key);
        }
    }

    @Test public void pasteCommandCreationAndSendErrorsDoNotLoseLaterReplies() {
        FakeClient client = start(3, WorldEditJob.Kind.PASTE);
        client.failSend = true;
        client.failAt = 1;
        for (int i = 0; i < 8; i++) tick();
        assertEquals(1, client.commands.size());
        assertTrue(client.completed.isEmpty());
        chat(new ChatComponentTranslation("commands.setblock.success"));
        tick();
        assertEquals(Collections.singletonList(false), client.completed);
        assertEquals(1, client.job.blockCount);
        assertEquals(2, client.job.failedBlockCount());
        assertTrue(client.job.completedTraversal);
    }

    @Test public void jobWideCommandErrorsCancelWithoutCompletingTheTraversal() {
        for (String key : new String[] {"commands.generic.permission", "commands.generic.notFound", "commands.generic.usage", "commands.generic.snytax"}) {
            FakeClient client = start(3, WorldEditJob.Kind.PASTE);
            tick();
            chat(new ChatComponentTranslation(key));
            tick();
            assertEquals(key, 1, client.commands.size());
            assertEquals(key, Collections.singletonList(false), client.completed);
            assertFalse(key, client.job.completedTraversal);
        }
    }

    private void tick() { queue.onTick(new TickEvent.ClientTickEvent(TickEvent.Phase.END)); }

    private void chat(IChatComponent message) {
        queue.onChat(new ClientChatReceivedEvent(message));
    }

    private static final class FakeClient implements CommandEditQueue.Client {
        boolean valid = true, skip, failSend;
        int failAt = -1;
        WorldEditJob job;
        final List<String> commands = new ArrayList<>();
        final List<Boolean> completed = new ArrayList<>();
        final List<PlayerMessage> messages = new ArrayList<>();
        public boolean valid(WorldEditJob job, World world) { return valid; }
        public String command(WorldEditJob job, int index, World world) {
            if (index == failAt) throw new IllegalStateException("Command creation failed");
            return skip ? null : "/setblock " + index + " 64 0 minecraft:stone";
        }
        public void send(String command) {
            if (failSend) { failSend = false; throw new IllegalStateException("Send failed"); }
            commands.add(command);
        }
        public void message(PlayerMessage message) { messages.add(message); }
    }
}
