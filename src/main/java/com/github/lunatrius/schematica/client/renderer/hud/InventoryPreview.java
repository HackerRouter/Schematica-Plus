// SPDX-License-Identifier: LGPL-3.0-only
// Litematica RenderUtils.renderInventoryOverlay(s) and MaLiLib InventoryOverlay, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.renderer.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import com.github.lunatrius.schematica.client.gui.framework.MinecraftUiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.reference.Reference;

/** The contents of the looked at container in the schematic (left) and in the world (right), above the block info overlay. */
public final class InventoryPreview {
    private static final String CHEST = "textures/gui/container/generic_54.png";
    private static final String FURNACE = "textures/gui/container/furnace.png";
    private static final String BREWING_STAND = "textures/gui/container/brewing_stand.png";
    private static final String DISPENSER = "textures/gui/container/dispenser.png";
    private static final String HOPPER = "textures/gui/container/hopper.png";
    private static final int MAX_ROWS = 9;

    private InventoryPreview() {}

    /** The inventory at a position, joining double chests like the chest block does. */
    public static IInventory inventory(World world, int x, int y, int z) {
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
    public static World bestWorld(Minecraft mc) {
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

    private enum Type { FURNACE, BREWING_STAND, DISPENSER, HOPPER, GENERIC }

    private static Type type(IInventory inventory, int size) {
        if (inventory instanceof TileEntityFurnace && size == 3) return Type.FURNACE;
        if (inventory instanceof TileEntityBrewingStand && size == 4) return Type.BREWING_STAND;
        if (inventory instanceof TileEntityDispenser && size == 9) return Type.DISPENSER;
        if (inventory instanceof TileEntityHopper && size == 5) return Type.HOPPER;
        return Type.GENERIC;
    }

    private static void piece(MinecraftUiDraw draw, String texture, int x, int y, int u, int v, int width, int height) {
        draw.texture(texture, new UiBounds(x, y, width, height), u, v, width, height, 256, 256);
    }

    private static void frame(MinecraftUiDraw draw, String texture, int x, int y, int[][] pieces) {
        for (int[] p : pieces) piece(draw, texture, x + p[0], y + p[1], p[2], p[3], p[4], p[5]);
    }

    /** side: -1 left of the center, 1 right of it, 0 centered. */
    private static int draw(MinecraftUiDraw draw, IInventory inventory, int side, boolean center, int offsetY, int screenWidth, int screenHeight) {
        int size = Math.max(0, inventory.getSizeInventory());
        if (size == 0) return 0;
        Type type = type(inventory, size);
        int perRow = perRow(inventory, size);
        int rows = Math.min(MAX_ROWS, (size + perRow - 1) / perRow);
        int bgw = Math.min(size, perRow) * 18 + 7, bgh = rows * 18 + 7;
        int width = type == Type.FURNACE ? 96 : type == Type.BREWING_STAND ? 117 : type == Type.DISPENSER ? 68 : type == Type.HOPPER ? 104 : bgw + 7;
        int height = type == Type.FURNACE ? 68 : type == Type.BREWING_STAND ? 72 : type == Type.DISPENSER ? 68 : type == Type.HOPPER ? 32 : bgh + 7;
        int x = screenWidth / 2 - width / 2 + side * (width / 2 + 4);
        int y = center ? screenHeight / 2 - height - offsetY : offsetY;
        int[][] slots;
        switch (type) {
            case FURNACE:
                frame(draw, FURNACE, x, y, new int[][] {{0, 0, 0, 0, 4, 64}, {4, 0, 84, 0, 92, 4}, {0, 64, 0, 162, 92, 4}, {92, 4, 172, 102, 4, 64}, {4, 4, 52, 13, 88, 60}});
                slots = new int[][] {{8, 8}, {8, 44}, {68, 26}};
                break;
            case BREWING_STAND:
                frame(draw, BREWING_STAND, x, y, new int[][] {{0, 0, 0, 0, 4, 68}, {4, 0, 63, 0, 113, 4}, {0, 68, 0, 162, 113, 4}, {113, 4, 172, 98, 4, 68}, {4, 4, 13, 13, 109, 64}});
                slots = new int[][] {{47, 37}, {70, 44}, {93, 37}, {70, 8}};
                break;
            case DISPENSER:
                frame(draw, DISPENSER, x, y, new int[][] {{0, 0, 0, 0, 7, 61}, {7, 0, 115, 0, 61, 7}, {0, 61, 0, 159, 61, 7}, {61, 7, 169, 105, 7, 61}, {7, 7, 61, 16, 54, 54}});
                slots = grid(size, 3, 8);
                break;
            case HOPPER:
                frame(draw, HOPPER, x, y, new int[][] {{0, 0, 0, 0, 7, 25}, {7, 0, 79, 0, 97, 7}, {0, 25, 0, 126, 97, 7}, {97, 7, 169, 108, 7, 25}, {7, 7, 43, 19, 90, 18}});
                slots = grid(size, 5, 8);
                break;
            default:
                frame(draw, CHEST, x, y, new int[][] {{0, 0, 0, 0, 7, bgh}, {7, 0, 176 - bgw, 0, bgw, 7}, {0, bgh, 0, 215, bgw, 7}, {bgw, 7, 169, 222 - bgh, 7, bgh}});
                for (int row = 0; row < rows; row++) {
                    int length = Math.max(1, Math.min(perRow, size - row * perRow));
                    piece(draw, CHEST, x + 7, y + 7 + row * 18, 7, 17, length * 18, 18);
                    if (rows > 1 && length < perRow) {
                        piece(draw, CHEST, x + 7 + length * 18, y + 7 + row * 18, 7, 3, (perRow - length) * 18, 9);
                        piece(draw, CHEST, x + 7 + length * 18, y + 16 + row * 18, 7, 3, (perRow - length) * 18, 9);
                    }
                }
                slots = grid(Math.min(size, rows * perRow), perRow, 8);
        }
        for (int i = 0; i < slots.length; i++) {
            ItemStack stack;
            try { stack = inventory.getStackInSlot(i); }
            catch (RuntimeException error) { stack = null; }
            if (stack == null || stack.getItem() == null) continue;
            int sx = x + slots[i][0], sy = y + slots[i][1];
            try {
                draw.item(stack, sx, sy);
                if (stack.stackSize > 1) {
                    String count = Integer.toString(stack.stackSize);
                    draw.shadowText(count, sx + 17 - draw.textWidth(count), sy + 9, 0xFFFFFFFF);
                }
            } catch (RuntimeException error) {
                Reference.logger.debug("Could not draw an inventory preview item", error);
            }
        }
        return height;
    }

    private static int[][] grid(int count, int perRow, int offset) {
        int[][] slots = new int[count][];
        for (int i = 0; i < count; i++) slots[i] = new int[] {offset + i % perRow * 18, offset + i / perRow * 18};
        return slots;
    }
}
