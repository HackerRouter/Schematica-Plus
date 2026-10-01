// SPDX-License-Identifier: LGPL-3.0-only
// MaLiLib targeted direction, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.tool;

import net.minecraftforge.common.util.ForgeDirection;

public final class RebuildDirection {
    private RebuildDirection() {}
    public static ForgeDirection targeted(ForgeDirection side, ForgeDirection facing, double x, double y, double z) {
        double h = 0, v = y;
        if (side == ForgeDirection.UP || side == ForgeDirection.DOWN) {
            switch (facing) {
                case NORTH: h = x; v = 1 - z; break;
                case SOUTH: h = 1 - x; v = z; break;
                case WEST: h = 1 - z; v = 1 - x; break;
                case EAST: h = z; v = x; break;
                default: break;
            }
            if (side == ForgeDirection.DOWN) v = 1 - v;
        } else if (side == ForgeDirection.NORTH || side == ForgeDirection.SOUTH) h = side == ForgeDirection.SOUTH ? x : 1 - x;
        else h = side == ForgeDirection.WEST ? z : 1 - z;
        double offH = Math.abs(h - 0.5), offV = Math.abs(v - 0.5);
        if (offH <= 0.25 && offV <= 0.25) return side;
        if (side == ForgeDirection.UP || side == ForgeDirection.DOWN) {
            if (offH > offV) return rotate(facing, h >= 0.5);
            return (side == ForgeDirection.DOWN ? v > 0.5 : v < 0.5) ? facing.getOpposite() : facing;
        }
        if (offH > offV) return rotate(side, h < 0.5);
        return v < 0.5 ? ForgeDirection.DOWN : ForgeDirection.UP;
    }
    private static ForgeDirection rotate(ForgeDirection side, boolean clockwise) {
        ForgeDirection[] directions = {ForgeDirection.NORTH, ForgeDirection.EAST, ForgeDirection.SOUTH, ForgeDirection.WEST};
        for (int i = 0; i < 4; i++) if (directions[i] == side) return directions[Math.floorMod(i + (clockwise ? 1 : -1), 4)];
        return side;
    }
}
