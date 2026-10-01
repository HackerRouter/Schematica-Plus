package com.github.lunatrius.schematica.handler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import cpw.mods.fml.common.gameevent.TickEvent;

import static org.junit.Assert.assertNotNull;

public class ServerSaveQueueLoadingTest {
    private static final String QUEUE = "com.github.lunatrius.schematica.handler.QueueTickHandler";

    @Test
    public void loadsAndExposesServerEventsWithoutClientClasses() throws Exception {
        ClassLoader loader = new ClassLoader(getClass().getClassLoader()) {
            @Override
            protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("net.minecraft.client.") || name.startsWith("org.lwjgl.")
                    || name.startsWith("com.github.lunatrius.schematica.client.")) {
                    throw new ClassNotFoundException("Client class on dedicated server: " + name);
                }
                if (!name.equals(QUEUE) && !name.startsWith(QUEUE + "$")) return super.loadClass(name, resolve);
                Class<?> type = findLoadedClass(name);
                if (type == null) {
                    try (InputStream input = getResourceAsStream(name.replace('.', '/') + ".class")) {
                        if (input == null) throw new ClassNotFoundException(name);
                        ByteArrayOutputStream output = new ByteArrayOutputStream();
                        byte[] buffer = new byte[4096];
                        for (int count; (count = input.read(buffer)) != -1;) output.write(buffer, 0, count);
                        byte[] bytes = output.toByteArray();
                        type = defineClass(name, bytes, 0, bytes.length);
                    } catch (IOException error) {
                        throw new ClassNotFoundException(name, error);
                    }
                }
                if (resolve) resolveClass(type);
                return type;
            }
        };
        Class<?> queue = Class.forName(QUEUE, true, loader);
        assertNotNull(queue.getField("INSTANCE").get(null));
        assertNotNull(queue.getMethod("onServerTick", TickEvent.ServerTickEvent.class));
        assertNotNull(queue.getDeclaredMethods());
    }
}
