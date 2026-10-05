// SPDX-License-Identifier: LGPL-3.0-only
// Litematica Placement Restriction (WorldUtils.placementRestrictionInEffect), adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.printer;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.client.printer.registry.PlacementData;
import com.github.lunatrius.schematica.client.printer.registry.PlacementRegistry;
import com.github.lunatrius.schematica.client.util.BlockToItemStack;
import com.github.lunatrius.schematica.client.world.CellState;
import com.github.lunatrius.schematica.client.world.RenderLayerSettings;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.client.world.SubRegionPlacements;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.tool.SchematicRebuild;

public final class PlacementRestriction {
    private PlacementRestriction() {}

    /** WorldUtils.handlePlacementRestriction: returns true, with the configured warning, when the use action must be cancelled. */
    public static boolean handle(Minecraft mc) {
        boolean cancel = inEffect(mc);
        if (cancel) EasyPlace.warn("litematica.message.placement_restriction_fail");
        return cancel;
    }

    /**
     * Whether using the held item on the targeted block would place something the schematic does not want there:
     * outside the layer range, where the schematic has air near its regions, into an occupied position, with the wrong
     * item, or with a known orientation that differs from the schematic's.
     */
    static boolean inEffect(Minecraft mc) {
        ItemStack held = mc.thePlayer == null ? null : mc.thePlayer.getHeldItem();
        MovingObjectPosition trace = mc.objectMouseOver;
        World world = mc.theWorld;
        if (held == null || world == null || trace == null || trace.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return false;
        int x = trace.blockX, y = trace.blockY, z = trace.blockZ;
        Block clicked = world.getBlock(x, y, z);
        if (clicked != Blocks.snow_layer && !clicked.isReplaceable(world, x, y, z)) {
            ForgeDirection side = ForgeDirection.getOrientation(trace.sideHit);
            x += side.offsetX; y += side.offsetY; z += side.offsetZ;
        }
        SchematicWorld schematic = schematicAt(x, y, z);
        if (schematic != null && !RenderLayerSettings.RANGE.contains(x, y, z)) return true;
        if (schematic == null) return nearRegions(x, y, z, 2);
        if (!world.getBlock(x, y, z).isReplaceable(world, x, y, z)) return true;
        int lx = x - schematic.position.x, ly = y - schematic.position.y, lz = z - schematic.position.z;
        Block block = schematic.getSchematic().getBlock(lx, ly, lz);
        int meta = schematic.getSchematic().getBlockMetadata(lx, ly, lz);
        ItemStack required = BlockToItemStack.getItemStack(mc.thePlayer, block, schematic, lx, ly, lz);
        ItemStack replaced = MaterialReplacements.replacement(schematic, required);
        if (replaced != null && held.isItemEqual(replaced)) return false;
        if (required != null && required.getItem() != null && !held.isItemEqual(required)) return true;
        PlacementData data = required == null ? null : PlacementRegistry.INSTANCE.getPlacementData(block, required);
        if (data == null) return false;
        float hx = (float) (trace.hitVec.xCoord - trace.blockX), hy = (float) (trace.hitVec.yCoord - trace.blockY), hz = (float) (trace.hitVec.zCoord - trace.blockZ);
        CellState placed = SchematicRebuild.simulate(mc.thePlayer, held, trace.sideHit, hx, hy, hz, x, y, z);
        return placed != null && placed.block == block && (placed.meta & data.maskMeta) != (meta & data.maskMeta);
    }

    /** The enabled placement whose schematic has a non-air block at the world position, ignoring the layer range. */
    private static SchematicWorld schematicAt(int x, int y, int z) {
        for (SchematicWorld world : ClientProxy.visiblePlacements()) {
            if (!world.isEnabled()) continue;
            int lx = x - world.position.x, ly = y - world.position.y, lz = z - world.position.z;
            if (!world.getSchematic().containsBlock(lx, ly, lz)) continue;
            Block block = world.getSchematic().getBlock(lx, ly, lz);
            if (block != null && block != Blocks.air) return world;
        }
        return null;
    }

    /** WorldUtils.isPositionWithinRangeOfSchematicRegions. */
    static boolean nearRegions(int x, int y, int z, int range) {
        for (SchematicWorld world : ClientProxy.visiblePlacements()) {
            if (!world.isEnabled() || world.subregions() == null) continue;
            for (SubRegionPlacements.Region region : world.subregions().regions()) {
                if (!region.enabled) continue;
                SchematicRegion box = world.subregionBounds(region.name());
                if (x >= box.minX - range && x <= box.maxX + range && y >= box.minY - range && y <= box.maxY + range
                    && z >= box.minZ - range && z <= box.maxZ + range) return true;
            }
        }
        return false;
    }
}
