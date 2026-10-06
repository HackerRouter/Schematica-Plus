// SPDX-License-Identifier: LGPL-3.0-only
// Litematica placement configuration layout, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.placement;

import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.input.Keyboard;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.gui.control.GuiSchematicMaterials;
import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiCheckBox;
import com.github.lunatrius.schematica.client.gui.framework.UiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiIntegerField;
import com.github.lunatrius.schematica.client.gui.framework.UiLabel;
import com.github.lunatrius.schematica.client.gui.framework.UiListModel;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;
import com.github.lunatrius.schematica.client.gui.framework.UiRowList;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;
import com.github.lunatrius.schematica.client.gui.framework.UiSprite;
import com.github.lunatrius.schematica.client.gui.framework.UiTextField;
import com.github.lunatrius.schematica.client.printer.SchematicPrinter;
import com.github.lunatrius.schematica.client.renderer.RendererSchematicGlobal;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.client.world.PlacementSettings;
import com.github.lunatrius.schematica.client.world.SubRegionPlacements;
import com.github.lunatrius.schematica.handler.client.WorldHandler;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Constants;
import com.github.lunatrius.schematica.reference.Reference;

public final class GuiPlacementConfiguration extends UiScreen {
    private final SchematicWorld placement;
    private final World clientWorld;
    private final UiPanel controls = root.add(new UiPanel());
    private final UiIntegerField[] coordinates = new UiIntegerField[3];
    private final UiLabel[] axes = new UiLabel[3];
    private final UiButton[] nudges = new UiButton[3];
    private final UiCheckBox[] coordinateLocks = new UiCheckBox[3];
    private final UiListModel<SubRegionPlacements.Region> regions;
    private UiRowList<SubRegionPlacements.Region> list;
    private UiTextField name;
    private UiTextField search;
    private UiButton searchButton, rename, allOn, allOff, enabled, rendering, locked, box, entities;
    private UiButton move, alignToMap, autoAlign, rotation, mirror, reset, grid, materials, verifier, placements;
    private UiLabel count, originLabel, feedback;
    private PlacementTransform.Orientation orientation;
    private int[] origin = new int[3];
    private int lastX, lastY, lastZ, lastOperationCount = -1, lastRevision = -1;
    private boolean syncing, searching;
    private String status = "";

    public GuiPlacementConfiguration(GuiScreen parent, SchematicWorld placement) {
        super(parent, UiTranslations.format("litematica.gui.title.configure_schematic_placement"));
        this.placement = java.util.Objects.requireNonNull(placement);
        this.clientWorld = Minecraft.getMinecraft().theWorld;
        regions = new UiListModel<>(22, SubRegionPlacements.Region::name);
        regions.setEntries(placement.subregions().regions());
    }

    private boolean available() {
        return mc.thePlayer != null && mc.theWorld != null && mc.theWorld == clientWorld
            && ClientProxy.loadedSchematics.contains(placement);
    }

    private UiButton button(Supplier<String> label, IntConsumer action) {
        return controls.add(new UiButton(label, mouse -> { if (available()) action.accept(mouse); }));
    }

    private UiButton button(String key, Runnable action) {
        return button(() -> UiTranslations.format(key), mouse -> { if (mouse == 0) action.run(); });
    }

    private String value(boolean enabled) {
        return (enabled ? "\u00a7a" : "\u00a7c") + UiTranslations.format(enabled ? "options.on" : "options.off") + "\u00a7r";
    }

    private UiButton toggle(String key, BooleanSupplier state, Runnable action) {
        return button(() -> UiTranslations.format(key, value(state.getAsBoolean())), mouse -> {
            if (mouse == 0) { action.run(); WorldHandler.INSTANCE.saveSession(); }
        });
    }

