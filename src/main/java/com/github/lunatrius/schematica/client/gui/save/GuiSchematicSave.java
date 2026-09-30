package com.github.lunatrius.schematica.client.gui.save;

import java.io.File;
import java.io.IOException;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.World;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.gui.browser.GuiSchematicDirectory;
import com.github.lunatrius.schematica.client.gui.framework.UiButton;
import com.github.lunatrius.schematica.client.gui.framework.UiIntegerField;
import com.github.lunatrius.schematica.client.gui.framework.UiLabel;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;
import com.github.lunatrius.schematica.client.gui.framework.UiTextField;
import com.github.lunatrius.schematica.client.gui.framework.UiToggleButton;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.QueueTickHandler;
import com.github.lunatrius.schematica.handler.client.WorldHandler;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.SchematicLimits;
import com.github.lunatrius.schematica.world.schematic.SchematicFormat;

public final class GuiSchematicSave extends UiScreen {

    private File directory = ConfigurationHandler.schematicDirectory;
    private boolean extended = ConfigurationHandler.useSchematicplusFormat;
    private UiButton destination;
    private final UiButton[] pointButtons = new UiButton[2];
    private final UiIntegerField[][] coordinates = new UiIntegerField[2][3];
    private final UiLabel[][] axes = new UiLabel[2][3];
    private UiToggleButton guide;
    private UiToggleButton nbt;
    private UiToggleButton entities;
    private UiLabel nameLabel;
    private UiTextField name;
    private UiButton format;
    private UiLabel feedback;
    private UiButton save;
    private UiButton back;
    private String status = "";
    private String problem = "";

    public GuiSchematicSave(GuiScreen parent) {
        super(parent, I18n.format("schematica.ui.save.title"));
    }

    @Override
    protected void createWidgets() {
        destination = root.add(new UiButton(() -> I18n.format("schematica.ui.save.destination", relativeDirectory()), button -> {
            if (button == 0) mc.displayGuiScreen(new GuiSchematicDirectory(this, directory, chosen -> {
                directory = chosen;
                edited();
            }));
        }));
        for (int point = 0; point < 2; point++) {
            final int index = point;
            String key = point == 0 ? "schematica.gui.point.red" : "schematica.gui.point.blue";
            pointButtons[point] = root.add(new UiButton(() -> I18n.format("schematica.ui.save.point_here", I18n.format(key)), button -> {
                if (button == 0 && mc.thePlayer != null) {
                    ClientProxy.movePointToPlayer(point(index));
                    syncCoordinates();
                    selectionChanged();
                }
            }));
            pointButtons[point].setTooltip(I18n.format("schematica.ui.save.point_here", I18n.format(key)));
            for (int axis = 0; axis < 3; axis++) {
                final int component = axis;
                axes[point][axis] = root.add(new UiLabel(() -> "XYZ".charAt(component) + ":"));
                coordinates[point][axis] = root.add(new UiIntegerField(fontRendererObj, 0,
                    Integer.MIN_VALUE, Integer.MAX_VALUE, value -> {
                        Vector3i target = point(index);
                        if (component == 0) target.x = value;
                        else if (component == 1) target.y = value;
                        else target.z = value;
                        selectionChanged();
                    }));
                coordinates[point][axis].setTooltip(I18n.format("schematica.ui.save.coordinate_hint"));
            }
        }
        guide = root.add(new UiToggleButton(() -> I18n.format("schematica.ui.save.guide"), () -> ClientProxy.isRenderingGuide,
            enabled -> {
                if (SchematicaPlus.proxy.isSaveEnabled) ClientProxy.isRenderingGuide = enabled;
                WorldHandler.INSTANCE.saveSession();
                edited();
            }));
        nbt = root.add(new UiToggleButton(() -> I18n.format("schematica.gui.savenbt"), () -> SchematicFormat.saveNBT,
            enabled -> { SchematicFormat.saveNBT = enabled; edited(); }));
        entities = root.add(new UiToggleButton(() -> I18n.format("schematica.gui.saveentities"), () -> SchematicFormat.saveEntities,
            enabled -> { SchematicFormat.saveEntities = enabled; edited(); }));
        guide.setTooltip(I18n.format("schematica.ui.save.guide_hint"));
        nbt.setTooltip(I18n.format("schematica.gui.savenbt"));
        entities.setTooltip(I18n.format("schematica.gui.saveentities"));
        nameLabel = root.add(new UiLabel(() -> I18n.format("schematica.gui.name")));
        name = root.add(new UiTextField(fontRendererObj, 210, text -> edited()));
        name.setTooltip(I18n.format("schematica.ui.save.name_hint"));
        format = root.add(new UiButton(() -> extended ? ".schemplus" : ".schematic", button -> {
            extended = !extended;
            edited();
        }));
        format.setTooltip(I18n.format("schematica.ui.save.format_hint"));
        feedback = root.add(new UiLabel(() -> status.isEmpty() ? problem.isEmpty() ? dimensions() : problem : status));
        save = addButton("schematica.gui.save", this::saveSelection);
        back = addButton("gui.back", this::closeScreen);
    }

    private Vector3i point(int index) {
        return index == 0 ? ClientProxy.pointA : ClientProxy.pointB;
    }

    private void syncCoordinates() {
        for (int index = 0; index < 2; index++) {
            Vector3i point = point(index).clone();
            coordinates[index][0].setValue(point.x);
            coordinates[index][1].setValue(point.y);
            coordinates[index][2].setValue(point.z);
        }
        ClientProxy.updatePoints();
    }

    private void selectionChanged() {
        ClientProxy.updatePoints();
        edited();
    }

