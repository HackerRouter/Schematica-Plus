// SPDX-License-Identifier: LGPL-3.0-only
// Server side of the accurate placement protocol, the Plus counterpart of Litematica's easyPlaceProtocol v3, by HackerRouter, 2026.
package com.github.lunatrius.schematica.handler;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockCocoa;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockEndPortalFrame;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockPumpkin;
import net.minecraft.block.BlockQuartz;
import net.minecraft.block.BlockRail;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockTripWireHook;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;

import com.github.lunatrius.schematica.network.message.MessagePlacementIntent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameData;
import net.minecraftforge.event.world.BlockEvent;

/**
 * Applies the metadata a Plus client asked for to the block its placement creates. As with Litematica's whitelisted
 * properties, only orientation-like bits (facing, slab half, axis, rail shape, repeater delay, comparator mode, open)
 * may change; powered states, ages and contents stay as vanilla placed them.
 */
public final class AccuratePlacement {
    public static final AccuratePlacement INSTANCE = new AccuratePlacement();
    static final long LIFETIME_NANOS = 2_000_000_000L;
    static final int MAX_PENDING = 64;

    private static final class Intent {
        final int metadata;
        final String block;
        final long time = System.nanoTime();

        Intent(MessagePlacementIntent message) {
            metadata = message.metadata; block = message.block;
        }
    }

    /** Intents arrive on the network thread ahead of the placements queued for the server thread, so several can wait. */
    private final Map<UUID, Map<Long, Intent>> intents = new ConcurrentHashMap<>();

    private AccuratePlacement() {}

    static long key(int x, int y, int z) {
        return ((long) x & 0x3FFFFFF) << 38 | ((long) z & 0x3FFFFFF) << 12 | (y & 0xFFF);
    }

    public void intent(EntityPlayerMP player, MessagePlacementIntent message) {
        if (player == null || !ConfigurationHandler.accuratePlacementEnabled) return;
        Map<Long, Intent> pending = intents.computeIfAbsent(player.getUniqueID(), id -> new LinkedHashMap<>());
        synchronized (pending) {
            long now = System.nanoTime();
            Iterator<Intent> iterator = pending.values().iterator();
            while (iterator.hasNext() && (now - iterator.next().time > LIFETIME_NANOS || pending.size() >= MAX_PENDING)) iterator.remove();
            long key = key(message.x, message.y, message.z);
            pending.remove(key);
            pending.put(key, new Intent(message));
        }
    }

    public void forget(EntityPlayer player) { intents.remove(player.getUniqueID()); }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlace(BlockEvent.PlaceEvent event) {
        if (event.world.isRemote || event instanceof BlockEvent.MultiPlaceEvent || !(event.player instanceof EntityPlayerMP)) return;
        Map<Long, Intent> pending = intents.get(event.player.getUniqueID());
        if (pending == null) return;
        Intent intent;
        synchronized (pending) {
            intent = pending.remove(key(event.x, event.y, event.z));
        }
        if (intent == null || !ConfigurationHandler.accuratePlacementEnabled || System.nanoTime() - intent.time > LIFETIME_NANOS) return;
        Block block = event.world.getBlock(event.x, event.y, event.z);
        if (block != event.placedBlock || !intent.block.equals(GameData.getBlockRegistry().getNameForObject(block))) return;
        int current = event.world.getBlockMetadata(event.x, event.y, event.z);
        if (allowed(block, current, intent.metadata)) event.world.setBlockMetadataWithNotify(event.x, event.y, event.z, intent.metadata, 2);
    }

    /** The metadata bits holding the block's whitelisted placement properties, 0 for blocks without any. */
    static int mask(Block block) {
        if (block instanceof BlockSlab) return block.isOpaqueCube() ? 0 : 0x8;
        if (block instanceof BlockStairs || block instanceof BlockPistonBase || block instanceof BlockDispenser
            || block instanceof BlockHopper || block instanceof BlockFurnace || block instanceof BlockChest
            || block instanceof BlockEnderChest || block instanceof BlockLadder || block instanceof BlockTorch
            || block instanceof BlockLever || block instanceof BlockButton || block instanceof BlockFenceGate
            || block instanceof BlockRedstoneComparator) return 0x7;
        if (block instanceof BlockRotatedPillar) return 0xC;
        if (block instanceof BlockRedstoneRepeater || block instanceof BlockTrapDoor || block instanceof BlockRail) return 0xF;
        if (block instanceof BlockRailBase) return 0x7;
        if (block instanceof BlockPumpkin || block instanceof BlockAnvil || block instanceof BlockCocoa
            || block instanceof BlockTripWireHook || block instanceof BlockEndPortalFrame) return 0x3;
        if (block == Blocks.standing_sign) return 0xF;
        return 0;
    }

    /** Whether a freshly placed block may take the desired metadata without gaining anything the player did not pay for. */
    static boolean allowed(Block block, int current, int desired) {
        if (desired == current || desired < 0 || desired > 15) return false;
        if (block instanceof BlockQuartz) {
            if (current < 2 || current > 4 || desired < 2 || desired > 4) return false;
        } else {
            int mask = mask(block);
            if (mask == 0 || (desired & ~mask) != (current & ~mask)) return false;
        }
        if (block.damageDropped(desired) != block.damageDropped(current)) return false;
        return block.getItemDropped(desired, new Random(0), 0) == block.getItemDropped(current, new Random(0), 0)
            && block.quantityDropped(desired, 0, new Random(0)) == block.quantityDropped(current, 0, new Random(0));
    }
}
