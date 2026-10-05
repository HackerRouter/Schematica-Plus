package com.github.lunatrius.schematica.client.printer;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockSlab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.IFluidBlock;

import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.github.lunatrius.schematica.client.printer.registry.PlacementData;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary;
import com.github.lunatrius.schematica.client.selection.AreaSelections;
import com.github.lunatrius.schematica.client.printer.registry.PlacementRegistry;
import com.github.lunatrius.schematica.client.util.BlockToItemStack;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Constants;
import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry;
import cpw.mods.fml.common.registry.GameData;

public class SchematicPrinter {

    public static final SchematicPrinter INSTANCE = new SchematicPrinter();
    public static final FMLControlledNamespacedRegistry<Block> BLOCK_REGISTRY = GameData.getBlockRegistry();

    private final Minecraft minecraft = Minecraft.getMinecraft();

    private volatile boolean isEnabled;
    private volatile boolean isPrinting;
    private volatile String pendingMessage;

    private SchematicWorld schematic = null;
    private final java.util.Map<Long, Long> cooldowns = new java.util.HashMap<>();
    private long tick;
    private Pass pass;
    /** The block being mined (x, y, z), after litematica-printer's printBreakWrongBlock/ExtraBlock. */
    private int[] mining;
    /** printAutoTool: the hotbar slot of the tool held while mining and the slot to go back to afterwards. */
    private int toolSlot = -1, toolRestoreSlot;

    public boolean isEnabled() {
        return this.isEnabled;
    }

    /** Whether the server allows printing (its printerEnabled option); turning it off stops the printer. */
    public void setEnabled(boolean isEnabled) {
        if (!isEnabled && this.isPrinting) {
            this.isPrinting = false;
            message("schematica.message.printer.disabled");
        }
        this.isEnabled = isEnabled;
    }

    /** The workingSwitch hotkey: toggles printing with the MaLiLib toggle message, unless the server forbids it. */
    public void toggleWithMessage() {
        if (!this.isEnabled) {
            this.isPrinting = false;
            message("schematica.message.printer.disabled");
            return;
        }
        this.isPrinting = !this.isPrinting;
        if (this.isPrinting) refresh();
        com.github.lunatrius.schematica.handler.VisualSettings.printToggle(
            com.github.lunatrius.schematica.client.gui.framework.UiTranslations.format("schematica.printer.name"), this.isPrinting);
    }

    /** Stops printing after death, like litematica-printer's auto disable. */
    public void stopWithMessage(String key) {
        if (!this.isPrinting) return;
        this.isPrinting = false;
        message(key);
    }

    /** Capabilities arrive on the network thread, so messages are shown on the next client tick. */
    private void message(String key) { this.pendingMessage = key; }

    public void showPendingMessage() {
        String key = this.pendingMessage;
        if (key == null || this.minecraft.ingameGUI == null) return;
        this.pendingMessage = null;
        this.minecraft.ingameGUI.func_110326_a(com.github.lunatrius.schematica.client.gui.framework.UiTranslations.format(key), false);
    }

    public boolean isPrinting() {
        return this.isPrinting;
    }

    public void setPrinting(boolean isPrinting) {
        if (!isPrinting) stopMining();
        this.isPrinting = isPrinting;
    }

    public SchematicWorld getSchematic() {
        return this.schematic;
    }

    public void setSchematic(SchematicWorld schematic) {
        this.schematic = schematic;
        refresh();
    }

    public void refresh() {
        this.cooldowns.clear();
        stopMining();
    }

    /** Stops a started mining once printing was turned off (the switch may change on the network thread). */
    public void tickIdle() {
        if (!this.isPrinting || !this.isEnabled) {
            if (this.mining != null) stopMining();
            PrinterMissingMaterials.clear();
        }
    }

    private void stopMining() {
        if (this.mining != null && this.minecraft.playerController != null) this.minecraft.playerController.resetBlockRemoving();
        this.mining = null;
        if (this.toolSlot >= 0 && this.minecraft.thePlayer != null) this.minecraft.thePlayer.inventory.currentItem = this.toolRestoreSlot;
        this.toolSlot = -1;
    }

