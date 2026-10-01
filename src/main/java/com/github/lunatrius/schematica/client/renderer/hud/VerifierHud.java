package com.github.lunatrius.schematica.client.renderer.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

import com.github.lunatrius.schematica.client.gui.framework.MinecraftUiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.verifier.VerificationManager;
import com.github.lunatrius.schematica.handler.InfoHudSettings;

public final class VerifierHud {
    private VerifierHud() {}

    public static void render(Minecraft mc) {
        VerificationManager.Session session = VerificationManager.INSTANCE.focused();
        if (mc.theWorld == null || mc.thePlayer == null || mc.currentScreen != null || mc.gameSettings.hideGUI
            || session == null || !session.available(mc) || !session.hud || session.scan() == null) return;
        ScaledResolution screen = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        String[] lines = {"§l" + UiTranslations.format("litematica.gui.title.schematic_verifier", session.placement.name),
            session.progressText(), session.countsText()};
        int lineHeight = mc.fontRenderer.FONT_HEIGHT + 2;
        double scale = InfoHudSettings.scale;
        int y = InfoHudSettings.alignment.y(screen.getScaledHeight(), lines.length * lineHeight, scale, InfoHudSettings.offsetY);
        int maxWidth = Math.max(0, (int) (screen.getScaledWidth() / scale) - 8);
        try (MinecraftUiDraw draw = new MinecraftUiDraw(mc)) {
            GL11.glScalef((float) scale, (float) scale, 1);
            for (String line : lines) {
                line = draw.trim(line, maxWidth);
                int width = draw.textWidth(line);
                int x = InfoHudSettings.alignment.x(screen.getScaledWidth(), width, scale, InfoHudSettings.offsetX);
                draw.fill(new UiBounds(x - 2, y - 1, width + 4, lineHeight), 0x80000000);
                draw.text(line, x, y, 0xFFFFFFFF);
                y += lineHeight;
            }
        }
    }
}
