// SPDX-License-Identifier: LGPL-3.0-only
// Litematica Block Info Lines and Block Info Overlay, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.renderer.hud;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.fluids.IFluidBlock;
import org.lwjgl.opengl.GL11;

import com.github.lunatrius.schematica.client.gui.VerifierBlockInfo;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.input.Hotkeys;
import com.github.lunatrius.schematica.client.verifier.VerificationScan;
import com.github.lunatrius.schematica.client.verifier.VerificationScan.Pair;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.BlockInfoHudSettings;
import com.github.lunatrius.schematica.handler.VisualSettings;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.VoxelRayTrace;
import cpw.mods.fml.common.registry.GameData;

public final class BlockInfoHud {
    public static final BlockInfoHud INSTANCE = new BlockInfoHud();
    private final List<String> lines = new ArrayList<>();
    private BlockInfoTarget target;
    private VerifierBlockInfo panel;
    private World world;
    private EntityLivingBase camera;
    private long nextUpdate;
    private boolean errorLogged;

    private BlockInfoHud() {}

    public void clear() {
        lines.clear();
        target = null;
        panel = null;
        nextUpdate = 0;
        world = null;
        camera = null;
        errorLogged = false;
    }

    /** Renders the block info lines and, while the info overlay key is held, the block info overlay panel. */
    public void render(Minecraft mc, float partialTicks, boolean overlayAllowed) {
        boolean showLines = BlockInfoHudSettings.enabled && BlockInfoHudSettings.scale >= 0.0125;
        boolean showOverlay = overlayAllowed && VisualSettings.blockInfoOverlay && Hotkeys.held("renderInfoOverlay");
        if (mc.theWorld == null || mc.thePlayer == null || mc.renderViewEntity == null || mc.currentScreen != null
            || mc.gameSettings.hideGUI || !showLines && !showOverlay) {
            clear();
            return;
        }
        long now = System.nanoTime();
        if (world != mc.theWorld || camera != mc.renderViewEntity) {
            world = mc.theWorld;
            camera = mc.renderViewEntity;
            nextUpdate = 0;
            errorLogged = false;
        }
        if (nextUpdate == 0 || now - nextUpdate >= 0) {
            nextUpdate = now + 50_000_000L;
            lines.clear();
            panel = null;
            try { update(partialTicks); }
            catch (RuntimeException error) {
                lines.clear();
                target = null;
                if (!errorLogged) Reference.logger.warn("Could not read the targeted block for the info HUD", error);
                errorLogged = true;
            }
        }
        if (showLines && !lines.isEmpty()) draw(mc);
        if (showOverlay && target != null) {
            if (panel == null) panel = panel(target);
            if (panel != null) VerifierHud.drawPanel(mc, panel);
        }
    }

    private static VerifierBlockInfo panel(BlockInfoTarget target) {
        if (target.showComparison()) return new VerifierBlockInfo(new Pair(state(target.schematic), state(target.client)));
        if (!target.schematicHit && target.client != null) return new VerifierBlockInfo(state(target.client), "litematica.gui.label.block_info.state_client");
        if (target.schematicHit && target.schematic != null) return new VerifierBlockInfo(state(target.schematic), "litematica.gui.label.block_info.state_schematic");
        return null;
    }

    private static VerificationScan.State state(BlockInfoTarget.State state) { return new VerificationScan.State(state.registryName, state.metadata); }