    /** Keeps mining the started block; false when there is none (any more) and the pass may place. */
    private boolean continueMining(World world, double eyeX, double eyeY, double eyeZ, double range) {
        if (this.mining == null) return false;
        int x = this.mining[0], y = this.mining[1], z = this.mining[2];
        double dx = x + 0.5 - eyeX, dy = y + 0.5 - eyeY, dz = z + 0.5 - eyeZ;
        if (world.getBlock(x, y, z).isAir(world, x, y, z) || dx * dx + dy * dy + dz * dz > range * range) {
            stopMining();
            return false;
        }
        if (this.toolSlot >= 0) this.minecraft.thePlayer.inventory.currentItem = this.toolSlot;
        this.minecraft.playerController.onPlayerDamageBlock(x, y, z, ForgeDirection.UP.ordinal());
        this.minecraft.thePlayer.swingItem();
        return true;
    }

    /**
     * printBreakExtraBlock / printBreakWrongBlock / printBreakWrongStateBlock: blocks where the schematic has air,
     * another block, or the same block in another state are mined first. Replaceable blocks, fluids and blocks a
     * slab click completes are left to the placement.
     */
    private boolean startBreak(World world, SchematicWorld schematic, int wx, int wy, int wz) {
        if (!ConfigurationHandler.printBreakExtraBlock && !ConfigurationHandler.printBreakWrongBlock && !ConfigurationHandler.printBreakWrongStateBlock) return false;
        Block real = world.getBlock(wx, wy, wz);
        if (real.isAir(world, wx, wy, wz) || FluidPrinter.isFluid(real) || real.getBlockHardness(world, wx, wy, wz) < 0) return false;
        int x = wx - schematic.position.x, y = wy - schematic.position.y, z = wz - schematic.position.z;
        Block block = schematic.getBlock(x, y, z);
        int meta = schematic.getBlockMetadata(x, y, z), realMeta = world.getBlockMetadata(wx, wy, wz);
        boolean wanted;
        if (block.isAir(schematic, x, y, z)) {
            wanted = ConfigurationHandler.printBreakExtraBlock && !ConfigurationHandler.isExtraAirBlock(real);
        } else if (block != real) {
            wanted = ConfigurationHandler.printBreakWrongBlock && !com.github.lunatrius.schematica.util.BlockGroups.tolerated(block, meta, real, realMeta)
                && !MaterialReplacements.built(schematic, BlockToItemStack.getItemStack(this.minecraft.thePlayer, block, schematic, x, y, z), real, realMeta)
                && !real.isReplaceable(world, wx, wy, wz)
                && !(block instanceof BlockSlab && EasyPlace.completesSlab(block, meta, real, realMeta, new ItemStack(block, 1, block.damageDropped(meta))));
        } else {
            MultiBlockPlacement.Kind kind = MultiBlockPlacement.kind(block);
            int mask = kind == null ? 0xF : MultiBlockPlacement.stateMask(kind, meta, AccuratePlacementClient.active(block));
            wanted = ConfigurationHandler.printBreakWrongStateBlock && (meta & mask) != (realMeta & mask) && !FluidPrinter.isFluid(block)
                && !com.github.lunatrius.schematica.util.BlockGroups.tolerated(block, meta, real, realMeta);
        }
        if (!wanted) return false;
        boolean switched = selectTool(this.minecraft.thePlayer, world, real, wx, wy, wz);
        this.minecraft.playerController.clickBlock(wx, wy, wz, ForgeDirection.UP.ordinal());
        this.minecraft.thePlayer.swingItem();
        PrinterHighlights.add(wx, wy, wz, PrinterHighlights.Type.BREAK);
        if (!world.getBlock(wx, wy, wz).isAir(world, wx, wy, wz) && !this.minecraft.playerController.isInCreativeMode()) {
            this.mining = new int[] {wx, wy, wz};
            if (switched) {
                this.toolSlot = this.minecraft.thePlayer.inventory.currentItem;
                this.toolRestoreSlot = this.pass != null ? this.pass.slot : this.toolSlot;
            }
        }
        return true;
    }

    /**
     * printAutoTool: holds the item of the hotbar, or of the inventory through the swap slots, that mines the block
     * fastest (harvest level and enchantments included); tools at or below printAutoToolDurability uses left are not
     * used. The held item stays when nothing is faster. True when another slot was selected.
     */
    private boolean selectTool(EntityClientPlayerMP player, World world, Block block, int x, int y, int z) {
        if (!ConfigurationHandler.printAutoTool || this.minecraft.playerController.isInCreativeMode()) return false;
        InventoryPlayer inventory = player.inventory;
        int current = inventory.currentItem;
        float best = usableTool(inventory.mainInventory[current]) ? strength(player, world, block, x, y, z, inventory.mainInventory[current]) : -1;
        int bestSlot = -1;
        int slots = ConfigurationHandler.swapSlotsQueue.isEmpty() ? Constants.Inventory.Size.HOTBAR : inventory.mainInventory.length;
        for (int slot = 0; slot < slots; slot++) {
            if (slot == current || !usableTool(inventory.mainInventory[slot])) continue;
            float strength = strength(player, world, block, x, y, z, inventory.mainInventory[slot]);
            if (strength > best) { best = strength; bestSlot = slot; }
        }
        if (bestSlot < 0) return false;
        if (bestSlot >= Constants.Inventory.Size.HOTBAR) {
            int target = getNextSlot();
            swapSlots(bestSlot, target);
            bestSlot = target;
        }
        inventory.currentItem = bestSlot;
        return true;
    }

