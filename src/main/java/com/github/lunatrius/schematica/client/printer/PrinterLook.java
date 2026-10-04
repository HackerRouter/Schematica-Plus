// Rotation the printer reports to the server before a placement, so facing blocks get the schematic orientation, by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.printer;

import net.minecraft.util.MathHelper;
import net.minecraftforge.common.util.ForgeDirection;

/** A player rotation; the candidates are the four horizontal directions and straight up and down. */
public final class PrinterLook {
    public final float yaw, pitch;

    public PrinterLook(float yaw, float pitch) { this.yaw = yaw; this.pitch = pitch; }

    /** The candidates in a stable order: the current yaw first keeps the view still when it already fits. */
    public static PrinterLook[] candidates(float currentYaw) {
        float snapped = (MathHelper.floor_double(currentYaw / 90.0 + 0.5) & 3) * 90f;
        return new PrinterLook[] {
            new PrinterLook(snapped, 0), new PrinterLook(snapped + 90, 0), new PrinterLook(snapped + 180, 0),
            new PrinterLook(snapped + 270, 0), new PrinterLook(snapped, 90), new PrinterLook(snapped, -90)};
    }

    /** The direction the placement registry's PLAYER mappings use (ClientProxy.getOrientation). */
    public static ForgeDirection orientation(float yaw, float pitch) {
        if (pitch > 45) return ForgeDirection.DOWN;
        if (pitch < -45) return ForgeDirection.UP;
        switch (MathHelper.floor_double(yaw / 90.0 + 0.5) & 3) {
            case 0: return ForgeDirection.SOUTH;
            case 1: return ForgeDirection.WEST;
            case 2: return ForgeDirection.NORTH;
            default: return ForgeDirection.EAST;
        }
    }
}
