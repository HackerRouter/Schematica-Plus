// SPDX-License-Identifier: LGPL-3.0-only
package com.github.lunatrius.schematica.client.printer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.util.Vec3;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.PrinterRange;
import com.github.lunatrius.schematica.network.PacketHandler;
import com.github.lunatrius.schematica.network.message.MessagePrinterAction;

final class PrinterReach {
    private PrinterReach() {}

    static boolean assisted() { return SchematicaPlus.proxy.printerMaxPlacementsPerTick > 0; }

    static boolean fastBreaking() { return assisted() && SchematicaPlus.proxy.printerMaxBreaksPerTick > 0; }

    static int placementLimit() {
        return Math.max(1, assisted() ? Math.min(ConfigurationHandler.placeBlocksPerTick, SchematicaPlus.proxy.printerMaxPlacementsPerTick)
            : ConfigurationHandler.placeBlocksPerTick);
    }

    static int breakLimit() {
        return fastBreaking() ? Math.max(1, Math.min(ConfigurationHandler.breakBlocksPerTick, SchematicaPlus.proxy.printerMaxBreaksPerTick)) : 1;
    }

    static double range() {
        return PrinterRange.effective(ConfigurationHandler.printerWorkRange,
            Minecraft.getMinecraft().playerController.getBlockReachDistance(), SchematicaPlus.proxy.printerMaxRange);
    }

    static boolean remote(int x, int y, int z) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP player = mc.thePlayer;
        return SchematicaPlus.proxy.printerMaxRange > 0 && !PrinterRange.contains(player.posX,
            player.posY + player.getEyeHeight() - player.getDefaultEyeHeight(), player.posZ, x, y, z,
            mc.playerController.getBlockReachDistance());
    }

    static void send(int action, int x, int y, int z, int side, Vec3 hit) {
        PacketHandler.INSTANCE.sendToServer(request(action, x, y, z, side, hit));
    }

    static MessagePrinterAction request(int action, int x, int y, int z, int side, Vec3 hit) {
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        MessagePrinterAction message = new MessagePrinterAction();
        message.action = action; message.x = x; message.y = y; message.z = z; message.side = side;
        message.slot = player.inventory.currentItem; message.yaw = player.rotationYaw;
        message.pitch = player.rotationPitch; message.sneaking = player.isSneaking();
        message.expectedItem = net.minecraft.item.ItemStack.copyItemStack(player.getCurrentEquippedItem());
        if (message.expectedItem != null) message.expectedItem.stackSize = 1;
        message.snapshot(player.worldObj, x, y, z);
        if (hit != null) {
            message.hitX = (float) (hit.xCoord - x); message.hitY = (float) (hit.yCoord - y); message.hitZ = (float) (hit.zCoord - z);
        }
        return message;
    }
}
