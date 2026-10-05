package com.github.lunatrius.schematica.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** The world the player is in, for preview tiles that follow its time of day. */
@SideOnly(Side.CLIENT)
final class ClientWorld {
    private ClientWorld() {}

    static World current() { return Minecraft.getMinecraft().theWorld; }
}
