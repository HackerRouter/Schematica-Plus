package com.github.lunatrius.schematica.handler.client;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.ForgeHooks;

import org.lwjgl.input.Keyboard;

import com.github.lunatrius.schematica.client.world.RenderLayerSettings;
import com.github.lunatrius.schematica.client.world.RenderLayerRange;
import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.gui.UiDemoScreen;
import com.github.lunatrius.schematica.client.gui.GuiSchematicMainMenu;
import com.github.lunatrius.schematica.client.gui.load.GuiSchematicLoad;
import com.github.lunatrius.schematica.client.gui.control.GuiSchematicControl;
import com.github.lunatrius.schematica.client.gui.save.GuiSchematicSave;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.tool.ToolHandler;
import com.github.lunatrius.schematica.tool.ToolManager;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;

public class InputHandler {

    public static final InputHandler INSTANCE = new InputHandler();

    private static final KeyBinding KEY_BINDING_SAVE = new KeyBinding(
        Names.Keys.SAVE,
        Keyboard.KEY_N,
        Names.Keys.CATEGORY);
    private static final KeyBinding KEY_BINDING_CONTROL = new KeyBinding(
        Names.Keys.CONTROL,
        Keyboard.KEY_M,
        Names.Keys.CATEGORY);
    private static final KeyBinding KEY_BINDING_LAYER_INC = new KeyBinding(
        Names.Keys.LAYER_INC,
        Keyboard.KEY_NONE,
        Names.Keys.CATEGORY);
    private static final KeyBinding KEY_BINDING_LAYER_DEC = new KeyBinding(
        Names.Keys.LAYER_DEC,
        Keyboard.KEY_NONE,
        Names.Keys.CATEGORY);
    private static final KeyBinding KEY_BINDING_EXECUTE = new KeyBinding(
        Names.Keys.EXECUTE,
        Keyboard.KEY_RETURN,
        Names.Keys.CATEGORY);
    private static final KeyBinding KEY_BINDING_UI_DEMO = new KeyBinding(
        Names.Keys.UI_DEMO,
        Keyboard.KEY_NONE,
        Names.Keys.CATEGORY);
    private static final KeyBinding KEY_BINDING_LOAD = new KeyBinding(Names.Keys.LOAD, Keyboard.KEY_NONE, Names.Keys.CATEGORY);
    private static final KeyBinding KEY_BINDING_MANIPULATE = new KeyBinding(Names.Keys.MANIPULATE, Keyboard.KEY_NONE, Names.Keys.CATEGORY);
    public static final KeyBinding RENDER_INFO_OVERLAY = new KeyBinding("litematica.config.hotkeys.name.renderInfoOverlay", Keyboard.KEY_I, Names.Keys.CATEGORY);

    public static final KeyBinding[] KEY_BINDINGS = new KeyBinding[] { KEY_BINDING_SAVE,
        KEY_BINDING_CONTROL, KEY_BINDING_LAYER_INC, KEY_BINDING_LAYER_DEC,
        KEY_BINDING_EXECUTE, KEY_BINDING_UI_DEMO, KEY_BINDING_LOAD, KEY_BINDING_MANIPULATE, RENDER_INFO_OVERLAY };

    private final Minecraft minecraft = Minecraft.getMinecraft();

    private InputHandler() {}

    @SubscribeEvent
    public void onKeyInput(InputEvent event) {
        if (this.minecraft.currentScreen == null) {
            if (KEY_BINDING_UI_DEMO.isPressed()) {
                this.minecraft.displayGuiScreen(new UiDemoScreen(null));
                return;
            }
            if (KEY_BINDING_SAVE.isPressed()) {
                this.minecraft.displayGuiScreen(new GuiSchematicSave(this.minecraft.currentScreen));
                return;
            }

            if (KEY_BINDING_CONTROL.isPressed()) {
                this.minecraft.displayGuiScreen(new GuiSchematicMainMenu(null));
                return;
            }
            if (KEY_BINDING_LOAD.isPressed()) {
                this.minecraft.displayGuiScreen(new GuiSchematicLoad(null));
                return;
            }
            if (KEY_BINDING_MANIPULATE.isPressed()) {
                this.minecraft.displayGuiScreen(new GuiSchematicControl(this.minecraft.currentScreen));
                return;
            }

            if (KEY_BINDING_LAYER_INC.isPressed()) moveLayer(1);
            if (KEY_BINDING_LAYER_DEC.isPressed()) moveLayer(-1);

            if (KEY_BINDING_EXECUTE.isPressed()) {
                if (ToolManager.isHoldingToolItem()) {
                    ToolHandler.onExecute(this.minecraft.thePlayer);
                }
            }

            handlePickBlock();
        }
    }

    private void moveLayer(int amount) {
        RenderLayerRange range = RenderLayerSettings.RANGE;
        if (range.mode() != RenderLayerRange.Mode.ALL) {
            net.minecraft.entity.Entity camera = minecraft.renderViewEntity;
            if (camera != null) {
                double coordinate = range.axis() == RenderLayerRange.Axis.X
                    ? camera.posX : range.axis() == RenderLayerRange.Axis.Y
                    ? camera.posY - camera.yOffset : camera.posZ;
                range.move(amount, coordinate);
            }
        } else {
            SchematicWorld schematic = ClientProxy.schematic;
            if (schematic != null && schematic.isRenderingLayer) {
                schematic.renderingLayer = MathHelper.clamp_int(schematic.renderingLayer + amount, 0, schematic.getHeight() - 1);
            }
        }
    }

    private void handlePickBlock() {
        try {
            PickBlockInput.dispatch(this.minecraft.gameSettings.keyBindPickBlock, () -> {
                SchematicWorld schematic = ClientProxy.schematic;
                return schematic != null && schematic.isRenderingEnabled() && this.minecraft.thePlayer != null
                    && pickBlock(schematic, RenderTickHandler.INSTANCE.rayTrace(schematic, 1));
            });
        } catch (Exception error) {
            Reference.logger.error("Could not pick block!", error);
        }
    }

    private boolean pickBlock(final SchematicWorld schematic, final MovingObjectPosition hit) {
        net.minecraft.entity.EntityLivingBase camera = this.minecraft.renderViewEntity;
        if (!PickBlockInput.schematicFirst(hit, this.minecraft.objectMouseOver, camera == null ? null : camera.getPosition(1),
            schematic.position.x, schematic.position.y, schematic.position.z)
            || !schematic.isBlockRendered(hit.blockX, hit.blockY, hit.blockZ)) return false;
        final EntityClientPlayerMP player = this.minecraft.thePlayer;
        if (!ForgeHooks.onPickBlock(hit, player, schematic)) return false;
        if (player.capabilities.isCreativeMode) {
            final Block block = schematic.getBlock(hit.blockX, hit.blockY, hit.blockZ);
            final int metadata = schematic.getBlockMetadata(hit.blockX, hit.blockY, hit.blockZ);
            if (block == Blocks.double_stone_slab || block == Blocks.double_wooden_slab || block == Blocks.snow_layer) {
                player.inventory.setInventorySlotContents(player.inventory.currentItem, new ItemStack(block, 1, metadata & 0xF));
            }
            final int slot = player.inventoryContainer.inventorySlots.size() - 9 + player.inventory.currentItem;
            this.minecraft.playerController.sendSlotPacket(player.inventory.getStackInSlot(player.inventory.currentItem), slot);
        }
        return true;
    }
}
