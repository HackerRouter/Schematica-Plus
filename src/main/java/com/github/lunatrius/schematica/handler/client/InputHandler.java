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

import com.github.lunatrius.schematica.Schematica;
import com.github.lunatrius.schematica.client.gui.control.GuiSchematicControl;
import com.github.lunatrius.schematica.client.gui.load.GuiSchematicLoad;
import com.github.lunatrius.schematica.client.gui.save.GuiSchematicSave;
import com.github.lunatrius.schematica.client.renderer.RendererSchematicGlobal;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.tool.ToolManager;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;

public class InputHandler {

    public static final InputHandler INSTANCE = new InputHandler();

    private static final KeyBinding KEY_BINDING_LOAD = new KeyBinding(
        Names.Keys.LOAD,
        Schematica.proxy.GTNH ? Keyboard.KEY_NONE : Keyboard.KEY_DIVIDE,
        Names.Keys.CATEGORY);
    private static final KeyBinding KEY_BINDING_SAVE = new KeyBinding(
        Names.Keys.SAVE,
        Schematica.proxy.GTNH ? Keyboard.KEY_NONE : Keyboard.KEY_MULTIPLY,
        Names.Keys.CATEGORY);
    private static final KeyBinding KEY_BINDING_CONTROL = new KeyBinding(
        Names.Keys.CONTROL,
        Schematica.proxy.GTNH ? Keyboard.KEY_NONE : Keyboard.KEY_SUBTRACT,
        Names.Keys.CATEGORY);
    private static final KeyBinding KEY_BINDING_LAYER_INC = new KeyBinding(
        Names.Keys.LAYER_INC,
        Keyboard.KEY_NONE,
        Names.Keys.CATEGORY);
    private static final KeyBinding KEY_BINDING_LAYER_DEC = new KeyBinding(
        Names.Keys.LAYER_DEC,
        Keyboard.KEY_NONE,
        Names.Keys.CATEGORY);
    private static final KeyBinding KEY_BINDING_TOOL_MODE = new KeyBinding(
        Names.Keys.TOOL_MODE,
        Keyboard.KEY_ADD,
        Names.Keys.CATEGORY);
    private static final KeyBinding KEY_BINDING_TOOL_USE = new KeyBinding(
        Names.Keys.TOOL_USE,
        Keyboard.KEY_NUMPAD1,
        Names.Keys.CATEGORY);
    private static final KeyBinding KEY_BINDING_TOOL_ATTACK = new KeyBinding(
        Names.Keys.TOOL_ATTACK,
        Keyboard.KEY_NUMPAD2,
        Names.Keys.CATEGORY);

    public static final KeyBinding[] KEY_BINDINGS = new KeyBinding[] { KEY_BINDING_LOAD, KEY_BINDING_SAVE,
        KEY_BINDING_CONTROL, KEY_BINDING_LAYER_INC, KEY_BINDING_LAYER_DEC,
        KEY_BINDING_TOOL_MODE, KEY_BINDING_TOOL_USE, KEY_BINDING_TOOL_ATTACK };

    private final Minecraft minecraft = Minecraft.getMinecraft();

    private InputHandler() {}

    @SubscribeEvent
    public void onKeyInput(InputEvent event) {
        if (this.minecraft.currentScreen == null) {
            if (KEY_BINDING_LOAD.isPressed()) {
                this.minecraft.displayGuiScreen(new GuiSchematicLoad(this.minecraft.currentScreen));
            }

            if (KEY_BINDING_SAVE.isPressed()) {
                this.minecraft.displayGuiScreen(new GuiSchematicSave(this.minecraft.currentScreen));
            }

            if (KEY_BINDING_CONTROL.isPressed()) {
                this.minecraft.displayGuiScreen(new GuiSchematicControl(this.minecraft.currentScreen));
            }

            if (KEY_BINDING_LAYER_INC.isPressed()) {
                final SchematicWorld schematic = ClientProxy.schematic;
                if (schematic != null && schematic.isRenderingLayer) {
                    schematic.renderingLayer = MathHelper
                        .clamp_int(schematic.renderingLayer + 1, 0, schematic.getHeight() - 1);
                    RendererSchematicGlobal.INSTANCE.refresh();
                }
            }

            if (KEY_BINDING_LAYER_DEC.isPressed()) {
                final SchematicWorld schematic = ClientProxy.schematic;
                if (schematic != null && schematic.isRenderingLayer) {
                    schematic.renderingLayer = MathHelper
                        .clamp_int(schematic.renderingLayer - 1, 0, schematic.getHeight() - 1);
                    RendererSchematicGlobal.INSTANCE.refresh();
                }
            }

            if (KEY_BINDING_TOOL_MODE.isPressed()) {
                ToolManager.INSTANCE.cycleMode();
            }

            if (KEY_BINDING_TOOL_USE.isPressed()) {
                ToolManager.INSTANCE.onToolUse(this.minecraft.thePlayer, ClientProxy.schematic);
            }

            if (KEY_BINDING_TOOL_ATTACK.isPressed()) {
                ToolManager.INSTANCE.onToolAttack(this.minecraft.thePlayer, ClientProxy.schematic);
            }

            handlePickBlock();
        }
    }

    private void handlePickBlock() {
        final KeyBinding keyPickBlock = this.minecraft.gameSettings.keyBindPickBlock;
        if (keyPickBlock.isPressed()) {
            try {
                final SchematicWorld schematic = ClientProxy.schematic;
                boolean revert = true;

                if (schematic != null && schematic.isRendering) {
                    revert = pickBlock(schematic, ClientProxy.movingObjectPosition);
                }

                if (revert) {
                    KeyBinding.onTick(keyPickBlock.getKeyCode());
                }
            } catch (Exception e) {
                Reference.logger.error("Could not pick block!", e);
            }
        }
    }

    private boolean pickBlock(final SchematicWorld schematic, final MovingObjectPosition objectMouseOver) {
        boolean revert = false;

        // Minecraft.func_147112_ai
        if (objectMouseOver != null) {
            final EntityClientPlayerMP player = this.minecraft.thePlayer;

            if (objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.MISS) {
                revert = true;
            }

            final MovingObjectPosition mcObjectMouseOver = this.minecraft.objectMouseOver;
            if (mcObjectMouseOver != null
                && mcObjectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                final int x = mcObjectMouseOver.blockX - schematic.position.x;
                final int y = mcObjectMouseOver.blockY - schematic.position.y;
                final int z = mcObjectMouseOver.blockZ - schematic.position.z;
                if (x == objectMouseOver.blockX && y == objectMouseOver.blockY && z == objectMouseOver.blockZ) {
                    return true;
                }
            }

            if (!ForgeHooks.onPickBlock(objectMouseOver, player, schematic)) {
                return revert;
            }

            if (player.capabilities.isCreativeMode) {
                final Block block = schematic
                    .getBlock(objectMouseOver.blockX, objectMouseOver.blockY, objectMouseOver.blockZ);
                final int metadata = schematic
                    .getBlockMetadata(objectMouseOver.blockX, objectMouseOver.blockY, objectMouseOver.blockZ);
                if (block == Blocks.double_stone_slab || block == Blocks.double_wooden_slab
                    || block == Blocks.snow_layer) {
                    player.inventory.setInventorySlotContents(
                        player.inventory.currentItem,
                        new ItemStack(block, 1, metadata & 0xF));
                }

                final int slot = player.inventoryContainer.inventorySlots.size() - 9 + player.inventory.currentItem;
                this.minecraft.playerController
                    .sendSlotPacket(player.inventory.getStackInSlot(player.inventory.currentItem), slot);
            }
        }

        return revert;
    }
}
