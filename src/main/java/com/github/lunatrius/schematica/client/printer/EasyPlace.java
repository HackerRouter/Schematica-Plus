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
        if (player == null || world == null || ClientProxy.visiblePlacements().isEmpty()) return Result.PASS;
        double range = SchematicTargets.validBlockRange();
        SchematicTargets.Hit hit = ConfigurationHandler.easyPlaceFirst ? SchematicTargets.closest(range, BlockInfoHudSettings.targetFluids)
            : SchematicTargets.furthestBeforeVanilla(range);
        if (hit == null) return PlacementRestriction.inEffect(mc) ? Result.FAIL : Result.PASS;
        SchematicWorld schematic = hit.world;
        int lx = hit.localX(), ly = hit.localY(), lz = hit.localZ(), x = hit.x, y = hit.y, z = hit.z;
        Block block = schematic.getBlock(lx, ly, lz);
        int meta = schematic.getBlockMetadata(lx, ly, lz);
        MultiBlockPlacement.Kind kind = MultiBlockPlacement.kind(block);
        if (kind != null) return multiBlock(player, world, schematic, lx, ly, lz, x, y, z, block, meta, kind);
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
        if (MaterialReplacements.built(schematic, stack, real, realMeta)) return Result.FAIL;
        ItemStack replaced = MaterialReplacements.replacement(schematic, stack);
        if (replaced != null) stack = replaced;
        boolean slab = completesSlab(block, meta, real, realMeta, stack);
        if (!slab && !real.isReplaceable(world, x, y, z)) return Result.FAIL;
        if (!pickStack(player, stack)) return Result.FAIL;
        ItemStack held = player.getCurrentEquippedItem();
        if (held == null || !held.isItemEqual(stack)) return Result.FAIL;

        boolean accurate = AccuratePlacementClient.active(block);
        PlacementSolver.Solution solved = null;
        int cx = x, cy = y, cz = z, side = hit.side, extraClicks = 0;
        Vec3 hitVec = Vec3.createVectorHelper(hit.hitX, hit.hitY, hit.hitZ);
        Click post = ConfigurationHandler.easyPlacePostRewrite && !accurate
            ? PostRewrite.click(world, x, y, z, block, meta, real, realMeta, stack, hitVec) : null;
        if (post == Click.FAIL) return Result.FAIL;
        if (post != null) {
            cx = post.x; cy = post.y; cz = post.z; side = post.side; hitVec = post.hit;
        } else if (slab) {
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
            } else if ((solved = ConfigurationHandler.printerPlacementSolver && !accurate
                ? PlacementSolver.solve(world, player, held, block, meta, x, y, z, printer.getSolidSides(world, x, y, z), true) : null) != null) {
                if (solved == PlacementSolver.REFUSE) return Result.FAIL;
                cx = solved.x; cy = solved.y; cz = solved.z; side = solved.side; hitVec = solved.hit;
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
        float yaw = player.rotationYaw, pitch = player.rotationPitch;
        printer.syncSneaking(player, true);
        if (solved != null && solved.look != null) look(player, solved.look.yaw, solved.look.pitch);
        try {
            boolean success = click(player, world, held, cx, cy, cz, side, hitVec);
            for (int i = 0; success && i < extraClicks; i++) success = click(player, world, held, cx, cy, cz, side, hitVec);
            // Post-Rewrite: a double slab gets its second half right away
            if (success && post != null && block instanceof BlockSlab && block.isOpaqueCube() && held.stackSize > 0
                && completesSlab(block, meta, world.getBlock(x, y, z), world.getBlockMetadata(x, y, z), stack)) {
                boolean top = (world.getBlockMetadata(x, y, z) & 8) != 0;
                click(player, world, held, x, y, z, top ? 0 : 1, Vec3.createVectorHelper(x + 0.5, y + 0.5, z + 0.5));
            }
        } finally {
            if (solved != null && solved.look != null) look(player, yaw, pitch);
            printer.syncSneaking(player, sneaking);
        }
        if (held.stackSize == 0) player.inventory.mainInventory[player.inventory.currentItem] = null;
        return Result.SUCCESS;
    }

    /** Turns the player and reports the rotation, so that the server places the block facing that way. */
    private static void look(EntityClientPlayerMP player, float yaw, float pitch) {
        player.rotationYaw = yaw;
        player.rotationPitch = pitch;
        player.sendQueue.addToSendQueue(new net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook(yaw, pitch, player.onGround));
    }

    /**
     * A door, bed or double plant: the item is used on the top of the block below its first block, also when the
     * upper half or the bed head is targeted; the facing follows the player unless the server applies it.
     */
    private static Result multiBlock(EntityClientPlayerMP player, World world, SchematicWorld schematic, int lx, int ly, int lz,
        int x, int y, int z, Block block, int meta, MultiBlockPlacement.Kind kind) {
        if (MultiBlockPlacement.secondary(meta)) {
            int[] offset = MultiBlockPlacement.secondOffset(kind, meta);
            lx -= offset[0]; ly -= offset[1]; lz -= offset[2];
            x -= offset[0]; y -= offset[1]; z -= offset[2];
            meta = schematic.getBlockMetadata(lx, ly, lz);
            if (schematic.getBlock(lx, ly, lz) != block || MultiBlockPlacement.secondary(meta)) return Result.FAIL;
        }
        if (cached(x, y, z) || System.nanoTime() - lastPickTime < 1_000_000L * ConfigurationHandler.easyPlaceSwapInterval) return Result.FAIL;
        int[] offset = MultiBlockPlacement.secondOffset(kind, meta);
        int sx = x + offset[0], sy = y + offset[1], sz = z + offset[2];
        Block real = world.getBlock(x, y, z), below = world.getBlock(x, y - 1, z);
        if (real == block || !real.isReplaceable(world, x, y, z) || !world.getBlock(sx, sy, sz).isReplaceable(world, sx, sy, sz)
            || below.isAir(world, x, y - 1, z) || below.isReplaceable(world, x, y - 1, z)) return Result.FAIL;
        ItemStack stack = BlockToItemStack.getItemStack(player, block, schematic, lx, ly, lz);
        if (stack == null || stack.getItem() == null) return Result.SUCCESS;
        ItemStack replaced = MaterialReplacements.replacement(schematic, stack);
        if (replaced != null) stack = replaced;
        if (!pickStack(player, stack)) return Result.FAIL;
        ItemStack held = player.getCurrentEquippedItem();
        if (held == null || !held.isItemEqual(stack)) return Result.FAIL;
        cache(x, y, z);
        if (kind == MultiBlockPlacement.Kind.DOOR && AccuratePlacementClient.active(block)) {
            AccuratePlacementClient.announce(x, y, z, block, meta);
            int secondMeta = schematic.getBlockMetadata(lx + offset[0], ly + offset[1], lz + offset[2]);
            if (schematic.getBlock(lx + offset[0], ly + offset[1], lz + offset[2]) == block && MultiBlockPlacement.secondary(secondMeta)) {
                AccuratePlacementClient.announce(sx, sy, sz, block, secondMeta);
            }
        }
        SchematicPrinter printer = SchematicPrinter.INSTANCE;
        boolean sneaking = player.isSneaking();
        printer.syncSneaking(player, true);
        try {
            click(player, world, held, x, y - 1, z, ForgeDirection.UP.ordinal(), Vec3.createVectorHelper(x + 0.5, y, z + 0.5));
        } finally {
            printer.syncSneaking(player, sneaking);
        }
        if (held.stackSize == 0) player.inventory.mainInventory[player.inventory.currentItem] = null;
        return Result.SUCCESS;
    }

    interface Swap { boolean run(); }

    /** A click on a block face; FAIL when no safe click exists. */
    static final class Click {
        static final Click FAIL = new Click(0, 0, 0, 0, null);
        final int x, y, z, side;
        final Vec3 hit;
        Click(int x, int y, int z, int side, Vec3 hit) { this.x = x; this.y = y; this.z = z; this.side = side; this.hit = hit; }
    }

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
        return pick(player, () -> PickBlockSlots.pickToHand(mc(), stack, false));
    }


    /** A single slab in the world that the schematic's double slab of the same item completes. */
    static boolean completesSlab(Block block, int meta, Block real, int realMeta, ItemStack stack) {
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
