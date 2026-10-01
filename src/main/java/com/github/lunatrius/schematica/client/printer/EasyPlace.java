// SPDX-License-Identifier: LGPL-3.0-only
// Litematica Easy Place (WorldUtils.doEasyPlaceAction, placement protocol "None"), adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.printer;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.input.Hotkeys;
import com.github.lunatrius.schematica.client.printer.registry.PlacementData;
import com.github.lunatrius.schematica.client.printer.registry.PlacementRegistry;
import com.github.lunatrius.schematica.client.util.BlockToItemStack;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.BlockInfoHudSettings;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.tool.SchematicTargets;
import com.github.lunatrius.schematica.tool.ToolManager;
import com.github.lunatrius.schematica.tool.ToolMode;

/**
 * Places the targeted schematic block with the item it needs. On a Plus server with accurate placement the server sets
 * the schematic's metadata, as upstream with protocol v3; otherwise orientation follows the clicked face, the hit
 * height and the player's facing, as upstream with protocol "None".
 */
public final class EasyPlace {
    public enum Result { PASS, SUCCESS, FAIL }

    private static final long CACHE_NANOS = 2_000_000_000L;
    private static final List<long[]> PLACED = new ArrayList<>();
    private static long lastPickTime = System.nanoTime();

    private EasyPlace() {}

    private static Minecraft mc() { return Minecraft.getMinecraft(); }

    /** The easyPlaceUseKey press (WorldUtils.handleEasyPlace); returns whether the use action is consumed. */
    public static boolean handle() {
        if (!ConfigurationHandler.easyPlaceMode || ToolManager.getCurrentMode() == ToolMode.REBUILD) return false;
        Result result = act();
        if (result == Result.FAIL) {
            warn("litematica.message.easy_place_fail");
            return true;
        }
        return result != Result.PASS;
    }

    /** Repeats Easy Place every client tick while the use key is held (easyPlaceOnUseTick). */
    public static void tick(Minecraft mc) {
        if (mc.thePlayer != null && mc.currentScreen == null && ConfigurationHandler.easyPlaceMode && ConfigurationHandler.easyPlaceHoldEnabled
            && ToolManager.getCurrentMode() != ToolMode.REBUILD && Hotkeys.held("easyPlaceUseKey")) act();
    }

    /** Prints a warning according to placementRestrictionWarn. */
    public static void warn(String key) {
        if ("message".equals(ConfigurationHandler.placementRestrictionWarn)) {
            if (mc().thePlayer != null) mc().thePlayer.addChatMessage(new ChatComponentTranslation(key));
        } else if ("actionbar".equals(ConfigurationHandler.placementRestrictionWarn) && mc().ingameGUI != null) {
            mc().ingameGUI.func_110326_a(UiTranslations.format(key), false);
        }
    }

