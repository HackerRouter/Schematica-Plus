// SPDX-License-Identifier: LGPL-3.0-only
// Litematica Icons/ButtonIcons, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.framework;

public enum UiSprite {
    PLUS_MINUS(0, 128, 16, 16, true),
    AREA_EDITOR(102, 70, 14, 14, true),
    AREA_SELECTION(102, 0, 14, 14, true),
    CONFIGURATION(102, 84, 14, 14, true),
    LOADED_SCHEMATICS(102, 14, 14, 14, true),
    SCHEMATIC_BROWSER(102, 28, 14, 14, true),
    SCHEMATIC_MANAGER(102, 56, 14, 14, true),
    SCHEMATIC_PLACEMENTS(102, 42, 14, 14, true),
    TASK_MANAGER(102, 112, 14, 14, true),
    FILE(144, 0, 12, 12, false),
    SCHEMATIC(144, 12, 12, 12, false),
    MEMORY(186, 0, 12, 12, false),
    DIRECTORY(156, 0, 12, 12, false),
    UP(156, 12, 12, 12, false),
    ROOT(156, 24, 12, 12, false),
    SEARCH(156, 36, 12, 12, false),
    CREATE_DIRECTORY(156, 48, 12, 12, false),
    CHECK_OFF(198, 0, 11, 11, false),
    CHECK_ON(198, 11, 11, 11, false);

    private static final String TEXTURE = "schematica_plus:textures/gui/litematica_widgets.png";
    public final int u;
    public final int v;
    public final int width;
    public final int height;
    private final boolean states;

    UiSprite(int u, int v, int width, int height, boolean states) {
        this.u = u;
        this.v = v;
        this.width = width;
        this.height = height;
        this.states = states;
    }

    public void draw(UiDraw draw, int x, int y, boolean enabled, boolean hovered) {
        int state = states ? !enabled ? 0 : hovered ? 2 : 1 : 0;
        draw.texture(TEXTURE, new UiBounds(x, y, width, height), u + state * width, v, width, height, 256, 256);
    }
}