    private static boolean usableTool(ItemStack stack) {
        return stack == null || !stack.isItemStackDamageable() || ConfigurationHandler.printAutoToolDurability <= 0
            || stack.getMaxDamage() - stack.getItemDamage() > ConfigurationHandler.printAutoToolDurability;
    }

    /** The block's mining progress per tick with the stack in hand (the held slot is swapped only on the client side). */
    private static float strength(EntityClientPlayerMP player, World world, Block block, int x, int y, int z, ItemStack stack) {
        InventoryPlayer inventory = player.inventory;
        ItemStack held = inventory.mainInventory[inventory.currentItem];
        inventory.mainInventory[inventory.currentItem] = stack;
        try {
            return block.getPlayerRelativeBlockHardness(player, world, x, y, z);
        } catch (RuntimeException error) {
            return -1;
        } finally {
            inventory.mainInventory[inventory.currentItem] = held;
        }
    }

    /**
     * One printer pass around the player over every visible placement, after the behavior of litematica-printer: the
     * positions within the work range in the configured shape and axis order, at most placeBlocksPerTick placements,
     * each position then waiting `timeout` ticks.
     * @return the number of placement clicks made
     */
    public int print() {
        final EntityClientPlayerMP player = this.minecraft.thePlayer;
        final World world = this.minecraft.theWorld;
        if (player == null || world == null) return 0;
        this.tick = world.getTotalWorldTime();
        if (!this.cooldowns.isEmpty()) this.cooldowns.values().removeIf(until -> until <= this.tick);
        boolean moving = Math.abs(player.posX - player.prevPosX) + Math.abs(player.posY - player.prevPosY) + Math.abs(player.posZ - player.prevPosZ) > 0.001;
        if (ConfigurationHandler.printerPauseWhileMoving && moving) return 0;
        if (ConfigurationHandler.printerLagCheck && LagMonitor.ticksSincePacket() > ConfigurationHandler.printerLagCheckMax) return 0;
        List<SchematicWorld> placements = new ArrayList<>();
        for (SchematicWorld placement : ClientProxy.visiblePlacements()) if (placement.isRenderingEnabled()) placements.add(placement);
        if (placements.isEmpty()) return 0;

        double eyeX = player.posX, eyeY = player.posY + player.getEyeHeight() - player.getDefaultEyeHeight(), eyeZ = player.posZ;
        double feetY = player.boundingBox.minY;
        double range = ConfigurationHandler.printerWorkRange > 0 ? ConfigurationHandler.printerWorkRange
            : this.minecraft.playerController.getBlockReachDistance();
        int bx = MathHelper.floor_double(eyeX), by = MathHelper.floor_double(eyeY), bz = MathHelper.floor_double(eyeZ);
        List<int[]> offsets = PrinterIteration.offsets((int) Math.ceil(range), ConfigurationHandler.printerIteratorShape,
            ConfigurationHandler.printerIteratorMode, ConfigurationHandler.printerXAxisReverse,
            ConfigurationHandler.printerYAxisReverse, ConfigurationHandler.printerZAxisReverse);
        long deadline = ConfigurationHandler.printerIterationTimeLimit > 0
            ? System.nanoTime() + ConfigurationHandler.printerIterationTimeLimit * 1_000_000L : Long.MAX_VALUE;
        AreaSelectionLibrary.Area area = "selection".equals(ConfigurationHandler.printSelectionType) ? AreaSelections.library().selected() : null;

        if (continueMining(world, eyeX, eyeY, eyeZ, range)) return 0;
        boolean layers = "layers".equals(ConfigurationHandler.printerBuildOrder);
        List<PrinterBuildOrder.Candidate> candidates = new ArrayList<>();
        for (int[] offset : offsets) {
            if (System.nanoTime() > deadline) break;
            int x = bx + offset[0], y = by + offset[1], z = bz + offset[2];
            if (y < 0 || y > 255) continue;
            double dx = x + 0.5 - eyeX, dy = y + 0.5 - eyeY, dz = z + 0.5 - eyeZ;
            if (dx * dx + dy * dy + dz * dz > range * range) continue;
            if (!selected(x, y, z, feetY, area)) continue;
            if (this.cooldowns.containsKey(key(x, y, z))) continue;
            SchematicWorld placement = placementAt(placements, x, y, z);
            if (placement == null) continue;
            int lx = x - placement.position.x, ly = y - placement.position.y, lz = z - placement.position.z;
            Block block = placement.getBlock(lx, ly, lz);
            Block real = world.getBlock(x, y, z);
            int meta = placement.getBlockMetadata(lx, ly, lz), realMeta = world.getBlockMetadata(x, y, z);
            if (block == real && meta == realMeta || com.github.lunatrius.schematica.util.BlockGroups.tolerated(block, meta, real, realMeta)) continue;
            candidates.add(new PrinterBuildOrder.Candidate(x, y, z, layers ? PrinterBuildOrder.tier(block) : 0, placement));
        }
        if (layers) candidates.sort(PrinterBuildOrder.order(eyeX, eyeY, eyeZ, feetY, ConfigurationHandler.printerYAxisReverse));
        this.pass = new Pass(player);
        int placed = 0;
        try {
            for (PrinterBuildOrder.Candidate candidate : candidates) {
                int x = candidate.x, y = candidate.y, z = candidate.z;
                long key = key(x, y, z);
                SchematicWorld placement = (SchematicWorld) candidate.placement;
                if (startBreak(world, placement, x, y, z)) {
                    this.cooldowns.put(key, this.tick + Math.max(1, ConfigurationHandler.timeout));
                    break;
                }
                if (placeBlock(world, player, placement, x, y, z)) {
                    PrinterHighlights.add(x, y, z, PrinterHighlights.Type.PLACE);
                    this.cooldowns.put(key, this.tick + Math.max(1, ConfigurationHandler.timeout));
                    if (++placed >= Math.max(1, ConfigurationHandler.placeBlocksPerTick)) break;
                }
            }
        } catch (RuntimeException e) {
            Reference.logger.error("Could not place block!", e);
        } finally {
            this.pass.restore();
            this.pass = null;
        }
        return placed;
    }

