// SPDX-License-Identifier: LGPL-3.0-only
// Litematica/MaLiLib icons, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.framework;

public enum UiSprite {
    PLUS_MINUS(0, 128, 16, 16, true),
    ENCLOSING_BOX_ENABLED(0, 144, 16, 16, true),
    ENCLOSING_BOX_DISABLED(0, 160, 16, 16, true),
    AREA_EDITOR(102, 70, 14, 14, true),
    AREA_SELECTION(102, 0, 14, 14, true),
    CONFIGURATION(102, 84, 14, 14, true),
    LOADED_SCHEMATICS(102, 14, 14, 14, true),
    SCHEMATIC_BROWSER(102, 28, 14, 14, true),
    SCHEMATIC_MANAGER(102, 56, 14, 14, true),
    SCHEMATIC_PLACEMENTS(102, 42, 14, 14, true),
    SCHEMATIC_PROJECTS(102, 98, 14, 14, true),
    TASK_MANAGER(102, 112, 14, 14, true),
    FILE(144, 0, 12, 12, false),
    SCHEMATIC(144, 12, 12, 12, false),
    SCHEMPLUS(0, 0, 12, 12, false),
    MEMORY(186, 0, 12, 12, false),
    JSON(144, 44, 12, 12, false),
    DIRECTORY(156, 0, 12, 12, false),
    UP(156, 12, 12, 12, false),
    ROOT(156, 24, 12, 12, false),
    SEARCH(156, 36, 12, 12, false),
    CREATE_DIRECTORY(156, 48, 12, 12, false),
    CHECK_OFF(198, 0, 11, 11, false),
    CHECK_ON(198, 11, 11, 11, false),
    INFO(168, 18, 11, 11, false),
    NOTICE(168, 29, 11, 11, false),
    SORT_UP(209, 0, 15, 15, false),
    SORT_DOWN(209, 15, 15, 15, false),
    CONFIG_SEARCH(201, 0, 12, 12, false, true),
    SLIDER(153, 0, 16, 16, true, true),
    TEXT_FIELD(153, 16, 16, 16, true, true),
    MOVE_UP(108, 0, 15, 15, true, true),
    MOVE_DOWN(108, 15, 15, 15, true, true),
    ADD(108, 30, 15, 15, true, true),
    REMOVE(108, 45, 15, 15, true, true);

    private static final String TEXTURE = "schematica_plus:textures/gui/litematica_widgets.png";
    public final int u;
    public final int v;
    public final int width;
    public final int height;
    private final boolean states;
    private final boolean malilib;

    UiSprite(int u, int v, int width, int height, boolean states) {
        this(u, v, width, height, states, false);
    }

    UiSprite(int u, int v, int width, int height, boolean states, boolean malilib) {
        this.u = u;
        this.v = v;
        this.width = width;
        this.height = height;
        this.states = states;
        this.malilib = malilib;
    }

    public void draw(UiDraw draw, int x, int y, boolean enabled, boolean hovered) {
        int state = states ? !enabled ? 0 : hovered ? 2 : 1 : 0;
        String texture = this == SCHEMPLUS ? "schematica_plus:textures/gui/schemplus.png"
            : malilib ? "schematica_plus:textures/gui/malilib_widgets.png" : TEXTURE;
        int size = this == SCHEMPLUS ? 12 : 256;
        draw.texture(texture, new UiBounds(x, y, width, height), u + state * width, v, width, height, size, size);
    }

    public static UiSprite schematicFile(String name) {
        if (name == null) return MEMORY;
        String filename = name.toLowerCase(java.util.Locale.ROOT);
        if (filename.endsWith(".litematic")) return FILE;
        if (filename.endsWith(".schemplus")) return SCHEMPLUS;
        if (filename.endsWith(".json")) return JSON;
        return SCHEMATIC;
    }
}
