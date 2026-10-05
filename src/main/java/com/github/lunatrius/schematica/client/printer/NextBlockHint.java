// Next block to place, after the behavior of Buildprint's next-block highlight, by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.printer;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.util.BlockGroups;

/**
 * highlightNextBlock: a pulsing box on the nearest schematic block that is still missing and can be placed now,
 * because a solid block is beside it. Searched every few ticks within a few blocks beyond reach.
 */
public final class NextBlockHint {
    private static final int SEARCH_EXTRA = 3, INTERVAL = 4;
    private static int[] next;
    private static int ticks;

    private NextBlockHint() {}

    public static void tick(Minecraft mc) {
        if (!ConfigurationHandler.highlightNextBlock || mc.thePlayer == null || mc.theWorld == null) {
            next = null;
            return;
        }
        if (ticks++ % INTERVAL != 0) return;
        next = find(mc.thePlayer, mc.theWorld, mc.playerController.getBlockReachDistance() + SEARCH_EXTRA);
    }

    static int[] find(EntityClientPlayerMP player, World world, double range) {
        List<SchematicWorld> placements = new ArrayList<>();
        for (SchematicWorld placement : ClientProxy.visiblePlacements()) if (placement.isRenderingEnabled()) placements.add(placement);
        if (placements.isEmpty()) return null;
        double eyeX = player.posX, eyeY = player.posY + player.getEyeHeight() - player.getDefaultEyeHeight(), eyeZ = player.posZ;
        int r = (int) Math.ceil(range), bx = MathHelper.floor_double(eyeX), by = MathHelper.floor_double(eyeY), bz = MathHelper.floor_double(eyeZ);
        double feetY = player.boundingBox.minY;
        int[] best = null;
        double bestDistance = range * range;
        SchematicPrinter printer = SchematicPrinter.INSTANCE;
        for (int x = bx - r; x <= bx + r; x++) for (int y = Math.max(0, by - r); y <= Math.min(255, by + r); y++) for (int z = bz - r; z <= bz + r; z++) {
            double dx = x + 0.5 - eyeX, dy = y + 0.5 - eyeY, dz = z + 0.5 - eyeZ, distance = dx * dx + dy * dy + dz * dz;
            if (distance >= bestDistance) continue;
            SchematicWorld placement = SchematicPrinter.placementAt(placements, x, y, z);
            if (placement == null) continue;
            int lx = x - placement.position.x, ly = y - placement.position.y, lz = z - placement.position.z;
            Block block = placement.getBlock(lx, ly, lz);
            if (block.isAir(placement, lx, ly, lz) || FluidPrinter.isFluid(block)) continue;
            Block real = world.getBlock(x, y, z);
            int meta = placement.getBlockMetadata(lx, ly, lz), realMeta = world.getBlockMetadata(x, y, z);
            if (real == block && realMeta == meta || BlockGroups.tolerated(block, meta, real, realMeta) || !real.isReplaceable(world, x, y, z)) continue;
            if (MultiBlockPlacement.kind(block) != null && MultiBlockPlacement.secondary(meta)) continue;
            // not where the player stands
            if (Math.abs(x + 0.5 - player.posX) < 0.8 && Math.abs(z + 0.5 - player.posZ) < 0.8 && y >= Math.floor(feetY) && y <= Math.floor(feetY + 1.7)) continue;
            if (printer.getSolidSides(world, x, y, z).length == 0) continue;
            best = new int[] {x, y, z};
            bestDistance = distance;
        }
        return best;
    }

    /** A pulsing outline through walls; world coordinates minus the camera. */
    public static void render(double cx, double cy, double cz) {
        int[] target = next;
        if (target == null) return;
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 150.0);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LINE_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glLineWidth(3f);
        Tessellator t = Tessellator.instance;
        t.startDrawing(GL11.GL_LINES);
        t.setColorRGBA_F(0.2f, 1f, 1f, 0.4f + 0.5f * pulse);
        double e = 0.02 + 0.04 * pulse, x0 = target[0] - e - cx, y0 = target[1] - e - cy, z0 = target[2] - e - cz;
        double x1 = target[0] + 1 + e - cx, y1 = target[1] + 1 + e - cy, z1 = target[2] + 1 + e - cz;
        for (double y : new double[] {y0, y1}) for (double z : new double[] {z0, z1}) { t.addVertex(x0, y, z); t.addVertex(x1, y, z); }
        for (double x : new double[] {x0, x1}) for (double z : new double[] {z0, z1}) { t.addVertex(x, y0, z); t.addVertex(x, y1, z); }
        for (double x : new double[] {x0, x1}) for (double y : new double[] {y0, y1}) { t.addVertex(x, y, z0); t.addVertex(x, y, z1); }
        t.draw();
        GL11.glPopAttrib();
        GL11.glDepthMask(true);
    }
}
