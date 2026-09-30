package com.github.lunatrius.schematica.client.renderer;

import java.lang.reflect.Field;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.MinecraftForgeClient;
import cpw.mods.fml.relauncher.ReflectionHelper;

final class SchematicRenderPass implements AutoCloseable {
    private static final Field WORLD_PASS = ReflectionHelper.findField(ForgeHooksClient.class, "worldRenderPass");
    private final int entityPass = MinecraftForgeClient.getRenderPass();
    private final int worldPass = ForgeHooksClient.getWorldRenderPass();

    SchematicRenderPass(int pass) {
        setWorldPass(pass);
        ForgeHooksClient.setRenderPass(pass);
    }

    private static void setWorldPass(int pass) {
        try {
            WORLD_PASS.setInt(null, pass);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot set Forge world render pass", e);
        }
    }

    @Override public void close() {
        ForgeHooksClient.setRenderPass(entityPass);
        setWorldPass(worldPass);
    }
}
