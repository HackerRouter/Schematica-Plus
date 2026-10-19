// SPDX-License-Identifier: LGPL-3.0-only
// Litematica WidgetSchematicBrowser info box, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.browser;

import java.io.File;

import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiInput;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;

public final class SchematicInfoPanel extends UiPanel {
    public interface Contents {
        void draw(UiDraw draw, int x, int y, int width);
    }

    private final UiInput input;
    private final UiPanel screen;
    private final int reserve;
    private final Contents contents;
    private final SchematicInfoCache cache = new SchematicInfoCache();
    private final SchematicPreviewPane preview;
    private int scroll, maxScroll, contentBottom;

    public SchematicInfoPanel(UiInput input, UiPanel screen, int reserve, Contents contents) {
        this.input = input;
        this.screen = screen;
        this.reserve = reserve;
        this.contents = contents;
        preview = add(new SchematicPreviewPane(input, false, this::expand));
    }

    SchematicInfoCache cache() { return cache; }

    SchematicPreviewPane.Content content() { return preview.content(); }

    boolean infoDone(File file) { return cache.infoDone(file); }

    public void setContentBottom(int y) { contentBottom = y; }

    public void show(File file, long size) {
        if (file == null) preview.setContent(null);
        else if (preview.content() == null || !preview.content().file.equals(file)) preview.setContent(new SchematicPreviewPane.Content(file, size, cache));
    }

    public void resetScroll() { scroll = 0; }

    public void clear() {
        cache.clear();
        preview.setContent(null);
    }

    @Override
    public void draw(UiDraw draw, int mouseX, int mouseY) {
        draw.fill(bounds(), 0xA0000000);
        draw.border(bounds(), 0xFF999999);
        preview.setVisible(false);
        contentBottom = bounds().y;
        int bottom = bounds().bottom() - reserve;
        try (UiDraw.Clip ignored = draw.clip(new UiBounds(bounds().x + 1, bounds().y + 1, bounds().width - 2, bottom - bounds().y - 2))) {
            contents.draw(draw, bounds().x + 3, bounds().y + 3 - scroll, bounds().width - 4);
            super.draw(draw, mouseX, mouseY);
        }
        maxScroll = Math.max(0, contentBottom + scroll - bottom + 4);
        scroll = Math.min(scroll, maxScroll);
        if (maxScroll > 0) {
            int track = bottom - bounds().y - 4;
            int thumb = Math.max(8, track * track / (track + maxScroll));
            int offset = (track - thumb) * scroll / maxScroll;
            draw.fill(new UiBounds(bounds().right() - 4, bounds().y + 2 + offset, 2, thumb), 0xFF999999);
        }
    }

    @Override
    public boolean scroll(int x, int y, int amount) {
        if (maxScroll == 0) return false;
        scroll = Math.max(0, Math.min(maxScroll, scroll - Integer.signum(amount) * 24));
        return true;
    }

    public void preview(int x, int y, int width) {
        SchematicPreviewPane.Content content = preview.content();
        if (content == null || !content.available()) return;
        int size = width - 14;
        if (size <= 8) return;
        preview.setVisible(true);
        int viewHeight = Math.min(SchematicPreview.SIZE, Math.max(32, bounds().height - reserve - 11));
        preview.setBounds(x + 4, y, size, viewHeight);
        preview.layout(screen.bounds());
        contentBottom = preview.bounds().bottom() + 4;
    }

    private void expand() {
        SchematicPreviewPane.Content content = preview.content();
        if (content == null || !content.canModel()) return;
        content.modelSelected = true;
        SchematicPreviewPane fullscreen = new SchematicPreviewPane(input, true, () -> input.popModal());
        fullscreen.setContent(content);
        fullscreen.layout(screen.bounds());
        input.focus(preview.viewport);
        input.pushModal(fullscreen);
        input.focus(fullscreen.viewport);
    }
}
