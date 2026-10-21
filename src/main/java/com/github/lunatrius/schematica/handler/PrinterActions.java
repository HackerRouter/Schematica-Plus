// SPDX-License-Identifier: LGPL-3.0-only
package com.github.lunatrius.schematica.handler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemSlab;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import com.github.lunatrius.schematica.network.message.MessagePrinterAction;
import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Executes bounded printer requests on the server thread through the native interaction handlers. */
public final class PrinterActions {
    public static final PrinterActions INSTANCE = new PrinterActions();
    private final Map<EntityPlayerMP, Queue<Request>> pending = new HashMap<>();
    private final Map<EntityPlayerMP, Request> mining = new HashMap<>();
    private int queued;

    private static final class Request {
        final World world;
        final MessagePrinterAction message;
        final long time = System.nanoTime();
        Request(EntityPlayerMP player, MessagePrinterAction message) { world = player.worldObj; this.message = message; }
    }

    private PrinterActions() {}

    public synchronized void enqueue(EntityPlayerMP player, MessagePrinterAction message) {
        if (player == null || !message.valid() || queued >= 8192) return;
        Queue<Request> queue = pending.computeIfAbsent(player, p -> new ArrayDeque<>());
        if (queue.size() >= 512) return;
        queue.add(new Request(player, message));
        queued++;
    }

    public synchronized void forget(EntityPlayerMP player) {
        Queue<Request> queue = pending.remove(player);
        if (queue != null) queued -= queue.size();
        cancelMining(player);
    }

    private void cancelMining(EntityPlayerMP player) {
        Request request = mining.remove(player);
        if (request != null) {
            MessagePrinterAction m = request.message;
            player.theItemInWorldManager.cancelDestroyingBlock(m.x, m.y, m.z);
        }
    }