    /** printSelectionType: render layers only, also inside the selected area, or below/above the player's feet. */
    private static boolean selected(int x, int y, int z, double feetY, AreaSelectionLibrary.Area area) {
        switch (ConfigurationHandler.printSelectionType) {
            case "selection":
                if (area == null) return false;
                for (AreaSelectionLibrary.Box box : area.boxes()) {
                    Vector3i a = box.first(), b = box.second();
                    if (x >= Math.min(a.x, b.x) && x <= Math.max(a.x, b.x) && y >= Math.min(a.y, b.y) && y <= Math.max(a.y, b.y)
                        && z >= Math.min(a.z, b.z) && z <= Math.max(a.z, b.z)) return true;
                }
                return false;
            case "below_player": return y <= MathHelper.floor_double(feetY);
            case "above_player": return y >= MathHelper.ceiling_double_int(feetY);
            default: return true;
        }
    }

    private static SchematicWorld placementAt(List<SchematicWorld> placements, int x, int y, int z) {
        for (SchematicWorld placement : placements) {
            if (placement.isBlockRendered(x - placement.position.x, y - placement.position.y, z - placement.position.z)) return placement;
        }
        return null;
    }

    private static long key(int x, int y, int z) { return ((long) x & 0x3ffffffL) << 38 | ((long) z & 0x3ffffffL) << 12 | y & 0xfffL; }

    /** The player state a pass changes (held slot, sneaking, reported rotation), restored when the pass ends. */
    private final class Pass {
        final EntityClientPlayerMP player;
        final int slot;
        final boolean sneaking;
        final float yaw, pitch;
        boolean sneakSent, lookSent;

        Pass(EntityClientPlayerMP player) {
            this.player = player; this.slot = player.inventory.currentItem; this.sneaking = player.isSneaking();
            this.yaw = player.rotationYaw; this.pitch = player.rotationPitch;
        }

        /** Sneaks for clicks on a neighbor so that containers and other interactive blocks are not opened. */
        void sneak() {
            if (sneakSent || player.isSneaking()) return;
            syncSneaking(player, true);
            sneakSent = true;
        }

