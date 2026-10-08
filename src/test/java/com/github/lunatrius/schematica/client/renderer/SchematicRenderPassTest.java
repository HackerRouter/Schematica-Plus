// Render pass restoration checks, by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.renderer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderManager;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.*;
import static org.junit.Assert.*;

public class SchematicRenderPassTest {
    private static final String CONTEXT = "com.github.lunatrius.schematica.client.renderer.SchematicRenderPass";
    private static final String HOOKS = "net.minecraftforge.client.ForgeHooksClient";
    private static final String CLIENT = "net.minecraftforge.client.MinecraftForgeClient";
    private static final String MANAGER = "me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderManager";

    @Test public void translucentPreviewReachesLegacyRendererAndRestoresBothStates() throws Exception {
        Environment env = new Environment(true);
        env.world(-1);
        env.entity(-1);
        ChunkRenderManager.setWorldRenderPass(BlockRenderPass.CUTOUT_MIPPED);
        try (AutoCloseable ignored = env.enter(1)) {
            assertEquals(1, env.effective());
            assertEquals(1, env.world());
            assertEquals(1, env.entity());
        }
        assertEquals(0, env.effective());
        assertEquals(-1, env.world());
        assertEquals(-1, env.entity());
    }

    @Test public void nestedPassesAndFailedDrawingRestoreOuterPass() throws Exception {
        Environment env = new Environment(true);
        ChunkRenderManager.setWorldRenderPass(BlockRenderPass.TRANSLUCENT);
        try {
            try (AutoCloseable ignored = env.enter(0)) {
                assertEquals(0, env.effective());
                assertThrows(IllegalArgumentException.class, () -> {
                    try (AutoCloseable inner = env.enter(1)) {
                        assertEquals(1, env.effective());
                        throw new IllegalArgumentException("Drawing failed");
                    }
                });
                assertEquals(0, env.effective());
                try (AutoCloseable overlay = env.enter(2)) {
                    assertEquals(0, env.effective());
                    assertEquals(2, env.entity());
                }
                assertEquals(0, env.entity());
            }
            assertEquals(1, env.effective());
        } finally { ChunkRenderManager.setWorldRenderPass(BlockRenderPass.CUTOUT_MIPPED); }
    }

    @Test public void missingLegacyClassesKeepForgePassesAndRestoreAfterOverlay() throws Exception {
        Environment env = new Environment(false);
        env.world(-1);
        env.entity(-1);
        try (AutoCloseable ignored = env.enter(1)) {
            assertEquals(1, env.effective());
            try (AutoCloseable overlay = env.enter(2)) { assertEquals(2, env.effective()); }
            assertEquals(1, env.effective());
        }
        assertEquals(-1, env.effective());
        assertEquals(-1, env.entity());
    }

    private static final class Environment extends ClassLoader {
        final boolean legacy;
        final Class<?> hooks, client, context;

        Environment(boolean legacy) throws Exception {
            super(SchematicRenderPassTest.class.getClassLoader());
            this.legacy = legacy;
            hooks = loadClass(HOOKS); client = loadClass(CLIENT); context = loadClass(CONTEXT);
        }

        AutoCloseable enter(int pass) throws Exception { return (AutoCloseable) context.getConstructor(int.class).newInstance(pass); }
        int world() throws Exception { return hooks.getField("worldRenderPass").getInt(null); }
        void world(int pass) throws Exception { hooks.getField("worldRenderPass").setInt(null, pass); }
        int entity() throws Exception { return (Integer) client.getMethod("getRenderPass").invoke(null); }
        void entity(int pass) throws Exception { hooks.getMethod("setRenderPass", int.class).invoke(null, pass); }
        int effective() throws Exception { return (Integer) hooks.getMethod("getWorldRenderPass").invoke(null); }

        @Override protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!legacy && name.startsWith("me.jellysquid.mods.sodium.")) throw new ClassNotFoundException(name);
            if (!name.startsWith(CONTEXT) && !name.equals(HOOKS) && !name.equals(CLIENT)) return super.loadClass(name, resolve);
            Class<?> type = findLoadedClass(name);
            if (type == null) {
                try {
                    byte[] bytes;
                    if (name.equals(HOOKS) || name.equals(CLIENT)) bytes = forgeState(name);
                    else try (InputStream input = getResourceAsStream(name.replace('.', '/') + ".class")) {
                        ByteArrayOutputStream output = new ByteArrayOutputStream();
                        byte[] buffer = new byte[4096];
                        for (int count; (count = input.read(buffer)) != -1;) output.write(buffer, 0, count);
                        bytes = output.toByteArray();
                    }
                    type = defineClass(name, bytes, 0, bytes.length);
                } catch (Exception e) { throw new ClassNotFoundException(name, e); }
            }
            if (resolve) resolveClass(type);
            return type;
        }

        // Isolate Forge's pass storage from its unrelated OpenGL and fluid bootstrap.
        private byte[] forgeState(String name) {
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            writer.visit(V1_8, ACC_PUBLIC, name.replace('.', '/'), null, "java/lang/Object", null);
            String owner = HOOKS.replace('.', '/');
            if (name.equals(HOOKS)) {
                writer.visitField(ACC_PUBLIC | ACC_STATIC, "worldRenderPass", "I", null, null).visitEnd();
                writer.visitField(ACC_PUBLIC | ACC_STATIC, "renderPass", "I", null, null).visitEnd();
                MethodVisitor setter = writer.visitMethod(ACC_PUBLIC | ACC_STATIC, "setRenderPass", "(I)V", null, null);
                setter.visitCode(); setter.visitVarInsn(ILOAD, 0); setter.visitFieldInsn(PUTSTATIC, owner, "renderPass", "I");
                setter.visitInsn(RETURN); setter.visitMaxs(0, 0); setter.visitEnd();
            }
            String method = name.equals(HOOKS) ? "getWorldRenderPass" : "getRenderPass";
            MethodVisitor getter = writer.visitMethod(ACC_PUBLIC | ACC_STATIC, method, "()I", null, null);
            getter.visitCode();
            if (name.equals(HOOKS) && legacy) getter.visitMethodInsn(INVOKESTATIC, MANAGER.replace('.', '/'), method, "()I", false);
            else getter.visitFieldInsn(GETSTATIC, owner, name.equals(HOOKS) ? "worldRenderPass" : "renderPass", "I");
            getter.visitInsn(IRETURN); getter.visitMaxs(0, 0); getter.visitEnd();
            writer.visitEnd();
            return writer.toByteArray();
        }
    }
}