    private void edited() {
        status = "";
        if (save != null) tickScreen();
    }

    @Override
    protected void opened() {
        String previousStatus = status;
        syncCoordinates();
        status = previousStatus;
        tickScreen();
    }

    private String relativeDirectory() {
        java.nio.file.Path base = ConfigurationHandler.schematicDirectory.toPath().toAbsolutePath().normalize();
        java.nio.file.Path current = directory.toPath().toAbsolutePath().normalize();
        if (!current.startsWith(base)) return directory.getPath();
        return "/" + base.relativize(current).toString().replace('\\', '/');
    }

    private String dimensions() {
        Vector3i min = ClientProxy.pointMin;
        Vector3i max = ClientProxy.pointMax;
        return I18n.format("schematica.ui.save.dimensions", (long) max.x - min.x + 1,
            (long) max.y - min.y + 1, (long) max.z - min.z + 1);
    }

    private String validateSelection() {
        if (mc.theWorld == null || mc.thePlayer == null || !SchematicaPlus.proxy.isSaveEnabled) {
            return I18n.format("schematica.ui.save.disabled");
        }
        if (!ClientProxy.isRenderingGuide) return I18n.format("schematica.ui.save.enable_guide");
        try {
            Vector3i min = ClientProxy.pointMin;
            Vector3i max = ClientProxy.pointMax;
            SchematicLimits.worldBounds(min.x, min.y, min.z, max.x, max.y, max.z);
        } catch (IllegalArgumentException e) {
            return I18n.format("schematica.ui.save.invalid_selection");
        }
        if (name.text().trim().isEmpty()) return I18n.format("schematica.ui.save.enter_name");
        try {
            SchematicSaveTarget.filename(name.text(), extended);
        } catch (IllegalArgumentException e) {
            return I18n.format("schematica.ui.save.invalid_name");
        }
        if (!directory.isDirectory()) return I18n.format("schematica.ui.save.invalid_directory");
        return "";
    }

    @Override
    protected void tickScreen() {
        problem = validateSelection();
        save.setEnabled(problem.isEmpty());
        save.setTooltip(problem.isEmpty() ? I18n.format("schematica.gui.save") : problem);
        boolean world = mc.theWorld != null && mc.thePlayer != null;
        guide.setEnabled(world && SchematicaPlus.proxy.isSaveEnabled);
        for (UiButton button : pointButtons) button.setEnabled(world);
        destination.setTooltip(directory.getAbsolutePath());
        String output;
        try {
            output = new File(directory, SchematicSaveTarget.filename(name.text(), extended)).getPath();
        } catch (IllegalArgumentException e) {
            output = I18n.format("schematica.ui.save.name_hint");
        }
        feedback.setTooltip(status.isEmpty() ? problem : status, dimensions(), output);
    }

    private void saveSelection() {
        tickScreen();
        if (!problem.isEmpty()) return;
        if (!QueueTickHandler.INSTANCE.canQueue(mc.thePlayer)) {
            status = I18n.format("schematica.ui.save.busy");
            return;
        }
        try {
            File file = SchematicSaveTarget.resolve(ConfigurationHandler.schematicDirectory, directory, name.text(), extended);
            World world = mc.theWorld;
            Vector3i from = ClientProxy.pointMin.clone();
            Vector3i to = ClientProxy.pointMax.clone();
            Runnable submit = () -> submit(file, world, from, to);
            if (file.exists()) {
                confirm(I18n.format("schematica.ui.save.overwrite_title"),
                    I18n.format("schematica.ui.save.overwrite", file.getName()), submit);
            } else submit.run();
        } catch (IOException | IllegalArgumentException e) {
            Reference.logger.error("Invalid schematic save target", e);
            status = I18n.format("schematica.ui.save.invalid_directory");
        }
    }

    private void submit(File file, World world, Vector3i from, Vector3i to) {
        if (mc.theWorld != world || mc.thePlayer == null || !SchematicaPlus.proxy.isSaveEnabled) {
            status = I18n.format("schematica.ui.save.disabled");
            return;
        }
        if (SchematicaPlus.proxy.saveSchematic(mc.thePlayer, file.getParentFile(), file.getName(), world, from, to)) {
            WorldHandler.INSTANCE.saveSession();
            status = I18n.format("schematica.ui.save.queued", file.getName());
        } else {
            status = I18n.format("schematica.ui.save.failed");
        }
        tickScreen();
    }

    @Override
    protected void layoutWidgets() {
        int available = width - 24;
        destination.setBounds(12, 28, available, 20);
        int column = (available - 12) / 2;
        for (int point = 0; point < 2; point++) {
            int x = 12 + point * (column + 12);
            pointButtons[point].setBounds(x, 52, column, 20);
            for (int axis = 0; axis < 3; axis++) {
                axes[point][axis].setBounds(x, 76 + axis * 24, 12, 20);
                coordinates[point][axis].setBounds(x + 16, 76 + axis * 24, column - 16, 20);
            }
        }
        UiToggleButton[] options = {guide, nbt, entities};
        int optionWidth = (available - 8) / 3;
        for (int i = 0; i < options.length; i++) options[i].setBounds(12 + i * (optionWidth + 4), height - 92, optionWidth, 20);
        nameLabel.setBounds(12, height - 68, 44, 20);
        name.setBounds(60, height - 68, available - 140, 20);
        format.setBounds(width - 100, height - 68, 88, 20);
        feedback.setBounds(12, height - 44, available, 12);
        save.setBounds(12, height - 28, (available - 4) / 2, 20);
        back.setBounds(16 + (available - 4) / 2, height - 28, (available - 4) / 2, 20);
    }
}