        void look(PrinterLook look) {
            player.rotationYaw = look.yaw;
            player.rotationPitch = look.pitch;
            player.sendQueue.addToSendQueue(new C05PacketPlayerLook(look.yaw, look.pitch, player.onGround));
            lookSent = true;
        }

        void restoreLook() {
            player.rotationYaw = yaw;
            player.rotationPitch = pitch;
        }

        void restore() {
            player.inventory.currentItem = mining != null && toolSlot >= 0 ? toolSlot : slot;
            restoreLook();
            if (lookSent) player.sendQueue.addToSendQueue(new C05PacketPlayerLook(yaw, pitch, player.onGround));
            if (sneakSent) syncSneaking(player, sneaking);
        }
    }

    private boolean placeBlock(World world, EntityClientPlayerMP player, SchematicWorld schematic, int wx, int wy, int wz) {
        final int x = wx - schematic.position.x, y = wy - schematic.position.y, z = wz - schematic.position.z;
        final Block block = schematic.getBlock(x, y, z);
        final Block realBlock = world.getBlock(wx, wy, wz);
        final int metadata = schematic.getBlockMetadata(x, y, z);
        final int realMetadata = world.getBlockMetadata(wx, wy, wz);

        if (block == realBlock && metadata == realMetadata) {
            return false;
        }

        if (FluidPrinter.isFluid(block)) {
            if (FluidPrinter.matches(schematic, x, y, z, world, wx, wy, wz)) return false;
            if (!world.isAirBlock(wx, wy, wz) && !FluidPrinter.isFluid(realBlock)) return false;
            if (FluidPrinter.isFluid(realBlock) && !FluidPrinter.sameFluid(block, realBlock)) return false;
            return FluidPrinter.place(this.minecraft, FluidPrinter.source(schematic, x, y, z), wx, wy, wz,
                bucket -> {
                    if (swapToItem(player.inventory, bucket, true, true)) return true;
                    PrinterMissingMaterials.record(bucket);
                    return false;
                });
        }

        if (ConfigurationHandler.destroyBlocks && !world.isAirBlock(wx, wy, wz)
            && this.minecraft.playerController.isInCreativeMode()) {
            this.minecraft.playerController.clickBlock(wx, wy, wz, 0);
            return !ConfigurationHandler.destroyInstantly;
        }

        if (block.isAir(schematic, x, y, z) || skipped(block)) {
            return false;
        }
        MultiBlockPlacement.Kind multi = MultiBlockPlacement.kind(block);
        if (multi != null && MultiBlockPlacement.secondary(metadata)) return false;

        ItemStack itemStack = BlockToItemStack.getItemStack(player, block, schematic, x, y, z);
        if (itemStack == null || itemStack.getItem() == null) {
            Reference.logger.debug("{} is missing a mapping!", BLOCK_REGISTRY.getNameForObject(block));
            return false;
        }
        if (isBlacklisted(block, itemStack)) {
            return false;
        }
        if (MaterialReplacements.built(schematic, itemStack, realBlock, realMetadata)) return false;
        ItemStack replaced = MaterialReplacements.replacement(schematic, itemStack);
        if (replaced != null) itemStack = replaced;

        if (multi != null) return placeMultiBlock(world, player, schematic, x, y, z, wx, wy, wz, block, metadata, itemStack, multi);

        boolean slab = EasyPlace.completesSlab(block, metadata, realBlock, realMetadata, itemStack);
        if (!slab) {
            if (!realBlock.isReplaceable(world, wx, wy, wz)) return false;
            // Gravity blocks need ground below, attached blocks their support (Entropy5's block supports)
            if (ConfigurationHandler.printFallingBlockCheck && block instanceof BlockFalling && BlockFalling.func_149831_e(world, wx, wy - 1, wz)) return false;
            if (!block.canPlaceBlockAt(world, wx, wy, wz)) return false;
        }

        PlacementData data = PlacementRegistry.INSTANCE.getPlacementData(block, itemStack);
        boolean accurate = AccuratePlacementClient.active(block);
        PrinterLook look = null;
        if (!accurate && data != null && data.type != PlacementData.PlacementType.BLOCK) {
            look = findLook(world, player, wx, wy, wz, data, metadata);
            if (look == null) return false;
        }

        Click click = slab ? slabClick(wx, wy, wz, realMetadata) : click(world, wx, wy, wz, data, metadata, accurate);
        if (click == null || !swapToItem(player.inventory, itemStack)) {
            if (click != null) PrinterMissingMaterials.record(itemStack);
            PrinterHighlights.add(wx, wy, wz, PrinterHighlights.Type.FAILED);
            return false;
        }
        ItemStack held = player.getCurrentEquippedItem();
        int extraClicks = data == null || slab ? 0 : data.getExtraClicks(block, metadata);
        if (held == null || !held.isItemEqual(itemStack)
            || !this.minecraft.playerController.isInCreativeMode() && held.stackSize <= extraClicks) {
            return false;
        }

        if (ConfigurationHandler.printForcedSneak || click.neighbor) this.pass.sneak();
        if (look != null) this.pass.look(look);
        if (accurate) AccuratePlacementClient.announce(wx, wy, wz, block, metadata);
        try {
            boolean success = placeBlock(world, player, held, click.x, click.y, click.z, click.side, click.hit);
            for (int i = 0; success && i < extraClicks; i++) {
                // A double slab: the second half goes onto the single slab just placed
                Click second = block instanceof BlockSlab && world.getBlock(wx, wy, wz) instanceof BlockSlab
                    ? slabClick(wx, wy, wz, world.getBlockMetadata(wx, wy, wz)) : click;
                success = placeBlock(world, player, held, second.x, second.y, second.z, second.side, second.hit);
            }
            if (held.stackSize == 0) player.inventory.mainInventory[player.inventory.currentItem] = null;
            return success;
        } finally {
            if (look != null) this.pass.restoreLook();
        }
    }

