package com.github.lunatrius.schematica.client.gui.save;

import java.util.function.Consumer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiCheckBox;
import com.github.lunatrius.schematica.client.gui.framework.UiIntegerField;
import com.github.lunatrius.schematica.client.gui.framework.UiLabel;
import com.github.lunatrius.schematica.client.gui.framework.UiPanel;
import com.github.lunatrius.schematica.client.gui.framework.UiSprite;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Area;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Box;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Corner;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;

final class AreaCornerControls extends UiPanel {
    private final Box box;
    private final Corner corner;
    private final UiCheckBox selected;
    private final UiIntegerField[] coordinates = new UiIntegerField[3];
    private final UiLabel[] axes = new UiLabel[3];
    private final UiButton[] nudges = new UiButton[3];
    private final UiButton move;
    private boolean syncing;

    AreaCornerControls(FontRenderer font, AreaSelectionLibrary library, Area area, Box box, Corner corner, Consumer<Runnable> change) {
        this.box = box; this.corner = corner;
        selected = add(new UiCheckBox(() -> UiTranslations.format("litematica.gui.label.area_editor.corner_" + (corner == Corner.FIRST ? 1 : 2)),
            () -> area.selectedBox() == box && !area.originSelected() && area.selectedCorner() == corner,
            value -> change.accept(() -> library.selectCorner(area, box, value ? corner : Corner.NONE))));
        for (int axis = 0; axis < 3; axis++) {
            final int component = axis;
            axes[axis] = add(new UiLabel(() -> "XYZ".charAt(component) + ":"));
            UiIntegerField field = add(new UiIntegerField(font, 0, axis == 1 ? 0 : -30000000, axis == 1 ? 255 : 29999999, value -> {
                if (syncing) return;
                change.accept(() -> {
                    Vector3i point = point();
                    if (component == 0) point.x = value;
                    else if (component == 1) point.y = value;
                    else point.z = value;
                    library.setPoints(area, box, corner == Corner.FIRST ? point : box.first(), corner == Corner.SECOND ? point : box.second());
                });
            }));
            coordinates[axis] = field;
            nudges[axis] = add(new UiButton(() -> "", button -> field.setValue((long) field.value() + AreaCornerControls.coordinateStep(button)))
                .setSprite(UiSprite.PLUS_MINUS).setBackground(false));
            nudges[axis].setTooltip(AreaCornerControls.plusMinusTip());
        }
        move = add(new UiButton(() -> UiTranslations.format("litematica.gui.button.move_to_player"), button -> {
            if (button == 0) {
                change.accept(() -> {
                    Vector3i point = GuiAreaSelectionManager.playerPoint();
                    library.setPoints(area, box, corner == Corner.FIRST ? point : box.first(), corner == Corner.SECOND ? point : box.second());
                });
                sync();
            }
        }));
    }

    /** MaLiLib's coordinate plus/minus step: right click decreases, Shift multiplies by 8 and Alt by 4. */
    static int coordinateStep(int button) {
        int amount = button == 1 ? -1 : 1;
        if (GuiScreen.isShiftKeyDown()) amount *= 8;
        if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)) amount *= 4;
        return amount;
    }

    static String[] plusMinusTip() { return UiTranslations.format("malilib.gui.button.hover.plus_minus_tip").split("\n"); }

    private Vector3i point() { return corner == Corner.FIRST ? box.first() : box.second(); }

    void sync() {
        syncing = true;
        try {
            Vector3i point = point();
            coordinates[0].setValue(point.x); coordinates[1].setValue(point.y); coordinates[2].setValue(point.z);
        } finally { syncing = false; }
    }

    @Override public void layout(UiBounds screen) {
        int x = bounds().x, y = bounds().y;
        selected.setBounds(x, y + 3, 100, 11);
        for (int axis = 0; axis < 3; axis++) {
            axes[axis].setBounds(x, y + 14 + axis * 20, 12, 20);
            coordinates[axis].setBounds(x + 12, y + 16 + axis * 20, 68, 16);
            nudges[axis].setBounds(x + 84, y + 16 + axis * 20, 16, 16);
        }
        move.setBounds(x + 10, y + 76, 100, 20);
    }
}
