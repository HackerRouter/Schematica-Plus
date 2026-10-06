// SPDX-License-Identifier: LGPL-3.0-only
// Litematica subregion configuration layout, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.placement;

import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;

import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.github.lunatrius.schematica.client.world.PlacementSettings;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiCheckBox;
import com.github.lunatrius.schematica.client.gui.framework.UiIntegerField;
import com.github.lunatrius.schematica.client.gui.framework.UiLabel;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;
import com.github.lunatrius.schematica.client.gui.framework.UiSprite;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.printer.SchematicPrinter;
import com.github.lunatrius.schematica.client.renderer.RendererSchematicGlobal;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.client.world.SubRegionPlacements.Region;
import com.github.lunatrius.schematica.handler.client.WorldHandler;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;

public final class GuiSubRegionConfiguration extends UiScreen {
    private final SchematicWorld placement;
    private final String regionName;
    private final World clientWorld;
    private final UiPanel controls = root.add(new UiPanel());
    private final UiIntegerField[] coordinates = new UiIntegerField[3];
    private final UiLabel[] axes = new UiLabel[3];
    private final UiButton[] nudges = new UiButton[3];
    private final UiCheckBox[] locks = new UiCheckBox[3];
    private UiButton enabled, rendering, entities, move, rotation, mirror, reset, slice, back, menu;
    private UiLabel name, position, feedback;
    private String status = "";
    private boolean syncing;
    private int revision = -1;
    private SchematicOrigin lastPosition;

    public GuiSubRegionConfiguration(GuiScreen parent, SchematicWorld placement, String regionName) {
        super(parent, UiTranslations.format("litematica.gui.title.configure_schematic_sub_region"));
        this.placement = placement;
        this.regionName = regionName;
        this.clientWorld = Minecraft.getMinecraft().theWorld;
    }

    private Region region() { return placement.subregions().get(regionName); }
    private boolean available() { return mc.thePlayer != null && mc.theWorld == clientWorld && ClientProxy.loadedSchematics.contains(placement); }
    private String value(boolean value) { return (value ? "\u00a7a" : "\u00a7c") + UiTranslations.format(value ? "options.on" : "options.off") + "\u00a7r"; }
    private UiButton button(Supplier<String> label, IntConsumer action) {
        return controls.add(new UiButton(label, mouse -> { if (available()) action.accept(mouse); }));
    }
    private UiButton button(String key, Runnable action) {
        return button(() -> UiTranslations.format(key), mouse -> { if (mouse == 0) action.run(); });
    }

