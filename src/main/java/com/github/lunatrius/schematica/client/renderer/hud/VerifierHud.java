package com.github.lunatrius.schematica.client.renderer.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

import com.github.lunatrius.schematica.client.gui.framework.MinecraftUiDraw;
import com.github.lunatrius.schematica.client.gui.framework.UiBounds;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.verifier.VerificationManager;
import com.github.lunatrius.schematica.handler.InfoHudSettings;
import com.github.lunatrius.schematica.handler.VerifierOverlaySettings;
import com.github.lunatrius.schematica.handler.client.InputHandler;
import com.github.lunatrius.schematica.client.gui.VerifierBlockInfo;
import com.github.lunatrius.schematica.client.renderer.VerifierOverlayRenderer;
import com.github.lunatrius.schematica.client.verifier.VerificationScan.Marker;
import com.github.lunatrius.schematica.client.verifier.VerificationScan.Pair;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public final class VerifierHud {
    private static Pair pair;
    private static VerifierBlockInfo info;
    private VerifierHud() {}

    public static void render(Minecraft mc, float partialTicks) {
        VerificationManager.Session session = VerificationManager.INSTANCE.overlay(mc);
        if (mc.theWorld == null || mc.thePlayer == null || mc.currentScreen != null || mc.gameSettings.hideGUI
            || session == null) { pair = null; info = null; return; }
        ScaledResolution screen = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        overlay(mc, session, partialTicks, screen);
        if (!session.hud) return;
        List<String> lines = new ArrayList<>();
        lines.add("§l" + UiTranslations.format("litematica.gui.title.schematic_verifier", session.placement.name));
        lines.add(session.progressText());
        lines.add(session.countsText());
        if (VerifierOverlaySettings.enabled && !session.markers.markers().isEmpty()) {
            lines.add("§l" + UiTranslations.format("litematica.gui.title.schematic_verifier_errors"));
            int count = 0;
            for (Marker marker : session.markers.markers()) {
                if (count++ == VerifierOverlaySettings.maxLines) break;
                lines.add(marker.group.type.color + "x: " + marker.x + ", y: " + marker.y + ", z: " + marker.z);
            }
        }
        int lineHeight = mc.fontRenderer.FONT_HEIGHT + 2;
        double scale = InfoHudSettings.scale;
        int maxLines = Math.max(1, (int) (screen.getScaledHeight() / scale - 4) / lineHeight);
        if (lines.size() > maxLines) lines = lines.subList(0, maxLines);
        int y = InfoHudSettings.alignment.y(screen.getScaledHeight(), lines.size() * lineHeight, scale, InfoHudSettings.offsetY);
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

    private static void overlay(Minecraft mc, VerificationManager.Session session, float partialTicks, ScaledResolution screen) {
        int key = InputHandler.RENDER_INFO_OVERLAY.getKeyCode();
        boolean held = key > 0 && key < Keyboard.KEYBOARD_SIZE ? Keyboard.isKeyDown(key)
            : key < 0 && key + 100 >= 0 && key + 100 < Mouse.getButtonCount() && Mouse.isButtonDown(key + 100);
        Marker marker = held && VerifierOverlaySettings.enabled ? VerifierOverlayRenderer.target(mc, session, partialTicks) : null;
        if (marker == null) { pair = null; info = null; return; }
        if (!marker.group.pair.equals(pair)) { pair = marker.group.pair; info = new VerifierBlockInfo(pair); }
        try (MinecraftUiDraw draw = new MinecraftUiDraw(mc)) {
            int width = info.width(draw, Math.max(0, screen.getScaledWidth() - 8));
            int y = ("center".equals(VerifierOverlaySettings.alignment) ? screen.getScaledHeight() / 2 : 0) + VerifierOverlaySettings.offsetY;
            info.draw(draw, (screen.getScaledWidth() - width) / 2,
                Math.max(4, Math.min(y, screen.getScaledHeight() - VerifierBlockInfo.HEIGHT - 4)), width);
        }
    }
}