    /**
     * A door, bed or double plant: its first block is placed by clicking the top of the block below while facing the
     * schematic's direction; the second block comes with it. On a Plus server the door halves' open bit and hinge
     * are sent as accurate placement intents.
     */
    private boolean placeMultiBlock(World world, EntityClientPlayerMP player, SchematicWorld schematic, int x, int y, int z,
        int wx, int wy, int wz, Block block, int metadata, ItemStack itemStack, MultiBlockPlacement.Kind kind) {
        int[] offset = MultiBlockPlacement.secondOffset(kind, metadata);
        int sx = wx + offset[0], sy = wy + offset[1], sz = wz + offset[2];
        if (sy > 255 || wy < 1) return false;
        if (kind == MultiBlockPlacement.Kind.BED) {
            if (!world.isAirBlock(wx, wy, wz) || !world.isAirBlock(sx, sy, sz)
                || !World.doesBlockHaveSolidTopSurface(world, wx, wy - 1, wz) || !World.doesBlockHaveSolidTopSurface(world, sx, sy - 1, sz)) return false;
        } else if (!block.canPlaceBlockAt(world, wx, wy, wz)) {
            return false;
        }
        Block below = world.getBlock(wx, wy - 1, wz);
        if (below.isAir(world, wx, wy - 1, wz) || below.isReplaceable(world, wx, wy - 1, wz)) return false;
        Block second = schematic.getBlock(x + offset[0], y + offset[1], z + offset[2]);
        int secondMeta = schematic.getBlockMetadata(x + offset[0], y + offset[1], z + offset[2]);
        int facing = MultiBlockPlacement.wantedFacing(kind, block, metadata, second, secondMeta);
        PrinterLook look = facing < 0 ? null : MultiBlockPlacement.look(kind, player.rotationYaw, facing);
        if (facing >= 0 && look == null) return false;

        if (!swapToItem(player.inventory, itemStack)) {
            PrinterMissingMaterials.record(itemStack);
            PrinterHighlights.add(wx, wy, wz, PrinterHighlights.Type.FAILED);
            return false;
        }
        ItemStack held = player.getCurrentEquippedItem();
        if (held == null || !held.isItemEqual(itemStack)) return false;
        this.pass.sneak();
        if (look != null) this.pass.look(look);
        if (kind == MultiBlockPlacement.Kind.DOOR && AccuratePlacementClient.active(block)) {
            AccuratePlacementClient.announce(wx, wy, wz, block, metadata);
            if (second == block && MultiBlockPlacement.secondary(secondMeta)) AccuratePlacementClient.announce(sx, sy, sz, block, secondMeta);
        }
        try {
            boolean success = placeBlock(world, player, held, wx, wy - 1, wz, ForgeDirection.UP.ordinal(), Vec3.createVectorHelper(wx + 0.5, wy, wz + 0.5));
            if (held.stackSize == 0) player.inventory.mainInventory[player.inventory.currentItem] = null;
            return success;
        } finally {
            if (look != null) this.pass.restoreLook();
        }
    }

    private static boolean skipped(Block block) {
        if (ConfigurationHandler.printSkipList.length == 0) return false;
        String name = BLOCK_REGISTRY.getNameForObject(block);
        for (String entry : ConfigurationHandler.printSkipList) if (entry.trim().equalsIgnoreCase(name)) return true;
        return false;
    }

