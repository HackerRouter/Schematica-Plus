// SPDX-License-Identifier: LGPL-3.0-only
// Litematica placement configuration layout, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.placement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private final UiListModel<SchematicWorld> regions;
    private UiRowList<SchematicWorld> list;
    private UiTextField name;
    private UiTextField search;
    private UiButton searchButton, rename, allOn, allOff, enabled, rendering, locked, box, entities;
    private UiButton move, rotation, mirror, reset, materials, verifier, placements;
    private UiLabel count, originLabel, feedback;
    private PlacementTransform.Orientation orientation;
    private int[] origin = new int[3];
    private int lastX, lastY, lastZ, lastOperationCount = -1;
    private boolean syncing, searching, selectedRegion;
    private String status = "";

    public GuiPlacementConfiguration(GuiScreen parent, SchematicWorld placement) {
        super(parent, UiTranslations.format("litematica.gui.title.configure_schematic_placement"));
        this.placement = java.util.Objects.requireNonNull(placement);
        this.clientWorld = Minecraft.getMinecraft().theWorld;
        regions = new UiListModel<>(22, entry -> regionName());
        regions.setEntries(Collections.singletonList(placement));
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
        count = controls.add(new UiLabel(() -> UiTranslations.format("litematica.gui.label.schematic_placement.sub_regions", 1), 0xFFFFFFFF));
        count.setTooltip(UiTranslations.format("schematica.ui.placement.merged_region"));
        allOn = unavailable(button("litematica.gui.button.schematic_placement.toggle_all_on", () -> {}));
        allOff = unavailable(button("litematica.gui.button.schematic_placement.toggle_all_off", () -> {}));
        enabled = toggle("litematica.gui.button.schematic_placements.placement_enabled", () -> placement.isRendering,
            () -> placement.isRendering = !placement.isRendering);
        enabled.setTooltip(UiTranslations.format("schematica.ui.placement.visibility"));
        rendering = unavailable(button(() -> (placement.isRendering ? "\u00a7a" : "\u00a7c")
            + UiTranslations.format("litematica.gui.button.schematic_placement.abbr.rendering"), mouse -> {}));
        rendering.setTooltip(UiTranslations.format("schematica.ui.placement.visibility"));
        locked = unavailable(toggle("litematica.gui.button.schematic_placements.locked", () -> false, () -> {}));
        box = unavailable(button(() -> "", mouse -> {}).setSprite(UiSprite.ENCLOSING_BOX_DISABLED).setBackground(false));
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
            coordinateLocks[i] = controls.add(new UiCheckBox(() -> "", () -> false, value -> {}));
            unavailable(coordinateLocks[i]);
        }
        move = button("litematica.gui.button.move_to_player", () -> moveTo(new int[] {
            MathHelper.floor_double(mc.thePlayer.posX), MathHelper.floor_double(mc.thePlayer.boundingBox.minY),
            MathHelper.floor_double(mc.thePlayer.posZ)}));
        rotation = button(() -> UiTranslations.format("litematica.gui.button.rotation_value", orientation == null ? "CUSTOM" : orientation.rotationName()),
            mouse -> transform(mouse == 1 ? "YYY" : "Y"));
        mirror = button(() -> UiTranslations.format("litematica.gui.button.mirror_value", orientation == null ? "CUSTOM" : orientation.mirrorName()),
            mouse -> { if (orientation != null) transform(orientation.cycleMirror(mouse == 1)); });
        reset = unavailable(button("litematica.gui.button.schematic_placement.reset_sub_region_placements", () -> {}));
        materials = button("litematica.gui.button.material_list", () -> mc.displayGuiScreen(new GuiSchematicMaterials(this, placement)));
        verifier = unavailable(button("litematica.gui.button.schematic_verifier", () -> {}));
        placements = addButton("litematica.gui.button.change_menu.show_schematic_placements", this::closeScreen);
        list = controls.add(new UiRowList<>(regions, (entry, index) -> new RegionRow(), 11));
        search = controls.add(new UiTextField(fontRendererObj, 256, text -> { regions.setQuery(text); list.sync(); }));
        searchButton = button(() -> "", mouse -> {
            if (mouse != 0) return;
            searching = !searching;
            search.setVisible(searching);
            if (searching) input.focus(search);
            else search.setText("");
        }).setSprite(UiSprite.SEARCH).setBackground(false);
        searchButton.setTooltip(UiTranslations.format("schematica.ui.browser.search_hint"));
        search.setVisible(false);
        feedback = root.add(new UiLabel(() -> status));
    }

    private String regionName() {
        return placement.sourceFilename == null ? placement.name
            : placement.sourceFilename.replaceFirst("(?i)\\.(schematic|schemplus|litematic)$", "");
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
            int[] pos = minimum(target, offset());
            placement.position.set(pos[0], pos[1], pos[2]);
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
            int[] delta = offset();
            placement.position.set(anchor[0] - delta[0], anchor[1] - delta[1], anchor[2] - delta[2]);
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
        syncing = true;
        try {
            for (int i = 0; i < 3; i++) coordinates[i].setValue(origin[i]);
        } finally {
            syncing = false;
        }
        rotation.setEnabled(orientation != null);
        mirror.setEnabled(orientation != null);
        String tooltip = UiTranslations.format(orientation == null ? "schematica.ui.placement.custom_transform" : "schematica.ui.placement.transform_hint");
        rotation.setTooltip(tooltip);
        mirror.setTooltip(tooltip);
    }

    @Override
    protected void opened() {
        syncGeometry();
        tickScreen();
    }

    @Override
    protected void tickScreen() {
        controls.setEnabled(available());
        feedback.setVisible(!status.isEmpty());
        if (!available()) { message("schematica.ui.placement.unloaded"); return; }
        if (lastX != placement.position.x || lastY != placement.position.y || lastZ != placement.position.z
            || lastOperationCount != placement.transformOperations.size()) syncGeometry();
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
        move.setBounds(x, 155, 120, 20);
        rotation.setBounds(x, 176, 120, 20);
        mirror.setBounds(x, 197, 120, 20);
        reset.setBounds(x, 218, 120, 20);
        if (height < 328) {
            materials.setBounds(10, height - 22, textWidth(materials), 20);
            verifier.setBounds(materials.bounds().right() + 1, height - 22, textWidth(verifier), 20);
            placements.setBounds(verifier.bounds().right() + 1, height - 22, textWidth(placements), 20);
        } else {
            materials.setBounds(x, 250, 120, 20);
            verifier.setBounds(x, 271, 120, 20);
            placements.setBounds(width - textWidth(placements) - 9, 303, textWidth(placements), 20);
        }
        searchButton.setBounds(12, 67, 12, 12);
        search.setBounds(28, 66, width - 192, 14);
        list.setBounds(12, 83, width - 153, Math.max(0, height - 109));
        feedback.setBounds(12, height - 42, Math.max(0, width - 170), 16);
    }

    private int textWidth(UiButton button) { return fontRendererObj.getStringWidth(button.label()) + 10; }

    private final class RegionRow extends UiPanel {
        private final UiButton configure;
        private final UiButton toggle;

        RegionRow() {
            configure = add(new UiButton(() -> UiTranslations.format("litematica.gui.button.schematic_placements.configure"), mouse -> {}));
            toggle = add(new UiButton(() -> UiTranslations.format("litematica.gui.button.schematic_placements.placement_enabled", value(placement.isRendering)), mouse -> {}));
            unavailable(configure);
            unavailable(toggle);
            configure.setTooltip(UiTranslations.format("schematica.ui.placement.merged_region"));
            toggle.setTooltip(UiTranslations.format("schematica.ui.placement.merged_region"));
            setTooltip(UiTranslations.format("schematica.ui.placement.merged_region"), regionName());
        }

        @Override
        public void layout(UiBounds screen) {
            toggle.setBounds(bounds().right() - textWidth(toggle) - 2, bounds().y + 1, textWidth(toggle), 20);
            configure.setBounds(toggle.bounds().x - textWidth(configure) - 2, bounds().y + 1, textWidth(configure), 20);
        }

        @Override
        public void draw(UiDraw draw, int mouseX, int mouseY) {
            draw.fill(bounds(), selectedRegion || containsVisible(mouseX, mouseY) ? 0xA0707070 : 0xA0303030);
            if (selectedRegion) draw.border(bounds(), 0xFFE0E0E0);
            (placement.sourceFilename == null ? UiSprite.MEMORY : UiSprite.FILE).draw(draw, bounds().x + 2, bounds().y + 5, false, false);
            draw.text(draw.trim((placement.isRendering ? "\u00a7a" : "\u00a7c") + regionName(), configure.bounds().x - bounds().x - 24),
                bounds().x + 20, bounds().y + 7, 0xFFFFFFFF);
            super.draw(draw, mouseX, mouseY);
        }

        @Override
        public boolean mouseDown(int x, int y, int button) {
            if (button != 0) return false;
            selectedRegion = !selectedRegion;
            return true;
        }
    }
}