    static Result act() {
        Minecraft mc = mc();
        EntityClientPlayerMP player = mc.thePlayer;
        World world = mc.theWorld;
        if (player == null || world == null || ClientProxy.loadedSchematics.isEmpty()) return Result.PASS;
        double range = SchematicTargets.validBlockRange();
        SchematicTargets.Hit hit = ConfigurationHandler.easyPlaceFirst ? SchematicTargets.closest(range, BlockInfoHudSettings.targetFluids)
            : SchematicTargets.furthestBeforeVanilla(range);
        if (hit == null) return PlacementRestriction.inEffect(mc) ? Result.FAIL : Result.PASS;
        SchematicWorld schematic = hit.world;
        int lx = hit.localX(), ly = hit.localY(), lz = hit.localZ(), x = hit.x, y = hit.y, z = hit.z;
        Block block = schematic.getBlock(lx, ly, lz);
        int meta = schematic.getBlockMetadata(lx, ly, lz);
        if (cached(x, y, z) || System.nanoTime() - lastPickTime < 1_000_000L * ConfigurationHandler.easyPlaceSwapInterval) return Result.FAIL;
        Block real = world.getBlock(x, y, z);
        int realMeta = world.getBlockMetadata(x, y, z);
        if (block == real && meta == realMeta) return Result.FAIL;
        SchematicPrinter printer = SchematicPrinter.INSTANCE;
        if (FluidPrinter.isFluid(block)) {
            if (FluidPrinter.matches(schematic, lx, ly, lz, world, x, y, z)) return Result.FAIL;
            if (!world.isAirBlock(x, y, z) && !(FluidPrinter.isFluid(real) && FluidPrinter.sameFluid(block, real))) return Result.FAIL;
            cache(x, y, z);
            FluidPrinter.place(mc, FluidPrinter.source(schematic, lx, ly, lz), x, y, z,
                bucket -> pick(player, () -> printer.swapToItem(player.inventory, bucket, true, true)));
            return Result.SUCCESS;
        }
        ItemStack stack = BlockToItemStack.getItemStack(player, block, schematic, lx, ly, lz);
        if (stack == null || stack.getItem() == null) return Result.SUCCESS;
        boolean slab = completesSlab(block, meta, real, realMeta, stack);
        if (!slab && !real.isReplaceable(world, x, y, z)) return Result.FAIL;
        if (!pickStack(player, stack)) return Result.FAIL;
        ItemStack held = player.getCurrentEquippedItem();
        if (held == null || !held.isItemEqual(stack)) return Result.FAIL;

        boolean accurate = AccuratePlacementClient.active(block);
        int cx = x, cy = y, cz = z, side = hit.side, extraClicks = 0;
        Vec3 hitVec = Vec3.createVectorHelper(hit.hitX, hit.hitY, hit.hitZ);
        if (slab) {
            side = (realMeta & 8) != 0 ? 0 : 1;
            hitVec = Vec3.createVectorHelper(x + 0.5, y + (side == 1 ? 1 : 0), z + 0.5);
        } else {
            PlacementData data = PlacementRegistry.INSTANCE.getPlacementData(block, stack);
            if (data != null) {
                ForgeDirection[] solid = printer.getSolidSides(world, x, y, z);
                ForgeDirection[] valid = data.getValidDirections(solid, meta);
                if (valid.length == 0 && accurate) valid = solid;
                if (valid.length > 0) {
                    ForgeDirection direction = valid[0];
                    cx = x + direction.offsetX; cy = y + direction.offsetY; cz = z + direction.offsetZ;
                    side = direction.getOpposite().ordinal();
                    hitVec = Vec3.createVectorHelper(cx, cy + data.getOffsetFromMetadata(meta), cz);
                } else if (!accurate && data.type == PlacementData.PlacementType.BLOCK && !data.mapping.isEmpty()) {
                    return Result.FAIL;
                } else {
                    side = ForgeDirection.NORTH.ordinal();
                    hitVec = Vec3.createVectorHelper(x + 0.5, y + (data.getOffsetFromMetadata(meta) >= 0.5f ? 0.75 : 0.25), z);
                }
                extraClicks = data.getExtraClicks(block, meta);
            } else {
                MovingObjectPosition vanilla = SchematicTargets.vanilla(range, false);
                if (vanilla != null && vanilla.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                    && !world.getBlock(vanilla.blockX, vanilla.blockY, vanilla.blockZ).isReplaceable(world, vanilla.blockX, vanilla.blockY, vanilla.blockZ)) {
                    ForgeDirection face = ForgeDirection.getOrientation(vanilla.sideHit);
                    if (vanilla.blockX + face.offsetX == x && vanilla.blockY + face.offsetY == y && vanilla.blockZ + face.offsetZ == z) {
                        cx = vanilla.blockX; cy = vanilla.blockY; cz = vanilla.blockZ;
                        side = vanilla.sideHit;
                        hitVec = vanilla.hitVec;
                    }
                }
            }
        }

        cache(x, y, z);
        if (accurate) AccuratePlacementClient.announce(x, y, z, block, meta);
        boolean sneaking = player.isSneaking();
        printer.syncSneaking(player, true);
        try {
            boolean success = click(player, world, held, cx, cy, cz, side, hitVec);
            for (int i = 0; success && i < extraClicks; i++) success = click(player, world, held, cx, cy, cz, side, hitVec);
        } finally {
            printer.syncSneaking(player, sneaking);
        }
        if (held.stackSize == 0) player.inventory.mainInventory[player.inventory.currentItem] = null;
        return Result.SUCCESS;
    }

    interface Swap { boolean run(); }

    /** Selects the needed item; a changed hotbar slot or stack starts the easyPlaceSwapInterval wait. */
    private static boolean pick(EntityClientPlayerMP player, Swap swap) {
        int slot = player.inventory.currentItem;
        ItemStack before = player.getCurrentEquippedItem();
        if (!swap.run()) return false;
        if (slot != player.inventory.currentItem || before != player.getCurrentEquippedItem()) lastPickTime = System.nanoTime();
        return true;
    }

    /** InventoryUtils.schematicWorldPickBlock: creative players get the item in the selected slot when it is not in the hotbar. */
    private static boolean pickStack(EntityClientPlayerMP player, ItemStack stack) {
        if (pick(player, () -> SchematicPrinter.INSTANCE.swapToItem(player.inventory, stack, true, false))) return true;
        if (!mc().playerController.isInCreativeMode()) return false;
        player.inventory.setInventorySlotContents(player.inventory.currentItem, stack.copy());
        mc().playerController.sendSlotPacket(player.getCurrentEquippedItem(), player.inventoryContainer.inventorySlots.size() - 9 + player.inventory.currentItem);
        lastPickTime = System.nanoTime();
        return true;
    }


    /** A single slab in the world that the schematic's double slab of the same item completes. */
    private static boolean completesSlab(Block block, int meta, Block real, int realMeta, ItemStack stack) {
        return block instanceof BlockSlab && block.isOpaqueCube() && real instanceof BlockSlab && !real.isOpaqueCube()
            && Item.getItemFromBlock(real) == stack.getItem() && (realMeta & 7) == (meta & 7);
    }

    private static boolean click(EntityClientPlayerMP player, World world, ItemStack stack, int x, int y, int z, int side, Vec3 hitVec) {
        if (ForgeEventFactory.onPlayerInteract(player, Action.RIGHT_CLICK_BLOCK, x, y, z, side, world).isCanceled()) return false;
        boolean success = mc().playerController.onPlayerRightClick(player, world, stack, x, y, z, side, hitVec);
        if (success && ConfigurationHandler.easyPlaceSwingHand) player.swingItem();
        return success;
    }

    private static boolean cached(int x, int y, int z) {
        long now = System.nanoTime();
        PLACED.removeIf(entry -> now - entry[3] > CACHE_NANOS);
        for (long[] entry : PLACED) if (entry[0] == x && entry[1] == y && entry[2] == z) return true;
        return false;
    }

    private static void cache(int x, int y, int z) { PLACED.add(new long[] {x, y, z, System.nanoTime()}); }
}
