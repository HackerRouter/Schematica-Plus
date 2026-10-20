// SPDX-License-Identifier: LGPL-3.0-only
// MaLiLib InfoUtils and StringUtils.sendOpenFileChatMessage, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.util;

import java.io.File;
import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import com.github.lunatrius.schematica.client.gui.framework.MessageRenderer;
import com.github.lunatrius.schematica.client.gui.framework.MinecraftUiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiScreen;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.util.MessageType;
import com.github.lunatrius.schematica.util.PlayerMessage;

import cpw.mods.fml.relauncher.ReflectionHelper;

public final class InfoUtils {
    private static final MessageRenderer IN_GAME_MESSAGES = new MessageRenderer(0xA0000000, 0, false, false);
    private static final Field ACTIONBAR_TICKS = ReflectionHelper.findField(GuiIngame.class, "field_73845_h", "recordPlayingUpFor");

    private InfoUtils() {}

    public static void showGuiMessage(MessageType type, String translationKey, Object... args) {
        showGuiMessage(type, 5000, translationKey, args);
    }

    public static void showGuiMessage(MessageType type, int lifeTime, String translationKey, Object... args) {
        UiScreen screen = screen();
        if (screen != null) screen.addMessage(type, lifeTime, translationKey, args);
    }

    public static void showGuiOrActionBarMessage(MessageType type, String translationKey, Object... args) {
        showGuiOrActionBarMessage(type, 5000, translationKey, args);
    }

    public static void showGuiOrActionBarMessage(MessageType type, int lifeTime, String translationKey, Object... args) {
        UiScreen screen = screen();
        if (screen != null) screen.addMessage(type, lifeTime, translationKey, args);
        else actionbar(type.formatting() + UiTranslations.format(translationKey, args) + EnumChatFormatting.RESET);
    }

    public static void showGuiOrInGameMessage(MessageType type, String translationKey, Object... args) {
        showGuiOrInGameMessage(type, 5000, translationKey, args);
    }

    public static void showGuiOrInGameMessage(MessageType type, int lifeTime, String translationKey, Object... args) {
        UiScreen screen = screen();
        if (screen != null) screen.addMessage(type, lifeTime, translationKey, args);
        else showInGameMessage(type, lifeTime, translationKey, args);
    }

    public static void showGuiOrInGameMessage(PlayerMessage message) {
        if (message != null) showGuiOrInGameMessage(message.type, message.key, arguments(message));
    }

    public static void showGuiAndInGameMessage(MessageType type, String translationKey, Object... args) {
        showGuiAndInGameMessage(type, 5000, translationKey, args);
    }

    public static void showGuiAndInGameMessage(MessageType type, int lifeTime, String translationKey, Object... args) {
        showGuiMessage(type, lifeTime, translationKey, args);
        showInGameMessage(type, lifeTime, translationKey, args);
    }

    public static void showInGameMessage(MessageType type, String translationKey, Object... args) {
        showInGameMessage(type, (long) (ConfigurationHandler.inGameMessageTimeout * 1000L), translationKey, args);
    }

    public static void showInGameMessage(MessageType type, long lifeTime, String translationKey, Object... args) {
        IN_GAME_MESSAGES.add(type, lifeTime, UiTranslations.format(translationKey, args));
    }

    public static void printActionbarMessage(String translationKey, Object... args) {
        actionbar(UiTranslations.format(translationKey, args));
    }

    private static void actionbar(String text) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!ConfigurationHandler.enableActionbarMessages || mc.theWorld == null || mc.ingameGUI == null) return;
        mc.ingameGUI.func_110326_a(text, false);
        if (ConfigurationHandler.actionbarHudTicks != 60) {
            try {
                ACTIONBAR_TICKS.setInt(mc.ingameGUI, ConfigurationHandler.actionbarHudTicks);
            } catch (IllegalAccessException ignored) {}
        }
    }

    public static void printBooleanConfigToggleMessage(String prettyName, boolean newValue) {
        String status = (newValue ? EnumChatFormatting.GREEN : EnumChatFormatting.RED)
            + UiTranslations.format("malilib.message.value." + (newValue ? "on" : "off")) + EnumChatFormatting.RESET;
        printActionbarMessage("malilib.message.toggled", prettyName, status);
    }

    public static void sendOpenFileChatMessage(ICommandSender player, String messageKey, File file) {
        if (player == null) return;
        IChatComponent name = new ChatComponentText(file.getName());
        name.getChatStyle().setUnderlined(true).setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getAbsolutePath()));
        player.addChatMessage(new ChatComponentTranslation(messageKey, name));
    }

    public static void renderInGameMessages(Minecraft mc) {
        if (IN_GAME_MESSAGES.isEmpty()) return;
        ScaledResolution resolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        try (MinecraftUiDraw draw = new MinecraftUiDraw(mc)) {
            IN_GAME_MESSAGES.draw(draw, resolution.getScaledWidth() / 2, resolution.getScaledHeight() - 76, mc.fontRenderer.FONT_HEIGHT + 1);
        }
    }

    private static Object[] arguments(PlayerMessage message) {
        Object[] arguments = message.arguments();
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i] instanceof PlayerMessage) {
                PlayerMessage nested = (PlayerMessage) arguments[i];
                arguments[i] = UiTranslations.format(nested.key, arguments(nested));
            } else if (arguments[i] instanceof IChatComponent) {
                arguments[i] = ((IChatComponent) arguments[i]).getFormattedText();
            }
        }
        return arguments;
    }

    private static UiScreen screen() {
        return Minecraft.getMinecraft().currentScreen instanceof UiScreen ? (UiScreen) Minecraft.getMinecraft().currentScreen : null;
    }
}
