// SPDX-License-Identifier: LGPL-3.0-only
// Client side of the accurate placement protocol, following Litematica's easyPlaceProtocolVersion, by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.printer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.AccuratePlacement;
import com.github.lunatrius.schematica.network.PacketHandler;
import com.github.lunatrius.schematica.network.message.MessagePlacementIntent;

import cpw.mods.fml.common.registry.GameData;

/** Tells a Plus server the metadata the next placement should get, so orientation no longer depends on the click. */
public final class AccuratePlacementClient {
    private AccuratePlacementClient() {}

    /** Whether the server will set this block's orientation: auto, v3 and v2 use the protocol, slabs_only only for slabs. */
    public static boolean active(Block block) {
        if (!SchematicaPlus.proxy.supportsAccuratePlacement
            || !AccuratePlacement.supports(block)) return false;
        switch (ConfigurationHandler.easyPlaceProtocolVersion) {
            case "none": return false;
            case "slabs_only": return block instanceof BlockSlab;
            default: return true;
        }
    }

    /** Sends the intent just before the placement click at the target position. */
    public static void announce(int x, int y, int z, Block block, int metadata) {
        String name = GameData.getBlockRegistry().getNameForObject(block);
        if (name != null) PacketHandler.INSTANCE.sendToServer(new MessagePlacementIntent(x, y, z, name, metadata & 15));
    }
}
