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
import com.github.lunatrius.schematica.client.gui.VerifierBlockInfo;
import com.github.lunatrius.schematica.client.renderer.VerifierOverlayRenderer;
import com.github.lunatrius.schematica.client.verifier.VerificationScan.Marker;
import com.github.lunatrius.schematica.client.verifier.VerificationScan.Pair;
import java.util.ArrayList;
import java.util.List;

public final class VerifierHud {
    private static Pair pair;
    private static VerifierBlockInfo info;
    private VerifierHud() {}

    /** Returns whether the verifier block overlay was drawn, which suppresses the generic block info overlay. */
    public static boolean render(Minecraft mc, float partialTicks) {
        VerificationManager.Session session = VerificationManager.INSTANCE.overlay(mc);
        if (mc.theWorld == null || mc.thePlayer == null || mc.currentScreen != null || mc.gameSettings.hideGUI
            || session == null) { pair = null; info = null; return false; }
        ScaledResolution screen = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        boolean drawn = overlay(mc, session, partialTicks, screen);
        if (!session.hud) return drawn;
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
        return drawn;
    }

    private static boolean overlay(Minecraft mc, VerificationManager.Session session, float partialTicks, ScaledResolution screen) {
        boolean held = com.github.lunatrius.schematica.client.input.Hotkeys.held("renderInfoOverlay");
        Marker marker = held && VerifierOverlaySettings.enabled ? VerifierOverlayRenderer.target(mc, session, partialTicks) : null;
        if (marker == null) { pair = null; info = null; return false; }
        if (!marker.group.pair.equals(pair)) { pair = marker.group.pair; info = new VerifierBlockInfo(pair); }
        try (MinecraftUiDraw draw = new MinecraftUiDraw(mc)) {
            int width = info.width(draw, Math.max(0, screen.getScaledWidth() - 8));
            int y = ("center".equals(VerifierOverlaySettings.alignment) ? screen.getScaledHeight() / 2 : 0) + VerifierOverlaySettings.offsetY;
            info.draw(draw, (screen.getScaledWidth() - width) / 2,
                Math.max(4, Math.min(y, screen.getScaledHeight() - VerifierBlockInfo.HEIGHT - 4)), width);
        }
        return true;
    }

    /** Draws a block info panel at the configured block info overlay position. */
    public static void drawPanel(Minecraft mc, VerifierBlockInfo panel) {
        ScaledResolution screen = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        try (MinecraftUiDraw draw = new MinecraftUiDraw(mc)) {
            int width = panel.width(draw, Math.max(0, screen.getScaledWidth() - 8));
            int y = ("center".equals(VerifierOverlaySettings.alignment) ? screen.getScaledHeight() / 2 : 0) + VerifierOverlaySettings.offsetY;
            panel.draw(draw, (screen.getScaledWidth() - width) / 2,
                Math.max(4, Math.min(y, screen.getScaledHeight() - VerifierBlockInfo.HEIGHT - 4)), width);
        }
    }
}
