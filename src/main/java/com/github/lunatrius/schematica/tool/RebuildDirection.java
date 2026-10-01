// SPDX-License-Identifier: LGPL-3.0-only
// MaLiLib targeted direction, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.tool;

import net.minecraftforge.common.util.ForgeDirection;

public final class RebuildDirection {
    public enum Part { CENTER, LEFT, RIGHT, BOTTOM, TOP }

    private RebuildDirection() {}

    /** Horizontal and vertical position of a hit on a face, as seen by a player facing the given horizontal direction. */
    public static double[] facePosition(ForgeDirection side, ForgeDirection facing, double x, double y, double z) {
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
        return new double[] {h, v};
    }

    /** The block-relative point on a face (pushed out by offset) at the given face position; inverse of {@link #facePosition}. */
    public static double[] facePoint(ForgeDirection side, ForgeDirection facing, double h, double v, double offset) {
        switch (side) {
            case NORTH: return new double[] {1 - h, v, -offset};
            case SOUTH: return new double[] {h, v, 1 + offset};
            case WEST: return new double[] {-offset, v, h};
            case EAST: return new double[] {1 + offset, v, 1 - h};
            default: break;
        }
        double raw = side == ForgeDirection.DOWN ? 1 - v : v, y = side == ForgeDirection.DOWN ? -offset : 1 + offset;
        switch (facing) {
            case SOUTH: return new double[] {1 - h, y, raw};
            case WEST: return new double[] {1 - raw, y, 1 - h};
            case EAST: return new double[] {raw, y, h};
            default: return new double[] {h, y, 1 - raw};
        }
    }

    public static Part part(double h, double v) {
        double offH = Math.abs(h - 0.5), offV = Math.abs(v - 0.5);
        if (offH <= 0.25 && offV <= 0.25) return Part.CENTER;
        if (offH > offV) return h < 0.5 ? Part.LEFT : Part.RIGHT;
        return v < 0.5 ? Part.BOTTOM : Part.TOP;
    }

    public static ForgeDirection targeted(ForgeDirection side, ForgeDirection facing, double x, double y, double z) {
        double[] position = facePosition(side, facing, x, y, z);
        double h = position[0], v = position[1];
        Part part = part(h, v);
        if (part == Part.CENTER) return side;
        if (side == ForgeDirection.UP || side == ForgeDirection.DOWN) {
            if (part == Part.LEFT || part == Part.RIGHT) return rotate(facing, part == Part.RIGHT);
            return (side == ForgeDirection.DOWN ? v > 0.5 : v < 0.5) ? facing.getOpposite() : facing;
        }
        if (part == Part.LEFT || part == Part.RIGHT) return rotate(side, part == Part.LEFT);
        return part == Part.BOTTOM ? ForgeDirection.DOWN : ForgeDirection.UP;
    }

    private static ForgeDirection rotate(ForgeDirection side, boolean clockwise) {
        ForgeDirection[] directions = {ForgeDirection.NORTH, ForgeDirection.EAST, ForgeDirection.SOUTH, ForgeDirection.WEST};
        for (int i = 0; i < 4; i++) if (directions[i] == side) return directions[Math.floorMod(i + (clockwise ? 1 : -1), 4)];
        return side;
    }
}
