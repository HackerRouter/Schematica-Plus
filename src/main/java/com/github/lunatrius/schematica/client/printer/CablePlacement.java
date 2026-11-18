package com.github.lunatrius.schematica.client.printer;

import java.util.function.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;

import com.github.lunatrius.schematica.compat.AE2Items;
import com.github.lunatrius.schematica.reference.Reference;

final class CablePlacement {
    private CablePlacement() {}

    static boolean place(Minecraft mc, TileEntity template, int x, int y, int z, Predicate<ItemStack> select) {
        World world = mc.theWorld;
        EntityClientPlayerMP player = mc.thePlayer;
        TileEntity host = world.getTileEntity(x, y, z);
        if (!AE2Items.supports(host) && !world.isAirBlock(x, y, z)) return false;
        double eyeY = player.posY + player.getEyeHeight() - player.getDefaultEyeHeight();
        Vec3 eye = Vec3.createVectorHelper(player.posX, eyeY, player.posZ);
        double reach = mc.playerController.getBlockReachDistance();
        try {
            for (AE2Items.Entry entry : AE2Items.additions(template, host)) {
                ForgeDirection face = entry.side == ForgeDirection.UNKNOWN ? ForgeDirection.UP : entry.side;
                if (!AE2Items.canAdd(host, entry, face)) continue;
                if (entry.facade && host == null) continue;
                Vec3 hit = Vec3.createVectorHelper(x + 0.5 + face.offsetX * 0.49, y + 0.5 + face.offsetY * 0.49,
                    z + 0.5 + face.offsetZ * 0.49);
                if (eye.distanceTo(hit) > reach) continue;
                PlacementAim aim = new PlacementAim(hit.xCoord - eye.xCoord, hit.yCoord - eye.yCoord, hit.zCoord - eye.zCoord);
                if (entry.facade) {
                    MovingObjectPosition target = world.rayTraceBlocks(eye, Vec3.createVectorHelper(x + 0.5, y + 0.5, z + 0.5));
                    if (target == null || target.blockX != x || target.blockY != y || target.blockZ != z || target.sideHit != face.ordinal()) continue;
                    hit = target.hitVec;
                    aim = new PlacementAim(hit.xCoord - eye.xCoord, hit.yCoord - eye.yCoord, hit.zCoord - eye.zCoord);
                }
                if (!select.test(entry.stack)) continue;
                float yaw = player.rotationYaw, pitch = player.rotationPitch;
                try {
                    player.rotationYaw = !entry.facade && entry.side.offsetY != 0 ? (entry.spin - 2) * 90 : aim.yaw;
                    player.rotationPitch = aim.pitch;
                    mc.playerController.updateController();
                    player.sendQueue.addToSendQueue(new C05PacketPlayerLook(player.rotationYaw, player.rotationPitch, player.onGround));
                    if (ForgeEventFactory.onPlayerInteract(player, Action.RIGHT_CLICK_BLOCK, x, y, z, face.ordinal(), world).isCanceled()) {
                        return entry.facade && AE2Items.additions(template, world.getTileEntity(x, y, z)).stream()
                            .noneMatch(missing -> missing.facade && missing.side == entry.side);
                    }
                    if (entry.facade) return false;
                    return mc.playerController.onPlayerRightClick(player, mc.theWorld, player.getHeldItem(), x, y, z, face.ordinal(), hit);
                } finally {
                    player.rotationYaw = yaw;
                    player.rotationPitch = pitch;
                    player.sendQueue.addToSendQueue(new C05PacketPlayerLook(yaw, pitch, player.onGround));
                }
            }
        } catch (ReflectiveOperationException | LinkageError error) {
            Reference.logger.debug("Could not assemble the cable host", error);
        }
        return false;
    }
}