    @Override
    protected void createWidgets() {
        name = controls.add(new UiTextField(fontRendererObj, 256, text -> {}));
        name.setText(placement.name);
        rename = button("litematica.gui.button.rename", () -> {
            String text = name.text().trim();
            if (text.isEmpty()) { message("schematica.ui.placement.empty_name"); return; }
            placement.name = text;
            name.setText(text);
            WorldHandler.INSTANCE.saveSession();
            status = "";
        });
        count = controls.add(new UiLabel(() -> UiTranslations.format("litematica.gui.label.schematic_placement.sub_regions", placement.subregions().regions().size()), 0xFFFFFFFF));
        allOn = button("litematica.gui.button.schematic_placement.toggle_all_on", () -> updateRegions(placement.subregions().enabled(true)));
        allOff = button("litematica.gui.button.schematic_placement.toggle_all_off", () -> updateRegions(placement.subregions().enabled(false)));
        enabled = toggle("litematica.gui.button.schematic_placements.placement_enabled", placement::isEnabled,
            () -> configure(placement.placementSettings().enabled(!placement.isEnabled())));
        rendering = button(() -> (placement.isRendering ? "\u00a7a" : "\u00a7c")
            + UiTranslations.format("litematica.gui.button.schematic_placement.abbr.rendering"), mouse -> {
                if (mouse == 0) { placement.toggleRendering(); WorldHandler.INSTANCE.saveSession(); }
            });
        locked = toggle("litematica.gui.button.schematic_placements.locked", () -> placement.placementSettings().locked,
            () -> configure(placement.placementSettings().locked(!placement.placementSettings().locked)));
        locked.setTooltip(UiTranslations.format("litematica.gui.button.schematic_placement.hover.lock"));
        box = button(() -> "", mouse -> {
            if (mouse == 0) { configure(placement.placementSettings().enclosingBox(!placement.placementSettings().enclosingBox)); WorldHandler.INSTANCE.saveSession(); }
        }).setSprite(UiSprite.ENCLOSING_BOX_DISABLED).setBackground(false);
        entities = toggle("litematica.gui.button.schematic_placement.ignore_entities", () -> !placement.isRenderingEntities,
            () -> placement.isRenderingEntities = !placement.isRenderingEntities);
        originLabel = controls.add(new UiLabel(() -> UiTranslations.format("litematica.gui.label.placement_settings.placement_origin"), 0xFFFFFFFF));
        for (int i = 0; i < 3; i++) {
            final int axis = i;
            axes[i] = controls.add(new UiLabel(() -> "XYZ".charAt(axis) + ":", 0xFFFFFFFF));
            coordinates[i] = controls.add(new UiIntegerField(fontRendererObj, 0,
                Constants.World.MINIMUM_COORD, Constants.World.MAXIMUM_COORD, value -> {
                    if (syncing || !available()) return;
                    int[] target = origin.clone();
                    target[axis] = value;
                    moveTo(target);
                }));
            nudges[i] = button(() -> "", mouse -> {
                int step = (isShiftKeyDown() ? 8 : 1) * (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU) ? 4 : 1);
                coordinates[axis].setValue((long) origin[axis] + (mouse == 1 ? -step : step));
            }).setSprite(UiSprite.PLUS_MINUS).setBackground(false);
            nudges[i].setTooltip(UiTranslations.format("litematica.gui.button.hover.plus_minus_tip"));
            coordinateLocks[i] = controls.add(new UiCheckBox(() -> "", () -> placement.placementSettings().coordinateLocked(axis), value -> {
                if (!available()) return;
                int mask = placement.placementSettings().coordinateLocks;
                configure(placement.placementSettings().coordinateLocks(value ? mask | 1 << axis : mask & ~(1 << axis)));
                WorldHandler.INSTANCE.saveSession();
            }));
            coordinateLocks[i].setTooltip(UiTranslations.format("litematica.hud.schematic_placement.hover_info.lock_coordinate"));
        }
        move = button("litematica.gui.button.move_to_player", () -> moveTo(new int[] {
            MathHelper.floor_double(mc.thePlayer.posX), MathHelper.floor_double(mc.thePlayer.boundingBox.minY),
            MathHelper.floor_double(mc.thePlayer.posZ)}));
        alignToMap = button(() -> "#", mouse -> {
            if (mouse == 0) moveTo(new int[] {PlacementTransform.mapAlignedStart(placement.position.x, placement.getWidth()) + offset()[0],
                origin[1], PlacementTransform.mapAlignedStart(placement.position.z, placement.getLength()) + offset()[2]});
        });
        autoAlign = button(() -> "A", mouse -> {
            if (mouse == 0 && available()) com.github.lunatrius.schematica.client.align.AutoAlignJob.start(placement);
        });
        autoAlign.setTooltip(UiTranslations.format("schematica.ui.placement.auto_align").split("\n"));
        rotation = button(() -> UiTranslations.format("litematica.gui.button.rotation_value", orientation == null ? UiTranslations.format("schematica.ui.placement.custom") : orientation.rotationName()),
            mouse -> transform(mouse == 1 ? "YYY" : "Y"));
        mirror = button(() -> UiTranslations.format("litematica.gui.button.mirror_value", orientation == null ? UiTranslations.format("schematica.ui.placement.custom") : orientation.mirrorName()),
            mouse -> { if (orientation != null) transform(orientation.cycleMirror(mouse == 1)); });
        reset = button("litematica.gui.button.schematic_placement.reset_sub_region_placements", () -> updateRegions(() -> placement.resetSubregions(null)));
        grid = button(() -> GuiPlacementGridSettings.toggleLabel(placement), mouse -> {
            if (mouse != 0) return;
            if (isShiftKeyDown()) {
                placement.grid.toggleEnabled();
                WorldHandler.INSTANCE.saveSession();
            } else mc.displayGuiScreen(new GuiPlacementGridSettings(this, placement));
        });
        grid.setTooltip(UiTranslations.format("schematica.ui.placement.grid_settings.hover"));
        materials = button("litematica.gui.button.material_list", () -> {
            com.github.lunatrius.schematica.client.gui.material.MaterialList list = com.github.lunatrius.schematica.client.gui.material.MaterialList.placement(placement);
            list.refresh();
            // Remember the last opened material list for the hotkey to (re-) open it
            com.github.lunatrius.schematica.client.gui.material.MaterialLists.setCurrent(list);
            mc.displayGuiScreen(new GuiSchematicMaterials(this, list));
        });
        verifier = button("litematica.gui.button.schematic_verifier", () -> mc.displayGuiScreen(
            new com.github.lunatrius.schematica.client.gui.GuiSchematicVerifier(this, placement)));
        placements = addButton("litematica.gui.button.change_menu.show_schematic_placements", this::closeScreen);
        list = controls.add(new UiRowList<>(regions, (entry, index) -> new RegionRow(entry.name(), index), 11));
        search = controls.add(new UiTextField(fontRendererObj, 256, text -> { regions.setQuery(text); list.sync(); }));
        searchButton = button(() -> "", mouse -> {
            if (mouse != 0) return;
            searching = !searching;
            search.setVisible(searching);
            if (searching) input.focus(search);
            else search.setText("");
        }).setSprite(UiSprite.SEARCH).setBackground(false);
        search.setVisible(false);
        feedback = root.add(new UiLabel(() -> status));
    }

    private void configure(PlacementSettings settings) {
        boolean participationChanged = settings.enabled != placement.isEnabled();
        placement.setPlacementSettings(settings);
        if (participationChanged) RendererSchematicGlobal.INSTANCE.refresh(placement);
        status = "";
        syncGeometry();
    }

    private void updateRegions(SubRegionPlacements next) { updateRegions(() -> placement.changeSubregions(next)); }

    private void updateRegions(Runnable action) {
        if (!available()) return;
        try {
            action.run();
            RendererSchematicGlobal.INSTANCE.createRendererSchematicChunks(placement);
            if (ClientProxy.schematic == placement) SchematicPrinter.INSTANCE.refresh();
            WorldHandler.INSTANCE.saveSession();
            status = "";
        } catch (RuntimeException e) {
            if (PlacementSettings.LOCKED_MESSAGE.equals(e.getMessage())) message(PlacementSettings.LOCKED_MESSAGE);
            else {
                Reference.logger.warn("Failed to update placement subregions", e);
                message("schematica.ui.placement.region_failed");
            }
        }
        syncGeometry();
    }

    private void message(String key) {
        status = UiTranslations.format(key);
        feedback.setTooltip(status);
    }

    private int[] offset() {
        return placement.getSchematic().getOrigin().coordinates();
    }

    private int[] minimum(int[] target, int[] offset) {
        int[] result = new int[3];
        for (int i = 0; i < 3; i++) {
            long value = (long) target[i] - offset[i];
            if (target[i] < Constants.World.MINIMUM_COORD || target[i] > Constants.World.MAXIMUM_COORD
                || value < Constants.World.MINIMUM_COORD || value > Constants.World.MAXIMUM_COORD) {
                throw new IllegalArgumentException("Placement outside coordinate limits");
            }
            result[i] = (int) value;
        }
        return result;
    }

    private void moveTo(int[] target) {
        if (!available()) return;
        try {
            com.github.lunatrius.schematica.api.SchematicOrigin adjusted = placement.placementSettings().constrainOrigin(
                placement.originPosition(), new com.github.lunatrius.schematica.api.SchematicOrigin(target[0], target[1], target[2]));
            minimum(adjusted.coordinates(), offset());
            placement.moveOriginTo(adjusted.x, adjusted.y, adjusted.z);
            RendererSchematicGlobal.INSTANCE.refresh(placement);
            WorldHandler.INSTANCE.saveSession();
            status = "";
        } catch (IllegalArgumentException e) {
            message("schematica.ui.placement.bounds");
        }
        syncGeometry();
    }

    private void transform(String steps) {
        if (!available() || orientation == null) return;
        if (placement.placementSettings().locked) { message(PlacementSettings.LOCKED_MESSAGE); return; }
        int[] anchor = origin.clone();
        try {
            minimum(anchor, PlacementTransform.transformOrigin(placement.getSchematic().getOrigin(),
                placement.getWidth(), placement.getHeight(), placement.getLength(), steps).coordinates());
        } catch (IllegalArgumentException | ArithmeticException e) {
            message("schematica.ui.placement.bounds");
            return;
        }
        try {
            for (int i = 0; i < steps.length(); i++) {
                char op = steps.charAt(i);
                if (op == 'Y') placement.rotate(ForgeDirection.UP);
                else placement.flip(op == 'x' ? ForgeDirection.EAST : ForgeDirection.SOUTH);
            }
            status = "";
        } catch (RuntimeException e) {
            Reference.logger.error("Failed to transform placement", e);
            message("schematica.ui.placement.transform_failed");
        } finally {
            RendererSchematicGlobal.INSTANCE.createRendererSchematicChunks(placement);
            if (ClientProxy.schematic == placement) SchematicPrinter.INSTANCE.refresh();
            WorldHandler.INSTANCE.saveSession();
            syncGeometry();
        }
    }

    private void syncGeometry() {
        int[] offset = offset();
        origin = new int[] {Math.addExact(placement.position.x, offset[0]), Math.addExact(placement.position.y, offset[1]),
            Math.addExact(placement.position.z, offset[2])};
        orientation = PlacementTransform.orientation(placement.transformOperations);
        lastX = placement.position.x; lastY = placement.position.y; lastZ = placement.position.z;
        lastOperationCount = placement.transformOperations.size();
        lastRevision = placement.placementRevision();
        regions.setEntries(placement.subregions().regions());
        list.sync();
        PlacementSettings settings = placement.placementSettings();
        reset.setEnabled(!settings.locked && placement.subregions().modified());
        reset.setTooltip(settings.locked ? UiTranslations.format(PlacementSettings.LOCKED_MESSAGE) : "");
        move.setEnabled(!settings.locked && settings.coordinateLocks != 7);
        move.setTooltip(settings.locked ? UiTranslations.format(PlacementSettings.LOCKED_MESSAGE) : "");
        alignToMap.setEnabled(!settings.locked && (settings.coordinateLocks & 5) != 5);
        autoAlign.setEnabled(!settings.locked);
        alignToMap.setTooltip(UiTranslations.format(settings.locked ? PlacementSettings.LOCKED_MESSAGE : "schematica.ui.placement.align_to_map").split("\n"));
        box.setSprite(settings.enclosingBox ? UiSprite.ENCLOSING_BOX_ENABLED : UiSprite.ENCLOSING_BOX_DISABLED);
        box.setTooltip(UiTranslations.format("litematica.gui.button.schematic_placement.hover.enclosing_box", value(settings.enclosingBox)));
        for (int i = 0; i < 3; i++) {
            boolean editable = !settings.locked && !settings.coordinateLocked(i);
            coordinates[i].setEnabled(editable);
            nudges[i].setEnabled(editable);
            coordinates[i].setTooltip(!editable ? UiTranslations.format(settings.locked ? PlacementSettings.LOCKED_MESSAGE
                : "litematica.hud.schematic_placement.hover_info.lock_coordinate") : "");
        }
        syncing = true;
        try {
            for (int i = 0; i < 3; i++) coordinates[i].setValue(origin[i]);
        } finally {
            syncing = false;
        }
        rotation.setEnabled(!settings.locked && orientation != null);
        mirror.setEnabled(!settings.locked && orientation != null);
        String key = settings.locked ? PlacementSettings.LOCKED_MESSAGE : orientation == null ? "schematica.ui.placement.custom_transform" : null;
        String[] tooltip = key == null ? new String[0] : new String[] {UiTranslations.format(key)};
        rotation.setTooltip(tooltip);
        mirror.setTooltip(tooltip);
    }

    @Override
    protected void opened() {
        syncGeometry();
        tickScreen();
    }

    /** The upstream rendering toggle hover, with the colored ON/OFF value. */
    static String[] renderingHover(boolean enabled) {
        String value = (enabled ? "\u00a7a" : "\u00a7c") + UiTranslations.format("litematica.message.value." + (enabled ? "on" : "off")) + "\u00a7r";
        return UiTranslations.format("litematica.gui.button.schematic_placement.hover.rendering", value).split("\n");
    }

    @Override
    protected void tickScreen() {
        controls.setEnabled(available());
        feedback.setVisible(!status.isEmpty());
        if (!available()) { message("schematica.ui.placement.unloaded"); return; }
        rendering.setTooltip(renderingHover(placement.isRendering));
        if (lastX != placement.position.x || lastY != placement.position.y || lastZ != placement.position.z
            || lastOperationCount != placement.transformOperations.size() || lastRevision != placement.placementRevision()) syncGeometry();
    }

    @Override
    protected void layoutWidgets() {
        controls.setBounds(0, 0, width, height);
        int nameWidth = Math.max(1, Math.min(300, width - 200));
        name.setBounds(12, 24, nameWidth, 16);
        rename.setBounds(16 + nameWidth, 22, textWidth(rename), 20);
        allOff.setBounds(width - 154 - textWidth(allOff), 44, textWidth(allOff), 20);
        allOn.setBounds(allOff.bounds().x - textWidth(allOn) - 2, 44, textWidth(allOn), 20);
        count.setBounds(14, 48, Math.max(0, allOn.bounds().x - 18), 20);
        int x = width - 130;
        enabled.setBounds(x, 22, 98, 20);
        rendering.setBounds(x + 100, 22, 20, 20);
        locked.setBounds(x, 43, 98, 20);
        box.setBounds(x + 100, 45, 16, 16);
        entities.setBounds(x, 64, 120, 20);
        originLabel.setBounds(x + 2, 85, 120, 20);
        for (int i = 0; i < 3; i++) {
            int y = 99 + i * 18;
            int offset = fontRendererObj.getStringWidth("XYZ".charAt(i) + ":") + 4;
            axes[i].setBounds(x + 2, y, 70, 20);
            coordinates[i].setBounds(x + 2 + offset, y + 2, 70, 14);
            nudges[i].setBounds(x + 87, y + 1, 16, 16);
            coordinateLocks[i].setBounds(x + 92 + offset, y + 3, 11, 11);
        }
        move.setBounds(x, 155, 76, 20);
        alignToMap.setBounds(x + 78, 155, 20, 20);
        autoAlign.setBounds(x + 100, 155, 20, 20);
        rotation.setBounds(x, 176, 120, 20);
        mirror.setBounds(x, 197, 120, 20);
        reset.setBounds(x, 218, 120, 20);
        grid.setBounds(x, 239, 120, 20);
        if (height < 349) {
            // Upstream moves these to the bottom left below 328; the grid button joins them, and the row stops left of
            // the column (upstream's runs under Reset), shrinking its buttons when needed
            UiButton[] row = height < 261 ? new UiButton[] {materials, verifier, placements, grid} : new UiButton[] {materials, verifier, placements};
            int total = 0;
            for (UiButton button : row) total += textWidth(button) + 1;
            int columnBottom = row.length == 4 ? reset.bounds().bottom() : grid.bounds().bottom();
            int available = (height - 22 < columnBottom ? x - 2 : width - 10) - 10;
            double scale = total > available ? Math.max(0, available - row.length) / (double) (total - row.length) : 1;
            int rowX = 10;
            for (UiButton button : row) {
                int w = Math.max(20, (int) (textWidth(button) * scale));
                button.setBounds(rowX, height - 22, w, 20);
                rowX += w + 1;
            }
        } else {
            materials.setBounds(x, 271, 120, 20);
            verifier.setBounds(x, 292, 120, 20);
            placements.setBounds(width - textWidth(placements) - 9, 324, textWidth(placements), 20);
        }
        searchButton.setBounds(12, 67, 12, 12);
        search.setBounds(28, 66, width - 192, 14);
        list.setBounds(12, 83, width - 153, Math.max(0, height - 109));
        feedback.setBounds(12, height - 42, Math.max(0, width - 170), 16);
    }

    private int textWidth(UiButton button) { return fontRendererObj.getStringWidth(button.label()) + 10; }

    private final class RegionRow extends UiPanel {
        private final String regionName;
        private final int index;
        private final UiButton configure;
        private final UiButton toggle;

        RegionRow(String regionName, int index) {
            this.regionName = regionName;
            this.index = index;
            configure = add(new UiButton(() -> UiTranslations.format("litematica.gui.button.schematic_placements.configure"), mouse -> {
                if (mouse == 0 && available()) mc.displayGuiScreen(new GuiSubRegionConfiguration(GuiPlacementConfiguration.this, placement, regionName));
            }));
            toggle = add(new UiButton(() -> UiTranslations.format("litematica.gui.button.schematic_placements.placement_enabled", value(region().enabled)), mouse -> {
                if (mouse == 0) updateRegions(placement.subregions().replace(region().enabled(!region().enabled)));
            }));
            setTooltip(UiTranslations.format("schematica.ui.placement.region_hint"));
        }

        private SubRegionPlacements.Region region() { return placement.subregions().get(regionName); }

        @Override
        public void layout(UiBounds screen) {
            toggle.setBounds(bounds().right() - textWidth(toggle) - 2, bounds().y + 1, textWidth(toggle), 20);
            configure.setBounds(toggle.bounds().x - textWidth(configure) - 2, bounds().y + 1, textWidth(configure), 20);
        }

        @Override
        public void draw(UiDraw draw, int mouseX, int mouseY) {
            boolean selected = regionName.equals(placement.subregions().selected);
            draw.fill(bounds(), selected || containsVisible(mouseX, mouseY) ? 0xA0707070 : index % 2 == 0 ? 0xA0303030 : 0xA0101010);
            if (selected) draw.border(bounds(), 0xFFE0E0E0);
            UiSprite.schematicFile(placement.sourceFilename).draw(draw, bounds().x + 2, bounds().y + 5, false, false);
            int reserve = region().modified() ? 15 : 0;
            draw.text(draw.trim((region().enabled ? "\u00a7a" : "\u00a7c") + regionName, configure.bounds().x - bounds().x - 24 - reserve),
                bounds().x + 20, bounds().y + 7, 0xFFFFFFFF);
            if (region().modified()) UiSprite.NOTICE.draw(draw, configure.bounds().x - 15, bounds().y + 6, false, false);
            super.draw(draw, mouseX, mouseY);
        }

        @Override
        public boolean mouseDown(int x, int y, int button) {
            if (button != 0 || !available()) return false;
            placement.selectSubregion(regionName.equals(placement.subregions().selected) ? null : regionName);
            WorldHandler.INSTANCE.saveSession();
            return true;
        }
    }
}