    /** A placement click: on a neighbor's face, or at the target itself (1.7.10 places into a replaceable clicked block). */
    private static final class Click {
        final int x, y, z, side;
        final Vec3 hit;
        final boolean neighbor;
        Click(int x, int y, int z, int side, Vec3 hit, boolean neighbor) {
            this.x = x; this.y = y; this.z = z; this.side = side; this.hit = hit; this.neighbor = neighbor;
        }
    }

    private static Click slabClick(int x, int y, int z, int realMetadata) {
        int side = (realMetadata & 8) != 0 ? 0 : 1;
        return new Click(x, y, z, side, Vec3.createVectorHelper(x + 0.5, y + (side == 1 ? 1 : 0), z + 0.5), false);
    }

    private Click click(World world, int x, int y, int z, PlacementData data, int metadata, boolean accurate) {
        ForgeDirection[] solidSides = getSolidSides(world, x, y, z);
        ForgeDirection direction = null;
        if (data != null) {
            ForgeDirection[] valid = data.getValidDirections(solidSides, metadata);
            if (valid.length == 0 && (accurate || data.type != PlacementData.PlacementType.BLOCK || data.mapping.isEmpty())
                && data.maskOffset == 0) valid = solidSides;
            if (valid.length > 0) direction = valid[0];
        } else if (solidSides.length > 0) {
            direction = solidSides[0];
        }
        float offsetY = data == null ? 0 : data.getOffsetFromMetadata(metadata);
        if (direction != null) {
            int cx = x + direction.offsetX, cy = y + direction.offsetY, cz = z + direction.offsetZ;
            return new Click(cx, cy, cz, direction.getOpposite().ordinal(), Vec3.createVectorHelper(cx, cy + offsetY, cz), true);
        }
        if (!ConfigurationHandler.placeInAir) return null;
        // In the air: the clicked side and height carry the orientation the neighbor click would have given
        int side = ForgeDirection.UP.ordinal();
        if (data != null && data.type == PlacementData.PlacementType.BLOCK && !data.mapping.isEmpty()) {
            side = -1;
            for (java.util.Map.Entry<ForgeDirection, Integer> entry : data.mapping.entrySet()) {
                if (entry.getValue() == (metadata & data.maskMeta)) { side = entry.getKey().getOpposite().ordinal(); break; }
            }
            if (side < 0) return null;
        } else if (data != null && data.maskOffset != 0) {
            side = ForgeDirection.NORTH.ordinal();
        }
        double hitY = data != null && data.maskOffset != 0 ? (offsetY >= 0.5f ? 0.75 : 0.25) : 0.5;
        return new Click(x, y, z, side, Vec3.createVectorHelper(x + 0.5, y + hitY, z + 0.5), false);
    }

    /** A rotation for which the block gets the schematic orientation, reported to the server before the click. */
    private PrinterLook findLook(World world, EntityClientPlayerMP player, int x, int y, int z, PlacementData data, int metadata) {
        float yaw = player.rotationYaw, pitch = player.rotationPitch;
        try {
            for (PrinterLook look : PrinterLook.candidates(yaw)) {
                if (data.type == PlacementData.PlacementType.PISTON) {
                    player.rotationYaw = look.yaw;
                    player.rotationPitch = look.pitch;
                    if (BlockPistonBase.determineOrientation(world, x, y, z, player) == BlockPistonBase.getPistonOrientation(metadata)) return look;
                } else {
                    Integer mapped = data.mapping.get(PrinterLook.orientation(look.yaw, look.pitch));
                    if (mapped != null && mapped == (metadata & data.maskMeta)) return look;
                }
            }
            return null;
        } finally {
            player.rotationYaw = yaw;
            player.rotationPitch = pitch;
        }
    }

    private boolean isSolid(World world, int x, int y, int z, ForgeDirection side) {
        x += side.offsetX;
        y += side.offsetY;
        z += side.offsetZ;

        Block block = world.getBlock(x, y, z);

        if (block == null) {
            return false;
        }

        if (block.isAir(world, x, y, z)) {
            return false;
        }

        if (block instanceof BlockFluidBase) {
            return false;
        }

        if (block.isReplaceable(world, x, y, z)) {
            return false;
        }

        return true;
    }

    ForgeDirection[] getSolidSides(World world, int x, int y, int z) {
        List<ForgeDirection> list = new ArrayList<>();

        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            if (isSolid(world, x, y, z, side)) {
                list.add(side);
            }
        }

