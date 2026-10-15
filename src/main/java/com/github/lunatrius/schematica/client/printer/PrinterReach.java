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
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        MessagePrinterAction message = new MessagePrinterAction();
        message.action = action; message.x = x; message.y = y; message.z = z; message.side = side;
        message.slot = player.inventory.currentItem; message.yaw = player.rotationYaw;
        message.pitch = player.rotationPitch; message.sneaking = player.isSneaking();
        if (hit != null) {
            message.hitX = (float) (hit.xCoord - x); message.hitY = (float) (hit.yCoord - y); message.hitZ = (float) (hit.zCoord - z);
        }
        PacketHandler.INSTANCE.sendToServer(message);
    }
}
