// Mining a block for the printer and the destroy assist without PlayerControllerMP, by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.printer;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.world.World;

/**
 * PlayerControllerMP's survival mining (clickBlock, then onPlayerDamageBlock every tick) with its own progress.
 * Minecraft.func_147115_a resets the controller's mining every tick the attack key is up, so a block mined for
 * the printer while the player does not hold the key would never break; the dig packets and the breaking
 * progress are therefore kept here, where that reset does not reach them.
 */
public final class BlockMiner {
    public enum State { IDLE, MINING, BROKEN }

    private final Minecraft mc = Minecraft.getMinecraft();
    private int x, y, z, side, slot = -1, ticks;
    /** PlayerControllerMP.blockHitDelay: five ticks after a block broke before the next one is started. */
    private long readyAt;
    private float progress;
    private boolean active;

    public boolean active() { return active; }
    public boolean at(int bx, int by, int bz) { return active && bx == x && by == y && bz == z; }

    /** Starts mining a block (a creative player breaks it right away); BROKEN when it is gone already. */
    public State start(int bx, int by, int bz, int face) {
        EntityClientPlayerMP player = mc.thePlayer;
        World world = mc.theWorld;
        if (active) abort();
        if (System.nanoTime() < readyAt) return State.IDLE;
        Block block = world.getBlock(bx, by, bz);
        if (block.getMaterial() == Material.air) return State.BROKEN;
        slot = -1;
        syncSlot(player);
        x = bx; y = by; z = bz; side = face; progress = 0; ticks = 0;
        player.sendQueue.addToSendQueue(new C07PacketPlayerDigging(0, x, y, z, side));
        if (mc.playerController.isInCreativeMode()) {
            mc.playerController.onPlayerDestroyBlock(x, y, z, side);
            readyAt = System.nanoTime() + 250_000_000L;
            return State.BROKEN;
        }
        block.onBlockClicked(world, x, y, z, player);
        if (block.getPlayerRelativeBlockHardness(player, world, x, y, z) >= 1.0F) {
            mc.playerController.onPlayerDestroyBlock(x, y, z, side);
            return State.BROKEN;
        }
        active = true;
        return State.MINING;
    }

    /** Mines on for a tick; BROKEN once the block broke, IDLE when there is nothing to mine. */
    public State tick() {
        if (!active) return State.IDLE;
        EntityClientPlayerMP player = mc.thePlayer;
        World world = mc.theWorld;
        Block block = world.getBlock(x, y, z);
        if (block.getMaterial() == Material.air) {
            clear();
            return State.BROKEN;
        }
        syncSlot(player);
        progress += block.getPlayerRelativeBlockHardness(player, world, x, y, z);
        if (ticks++ % 4 == 0) {
            mc.getSoundHandler().playSound(new net.minecraft.client.audio.PositionedSoundRecord(
                new net.minecraft.util.ResourceLocation(block.stepSound.getStepResourcePath()), (block.stepSound.getVolume() + 1.0F) / 8.0F,
                block.stepSound.getPitch() * 0.5F, x + 0.5F, y + 0.5F, z + 0.5F));
        }
        player.swingItem();
        if (progress >= 1.0F) {
            player.sendQueue.addToSendQueue(new C07PacketPlayerDigging(2, x, y, z, side));
            mc.playerController.onPlayerDestroyBlock(x, y, z, side);
            clear();
            readyAt = System.nanoTime() + 250_000_000L;
            return State.BROKEN;
        }
        world.destroyBlockInWorldPartially(player.getEntityId(), x, y, z, (int) (progress * 10.0F) - 1);
        return State.MINING;
    }

    /** Stops mining and tells the server, like PlayerControllerMP.resetBlockRemoving. */
    public void abort() {
        if (!active) return;
        if (mc.thePlayer != null) mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(1, x, y, z, -1));
        clear();
    }

    private void clear() {
        if (active && mc.theWorld != null && mc.thePlayer != null) mc.theWorld.destroyBlockInWorldPartially(mc.thePlayer.getEntityId(), x, y, z, -1);
        active = false;
        progress = 0;
    }

    /** The server must know the held tool before the dig packets (PlayerControllerMP.syncCurrentPlayItem). */
    private void syncSlot(EntityClientPlayerMP player) {
        int current = player.inventory.currentItem;
        if (current != slot) {
            player.sendQueue.addToSendQueue(new C09PacketHeldItemChange(current));
            slot = current;
        }
    }
}
