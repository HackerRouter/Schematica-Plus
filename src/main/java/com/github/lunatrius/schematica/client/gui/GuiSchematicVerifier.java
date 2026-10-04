// SPDX-License-Identifier: LGPL-3.0-only
// Litematica verifier layout, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.client.gui.GuiScreen;

import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiLabel;
import com.github.lunatrius.schematica.client.gui.framework.UiListModel;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;
import com.github.lunatrius.schematica.client.gui.framework.UiRowList;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;
import com.github.lunatrius.schematica.client.gui.framework.UiSprite;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.input.Hotkeys;
import com.github.lunatrius.schematica.client.verifier.VerificationManager;
import com.github.lunatrius.schematica.client.verifier.VerificationScan;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.client.verifier.VerificationScan.Group;
import com.github.lunatrius.schematica.client.verifier.VerificationScan.State;
import com.github.lunatrius.schematica.client.verifier.VerificationScan.Type;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.client.gui.VerifierBlockInfo.Visual;
import com.github.lunatrius.schematica.handler.VisualSettings;
import com.github.lunatrius.schematica.util.MessageException;

public final class GuiSchematicVerifier extends UiScreen {
    private final VerificationManager.Session session;
    private final UiListModel<Row> model = new UiListModel<>(22, row -> row.type.key);
    private final List<UiButton> actions = new ArrayList<>(), filters = new ArrayList<>();
    private final Map<State, Visual> visuals = new HashMap<>();
    private UiButton start, stop, reset, ignored, menu;
    private UiRowList<Row> list;
    private UiLabel status, counts;
    private UiPanel header;
    private final UiButton[] columns = new UiButton[3];
    private VerificationScan previous;
    private long revision = -1;
    private int expectedWidth, foundWidth;
    private int refreshTicks;
    private String message = "";

    public GuiSchematicVerifier(GuiScreen parent, SchematicWorld placement) {
        super(parent, UiTranslations.format("litematica.gui.title.schematic_verifier", placement.name));
        session = VerificationManager.INSTANCE.get(placement);
    }

    private UiButton action(Supplier<String> text, Runnable run) {
        UiButton button = root.add(new UiButton(text, mouse -> { if (mouse == 0) { run.run(); refresh(); layoutWidgets(); } }));
        actions.add(button);
        return button;
    }

