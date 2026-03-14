package com.github.lunatrius.schematica.client.gui.control;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import com.github.lunatrius.core.client.gui.GuiScreenBase;
import com.github.lunatrius.schematica.Schematica;
import com.github.lunatrius.schematica.client.renderer.RendererSchematicGlobal;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Names;

import cpw.mods.fml.client.config.GuiCheckBox;

public class GuiSchematicInstances extends GuiScreenBase {

    private GuiSchematicInstancesSlot slotList;
    private GuiButton btnSwitch;
    private GuiCheckBox cbToggleVisible;
    private GuiCheckBox cbToggleEntities;
    private GuiCheckBox cbToggleBlockNBT;
    private GuiButton btnRemove;
    private GuiButton btnDone;

    private final String strTitle = I18n.format(Names.Gui.Instances.TITLE);

    public GuiSchematicInstances(GuiScreen parent) {
        super(parent);
    }

    @Override
    public void initGui() {
        int id = 0;
        int btnWidth = 60;
        int gap = 2;
        int btnY = this.height - 28;

        // Bottom row: Switch, Remove, Done buttons
        int btnCount = 3;
        int totalBtnWidth = btnWidth * btnCount + gap * (btnCount - 1);
        int btnStartX = (this.width - totalBtnWidth) / 2;

        this.btnSwitch = new GuiButton(id++, btnStartX, btnY, btnWidth, 20, I18n.format(Names.Gui.Instances.SWITCH));
        this.buttonList.add(this.btnSwitch);

        this.btnRemove = new GuiButton(id++, btnStartX + (btnWidth + gap), btnY, btnWidth, 20, I18n.format(Names.Gui.Control.UNLOAD));
        this.buttonList.add(this.btnRemove);

        this.btnDone = new GuiButton(id++, btnStartX + (btnWidth + gap) * 2, btnY, btnWidth, 20, I18n.format(Names.Gui.DONE));
        this.buttonList.add(this.btnDone);

        // Checkboxes row above buttons
        int cbY = this.height - 50;
        int cbGap = 10;
        String visibleLabel = I18n.format(Names.Gui.Instances.TOGGLE_VISIBLE);
        String entitiesLabel = I18n.format(Names.Gui.Instances.TOGGLE_ENTITIES);
        String blockNBTLabel = I18n.format(Names.Gui.Instances.TOGGLE_BLOCK_NBT);

        // Estimate checkbox widths (11px box + 2px gap + text width)
        int cbBoxSize = 11;
        int cbTextGap = 2;
        int visibleW = cbBoxSize + cbTextGap + this.fontRendererObj.getStringWidth(visibleLabel);
        int entitiesW = cbBoxSize + cbTextGap + this.fontRendererObj.getStringWidth(entitiesLabel);
        int blockNBTW = cbBoxSize + cbTextGap + this.fontRendererObj.getStringWidth(blockNBTLabel);
        int totalCbWidth = visibleW + entitiesW + blockNBTW + cbGap * 2;
        int cbStartX = (this.width - totalCbWidth) / 2;

        this.cbToggleVisible = new GuiCheckBox(id++, cbStartX, cbY, visibleLabel, true);
        this.buttonList.add(this.cbToggleVisible);

        this.cbToggleEntities = new GuiCheckBox(id++, cbStartX + visibleW + cbGap, cbY, entitiesLabel, true);
        this.buttonList.add(this.cbToggleEntities);

        this.cbToggleBlockNBT = new GuiCheckBox(id++, cbStartX + visibleW + entitiesW + cbGap * 2, cbY, blockNBTLabel, true);
        this.buttonList.add(this.cbToggleBlockNBT);

        this.slotList = new GuiSchematicInstancesSlot(this);

        updateButtonStates();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (!button.enabled) return;

        if (button.id == this.btnSwitch.id) {
            switchToSelected();
        } else if (button.id == this.cbToggleVisible.id) {
            toggleVisibility();
        } else if (button.id == this.cbToggleEntities.id) {
            toggleEntities();
        } else if (button.id == this.cbToggleBlockNBT.id) {
            toggleBlockNBT();
        } else if (button.id == this.btnRemove.id) {
            removeSelected();
        } else if (button.id == this.btnDone.id) {
            this.mc.displayGuiScreen(this.parentScreen);
        }
    }

