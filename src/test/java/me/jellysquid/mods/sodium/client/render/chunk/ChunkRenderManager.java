// Legacy renderer thread-state fixture, by HackerRouter, 2026.
package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;

public final class ChunkRenderManager {
    private static final ThreadLocal<BlockRenderPass> PASS =
        ThreadLocal.withInitial(() -> BlockRenderPass.CUTOUT_MIPPED);

    public static int getWorldRenderPass() { return PASS.get().ordinal(); }
    public static void setWorldRenderPass(BlockRenderPass pass) { PASS.set(pass); }
}
