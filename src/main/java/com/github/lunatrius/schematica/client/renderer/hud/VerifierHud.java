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
import com.github.lunatrius.schematica.client.verifier.VerificationScan;
import com.github.lunatrius.schematica.client.verifier.VerificationScan.Marker;
import com.github.lunatrius.schematica.client.verifier.VerificationScan.Pair;
import java.util.ArrayList;
import java.util.List;

public final class VerifierHud {
    private static Pair pair;
    private static VerifierBlockInfo info;
    private static int textHeight;
    private VerifierHud() {}

    /** The height of the verifier lines of the info HUD drawn last frame, for the material list HUD below them. */
    public static int textHeight() { return textHeight; }

    /** Returns whether the verifier block overlay was drawn, which suppresses the generic block info overlay. */
    public static boolean render(Minecraft mc, float partialTicks) {
        textHeight = 0;
        if (mc.theWorld == null || mc.thePlayer == null || mc.currentScreen != null || mc.gameSettings.hideGUI) {
            pair = null; info = null; return false;
        }
        VerificationManager.Session session = VerificationManager.INSTANCE.overlay(mc);
        ScaledResolution screen = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        boolean drawn = session != null && overlay(mc, session, partialTicks, screen);
        if (session == null) { pair = null; info = null; }
        List<String> lines = new ArrayList<>();
        if (VerifierOverlaySettings.enabled) {
            for (VerificationManager.Session each : VerificationManager.INSTANCE.sessions()) if (each.showsHud(mc)) lines.addAll(lines(mc, each));
        }
        if (lines.isEmpty()) return drawn;
        int lineHeight = mc.fontRenderer.FONT_HEIGHT + 2;
        double scale = InfoHudSettings.scale;
        int maxLines = Math.max(1, (int) (screen.getScaledHeight() / scale - 4) / lineHeight);
        if (lines.size() > maxLines) lines = lines.subList(0, maxLines);
        // MaLiLib RenderUtils.renderText: shadowed lines on a per-line background
        int y = InfoHudSettings.alignment.y(screen.getScaledHeight(), lines.size() * lineHeight - 2, scale, InfoHudSettings.offsetY);
        textHeight = (int) Math.ceil(lines.size() * lineHeight * scale);
        int maxWidth = Math.max(0, (int) (screen.getScaledWidth() / scale) - 8);
        try (MinecraftUiDraw draw = new MinecraftUiDraw(mc)) {
            GL11.glScalef((float) scale, (float) scale, 1);
            for (String line : lines) {
                line = draw.trim(line, maxWidth);
                int width = draw.textWidth(line);
                int x = InfoHudSettings.alignment.x(screen.getScaledWidth(), width, scale, InfoHudSettings.offsetX);
                draw.fill(new UiBounds(x - 2, y - 2, width + 2, mc.fontRenderer.FONT_HEIGHT + 2), 0x80000000);
                draw.text(line, x, y, 0xFFFFFFFF);
                y += lineHeight;
            }
        }
        return drawn;
    }

    /** SchematicVerifier's info HUD lines: the closest remaining chunks while verifying, then the closest selected errors. */
    static List<String> lines(Minecraft mc, VerificationManager.Session session) {
        List<String> lines = new ArrayList<>();
        VerificationScan scan = session.scan();
        if (!scan.done()) {
            List<int[]> chunks = scan.pendingChunks((int) Math.floor(mc.thePlayer.posX), (int) Math.floor(mc.thePlayer.posZ));
            lines.add("§f§l" + UiTranslations.format("litematica.gui.label.task.title.remaining_chunks",
                UiTranslations.format("litematica.gui.label.schematic_verifier.verifier"), chunks.size()) + "§r");
            for (int i = 0; i < Math.min(chunks.size(), VerifierOverlaySettings.maxLines); i++) {
                int[] chunk = chunks.get(i);
                lines.add(String.format("cx: %5d, cz: %5d (x: %d, z: %d)", chunk[0], chunk[1], chunk[0] << 4, chunk[1] << 4));
            }
            return lines;
        }
        List<Marker> markers = session.markers.markers();
        if (markers.isEmpty()) return lines;
        VerificationScan.Type single = session.markers.selection.single();
        lines.add(single != null ? single.color + "§l" + UiTranslations.format(single.key) + "§r"
            : "§l" + UiTranslations.format("litematica.gui.title.schematic_verifier_errors") + "§r");
        for (int i = 0; i < Math.min(markers.size(), VerifierOverlaySettings.maxLines); i++) {
            Marker marker = markers.get(i);
            lines.add(String.format("%sx: %5d, y: %3d, z: %5d§r", marker.group.type.color, marker.x, marker.y, marker.z));
        }
        return lines;
    }

    private static boolean overlay(Minecraft mc, VerificationManager.Session session, float partialTicks, ScaledResolution screen) {
        boolean held = com.github.lunatrius.schematica.client.input.Hotkeys.held("renderInfoOverlay");
        Marker marker = held && VerifierOverlaySettings.enabled ? VerifierOverlayRenderer.target(mc, session, partialTicks) : null;
        if (marker == null) { pair = null; info = null; return false; }
        if (!marker.group.pair.equals(pair)) { pair = marker.group.pair; info = new VerifierBlockInfo(pair); }
        int inventories = InventoryPreview.render(mc, session.placement, marker.x, marker.y, marker.z,
            "center".equals(VerifierOverlaySettings.alignment), VerifierOverlaySettings.offsetY, screen.getScaledWidth(), screen.getScaledHeight());
        drawPanel(mc, info, inventories);
        return true;
    }

    /** Draws a block info panel at the configured block info overlay position. */
    public static void drawPanel(Minecraft mc, VerifierBlockInfo panel, int inventoryHeight) {
        ScaledResolution screen = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        try (MinecraftUiDraw draw = new MinecraftUiDraw(mc)) {
            int width = panel.width(draw, Math.max(0, screen.getScaledWidth() - 8));
            boolean center = "center".equals(VerifierOverlaySettings.alignment);
            int y = (center ? screen.getScaledHeight() / 2 : 0) + VerifierOverlaySettings.offsetY;
            // Top center: below the inventory previews
            if (!center && inventoryHeight > 0) y += inventoryHeight + VerifierOverlaySettings.offsetY;
            panel.draw(draw, (screen.getScaledWidth() - width) / 2,
                Math.max(4, Math.min(y, screen.getScaledHeight() - VerifierBlockInfo.HEIGHT - 4)), width);
        }
    }
}
