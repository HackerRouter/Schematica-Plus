package com.github.lunatrius.schematica.handler.client;

import java.util.UUID;

import com.github.lunatrius.schematica.task.TaskRegistry;
import com.github.lunatrius.schematica.tool.WorldEditJob;
import com.github.lunatrius.schematica.util.MessageException;
import cpw.mods.fml.common.gameevent.TickEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

public class CommandEditQueueTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private com.github.lunatrius.schematica.proxy.CommonProxy previousProxy;
    private CommandEditQueue queue = new CommandEditQueue();
    private final UUID owner = UUID.randomUUID();

    @Before public void proxy() {
        previousProxy = com.github.lunatrius.schematica.SchematicaPlus.proxy;
        if (previousProxy == null) com.github.lunatrius.schematica.SchematicaPlus.proxy = new com.github.lunatrius.schematica.proxy.ServerProxy() {
            @Override public java.io.File getDataDirectory() { return temporary.getRoot(); }
        };
    }

    @After public void clear() {
        queue.cancel();
        com.github.lunatrius.schematica.SchematicaPlus.proxy = previousProxy;
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
            1, 1, 1, null, 0, null, 0, true, com.github.lunatrius.schematica.tool.ReplaceBehavior.ALL);
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
        chat(new net.minecraft.util.ChatComponentTranslation("commands.generic.permission"));
        tick();
        assertEquals(java.util.Collections.singletonList(false), client.completed);
        assertEquals(1, client.commands.size());
        assertFalse(queue.busy());
    }

    @Test public void finalCommandRequiresItsOwnReplyBeforeCompletingOrCallingTheNextProjectStep() {
        FakeClient client = start(3);
        tick();
        chat(new net.minecraft.util.ChatComponentTranslation("commands.setblock.success"));
        tick();
        assertEquals(3, client.commands.size());
        assertTrue(queue.busy());
        chat(new net.minecraft.util.ChatComponentTranslation("commands.setblock.success"));
        tick();
        assertTrue(client.completed.isEmpty());
        chat(new net.minecraft.util.ChatComponentTranslation("commands.setblock.success"));
        tick();
        assertEquals(java.util.Collections.singletonList(true), client.completed);
        assertFalse(queue.busy());
    }

    @Test public void aSingleCommandDeniedAfterSendingCannotCompleteEarly() {
        FakeClient client = start(1);
        tick();
        assertTrue(client.completed.isEmpty());
        chat(new net.minecraft.util.ChatComponentTranslation("commands.setblock.outOfWorld"));
        assertEquals(java.util.Collections.singletonList(false), client.completed);
    }

    @Test public void missingOrUnrelatedRepliesStopWithoutClaimingCompletion() {
        FakeClient client = start(3);
        tick();
        chat(new net.minecraft.util.ChatComponentTranslation("chat.type.admin", "Other player",
            new net.minecraft.util.ChatComponentTranslation("commands.setblock.success")));
        chat(new net.minecraft.util.ChatComponentTranslation("commands.time.set", 6000));
        for (int i = 0; i < 200; i++) tick();
        assertEquals(1, client.commands.size());
        assertEquals(java.util.Collections.singletonList(false), client.completed);
        assertEquals("schematica.message.edit.commands_unconfirmed",
            ((net.minecraft.util.ChatComponentTranslation) client.messages.get(0)).getKey());
    }

    @Test public void wrappedCommandErrorsAndNoChangeDoNotCountAsAcknowledgedWrites() {
        for (String key : new String[] {"commands.generic.permission", "commands.generic.usage", "commands.setblock.noChange"}) {
            FakeClient client = start(1);
            tick();
            chat(new net.minecraft.util.ChatComponentText("").appendSibling(new net.minecraft.util.ChatComponentTranslation(key)));
            assertEquals(java.util.Collections.singletonList(false), client.completed);
        }
    }

    @Test public void lostContextAndTaskCancellationStopWhileWaitingForReplies() {
        for (boolean cancelTask : new boolean[] {false, true}) {
            FakeClient client = start(3);
            tick();
            if (cancelTask) TaskRegistry.INSTANCE.tasks(owner, 0).get(0).cancel(owner);
            else client.valid = false;
            tick();
            chat(new net.minecraft.util.ChatComponentTranslation("commands.setblock.success"));
            assertEquals(1, client.commands.size());
            assertEquals(java.util.Collections.singletonList(false), client.completed);
        }
    }

    private FakeClient start(int size) {
        queue.cancel();
        FakeClient client = new FakeClient();
        queue = new CommandEditQueue(client);
        WorldEditJob job = new WorldEditJob(owner, 0, WorldEditJob.Kind.FILL, 0, 64, 0, size, 1, 1, null, 0, null, 0);
        job.completion = client.completed::add;
        queue.submit(job, null);
        return client;
    }

    private void tick() { queue.onTick(new TickEvent.ClientTickEvent(TickEvent.Phase.END)); }

    private void chat(net.minecraft.util.IChatComponent message) {
        queue.onChat(new net.minecraftforge.client.event.ClientChatReceivedEvent(message));
    }

    private static final class FakeClient implements CommandEditQueue.Client {
        boolean valid = true;
        final java.util.List<String> commands = new java.util.ArrayList<>();
        final java.util.List<Boolean> completed = new java.util.ArrayList<>();
        final java.util.List<net.minecraft.util.IChatComponent> messages = new java.util.ArrayList<>();
        public boolean valid(WorldEditJob job, net.minecraft.world.World world) { return valid; }
        public String command(WorldEditJob job, int index, net.minecraft.world.World world) { return "/setblock " + index + " 64 0 minecraft:stone"; }
        public void send(String command) { commands.add(command); }
        public void message(net.minecraft.util.IChatComponent message) { messages.add(message); }
    }
}
