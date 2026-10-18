// Schematic Synchronizer browser info panel, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.cloud;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.cloud.CloudFile;
import com.github.lunatrius.schematica.world.schematic.DataVersions;

final class CloudInfo {
    static final int LABEL = 0xC0C0C0C0, VALUE = 0xFFFFFFFF, LINE = 11;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private CloudInfo() {}

    static int infoWidth(int width) { return Math.max(170, Math.min(230, (int) ((width - 24) * 0.33))); }

    static int browserWidth(int width) { return width - 24 - infoWidth(width) - 4; }

    static void box(UiDraw draw, UiBounds bounds) {
        draw.fill(bounds, 0xA0000000);
        draw.border(bounds, 0xFF999999);
    }

    static int divider(UiDraw draw, int x, int y, int width) {
        draw.fill(new UiBounds(x, y + 2, width, 1), 0x40FFFFFF);
        return y + 7;
    }

    static int schematic(UiDraw draw, int x, int y, int width, CloudFile file) {
        int limit = width - 4;
        draw.text(UiTranslations.format("litematica.gui.label.schematic_info.name"), x, y, LABEL);
        draw.text(draw.trim(file.name, limit - 4), x + 4, y += LINE, VALUE);
        y += LINE;
        if (!file.author.isEmpty() && !"?".equals(file.author)) {
            draw.text(draw.trim(UiTranslations.format("litematica.gui.label.schematic_info.schematic_author", file.author), limit), x, y, LABEL);
            y += LINE;
        }
        if (file.created > 0) {
            draw.text(draw.trim(UiTranslations.format("litematica.gui.label.schematic_info.time_created",
                DATE_FORMAT.format(new Date(file.created))), limit), x, y, LABEL);
            y += LINE;
        }
        draw.text(UiTranslations.format("litematica.gui.label.schematic_info.region_count", file.regions), x, y, LABEL);
        y += LINE;
        if (file.volume > 0) {
            draw.text(UiTranslations.format("litematica.gui.label.schematic_info.total_volume", file.volume), x, y, LABEL);
            y += LINE;
        }
        if (file.blocks > 0) {
            draw.text(UiTranslations.format("litematica.gui.label.schematic_info.total_blocks", file.blocks), x, y, LABEL);
            y += LINE;
        }
        if (file.sizeX > 0) {
            draw.text(UiTranslations.format("litematica.gui.label.schematic_info.enclosing_size"), x, y, LABEL);
            draw.text(String.format("%d x %d x %d", file.sizeX, file.sizeY, file.sizeZ), x + 4, y += LINE, VALUE);
            y += LINE;
        }
        String schema = file.dataVersion < 0 ? null : DataVersions.name(file.dataVersion);
        if (schema != null) {
            draw.text(draw.trim(UiTranslations.format("litematica.gui.label.schematic_info.schema", schema, file.dataVersion), limit), x, y, LABEL);
            y += LINE;
        }
        return y;
    }
}
