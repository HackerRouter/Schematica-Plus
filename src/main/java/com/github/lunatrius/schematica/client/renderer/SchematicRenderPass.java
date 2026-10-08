package com.github.lunatrius.schematica.client.renderer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.MinecraftForgeClient;
import cpw.mods.fml.relauncher.ReflectionHelper;
import com.github.lunatrius.schematica.reference.Reference;

/** Sets Forge's world and entity render pass while schematic blocks are drawn, as world rendering does. */
public final class SchematicRenderPass implements AutoCloseable {
    private static final Field WORLD_PASS = ReflectionHelper.findField(ForgeHooksClient.class, "worldRenderPass");
    private static final LegacyPass LEGACY_PASS = LegacyPass.find();
    private final int entityPass = MinecraftForgeClient.getRenderPass();
    private final int worldPass = getWorldPass();
    private final int legacyPass = LEGACY_PASS == null ? -1 : LEGACY_PASS.get();

    public SchematicRenderPass(int pass) {
        setWorldPass(pass);
        ForgeHooksClient.setRenderPass(pass);
        if (LEGACY_PASS != null && pass < 2) LEGACY_PASS.set(pass);
    }

    private static int getWorldPass() {
        try { return WORLD_PASS.getInt(null); }
        catch (IllegalAccessException e) { throw new IllegalStateException("Cannot read Forge world render pass", e); }
    }

    private static void setWorldPass(int pass) {
        try {
            WORLD_PASS.setInt(null, pass);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot set Forge world render pass", e);
        }
    }

    @Override public void close() {
        if (LEGACY_PASS != null) LEGACY_PASS.set(legacyPass);
        ForgeHooksClient.setRenderPass(entityPass);
        setWorldPass(worldPass);
    }

    // Older Angelica replaces Forge's getter with a separate thread-local pass.
    private static final class LegacyPass {
        final Method getter, setter;
        final Object[] passes;

        LegacyPass(Class<?> manager, Class<?> pass) throws ReflectiveOperationException {
            getter = manager.getMethod("getWorldRenderPass");
            setter = manager.getMethod("setWorldRenderPass", pass);
            passes = pass.getEnumConstants();
        }

        static LegacyPass find() {
            try {
                ClassLoader loader = SchematicRenderPass.class.getClassLoader();
                Class<?> manager = Class.forName(
                    "me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderManager", false, loader);
                Class<?> pass = Class.forName(
                    "me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass", false, loader);
                return new LegacyPass(manager, pass);
            } catch (ClassNotFoundException absent) { return null; }
            catch (ReflectiveOperationException | LinkageError e) {
                Reference.logger.warn("Could not access legacy renderer passes", e);
                return null;
            }
        }

        int get() {
            try { return (Integer) getter.invoke(null); }
            catch (ReflectiveOperationException e) { throw new IllegalStateException("Cannot read legacy render pass", e); }
        }

        void set(int pass) {
            try { setter.invoke(null, passes[pass]); }
            catch (ReflectiveOperationException e) { throw new IllegalStateException("Cannot set legacy render pass", e); }
        }
    }
}