    private void update(float partialTicks) {
        target = null;
        List<WorldLayer> placements = new ArrayList<>();
        if (VisualSettings.schematicVisible()) {
            if (ClientProxy.schematic != null && ClientProxy.schematic.isRenderingEnabled()) placements.add(new WorldLayer(ClientProxy.schematic));
            for (SchematicWorld placement : ClientProxy.loadedSchematics) {
                if (placement != ClientProxy.schematic && placement.isRenderingEnabled()) placements.add(new WorldLayer(placement));
            }
        }
        if (placements.isEmpty() && !(VisualSettings.blockInfoOverlay && Hotkeys.held("renderInfoOverlay"))) return;
        Vec3 start = camera.getPosition(partialTicks);
        Vec3 direction = camera.getLook(partialTicks);
        Vec3 end = start.addVector(direction.xCoord * 10, direction.yCoord * 10, direction.zCoord * 10);
        target = BlockInfoTarget.trace(new WorldLayer(world), placements, start, end, BlockInfoHudSettings.targetFluids);
        if (target == null || !target.showSchematic()) return;
        add("litematica.gui.label.block_info.state_schematic", target.schematic);
        if (target.showComparison()) {
            lines.add("");
            add("litematica.gui.label.block_info.state_client", target.client);
        }
    }

    private void add(String title, BlockInfoTarget.State state) {
        lines.add("§n" + UiTranslations.format(title));
        lines.add(state.registryName);
        lines.add(VerifierBlockInfo.metadata(state.metadata, ": "));
    }

    private void draw(Minecraft mc) {
        ScaledResolution screen = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int lineHeight = mc.fontRenderer.FONT_HEIGHT + 2;
        double scale = BlockInfoHudSettings.scale;
        int y = BlockInfoHudSettings.alignment.y(screen.getScaledHeight(), lines.size() * lineHeight - 2, scale, BlockInfoHudSettings.offsetY);
        int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        try {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glScalef((float) scale, (float) scale, 1);
            for (String line : lines) {
                if (!line.isEmpty()) {
                    int width = mc.fontRenderer.getStringWidth(line);
                    int x = BlockInfoHudSettings.alignment.x(screen.getScaledWidth(), width, scale, BlockInfoHudSettings.offsetX);
                    Gui.drawRect(x - 2, y - 2, x + width + 2, y + mc.fontRenderer.FONT_HEIGHT + 2, 0xA0505050);
                    mc.fontRenderer.drawString(line, x, y, 0xFFFFFFFF);
                }
                y += lineHeight;
            }
        } finally {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            GL11.glMatrixMode(matrixMode);
        }
    }

    private static final class WorldLayer extends BlockInfoTarget.Layer {
        private final World world;
        private final SchematicWorld schematic;

        WorldLayer(World world) {
            super(world instanceof SchematicWorld ? ((SchematicWorld) world).position.x : 0,
                world instanceof SchematicWorld ? ((SchematicWorld) world).position.y : 0,
                world instanceof SchematicWorld ? ((SchematicWorld) world).position.z : 0);
            this.world = world;
            schematic = world instanceof SchematicWorld ? (SchematicWorld) world : null;
        }

        private boolean available(int x, int y, int z) {
            return schematic != null ? schematic.isRenderingEnabled() && schematic.isBlockRendered(x, y, z)
                : world.blockExists(x, y, z);
        }

        @Override public BlockInfoTarget.State state(int x, int y, int z) {
            if (!available(x, y, z)) return null;
            Block block = world.getBlock(x, y, z);
            if (block == null || block.isAir(world, x, y, z)) return null;
            String name = GameData.getBlockRegistry().getNameForObject(block);
            return new BlockInfoTarget.State(name == null ? "#" + Block.getIdFromBlock(block) : name, world.getBlockMetadata(x, y, z));
        }

        @Override public MovingObjectPosition trace(Vec3 start, Vec3 end, boolean fluids) {
            return VoxelRayTrace.trace(start, end, (x, y, z) -> {
                if (!available(x, y, z)) return null;
                Block block = world.getBlock(x, y, z);
                if (block == null || block.isAir(world, x, y, z)) return null;
                boolean liquid = block.getMaterial().isLiquid() || block instanceof IFluidBlock;
                if (liquid ? !fluids : !block.canCollideCheck(world.getBlockMetadata(x, y, z), false)) return null;
                return block.collisionRayTrace(world, x, y, z, start.addVector(0, 0, 0), end.addVector(0, 0, 0));
            });
        }
    }
}
