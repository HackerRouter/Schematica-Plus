// SPDX-License-Identifier: LGPL-3.0-only
// Litematica schematic world ray traces, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.tool;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.lunatrius.schematica.client.world.RenderLayerSettings;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.VisualSettings;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.util.VoxelRayTrace;

/** Traces against the rendered blocks of every enabled placement, the counterpart of Litematica's schematic world. */
public final class SchematicTargets {
    private SchematicTargets() {}

    /** A schematic block hit, in world coordinates. */
    public static final class Hit {
        public final SchematicWorld world;
        public final int x, y, z, side;
        public final double hitX, hitY, hitZ, distance;

        Hit(SchematicWorld world, MovingObjectPosition hit, double distance) {
            this.world = world;
            x = hit.blockX + world.position.x; y = hit.blockY + world.position.y; z = hit.blockZ + world.position.z;
            side = hit.sideHit;
            hitX = hit.hitVec.xCoord + world.position.x; hitY = hit.hitVec.yCoord + world.position.y; hitZ = hit.hitVec.zCoord + world.position.z;
            this.distance = distance;
        }

        Hit(SchematicWorld world, int x, int y, int z, int side) {
            this.world = world;
            this.x = x; this.y = y; this.z = z; this.side = side;
            hitX = x + 0.5; hitY = y + 0.5; hitZ = z + 0.5;
            distance = 0;
        }

        public ForgeDirection face() { return ForgeDirection.getOrientation(side); }
        int[] adjacent() { ForgeDirection d = face(); return new int[] {x + d.offsetX, y + d.offsetY, z + d.offsetZ}; }
        public int localX() { return x - world.position.x; }
        public int localY() { return y - world.position.y; }
        public int localZ() { return z - world.position.z; }
    }

