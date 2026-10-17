package com.github.lunatrius.schematica.handler.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.lunatrius.schematica.network.message.MessageEditStatus;
import com.github.lunatrius.schematica.tool.WorldEditJob;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.junit.Assert.*;

public class RemoteEditClientTest {
    @Test public void partialResultsContinueOnlyWhenNotCancelled() throws Exception {
        for (boolean cancelled : new boolean[] {false, true}) {
            Constructor<RemoteEditClient> constructor = RemoteEditClient.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            RemoteEditClient client = constructor.newInstance();
            WorldEditJob job = new WorldEditJob(UUID.randomUUID(), 0, WorldEditJob.Kind.PASTE,
                0, 64, 0, 1, 1, 1, null, 0, null, 0);
            AtomicInteger callbacks = new AtomicInteger();
            client.submit(job, null, callbacks::incrementAndGet);
            Field id = RemoteEditClient.class.getDeclaredField("id");
            id.setAccessible(true);
            Field cancel = RemoteEditClient.class.getDeclaredField("cancelSent");
            cancel.setAccessible(true);
            cancel.setBoolean(client, cancelled);
            ByteBuf buffer = Unpooled.buffer();
            try {
                new MessageEditStatus(id.getLong(client), MessageEditStatus.State.PARTIAL).toBytes(buffer);
                MessageEditStatus result = new MessageEditStatus();
                result.fromBytes(buffer);
                assertEquals(MessageEditStatus.State.PARTIAL, result.state);
                client.status(result);
                assertEquals(cancelled ? 0 : 1, callbacks.get());
                assertFalse(client.busy());
            } finally { buffer.release(); }
        }
    }
}
