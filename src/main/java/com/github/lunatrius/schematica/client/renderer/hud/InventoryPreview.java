// SPDX-License-Identifier: LGPL-3.0-only
// Litematica RenderUtils.renderInventoryOverlay(s) and MaLiLib InventoryOverlay, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.renderer.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import com.github.lunatrius.schematica.client.gui.framework.MinecraftUiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.reference.Reference;

/** The contents of the looked at container in the schematic (left) and in the world (right), above the block info overlay. */
public final class InventoryPreview {
    private static final String TEXTURE = "textures/gui/container/generic_54.png";
    private static final int MAX_ROWS = 9;

    private InventoryPreview() {}

    /** The inventory at a position, joining double chests like the chest block does. */
    static IInventory inventory(World world, int x, int y, int z) {
        if (world == null) return null;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof IInventory)) return null;
        if (tile instanceof TileEntityChest) {
            int[][] sides = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] side : sides) {
                TileEntity other = world.getTileEntity(x + side[0], y, z + side[1]);
                if (other instanceof TileEntityChest && world.getBlock(x + side[0], y, z + side[1]) == world.getBlock(x, y, z)) {
                    boolean first = side[0] < 0 || side[1] < 0;
                    IInventory a = first ? (IInventory) other : (IInventory) tile, b = first ? (IInventory) tile : (IInventory) other;
                    return new InventoryLargeChest("container.chestDouble", a, b);
                }
            }
        }
        return (IInventory) tile;
    }

    /** WorldUtils.getBestWorld: the integrated server's world, whose containers hold their items, else the client world. */
    static World bestWorld(Minecraft mc) {
        if (mc.theWorld == null) return null;
        try {
            MinecraftServer server = mc.isIntegratedServerRunning() ? MinecraftServer.getServer() : null;
            WorldServer world = server == null ? null : server.worldServerForDimension(mc.theWorld.provider.dimensionId);
            if (world != null) return world;
        } catch (RuntimeException ignored) {}
        return mc.theWorld;
    }

    /** Draws the previews of a position; returns their height, or 0 without containers. */
    public static int render(Minecraft mc, SchematicWorld schematic, int x, int y, int z, boolean center, int offsetY, int screenWidth, int screenHeight) {
        IInventory expected = null, found = null;
        try {
            if (schematic != null) expected = inventory(schematic, x - schematic.position.x, y - schematic.position.y, z - schematic.position.z);
            found = inventory(bestWorld(mc), x, y, z);
        } catch (RuntimeException error) {
            Reference.logger.debug("Could not read a container for the inventory preview", error);
        }
        if (expected == null && found == null) return 0;
        int height = 0;
        try (MinecraftUiDraw draw = new MinecraftUiDraw(mc)) {
            if (expected != null) height = Math.max(height, draw(draw, expected, found != null ? -1 : 0, center, offsetY, screenWidth, screenHeight));
            if (found != null) height = Math.max(height, draw(draw, found, expected != null ? 1 : 0, center, offsetY, screenWidth, screenHeight));
        }
        return height;
    }

    static int perRow(IInventory inventory, int size) {
        if (inventory instanceof TileEntityDispenser && size == 9) return 3;
        if (inventory instanceof TileEntityHopper) return 5;
        return Math.min(9, Math.max(1, size));
    }

    /** side: -1 left of the center, 1 right of it, 0 centered. */
    private static int draw(MinecraftUiDraw draw, IInventory inventory, int side, boolean center, int offsetY, int screenWidth, int screenHeight) {
        int size = Math.max(0, inventory.getSizeInventory());
        if (size == 0) return 0;
        int perRow = perRow(inventory, size);
        int rows = Math.min(MAX_ROWS, (size + perRow - 1) / perRow);
        int width = perRow * 18 + 14, height = rows * 18 + 14;
        int x = screenWidth / 2 - width / 2 + side * (width / 2 + 4);
        int y = center ? screenHeight / 2 - height - offsetY : offsetY;
        UiBounds box = new UiBounds(x, y, width, height);
        draw.fill(box, 0xFFC6C6C6);
        draw.fill(new UiBounds(x, y, width - 1, 1), 0xFFFFFFFF);
        draw.fill(new UiBounds(x, y, 1, height - 1), 0xFFFFFFFF);
        draw.fill(new UiBounds(x + 1, y + height - 1, width - 1, 1), 0xFF555555);
        draw.fill(new UiBounds(x + width - 1, y + 1, 1, height - 1), 0xFF555555);
        int shown = Math.min(size, rows * perRow);
        for (int i = 0; i < shown; i++) {
            int sx = x + 7 + i % perRow * 18, sy = y + 7 + i / perRow * 18;
            draw.texture(TEXTURE, new UiBounds(sx, sy, 18, 18), 7, 17, 18, 18, 256, 256);
            ItemStack stack;
            try { stack = inventory.getStackInSlot(i); }
            catch (RuntimeException error) { stack = null; }
            if (stack == null || stack.getItem() == null) continue;
            try {
                draw.item(stack, sx + 1, sy + 1);
                if (stack.stackSize > 1) {
                    String count = Integer.toString(stack.stackSize);
                    draw.text(count, sx + 18 - draw.textWidth(count), sy + 10, 0xFFFFFFFF);
                }
            } catch (RuntimeException error) {
                Reference.logger.debug("Could not draw an inventory preview item", error);
            }
        }
        return height;
    }
}