    @Override protected void createWidgets() {
        name = controls.add(new UiLabel(() -> UiTranslations.format("litematica.gui.label.placement_sub.region_name", regionName), 0xFFFFFFFF));
        enabled = button(() -> UiTranslations.format("litematica.gui.button.schematic_placement.region_enabled", value(region().enabled)),
            mouse -> { if (mouse == 0) change(region -> region.enabled(!region.enabled)); });
        rendering = button(() -> (region().rendering ? "\u00a7a" : "\u00a7c") + UiTranslations.format("litematica.gui.button.schematic_placement.abbr.rendering"),
            mouse -> { if (mouse == 0) change(region -> region.rendering(!region.rendering)); });
        entities = button(() -> UiTranslations.format("litematica.gui.button.schematic_placement.ignore_entities", value(region().ignoreEntities)),
            mouse -> { if (mouse == 0) change(region -> region.ignoreEntities(!region.ignoreEntities)); });
        position = controls.add(new UiLabel(() -> UiTranslations.format("litematica.gui.label.placement_sub.region_position"), 0xFFFFFFFF));
        for (int i = 0; i < 3; i++) {
            final int axis = i;
            axes[i] = controls.add(new UiLabel(() -> "XYZ".charAt(axis) + ":", 0xFFFFFFFF));
            coordinates[i] = controls.add(new UiIntegerField(fontRendererObj, 0, -30000000, 29999999, value -> {
                if (syncing || !available()) return;
                int[] target = placement.subregionPosition(regionName).coordinates();
                target[axis] = value;
                moveTo(target);
            }));
            nudges[i] = button(() -> "", mouse -> {
                int step = (isShiftKeyDown() ? 8 : 1) * (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU) ? 4 : 1);
                coordinates[axis].setValue((long) placement.subregionPosition(regionName).coordinates()[axis] + (mouse == 1 ? -step : step));
            }).setSprite(UiSprite.PLUS_MINUS).setBackground(false);
            nudges[i].setTooltip(UiTranslations.format("litematica.gui.button.hover.plus_minus_tip"));
            locks[i] = controls.add(new UiCheckBox(() -> "", () -> (region().locks & 1 << axis) != 0,
                checked -> change(region -> region.locks(checked ? region.locks | 1 << axis : region.locks & ~(1 << axis)))));
            locks[i].setTooltip(UiTranslations.format("litematica.hud.schematic_placement.hover_info.lock_coordinate"));
        }
        move = button("litematica.gui.button.move_to_player", () -> moveTo(new int[] {MathHelper.floor_double(mc.thePlayer.posX),
            MathHelper.floor_double(mc.thePlayer.boundingBox.minY), MathHelper.floor_double(mc.thePlayer.posZ)}));
        rotation = button(() -> UiTranslations.format("litematica.gui.button.rotation_value", new String[] {"NONE", "CW_90", "CW_180", "CCW_90"}[region().rotation]),
            mouse -> change(region -> region.rotation(region.rotation + (mouse == 1 ? -1 : 1))));
        mirror = button(() -> UiTranslations.format("litematica.gui.button.mirror_value", new String[] {"NONE", "LEFT_RIGHT", "FRONT_BACK"}[region().mirror]),
            mouse -> change(region -> region.mirror(region.mirror + (mouse == 1 ? -1 : 1))));
        reset = button(() -> (region().modified() ? "\u00a76" : "") + UiTranslations.format("litematica.gui.button.placement_sub.reset_sub_region_placement"),
            mouse -> { if (mouse == 0) update(() -> placement.resetSubregions(regionName)); });
        slice = unavailable(button(() -> UiTranslations.format("litematica.gui.button.placement_sub.slice_type", "-"), mouse -> {}));
        back = addButton("litematica.gui.button.placement_sub.placement_configuration", this::closeScreen);
        menu = addButton("litematica.gui.button.change_menu.to_main_menu", this::mainMenu);
        feedback = root.add(new UiLabel(() -> status));
    }

    private void change(UnaryOperator<Region> change) {
        if (!available()) return;
        update(() -> placement.changeSubregions(placement.subregions().replace(change.apply(region()))));
    }

    private void moveTo(int[] target) {
        if (!available()) return;
        update(() -> placement.moveSubregionTo(regionName, target[0], target[1], target[2]));
    }

    private void update(Runnable action) {
        try {
            action.run();
            RendererSchematicGlobal.INSTANCE.createRendererSchematicChunks(placement);
            if (ClientProxy.schematic == placement) SchematicPrinter.INSTANCE.refresh();
            WorldHandler.INSTANCE.saveSession();
            status = "";
        } catch (RuntimeException e) {
            if (PlacementSettings.LOCKED_MESSAGE.equals(e.getMessage())) status = UiTranslations.format(PlacementSettings.LOCKED_MESSAGE);
            else {
                Reference.logger.warn("Failed to update subregion {}", regionName, e);
                status = UiTranslations.format("schematica.ui.placement.region_failed");
            }
        }
        sync();
    }

    private void sync() {
        lastPosition = placement.subregionPosition(regionName);
        revision = placement.placementRevision();
        syncing = true;
        try { for (int i = 0; i < 3; i++) coordinates[i].setValue(lastPosition.coordinates()[i]); }
        finally { syncing = false; }
        boolean editable = !placement.placementSettings().locked;
        for (int i = 0; i < 3; i++) {
            coordinates[i].setEnabled(editable);
            nudges[i].setEnabled(editable);
        }
        move.setEnabled(editable);
        rotation.setEnabled(editable);
        mirror.setEnabled(editable);
        reset.setEnabled(editable && region().modified());
        String hint = editable ? "" : UiTranslations.format(PlacementSettings.LOCKED_MESSAGE);
        for (UiButton button : new UiButton[] {move, rotation, mirror, reset}) button.setTooltip(hint);
        for (UiIntegerField coordinate : coordinates) coordinate.setTooltip(hint);
        feedback.setTooltip(status);
    }

    @Override protected int titleRightMargin() { return 145; }
    @Override protected void opened() { sync(); tickScreen(); }
    @Override protected void tickScreen() {
        controls.setEnabled(available());
        if (!available()) status = UiTranslations.format("schematica.ui.placement.unloaded");
        else rendering.setTooltip(GuiPlacementConfiguration.renderingHover(region().rendering));
        if (available() && (revision != placement.placementRevision() || !java.util.Arrays.equals(lastPosition.coordinates(), placement.subregionPosition(regionName).coordinates()))) sync();
        feedback.setVisible(!status.isEmpty());
    }

    @Override protected void layoutWidgets() {
        controls.setBounds(0, 0, width, height);
        int x = width - 130;
        name.setBounds(20, 26, Math.max(0, x - 30), 16);
        enabled.setBounds(x, 10, 98, 20);
        rendering.setBounds(x + 100, 10, 20, 20);
        entities.setBounds(x, 31, 120, 20);
        position.setBounds(x, 49, 120, 20);
        for (int i = 0; i < 3; i++) {
            int y = 63 + i * 18, offset = fontRendererObj.getStringWidth("XYZ".charAt(i) + ":") + 4;
            axes[i].setBounds(x + 2, y, 70, 20);
            coordinates[i].setBounds(x + 2 + offset, y + 2, 70, 14);
            nudges[i].setBounds(x + 87, y + 1, 16, 16);
            locks[i].setBounds(x + offset + 92, y + 3, 11, 11);
        }
        move.setBounds(x, 120, 120, 20);
        rotation.setBounds(x, 141, 120, 20);
        mirror.setBounds(x, 162, 120, 20);
        reset.setBounds(x, 183, 120, 20);
        slice.setBounds(x, 204, 120, 20);
        int backWidth = fontRendererObj.getStringWidth(back.label()) + 10, menuWidth = fontRendererObj.getStringWidth(menu.label()) + 20;
        back.setBounds(10, height - 36, backWidth, 20);
        // upstream puts the menu button next to the back button below 270 wide; also when it would cover the column
        boolean beside = width < 270 || height - 36 < slice.bounds().bottom();
        menu.setBounds(beside ? back.bounds().right() + 4 : width - menuWidth - 10, height - 36, menuWidth, 20);
        feedback.setBounds(12, height - 54, Math.max(0, x - 24), 16);
    }
}
