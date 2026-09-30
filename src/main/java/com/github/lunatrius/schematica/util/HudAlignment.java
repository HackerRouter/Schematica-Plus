// SPDX-License-Identifier: LGPL-3.0-only
// MaLiLib HUD alignment, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.util;

public enum HudAlignment {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER;

    public String value() { return name().toLowerCase(java.util.Locale.ROOT); }
    public String translationKey() { return "malilib.label.alignment." + value(); }

    public static HudAlignment parse(String value) {
        for (HudAlignment alignment : values()) if (alignment.value().equalsIgnoreCase(value)) return alignment;
        return TOP_RIGHT;
    }

    public int x(int screenWidth, int textWidth, double scale, int offset) {
        switch (this) {
            case TOP_RIGHT:
            case BOTTOM_RIGHT: return (int) (screenWidth / scale - textWidth - offset - 2);
            case CENTER: return (int) (screenWidth / scale / 2 - textWidth / 2.0 - offset);
            default: return offset + 2;
        }
    }

    public int y(int screenHeight, int contentHeight, double scale, int offset) {
        switch (this) {
            case BOTTOM_LEFT:
            case BOTTOM_RIGHT: return (int) (screenHeight / scale - contentHeight - offset);
            case CENTER: return (int) (screenHeight / scale / 2 - contentHeight / 2.0 + offset);
            default: return offset + 2;
        }
    }
}
