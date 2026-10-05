// Containers that still need items, labelled in the world, after the behavior of Buildprint's container glance, by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.container;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.renderer.hud.InventoryPreview;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.proxy.ClientProxy;

/**
 * containerLabels: a built container whose schematic container holds items gets a "Wanted" label with a few of
 * those items above it: grey while its contents are unknown (a client only learns them by opening it), amber
 * once it was opened and still lacks some. Containers that hold everything get none.
 */
public final class ContainerLabels {
    private static final double RANGE = 12;
    private static final int MAX_LABELS = 12, MAX_ICONS = 3, INTERVAL = 10;
    private static final Map<Long, List<ItemStack>> SEEN = new HashMap<>();
    private static final RenderItem ITEMS = new RenderItem();
    private static World seenWorld;
    private static List<Label> labels = new ArrayList<>();
    private static int ticks;

    private static final class Label {
        final int x, y, z;
        final boolean known;
        final List<ItemStack> items;
        Label(int x, int y, int z, boolean known, List<ItemStack> items) { this.x = x; this.y = y; this.z = z; this.known = known; this.items = items; }
    }

    private ContainerLabels() {}

    static long key(int x, int y, int z) { return ((long) x & 0x3FFFFFF) << 38 | ((long) y & 0xFFF) << 26 | (long) z & 0x3FFFFFF; }

    /** What an opened container of the world holds, by slot, kept for the session. */
    static void seen(World world, int x, int y, int z, List<ItemStack> contents) {
        if (world != seenWorld) {
            SEEN.clear();
            seenWorld = world;
        }
        SEEN.put(key(x, y, z), contents);
    }

    public static void tick(Minecraft mc) {
        if (!ConfigurationHandler.containerLabels || mc.thePlayer == null || mc.theWorld == null) {
            labels = new ArrayList<>();
            return;
        }
        if (mc.theWorld != seenWorld) {
            SEEN.clear();
            seenWorld = mc.theWorld;
        }
        if (ticks++ % INTERVAL != 0) return;
        List<Label> found = new ArrayList<>();
        double px = mc.thePlayer.posX, py = mc.thePlayer.posY, pz = mc.thePlayer.posZ;
        for (SchematicWorld placement : ClientProxy.visiblePlacements()) {
            if (!placement.isRenderingEnabled()) continue;
            for (Object object : placement.getSchematic().getTileEntities()) {
                net.minecraft.tileentity.TileEntity tile = (net.minecraft.tileentity.TileEntity) object;
                if (!(tile instanceof IInventory)) continue;
                int lx = tile.xCoord, ly = tile.yCoord, lz = tile.zCoord;
                int x = placement.position.x + lx, y = placement.position.y + ly, z = placement.position.z + lz;
                double dx = x + 0.5 - px, dy = y + 0.5 - py, dz = z + 0.5 - pz;
                if (dx * dx + dy * dy + dz * dz > RANGE * RANGE || !placement.isBlockRendered(lx, ly, lz)) continue;
                Block block = placement.getBlock(lx, ly, lz);
                if (mc.theWorld.getBlock(x, y, z) != block) continue;
                // a double chest is labelled once, on its half at the lower x or z, and known when either half was opened
                int[] partner = partner(placement, tile, block);
                if (partner != null && (partner[0] < lx || partner[2] < lz)) continue;
                IInventory inventory = InventoryPreview.inventory(placement, lx, ly, lz);
                if (inventory == null) continue;
                List<ItemStack> expected = new ArrayList<>();
                for (int i = 0; i < inventory.getSizeInventory(); i++) expected.add(inventory.getStackInSlot(i));
                List<ItemStack> actual = SEEN.get(key(x, y, z));
                if (actual == null && partner != null) actual = SEEN.get(key(placement.position.x + partner[0], y, placement.position.z + partner[2]));
                List<ItemStack> missing = ContainerComparison.missing(expected, actual);
                if (!missing.isEmpty()) found.add(new Label(x, y, z, actual != null, missing));
            }
        }
        found.sort((a, b) -> Double.compare(distance(a, px, py, pz), distance(b, px, py, pz)));
        labels = found.size() > MAX_LABELS ? new ArrayList<>(found.subList(0, MAX_LABELS)) : found;
    }

    private static int[] partner(SchematicWorld placement, net.minecraft.tileentity.TileEntity tile, Block block) {
        if (!(tile instanceof net.minecraft.tileentity.TileEntityChest)) return null;
        int[][] sides = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] side : sides) {
            int x = tile.xCoord + side[0], z = tile.zCoord + side[1];
            if (placement.getBlock(x, tile.yCoord, z) == block && placement.getTileEntity(x, tile.yCoord, z) instanceof net.minecraft.tileentity.TileEntityChest) {
                return new int[] {x, tile.yCoord, z};
            }
        }
        return null;
    }

    private static double distance(Label label, double x, double y, double z) {
        double dx = label.x + 0.5 - x, dy = label.y + 0.5 - y, dz = label.z + 0.5 - z;
        return dx * dx + dy * dy + dz * dz;
    }

    /** Billboards above the containers, facing the camera; world coordinates minus the camera. */
    public static void render(double cx, double cy, double cz) {
        List<Label> current = labels;
        if (current.isEmpty()) return;
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer font = mc.fontRenderer;
        RenderManager manager = RenderManager.instance;
        String wanted = UiTranslations.format("schematica.container.wanted");
        for (Label label : current) {
            GL11.glPushMatrix();
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LIGHTING_BIT);
            GL11.glTranslated(label.x + 0.5 - cx, label.y + 1.45 - cy, label.z + 0.5 - cz);
            GL11.glRotatef(-manager.playerViewY, 0, 1, 0);
            GL11.glRotatef(manager.playerViewX, 1, 0, 0);
            GL11.glScalef(-0.025f, -0.025f, 0.025f);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            int icons = Math.min(MAX_ICONS, label.items.size());
            int width = Math.max(font.getStringWidth(wanted), icons * 18 - 2);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(0, 0, 0, 0.4f);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex3f(-width / 2f - 2, -2, 0.01f);
            GL11.glVertex3f(-width / 2f - 2, 29, 0.01f);
            GL11.glVertex3f(width / 2f + 2, 29, 0.01f);
            GL11.glVertex3f(width / 2f + 2, -2, 0.01f);
            GL11.glEnd();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1, 1, 1, 1);
            font.drawString(wanted, -font.getStringWidth(wanted) / 2, 0, label.known ? 0xFFFFAA00 : 0xFFAAAAAA);
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            RenderHelper.enableGUIStandardItemLighting();
            for (int i = 0; i < icons; i++) {
                ItemStack stack = label.items.get(i);
                int x = -icons * 18 / 2 + 1 + i * 18;
                GL11.glPushMatrix();
                GL11.glScalef(1, 1, 0.001f);
                ITEMS.renderItemAndEffectIntoGUI(font, mc.getTextureManager(), stack, x, 10);
                GL11.glPopMatrix();
                ITEMS.renderItemOverlayIntoGUI(font, mc.getTextureManager(), stack, x, 10, String.valueOf(stack.stackSize));
            }
            RenderHelper.disableStandardItemLighting();
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }
}
