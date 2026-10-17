// SPDX-License-Identifier: LGPL-3.0-only
package com.github.lunatrius.schematica.handler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.lunatrius.schematica.network.message.MessagePrinterAction;
import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
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
        if (player == null || !message.valid() || queued >= 1024) return;
        Queue<Request> queue = pending.computeIfAbsent(player, p -> new ArrayDeque<>());
        if (queue.size() >= 64) return;
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
        Map<EntityPlayerMP, Queue<Request>> batch = new HashMap<>(pending);
        pending.clear(); queued = 0;
        for (Map.Entry<EntityPlayerMP, Queue<Request>> entry : batch.entrySet()) {
            for (Request request : entry.getValue()) execute(entry.getKey(), request);
        }
    }

    private static boolean allowed(EntityPlayerMP player, Request request) {
        MessagePrinterAction m = request.message;
        if (!ConfigurationHandler.printerEnabled || ConfigurationHandler.printerMaxRange <= 0
            || player.isDead || player.worldObj != request.world || !player.playerNetServerHandler.netManager.isChannelOpen()) return false;
        double range = PrinterRange.clamp(ConfigurationHandler.printerMaxRange);
        double eyeY = player.posY + player.getEyeHeight();
        // A placement may click a supporting neighbor just outside the target sphere.
        if (!PrinterRange.contains(player.posX, eyeY, player.posZ, m.x, m.y, m.z,
            range + (m.action == MessagePrinterAction.PLACE ? 1 : 0))) return false;
        if (!request.world.blockExists(m.x, m.y, m.z)) return false;
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
        if (System.nanoTime() - request.time > 2_000_000_000L || !allowed(player, request)) {
            cancelMining(player);
            if (player.worldObj == request.world && request.world.blockExists(m.x, m.y, m.z))
                player.playerNetServerHandler.sendPacket(new S23PacketBlockChange(m.x, m.y, m.z, request.world));
            return;
        }
        if (m.action == 2) {
            Request started = mining.get(player);
            if (started == null || started.message.x != m.x || started.message.y != m.y || started.message.z != m.z) return;
        }
        ItemInWorldManager manager = player.theItemInWorldManager;
        double reach = manager.getBlockReachDistance();
        int slot = player.inventory.currentItem;
        float yaw = player.rotationYaw, pitch = player.rotationPitch;
        boolean sneaking = player.isSneaking();
        try {
            manager.setBlockReachDistance(Math.max(reach, PrinterRange.clamp(ConfigurationHandler.printerMaxRange) + 3));
            player.inventory.currentItem = m.slot;
            player.rotationYaw = m.yaw; player.rotationPitch = m.pitch; player.setSneaking(m.sneaking);
            if (m.action == MessagePrinterAction.PLACE) {
                player.playerNetServerHandler.processPlayerBlockPlacement(placement(m, player.getCurrentEquippedItem()));
            } else {
                player.playerNetServerHandler.processPlayerDigging(digging(m));
                if (m.action == 0) mining.put(player, request);

            }
        } catch (RuntimeException | LinkageError exception) {
            cancelMining(player);
            Reference.logger.error("Printer interaction failed", exception);
        } finally {
            manager.setBlockReachDistance(reach);
            player.inventory.currentItem = slot;
            player.rotationYaw = yaw; player.rotationPitch = pitch; player.setSneaking(sneaking);
            player.inventoryContainer.detectAndSendChanges();
        }
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
