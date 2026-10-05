// ForgeMultipart parts under schematic rotation and mirroring, by HackerRouter, 2026.
package com.github.lunatrius.schematica.compat;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.lunatrius.schematica.util.SchematicTransform;

/**
 * Saved ForgeMultipart parts ("parts" list of a TileMultipart): microblock shapes (low nibble = slot: faces and
 * hollows a side, corners and edges as CodeChickenLib places them, posts an axis) and ProjectRed parts (gates and
 * other face parts: "orient" = side << 2 | CodeChickenLib rotation; wires and lamps: "side").
 */
final class MultipartTransforms {
    /** CodeChickenLib Rotation.sideRotMap: the direction rotation r points to on side s. */
    private static final int[] SIDE_ROT_MAP = {3, 4, 2, 5, 3, 5, 2, 4, 1, 5, 0, 4, 1, 4, 0, 5, 1, 2, 0, 3, 1, 3, 0, 2};

    private MultipartTransforms() {}

    static void transformTile(NBTTagCompound tile, char operation) {
        NBTTagList parts = tile.getTagList("parts", 10);
        for (int i = 0; i < parts.tagCount(); i++) {
            NBTTagCompound part = parts.getCompoundTagAt(i);
            transformPart(part.getString("id"), part, operation);
        }
    }

    static void transformPart(String id, NBTTagCompound part, char operation) {
        if (id.startsWith("mcr_") && part.hasKey("shape", 1)) {
            int shape = part.getByte("shape") & 255;
            part.setByte("shape", (byte) (shape & 0xF0 | slot(id, shape & 15, operation)));
        } else if (id.startsWith("pr_") && part.hasKey("orient", 1)) {
            part.setByte("orient", (byte) orientation(part.getByte("orient") & 255, operation));
        } else if (id.startsWith("pr_") && part.hasKey("side", 1)) {
            int side = part.getByte("side");
            if (side >= 0 && side < 6) part.setByte("side", (byte) turn(operation, side));
        }
    }

    private static int turn(char operation, int side) {
        return SchematicTransform.direction(operation, ForgeDirection.getOrientation(side)).ordinal();
    }

    private static int[] vector(char operation, int x, int y, int z) {
        double[] p = SchematicTransform.point(operation, x, y, z, 0, 0, 0);
        return new int[] {(int) Math.round(p[0]), (int) Math.round(p[1]), (int) Math.round(p[2])};
    }

    static int slot(String id, int slot, char operation) {
        switch (id) {
            case "mcr_face": case "mcr_hollow": return slot < 6 ? turn(operation, slot) : slot;
            case "mcr_cnr": {
                int[] v = vector(operation, (slot & 4) != 0 ? 1 : -1, (slot & 1) != 0 ? 1 : -1, (slot & 2) != 0 ? 1 : -1);
                return (v[0] > 0 ? 4 : 0) | (v[1] > 0 ? 1 : 0) | (v[2] > 0 ? 2 : 0);
            }
            case "mcr_edge": {
                if (slot >= 12) return slot;
                int k = slot >> 2, sx = (slot & 2) != 0 ? 1 : -1, sz = (slot & 1) != 0 ? 1 : -1;
                int[] v = k == 0 ? new int[] {sx, 0, sz} : k == 1 ? new int[] {sz, sx, 0} : new int[] {0, sz, sx};
                v = vector(operation, v[0], v[1], v[2]);
                if (v[1] == 0) return (v[0] > 0 ? 2 : 0) | (v[2] > 0 ? 1 : 0);
                if (v[2] == 0) return 4 | (v[1] > 0 ? 2 : 0) | (v[0] > 0 ? 1 : 0);
                return 8 | (v[2] > 0 ? 2 : 0) | (v[1] > 0 ? 1 : 0);
            }
            case "mcr_post": {
                if (slot > 2) return slot;
                int[] v = vector(operation, slot == 2 ? 1 : 0, slot == 0 ? 1 : 0, slot == 1 ? 1 : 0);
                return v[1] != 0 ? 0 : v[2] != 0 ? 1 : 2;
            }
            default: return slot;
        }
    }

    /** A face part's side and the direction its rotation points to, both turned; mirrors keep the pointed direction. */
    static int orientation(int orient, char operation) {
        int side = orient >> 2 & 7, rotation = orient & 3;
        if (side > 5) return orient;
        int newSide = turn(operation, side), front = turn(operation, SIDE_ROT_MAP[side << 2 | rotation]);
        for (int r = 0; r < 4; r++) if (SIDE_ROT_MAP[newSide << 2 | r] == front) return orient & ~0x1F | newSide << 2 | r;
        return orient;
    }
}