    /** Called by the slot when selection changes. */
    public void onSelectionChanged() {
        updateButtonStates();
    }

    /** Called by the slot on double-click. */
    public void switchToSelected() {
        int idx = this.slotList.selectedIndex;
        if (idx < 0 || idx >= ClientProxy.loadedSchematics.size()) return;

        SchematicWorld sw = ClientProxy.loadedSchematics.get(idx);
        ClientProxy.selectSchematic(sw);
        updateButtonStates();
    }

    private void toggleVisibility() {
        int idx = this.slotList.selectedIndex;
        if (idx < 0 || idx >= ClientProxy.loadedSchematics.size()) return;

        SchematicWorld sw = ClientProxy.loadedSchematics.get(idx);
        sw.isRendering = this.cbToggleVisible.isChecked();
        RendererSchematicGlobal.INSTANCE.refresh();
    }

    private void toggleEntities() {
        int idx = this.slotList.selectedIndex;
        if (idx < 0 || idx >= ClientProxy.loadedSchematics.size()) return;

        SchematicWorld sw = ClientProxy.loadedSchematics.get(idx);
        sw.isRenderingEntities = this.cbToggleEntities.isChecked();
    }

    private void toggleBlockNBT() {
        int idx = this.slotList.selectedIndex;
        if (idx < 0 || idx >= ClientProxy.loadedSchematics.size()) return;

        SchematicWorld sw = ClientProxy.loadedSchematics.get(idx);
        sw.isPastingBlockNBT = this.cbToggleBlockNBT.isChecked();
    }

    private void removeSelected() {
        int idx = this.slotList.selectedIndex;
        if (idx < 0 || idx >= ClientProxy.loadedSchematics.size()) return;

        SchematicWorld sw = ClientProxy.loadedSchematics.get(idx);
        boolean wasActive = (sw == ClientProxy.schematic);

        ClientProxy.loadedSchematics.remove(idx);
        RendererSchematicGlobal.INSTANCE.removeRendererSchematicChunks(sw);

        if (wasActive) {
            if (!ClientProxy.loadedSchematics.isEmpty()) {
                int newIdx = Math.min(idx, ClientProxy.loadedSchematics.size() - 1);
                ClientProxy.selectSchematic(ClientProxy.loadedSchematics.get(newIdx));
            } else {
                Schematica.proxy.unloadSchematic();
            }
        }

        // Adjust selection
        if (this.slotList.selectedIndex >= ClientProxy.loadedSchematics.size()) {
            this.slotList.selectedIndex = ClientProxy.loadedSchematics.size() - 1;
        }
        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean hasSelection = this.slotList != null
            && this.slotList.selectedIndex >= 0
            && this.slotList.selectedIndex < ClientProxy.loadedSchematics.size();

        this.btnSwitch.enabled = hasSelection;
        this.cbToggleVisible.enabled = hasSelection;
        this.cbToggleEntities.enabled = hasSelection;
        this.cbToggleBlockNBT.enabled = hasSelection;
        this.btnRemove.enabled = hasSelection;

        if (hasSelection) {
            SchematicWorld sw = ClientProxy.loadedSchematics.get(this.slotList.selectedIndex);
            this.cbToggleVisible.setIsChecked(sw.isRendering);
            this.cbToggleEntities.setIsChecked(sw.isRenderingEntities);
            this.cbToggleBlockNBT.setIsChecked(sw.isPastingBlockNBT);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.slotList.drawScreen(mouseX, mouseY, partialTicks);

        drawCenteredString(this.fontRendererObj, this.strTitle, this.width / 2, 4, 0x00FFFFFF);

        // Show count
        String countStr = ClientProxy.loadedSchematics.size() + " " + I18n.format(Names.Gui.Instances.LOADED);
        drawCenteredString(this.fontRendererObj, countStr, this.width / 2, this.height - 62, 0x00808080);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