    private static EntityLivingBase camera() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.renderViewEntity == null ? mc.thePlayer : mc.renderViewEntity;
    }

    /**
     * The distance to the entity the crosshair targets (vanilla's objectMouseOver, which includes entities), or -1.
     * A schematic block behind a targeted mob is not picked, so middle click gives the mob's spawn egg.
     */
    private static double targetedEntityDistance(Vec3 eye) {
        MovingObjectPosition target = Minecraft.getMinecraft().objectMouseOver;
        if (target == null || target.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY || target.entityHit == null) return -1;
        return target.hitVec != null ? eye.distanceTo(target.hitVec)
            : eye.distanceTo(Vec3.createVectorHelper(target.entityHit.posX, target.entityHit.posY, target.entityHit.posZ));
    }

    private static Vec3 copy(Vec3 vector, double x, double y, double z) {
        return Vec3.createVectorHelper(vector.xCoord + x, vector.yCoord + y, vector.zCoord + z);
    }

    /** The nearest schematic block when it is not farther than the nearest real block (getGenericTrace). */
    public static Hit closest(double range, boolean fluids) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityLivingBase camera = camera();
        if (camera == null || mc.theWorld == null || !VisualSettings.schematicVisible()) return null;
        Vec3 eye = camera.getPosition(1), look = camera.getLook(1);
        double limit = range;
        MovingObjectPosition real = mc.theWorld.rayTraceBlocks(copy(eye, 0, 0, 0), eye.addVector(look.xCoord * range, look.yCoord * range, look.zCoord * range), fluids);
        if (real != null && real.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) limit = eye.distanceTo(real.hitVec);
        double entity = targetedEntityDistance(eye);
        if (entity >= 0) limit = Math.min(limit, entity);
        Hit best = null;
        for (SchematicWorld world : ClientProxy.visiblePlacements()) {
            if (!world.isRenderingEnabled()) continue;
            Vec3 start = copy(eye, -world.position.x, -world.position.y, -world.position.z);
            MovingObjectPosition hit = world.rayTraceRendered(copy(start, 0, 0, 0),
                start.addVector(look.xCoord * range, look.yCoord * range, look.zCoord * range), fluids);
            if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) continue;
            double distance = start.distanceTo(hit.hitVec);
            if (distance <= limit + 1.0E-7 && (best == null || distance < best.distance)) best = new Hit(world, hit, distance);
        }
        return best;
    }

    /**
     * The farthest schematic block in front of the targeted real block, or the schematic block in the empty
     * space against the targeted face (getFurthestSchematicWorldBlockBeforeVanilla with a required real block).
     */
    public static Hit furthestBeforeVanilla(double range) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityLivingBase camera = camera();
        if (camera == null || mc.theWorld == null || !VisualSettings.schematicVisible()) return null;
        Vec3 eye = camera.getPosition(1), look = camera.getLook(1);
        MovingObjectPosition real = mc.theWorld.rayTraceBlocks(copy(eye, 0, 0, 0), eye.addVector(look.xCoord * range, look.yCoord * range, look.zCoord * range), false);
        if (real == null || real.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return null;
        double vanilla = eye.distanceTo(real.hitVec);
        double entity = targetedEntityDistance(eye);
        if (entity >= 0 && entity < vanilla) return null;
        Hit[] best = {null};
        for (SchematicWorld world : ClientProxy.visiblePlacements()) {
            if (!world.isRenderingEnabled()) continue;
            Vec3 start = copy(eye, -world.position.x, -world.position.y, -world.position.z);
            Vec3 end = start.addVector(look.xCoord * range, look.yCoord * range, look.zCoord * range);
            VoxelRayTrace.trace(start, end, (x, y, z) -> {
                if (!world.isBlockRendered(x, y, z)) return null;
                Block block = world.getBlock(x, y, z);
                if (block.isAir(world, x, y, z) || !block.canCollideCheck(world.getBlockMetadata(x, y, z), false)) return null;
                MovingObjectPosition hit = block.collisionRayTrace(world, x, y, z, copy(start, 0, 0, 0), copy(end, 0, 0, 0));
                if (hit == null || hit.hitVec == null) return null;
                double distance = start.distanceTo(hit.hitVec);
                int wx = x + world.position.x, wy = y + world.position.y, wz = z + world.position.z;
                if (distance < vanilla && (best[0] == null || distance > best[0].distance)
                    && (wx != real.blockX || wy != real.blockY || wz != real.blockZ)) best[0] = new Hit(world, hit, distance);
                return distance > vanilla ? Boolean.TRUE : null;
            });
        }
        if (best[0] != null) return best[0];
        ForgeDirection side = ForgeDirection.getOrientation(real.sideHit);
        int x = real.blockX + side.offsetX, y = real.blockY + side.offsetY, z = real.blockZ + side.offsetZ;
        if (!RenderLayerSettings.RANGE.contains(x, y, z) || !mc.theWorld.isAirBlock(x, y, z)) return null;
        for (SchematicWorld world : ClientProxy.visiblePlacements()) {
            int lx = x - world.position.x, ly = y - world.position.y, lz = z - world.position.z;
            if (world.isRenderingEnabled() && world.isBlockRendered(lx, ly, lz) && !world.getBlock(lx, ly, lz).isAir(world, lx, ly, lz)) {
                return new Hit(world, x, y, z, real.sideHit);
            }
        }
        return null;
    }

    /** The real world block trace from the camera (RayTraceUtils.getRayTraceFromEntity). */
    public static MovingObjectPosition vanilla(double range, boolean fluids) {
        EntityLivingBase camera = camera();
        if (camera == null || Minecraft.getMinecraft().theWorld == null) return null;
        Vec3 eye = camera.getPosition(1), look = camera.getLook(1);
        return Minecraft.getMinecraft().theWorld.rayTraceBlocks(copy(eye, 0, 0, 0), eye.addVector(look.xCoord * range, look.yCoord * range, look.zCoord * range), fluids);
    }

    /** Litematica's WorldUtils.getValidBlockRange: reach + 1 unless easyPlaceVanillaReach is set. */
    public static double validBlockRange() {
        return Minecraft.getMinecraft().playerController.getBlockReachDistance() + (com.github.lunatrius.schematica.handler.ConfigurationHandler.easyPlaceVanillaReach ? 0 : 1);
    }

    public static ForgeDirection facing() {
        switch (MathHelper.floor_double(Minecraft.getMinecraft().thePlayer.rotationYaw * 4.0F / 360.0F + 0.5D) & 3) {
            case 0: return ForgeDirection.SOUTH;
            case 1: return ForgeDirection.WEST;
            case 2: return ForgeDirection.NORTH;
            default: return ForgeDirection.EAST;
        }
    }
}
