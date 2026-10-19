// Schematic Synchronizer browser info panel, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.cloud;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.cloud.CloudFile;
import com.github.lunatrius.schematica.world.schematic.DataVersions;

final class CloudInfo {
    static final int LABEL = 0xC0C0C0C0, VALUE = 0xFFFFFFFF, LINE = 12;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private CloudInfo() {}

    static int schematic(UiDraw draw, int x, int y, int width, int screenHeight, CloudFile file) {
        int limit = width - 8;
        draw.text(UiTranslations.format("litematica.gui.label.schematic_info.name"), x, y, LABEL);
        draw.text(draw.trim(file.name.isEmpty() ? file.path.name() : file.name, limit - 4), x + 4, y += LINE, VALUE);
        draw.text(draw.trim(UiTranslations.format("litematica.gui.label.schematic_info.schematic_author", file.author), limit), x, y += LINE, LABEL);
        if (file.created > 0) {
            draw.text(draw.trim(UiTranslations.format("litematica.gui.label.schematic_info.time_created",
                DATE_FORMAT.format(new Date(file.created))), limit), x, y += LINE, LABEL);
        }
        draw.text(UiTranslations.format("litematica.gui.label.schematic_info.region_count", file.regions), x, y += LINE, LABEL);
        String size = String.format("%d x %d x %d", file.sizeX, file.sizeY, file.sizeZ);
        if (screenHeight >= 340) {
            draw.text(UiTranslations.format("litematica.gui.label.schematic_info.total_volume", file.volume), x, y += LINE, LABEL);
            if (file.blocks > 0) draw.text(UiTranslations.format("litematica.gui.label.schematic_info.total_blocks", file.blocks), x, y += LINE, LABEL);
            draw.text(UiTranslations.format("litematica.gui.label.schematic_info.enclosing_size"), x, y += LINE, LABEL);
            draw.text(size, x + 4, y += LINE, VALUE);
        } else {
            draw.text(draw.trim(file.blocks > 0
                ? UiTranslations.format("litematica.gui.label.schematic_info.total_blocks_and_volume", file.blocks, file.volume)
                : UiTranslations.format("litematica.gui.label.schematic_info.total_volume", file.volume), limit), x, y += LINE, LABEL);
            draw.text(UiTranslations.format("litematica.gui.label.schematic_info.enclosing_size_value", size), x, y += LINE, LABEL);
        }
        if (!file.format.isEmpty()) {
            draw.text("litematic".equals(file.format) ? UiTranslations.format("litematica.gui.label.schematic_info.version", file.version)
                : "nbt".equals(file.format) ? UiTranslations.format("litematica.gui.label.schematic_info.vanilla_version")
                : "." + file.format, x, y += LINE, LABEL);
        }
        String schema = file.dataVersion < 0 ? null : DataVersions.name(file.dataVersion);
        if (schema != null) {
            draw.text(draw.trim(UiTranslations.format("litematica.gui.label.schematic_info.schema", schema, file.dataVersion), limit), x, y += LINE, LABEL);
        }
        return y + LINE;
    }
}
