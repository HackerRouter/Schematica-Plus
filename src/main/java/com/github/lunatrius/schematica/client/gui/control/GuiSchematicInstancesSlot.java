package com.github.lunatrius.schematica.client.gui.control;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.Tessellator;

import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.proxy.ClientProxy;

public class GuiSchematicInstancesSlot extends GuiSlot {

    private final Minecraft minecraft = Minecraft.getMinecraft();
    private final GuiSchematicInstances parent;

    protected int selectedIndex = -1;

    public GuiSchematicInstancesSlot(GuiSchematicInstances parent) {
        super(Minecraft.getMinecraft(), parent.width, parent.height, 16, parent.height - 68, 24);
        this.parent = parent;

        // Pre-select the active schematic
        for (int i = 0; i < ClientProxy.loadedSchematics.size(); i++) {
            if (ClientProxy.loadedSchematics.get(i) == ClientProxy.schematic) {
                this.selectedIndex = i;
                break;
            }
        }
    }

    @Override
    protected int getSize() {
        return ClientProxy.loadedSchematics.size();
    }

    @Override
    protected void elementClicked(int index, boolean doubleClick, int mouseX, int mouseY) {
        this.selectedIndex = index;
        parent.onSelectionChanged();
        if (doubleClick && index >= 0 && index < ClientProxy.loadedSchematics.size()) {
            // Double-click switches to that schematic
            parent.switchToSelected();
        }
    }

    @Override
    protected boolean isSelected(int index) {
        return index == this.selectedIndex;
    }

    @Override
    protected void drawBackground() {}

    @Override
    protected void drawContainerBackground(Tessellator tessellator) {}

    @Override
    protected void drawSlot(int index, int x, int y, int slotHeight, Tessellator tessellator, int mouseX, int mouseY) {
        if (index < 0 || index >= ClientProxy.loadedSchematics.size()) {
            return;
        }

        SchematicWorld sw = ClientProxy.loadedSchematics.get(index);
        boolean isActive = (sw == ClientProxy.schematic);
        boolean isVisible = sw.isRendering;

        // Build display string
        String name = sw.name;
        if (name != null) {
            name = name.replaceAll("(?i)\\.(schematic|litematic|schemplus)$", "");
        }

        String prefix = isActive ? "\u00a7a\u25b6 " : "  ";
        String visibility = isVisible ? "" : " \u00a77[H]";
        // Show status indicators for disabled features
        String entityStatus = sw.isRenderingEntities ? "" : " \u00a78[!E]";
        String nbtStatus = sw.isPastingBlockNBT ? "" : " \u00a78[!N]";
        String displayStr = prefix + name + visibility + entityStatus + nbtStatus;

        this.parent.drawString(this.minecraft.fontRenderer, displayStr, x + 2, y + 6, 0x00FFFFFF);
    }
}
