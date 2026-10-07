package com.github.lunatrius.schematica.handler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import cpw.mods.fml.common.gameevent.TickEvent;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

public class ServerSaveQueueLoadingTest {
    private static final String QUEUE = "com.github.lunatrius.schematica.handler.QueueTickHandler";
    private static final String UTIL = "com.github.lunatrius.schematica.world.schematic.SchematicUtil";

    @Test
    public void loadsAndExposesServerEventsWithoutClientClasses() throws Exception {
        Class<?> queue = Class.forName(QUEUE, true, serverClassLoader(QUEUE));
        assertNotNull(queue.getField("INSTANCE").get(null));
        assertNotNull(queue.getMethod("onServerTick", TickEvent.ServerTickEvent.class));
        assertNotNull(queue.getDeclaredMethods());
    }

    @Test
    public void savesWithDefaultIconsWithoutClientClasses() throws Exception {
        Class<?> util = Class.forName(UTIL, true, serverClassLoader(UTIL));
        ItemStack empty = (ItemStack) util.getMethod("getIconFromName", String.class).invoke(null, "");
        ItemStack missing = (ItemStack) util.getMethod("getIconFromName", String.class).invoke(null, "missing:icon");
        assertNotNull(empty);
        assertNotNull(missing);
        assertNotSame(empty, missing);
    }

    @Test
    public void readsMissingAndInvalidIconsWithoutClientClasses() throws Exception {
        Class<?> util = Class.forName(UTIL, true, serverClassLoader(UTIL));
        assertNotNull(util.getMethod("getIconFromNBT", NBTTagCompound.class).invoke(null, (Object) null));
        NBTTagCompound tag = new NBTTagCompound();
        assertNotNull(util.getMethod("getIconFromNBT", NBTTagCompound.class).invoke(null, tag));
        tag.setTag("Icon", new NBTTagCompound());
        assertNotNull(util.getMethod("getIconFromNBT", NBTTagCompound.class).invoke(null, tag));
    }

    private ClassLoader serverClassLoader(String root) {
        return new ClassLoader(getClass().getClassLoader()) {
            @Override
            protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("net.minecraft.client.") || name.startsWith("org.lwjgl.")
                    || name.startsWith("com.github.lunatrius.schematica.client.")) {
                    throw new ClassNotFoundException("Client class on dedicated server: " + name);
                }
                if (!name.equals(root) && !name.startsWith(root + "$")) return super.loadClass(name, resolve);
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
    }
}