    @Override protected void createWidgets() {
        start = action(() -> UiTranslations.format(session.scan() != null && !session.running()
            ? "litematica.gui.button.schematic_verifier.resume" : "litematica.gui.button.schematic_verifier.start"), () -> {
                message = "";
                try { session.start(mc); }
                catch (MessageException error) { message = UiTranslations.format(error.key(), error.arguments()); }
            });
        start.setTooltip(UiTranslations.format("schematica.ui.verifier.comparison"));
        stop = action(() -> UiTranslations.format("litematica.gui.button.schematic_verifier.stop"), session::pause);
        reset = action(() -> UiTranslations.format("litematica.gui.button.schematic_verifier.reset_verifier"), () -> { session.reset(); message = ""; });
        action(() -> UiTranslations.format("litematica.gui.button.schematic_verifier.range_type", UiTranslations.format(session.layers()
            ? "litematica.gui.label.block_info_list_type.render_layers" : "litematica.gui.label.block_info_list_type.all")),
            () -> { session.setLayers(!session.layers()); message = ""; });
        ignored = action(() -> UiTranslations.format("litematica.gui.button.schematic_verifier.reset_ignored"), () -> {
            if (session.scan() != null) session.scan().resetIgnored();
        });
        action(() -> UiTranslations.format("litematica.gui.button.schematic_verifier.toggle_info_hud", UiTranslations.format(session.hud
            ? "litematica.message.value.on" : "litematica.message.value.off")), () -> session.hud = !session.hud);
        for (Type type : Type.values()) filters.add(root.add(new UiButton(() -> type.color + UiTranslations.format(type.key), mouse -> {
            if (mouse == 0) { session.filter = type; refresh(); }
        })));
        header = root.add(new UiPanel());
        String[] keys = {"litematica.gui.label.schematic_verifier.expected", "litematica.gui.label.schematic_verifier.found",
            "litematica.gui.label.schematic_verifier.count"};
        for (int i = 0; i < columns.length; i++) {
            final int column = i;
            columns[i] = header.add(new UiButton(() -> column == 1 && session.filter == Type.CORRECT ? "" : UiTranslations.format(keys[column]), mouse -> {
                if (mouse != 0) return;
                session.reverse = session.sortColumn == column && !session.reverse;
                session.sortColumn = column;
                refresh();
            }) {
                @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
                    draw.text(draw.trim("§l" + label(), Math.max(0, bounds().width - 18)), bounds().x, bounds().y + 7, 0xFFFFFFFF);
                    if (session.sortColumn == column) (session.reverse ? UiSprite.SORT_UP : UiSprite.SORT_DOWN)
                        .draw(draw, bounds().right() - 16, bounds().y + 3, true, false);
                    if (isFocused()) draw.border(bounds(), 0xFFE0E0E0);
                }
            });
        }
        list = root.add(new UiRowList<>(model, Entry::new));
        status = root.add(new UiLabel(() -> message.isEmpty() ? session.progressText() : message));
        counts = root.add(new UiLabel(session::countsText));
        menu = addButton("litematica.gui.button.change_menu.to_main_menu", this::mainMenu);
    }

    @Override protected void opened() { refresh(); }

    private void refresh() {
        VerificationScan scan = session.scan();
        if (previous != scan) visuals.clear();
        previous = scan;
        revision = scan == null ? -1 : scan.revision();
        List<Row> rows = new ArrayList<>();
        if (scan != null) for (Type type : Type.values()) {
            if (type == Type.ALL || (session.filter == Type.ALL ? type == Type.CORRECT : type != session.filter)) continue;
            rows.add(new Row(type, null));
            List<Group> groups = scan.groups(type);
            Comparator<Group> order = session.sortColumn == 0 ? Comparator.comparing(group -> visual(group.pair.expected).name)
                : session.sortColumn == 1 ? Comparator.comparing(group -> visual(group.pair.found).name)
                : Comparator.comparingInt(Group::count).reversed();
            order = order.thenComparing(group -> group.pair.expected.toString()).thenComparing(group -> group.pair.found.toString());
            if (session.reverse) order = order.reversed();
            groups.sort(order);
            for (Group group : groups) rows.add(new Row(type, group));
        }
        model.setEntries(rows);
        list.sync();
        updateButtons();
    }

    private void updateButtons() {
        VerificationScan scan = session.scan();
        start.setEnabled(session.available(mc) && (!session.running() || scan == null || scan.done()));
        stop.setEnabled(session.running() && scan != null && !scan.done());
        reset.setEnabled(scan != null || !session.notice().isEmpty() || !message.isEmpty());
        ignored.setEnabled(scan != null && !scan.ignored().isEmpty());
        columns[1].setEnabled(session.filter != Type.CORRECT);
        for (int i = 0; i < filters.size(); i++) filters.get(i).setEnabled(Type.values()[i] != session.filter);
        filters.get(Type.DIFF_BLOCK.ordinal()).setVisible(com.github.lunatrius.schematica.util.BlockGroups.enabled);
        status.setTooltip(message.isEmpty() ? session.progressText() : message, UiTranslations.format("schematica.ui.verifier.comparison"));
        counts.setTooltip(session.countsText());
    }

    @Override protected void tickScreen() {
        if (previous != session.scan() || (++refreshTicks % 5 == 0 && previous != null && previous.revision() != revision)) refresh();
        updateButtons();
        layoutWidgets();
    }

    private int buttonRow(List<UiButton> buttons, int y) {
        int x = 12;
        for (UiButton button : buttons) {
            if (!button.isVisible()) continue;
            int w = Math.min(Math.max(20, width - 24), fontRendererObj.getStringWidth(button.label()) + 10);
            if (x > 12 && x + w > width - 12) { x = 12; y += 22; }
            button.setBounds(x, y, w, 20);
            x += w + 4;
        }
        return y + 22;
    }

    @Override protected void layoutWidgets() {
        int top = buttonRow(filters, buttonRow(actions, 20)) - 2;
        int usable = Math.max(0, width - 34);
        int ignoreWidth = fontRendererObj.getStringWidth(UiTranslations.format("litematica.gui.button.schematic_verifier.ignore")) + 16;
        int content = Math.max(0, usable - ignoreWidth - 58);
        expectedWidth = Math.max(0, content / 2);
        foundWidth = Math.max(0, content - expectedWidth);
        header.setBounds(12, top, usable, 22);
        columns[0].setBounds(16, top, expectedWidth, 22);
        columns[1].setBounds(16 + expectedWidth, top, foundWidth, 22);
        columns[2].setBounds(16 + expectedWidth + foundWidth, top, 52, 22);
        list.setBounds(12, top + 22, Math.max(0, width - 22), Math.max(0, height - 36 - top - 22));
        int w = fontRendererObj.getStringWidth(menu.label()) + 20;
        menu.setBounds(width - w - 10, height - 36, w, 20);
        status.setBounds(12, height - 36, Math.max(0, width - w - 30), 12);
        counts.setBounds(12, height - 22, Math.max(0, width - w - 30), 12);
    }

    private Visual visual(State state) { return visuals.computeIfAbsent(state, Visual::new); }

    private static final class Row {
        final Type type;
        final Group group;
        Row(Type type, Group group) { this.type = type; this.group = group; }
        @Override public boolean equals(Object other) { return other instanceof Row && type == ((Row) other).type && group == ((Row) other).group; }
        @Override public int hashCode() { return 31 * type.hashCode() + System.identityHashCode(group); }
    }

    private final class Entry extends UiPanel {
        private final Row row;
        private final int index;
        private UiButton ignore;
        private final UiButton select;
        private VerifierBlockInfo info;
        Entry(Row row, int index) {
            this.row = row; this.index = index;
            select = add(new UiButton(() -> "", mouse -> {
                if (mouse == 0 && session.scan() == previous) {
                    if (row.group == null) session.markers.selection.toggle(row.type);
                    else session.markers.selection.toggle(row.group);
                    message = com.github.lunatrius.schematica.handler.VerifierOverlaySettings.enabled ? ""
                        : UiTranslations.format("litematica.message.warn.schematic_verifier.overlay_disabled",
                            VisualSettings.prettyName("info_overlays", "verifierOverlayEnabled"),
                            VisualSettings.prettyName("hotkeys", "toggleVerifierOverlayRendering"),
                            GuiModConfig.chordLabel(Hotkeys.get("toggleVerifierOverlayRendering")));
                }
            }) {
                @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
                    if (isFocused()) draw.border(bounds(), 0xFFE0E0E0);
                }
                @Override public boolean drawTooltip(UiDraw draw, int mouseX, int mouseY, UiBounds screen) {
                    if (row.group == null) return false;
                    if (info == null) info = new VerifierBlockInfo(row.group.pair);
                    int width = info.width(draw, Math.max(0, screen.width - 8));
                    int x = Math.max(4, Math.min(mouseX + 10, screen.right() - width - 4));
                    int y = Math.max(4, Math.min(mouseY + 10, screen.bottom() - VerifierBlockInfo.HEIGHT - 4));
                    info.draw(draw, x, y, width);
                    return true;
                }
            });
            select.setEnabled(row.type != Type.CORRECT);
            if (row.group != null) {
                setTooltip(row.type.color + UiTranslations.format(row.type.key),
                    UiTranslations.format("litematica.gui.label.schematic_verifier.expected") + ": " + row.group.pair.expected,
                    UiTranslations.format("litematica.gui.label.schematic_verifier.found") + ": " + row.group.pair.found);
                if (row.type != Type.CORRECT) ignore = add(new UiButton(() -> UiTranslations.format("litematica.gui.button.schematic_verifier.ignore"), mouse -> {
                    if (mouse == 0 && session.scan() == previous) { previous.ignore(row.group); refresh(); }
                }));
            }
        }
        @Override public void layout(UiBounds screen) {
            select.setBounds(bounds().x, bounds().y, bounds().width, bounds().height);
            if (ignore != null) {
                int w = fontRendererObj.getStringWidth(ignore.label()) + 10;
                ignore.setBounds(bounds().right() - w - 2, bounds().y + 1, w, 20);
            }
        }
        @Override public void draw(UiDraw draw, int mouseX, int mouseY) {
            boolean selected = row.group == null ? session.markers.selection.category(row.type) : session.markers.selection.entry(row.group);
            draw.fill(bounds(), selected ? 0xA0707070 : containsVisible(mouseX, mouseY) ? 0xA0505050 : index % 2 == 1 ? 0xA0101010 : 0xA0303030);
            if (selected) draw.border(bounds(), 0xFFE0E0E0);
            int x = bounds().x + 4, y = bounds().y;
            if (row.group == null) draw.text(row.type.color + UiTranslations.format(row.type.key), x, y + 7, 0xFFFFFFFF);
            else {
                visual(row.group.pair.expected).draw(draw, x, y, expectedWidth);
                if (row.type != Type.CORRECT) visual(row.group.pair.found).draw(draw, x + expectedWidth, y, foundWidth);
                draw.text(String.valueOf(row.group.count()), x + expectedWidth + foundWidth, y + 7, 0xFFFFFFFF);
            }
            super.draw(draw, mouseX, mouseY);
        }
    }

}