    @SubscribeEvent public synchronized void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            for (EntityPlayerMP player : new ArrayList<>(mining.keySet())) {
                Request request = mining.get(player);
                if (!allowed(player, request)) cancelMining(player);
                else if (request.world.isAirBlock(request.message.x, request.message.y, request.message.z)) mining.remove(player);
            }
            return;
        }
        for (EntityPlayerMP player : new ArrayList<>(pending.keySet())) {
            Queue<Request> queue = pending.get(player);
            int placed = 0, broken = 0;
            while (!queue.isEmpty()) {
                Request request = queue.peek();
                int action = request.message.action;
                if (System.nanoTime() - request.time <= 2_000_000_000L) {
                    if (action == MessagePrinterAction.PLACE && placed >= ConfigurationHandler.printerMaxPlacementsPerTick) break;
                    if (action == MessagePrinterAction.BREAK && ConfigurationHandler.printerMaxBreaksPerTick > 0
                        && broken >= ConfigurationHandler.printerMaxBreaksPerTick) break;
                }
                queue.remove(); queued--;
                if (action == MessagePrinterAction.PLACE) placed++;
                if (action == MessagePrinterAction.BREAK) broken++;
                execute(player, request);
            }
            if (queue.isEmpty()) pending.remove(player);
            player.inventoryContainer.detectAndSendChanges();
        }
    }

    private static boolean allowed(EntityPlayerMP player, Request request) {
        MessagePrinterAction m = request.message;
        if (!ConfigurationHandler.printerEnabled
            || player.isDead || player.worldObj != request.world || !player.playerNetServerHandler.netManager.isChannelOpen()) return false;
        double range = range(player);
        double eyeY = player.posY + player.getEyeHeight();
        // A placement may click a supporting neighbor just outside the target sphere.
        if (!PrinterRange.contains(player.posX, eyeY, player.posZ, m.x, m.y, m.z,
            range + (m.action == MessagePrinterAction.PLACE ? 1 : 0))) return false;
        if (!request.world.blockExists(m.x, m.y, m.z)) return false;
        if (!request.world.blockExists(m.targetX, m.targetY, m.targetZ)
            || !PrinterRange.contains(player.posX, eyeY, player.posZ, m.targetX, m.targetY, m.targetZ, range)) return false;
        if (m.action == MessagePrinterAction.BREAK && ConfigurationHandler.printerMaxBreaksPerTick <= 0) return false;
        if (m.action == MessagePrinterAction.PLACE) {
            ForgeDirection face = ForgeDirection.getOrientation(m.side);
            if (!request.world.blockExists(m.x + face.offsetX, m.y + face.offsetY, m.z + face.offsetZ)) return false;
            return PrinterRange.contains(player.posX, eyeY, player.posZ, m.x, m.y, m.z, range)
                || PrinterRange.contains(player.posX, eyeY, player.posZ, m.x + face.offsetX, m.y + face.offsetY, m.z + face.offsetZ, range);
        }
        return true;
    }

    private void execute(EntityPlayerMP player, Request request) {
        MessagePrinterAction m = request.message;
        if (m.action == 1) { cancelMining(player); return; }
        if (System.nanoTime() - request.time > 2_000_000_000L || !allowed(player, request) || !m.matches(request.world)) {
            cancelMining(player);
            resync(player, request);
            return;
        }
        if (m.action == 2) {
            Request started = mining.get(player);
            if (started == null || started.message.x != m.x || started.message.y != m.y || started.message.z != m.z) return;
        }
        ItemInWorldManager manager = player.theItemInWorldManager;
        double reach = manager.getBlockReachDistance();
        int slot = player.inventory.currentItem;
        int source = findItem(player.inventory.mainInventory, m.slot, m.expectedItem);
        boolean creativeItem = source < 0 && manager.isCreative();
        ItemStack previousItem = player.inventory.mainInventory[m.slot];
        if (creativeItem) {
            source = m.slot;
            player.inventory.mainInventory[source] = ItemStack.copyItemStack(m.expectedItem);
        } else if (source < 0) { resync(player, request); return; }
        float yaw = player.rotationYaw, pitch = player.rotationPitch;
        boolean sneaking = player.isSneaking();
        swap(player, source, m.slot);
        ItemStack before = m.action == MessagePrinterAction.BREAK ? ItemStack.copyItemStack(player.inventory.mainInventory[m.slot]) : null;
        try {
            manager.setBlockReachDistance(Math.max(reach, range(player) + 3));
            player.inventory.currentItem = m.slot;
            player.rotationYaw = m.yaw; player.rotationPitch = m.pitch; player.setSneaking(m.sneaking);
            if (m.action == MessagePrinterAction.PLACE) {
                AccuratePlacement.INSTANCE.beginAction(player, m.intents);
                player.playerNetServerHandler.processPlayerBlockPlacement(placement(m, player.getCurrentEquippedItem()));
                if (m.completeSlab) completeSlab(player, m);
            } else if (m.action == MessagePrinterAction.BREAK) {
                cancelMining(player);
                harvest(player, m);
            } else {
                player.playerNetServerHandler.processPlayerDigging(digging(m));
                if (m.action == 0) mining.put(player, request);
            }
        } catch (RuntimeException | LinkageError exception) {
            cancelMining(player);
            Reference.logger.error("Printer interaction failed", exception);
        } finally {
            AccuratePlacement.INSTANCE.endAction(player);
            manager.setBlockReachDistance(reach);
            if (m.action == MessagePrinterAction.BREAK) refreshQueuedTool(player, before, player.inventory.mainInventory[m.slot]);
            swap(player, source, m.slot);
            if (creativeItem) player.inventory.mainInventory[m.slot] = previousItem;
            player.inventory.currentItem = slot;
            player.rotationYaw = yaw; player.rotationPitch = pitch; player.setSneaking(sneaking);
            resync(player, request);
        }
    }

    private void refreshQueuedTool(EntityPlayerMP player, ItemStack before, ItemStack after) {
        if (before == null || after == null || after.stackSize <= 0 || before.getItem() != after.getItem()
            || ItemStack.areItemStackTagsEqual(before, after)) return;
        Queue<Request> queue = pending.get(player);
        if (queue == null) return;
        for (Request next : queue) {
            MessagePrinterAction message = next.message;
            if (next.world != player.worldObj || message.action != MessagePrinterAction.BREAK || !matchesItem(before, message.expectedItem)) continue;
            message.expectedItem = after.copy();
            message.expectedItem.stackSize = 1;
        }
    }

    private static double range(EntityPlayerMP player) {
        return ConfigurationHandler.printerMaxRange > 0 ? PrinterRange.clamp(ConfigurationHandler.printerMaxRange)
            : player.theItemInWorldManager.getBlockReachDistance();
    }

    private static void resync(EntityPlayerMP player, Request request) {
        if (player.worldObj != request.world) return;
        MessagePrinterAction m = request.message;
        if (request.world.blockExists(m.x, m.y, m.z))
            player.playerNetServerHandler.sendPacket(new S23PacketBlockChange(m.x, m.y, m.z, request.world));
        if (request.world.blockExists(m.targetX, m.targetY, m.targetZ))
            player.playerNetServerHandler.sendPacket(new S23PacketBlockChange(m.targetX, m.targetY, m.targetZ, request.world));
    }

    static int findItem(ItemStack[] inventory, int preferred, ItemStack expected) {
        if (matchesItem(inventory[preferred], expected)) return preferred;
        for (int i = 0; i < inventory.length; i++) if (matchesItem(inventory[i], expected)) return i;
        return -1;
    }

    static boolean matchesItem(ItemStack actual, ItemStack expected) {
        if (expected == null) return actual == null;
        return actual != null && actual.stackSize > 0 && actual.getItem() == expected.getItem()
            && (actual.isItemStackDamageable() || actual.getItemDamage() == expected.getItemDamage())
            && ItemStack.areItemStackTagsEqual(actual, expected);
    }

    private static void swap(EntityPlayerMP player, int from, int to) {
        if (from == to) return;
        ItemStack stack = player.inventory.mainInventory[from];
        player.inventory.mainInventory[from] = player.inventory.mainInventory[to];
        player.inventory.mainInventory[to] = stack;
    }

    private static void completeSlab(EntityPlayerMP player, MessagePrinterAction original) {
        World world = player.worldObj;
        Block block = world.getBlock(original.targetX, original.targetY, original.targetZ);
        ItemStack held = player.getCurrentEquippedItem();
        if (!(block instanceof BlockSlab) || block.isOpaqueCube() || held == null || !(held.getItem() instanceof ItemSlab)) return;
        MessagePrinterAction second = new MessagePrinterAction();
        second.x = original.targetX; second.y = original.targetY; second.z = original.targetZ;
        second.side = (world.getBlockMetadata(second.x, second.y, second.z) & 8) == 0 ? 1 : 0;
        second.hitX = .5f; second.hitY = second.side; second.hitZ = .5f;
        player.playerNetServerHandler.processPlayerBlockPlacement(placement(second, held));
    }

    static boolean harvest(EntityPlayerMP player, MessagePrinterAction m) {
        World world = player.worldObj;
        ItemInWorldManager manager = player.theItemInWorldManager;
        Block block = world.getBlock(m.x, m.y, m.z);
        if (block.isAir(world, m.x, m.y, m.z) || !manager.isCreative() && block.getBlockHardness(world, m.x, m.y, m.z) < 0
            || manager.getGameType().isAdventure() && !player.isCurrentToolAdventureModeExempt(m.x, m.y, m.z)
            || MinecraftServer.getServer().isBlockProtected((WorldServer) world, m.x, m.y, m.z, player)) return false;
        ItemStack tool = player.getCurrentEquippedItem();
        if (!manager.isCreative() && tool != null && tool.isItemStackDamageable()
            && tool.getMaxDamage() - tool.getItemDamage() <= m.minimumDurability) return false;
        PlayerInteractEvent event = ForgeEventFactory.onPlayerInteract(player, PlayerInteractEvent.Action.LEFT_CLICK_BLOCK, m.x, m.y, m.z, m.side, world);
        if (event.isCanceled() || event.useBlock == Event.Result.DENY || event.useItem == Event.Result.DENY) return false;
        block.onBlockClicked(world, m.x, m.y, m.z, player);
        if (!m.matches(world)) return false;
        player.func_143004_u();
        return manager.tryHarvestBlock(m.x, m.y, m.z);
    }

    // Parameterized packet constructors are client-only. These requests are consumed locally, never serialized.
    static C08PacketPlayerBlockPlacement placement(MessagePrinterAction m, ItemStack stack) {
        ItemStack reported = ItemStack.copyItemStack(stack);
        return new C08PacketPlayerBlockPlacement() {
            @Override public int func_149576_c() { return m.x; }
            @Override public int func_149571_d() { return m.y; }
            @Override public int func_149570_e() { return m.z; }
            @Override public int func_149568_f() { return m.side; }
            @Override public ItemStack func_149574_g() { return reported; }
            @Override public float func_149573_h() { return m.hitX; }
            @Override public float func_149569_i() { return m.hitY; }
            @Override public float func_149575_j() { return m.hitZ; }
        };
    }

    static C07PacketPlayerDigging digging(MessagePrinterAction m) {
        return new C07PacketPlayerDigging() {
            @Override public int func_149506_g() { return m.action; }
            @Override public int func_149505_c() { return m.x; }
            @Override public int func_149503_d() { return m.y; }
            @Override public int func_149502_e() { return m.z; }
            @Override public int func_149501_f() { return m.side; }
        };
    }
}