        ForgeDirection[] sides = new ForgeDirection[list.size()];
        return list.toArray(sides);
    }

    boolean isBlacklisted(Block block, ItemStack itemStack) {
        if (block instanceof IFluidBlock || block instanceof BlockLiquid) {
            return true;
        }

        if (itemStack.getItem() instanceof ItemBucket) {
            return true;
        }

        if (itemStack.getItem() == Items.sign) {
            return true;
        }

        if (ClientProxy.lotrProxy.isBlackListed(block, itemStack)) {
            return true;
        }

        return false;
    }

    boolean placeBlock(World world, EntityPlayer player, ItemStack itemStack, int x, int y, int z, int side,
        Vec3 hitVec) {
        boolean success = !ForgeEventFactory.onPlayerInteract(player, Action.RIGHT_CLICK_BLOCK, x, y, z, side, world)
            .isCanceled();
        if (success) {
            success = this.minecraft.playerController
                .onPlayerRightClick(player, world, itemStack, x, y, z, side, hitVec);
            if (success) {
                player.swingItem();
            }
        }

        return success;
    }

    void syncSneaking(EntityClientPlayerMP player, boolean isSneaking) {
        player.setSneaking(isSneaking);
        player.sendQueue.addToSendQueue(new C0BPacketEntityAction(player, isSneaking ? 1 : 2));
    }

    private boolean swapToItem(InventoryPlayer inventory, ItemStack itemStack) {
        return swapToItem(inventory, itemStack, true);
    }

    private boolean swapToItem(InventoryPlayer inventory, ItemStack itemStack, boolean swapSlots) {
        return swapToItem(inventory, itemStack, swapSlots, false);
    }

    boolean swapToItem(InventoryPlayer inventory, ItemStack itemStack, boolean swapSlots, boolean matchNBT) {
        int slot = getInventorySlotWithItem(inventory, itemStack, matchNBT);

        if (this.minecraft.playerController.isInCreativeMode()
            && (slot < Constants.Inventory.InventoryOffset.HOTBAR
                || slot >= Constants.Inventory.InventoryOffset.HOTBAR + Constants.Inventory.Size.HOTBAR)
            && !ConfigurationHandler.swapSlotsQueue.isEmpty()) {
            inventory.currentItem = getNextSlot();
            inventory.setInventorySlotContents(inventory.currentItem, itemStack.copy());
            this.minecraft.playerController.sendSlotPacket(
                inventory.getStackInSlot(inventory.currentItem),
                Constants.Inventory.SlotOffset.HOTBAR + inventory.currentItem);
            return true;
        }

        if (slot >= Constants.Inventory.InventoryOffset.HOTBAR
            && slot < Constants.Inventory.InventoryOffset.HOTBAR + Constants.Inventory.Size.HOTBAR) {
            inventory.currentItem = slot;
            return true;
        } else if (swapSlots && slot >= Constants.Inventory.InventoryOffset.INVENTORY
            && slot < Constants.Inventory.InventoryOffset.INVENTORY + Constants.Inventory.Size.INVENTORY) {
                if (swapSlots(inventory, slot)) {
                    return swapToItem(inventory, itemStack, false, matchNBT);
                }
            }
        return false;
    }

    private int getInventorySlotWithItem(final InventoryPlayer inventory, final ItemStack itemStack, boolean matchNBT) {
        for (int i = 0; i < inventory.mainInventory.length; i++) {
            if (inventory.mainInventory[i] != null && inventory.mainInventory[i].isItemEqual(itemStack)
                && (!matchNBT || ItemStack.areItemStackTagsEqual(inventory.mainInventory[i], itemStack))) {
                return i;
            }
        }
        return -1;
    }

    private boolean swapSlots(InventoryPlayer inventory, int from) {
        if (!ConfigurationHandler.swapSlotsQueue.isEmpty()) {
            int slot = getNextSlot();

            swapSlots(from, slot);
            return true;
        }

        return false;
    }

    private int getNextSlot() {
        int slot = ConfigurationHandler.swapSlotsQueue.poll() % Constants.Inventory.Size.HOTBAR;
        ConfigurationHandler.swapSlotsQueue.offer(slot);
        return slot;
    }

    private boolean swapSlots(final int from, final int to) {
        return this.minecraft.playerController
            .windowClick(this.minecraft.thePlayer.inventoryContainer.windowId, from, to, 2, this.minecraft.thePlayer)
            == null;
    }
}
