package com.github.lunatrius.schematica.tool;

import java.util.concurrent.ArrayBlockingQueue;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentTranslation;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Area;
import com.github.lunatrius.schematica.client.selection.AreaSelections;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.github.lunatrius.schematica.network.PacketHandler;
import com.github.lunatrius.schematica.network.message.MessageMoveRequest;
import com.github.lunatrius.schematica.network.message.MessageMoveResult;

public final class WorldMoveController {
    private static final ArrayBlockingQueue<MessageMoveResult> RESULTS = new ArrayBlockingQueue<>(16);
    private static Area area;
    private static Object world, library;
    private static String geometry;
    private static Vector3i delta;
    private static long id;
    private WorldMoveController() {}

    public static void finished(long request, boolean success) { RESULTS.offer(new MessageMoveResult(request, success)); }

    public static void moveTo(Vector3i target) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        if (!mc.thePlayer.capabilities.isCreativeMode) { message("litematica.error.generic.creative_mode_only"); return; }
        if (area != null) { message("litematica.message.error.move.pending_tasks"); return; }
        if (mc.getIntegratedServer() == null && !SchematicaPlus.proxy.supportsWorldMove) { message("schematica.message.move.server_required"); return; }
        Area selected = AreaSelections.library().selected();
        if (selected == null || selected.boxes().isEmpty()) { message("litematica.message.error.no_area_selected"); return; }
        AreaSelections.capture(); Vector3i origin = selected.origin();
        Vector3i shift = new Vector3i(Math.subtractExact(target.x, origin.x), Math.subtractExact(target.y, origin.y), Math.subtractExact(target.z, origin.z));
        if (shift.x == 0 && shift.y == 0 && shift.z == 0) return;
        long request = System.nanoTime();
        WorldMoveJob job = new WorldMoveJob(mc.thePlayer.getUniqueID(), mc.thePlayer.dimension, selected.regions(), shift.x, shift.y, shift.z);
        if (mc.getIntegratedServer() != null) {
            job.completion = success -> finished(request, success);
            if (!com.github.lunatrius.schematica.handler.WorldEditQueue.INSTANCE.submit(mc.getIntegratedServer(), job)) { message("litematica.message.error.move.pending_tasks"); return; }
        } else PacketHandler.INSTANCE.sendToServer(new MessageMoveRequest(request, mc.thePlayer.dimension, selected.regions(), shift.x, shift.y, shift.z));
        area = selected; world = mc.theWorld; library = AreaSelections.library(); geometry = geometry(selected); delta = shift; id = request;
    }
    public static void tick() {
        Minecraft mc = Minecraft.getMinecraft();
        if (area != null && (mc.theWorld != world || AreaSelections.library() != library)) area = null;
        MessageMoveResult result;
        while ((result = RESULTS.poll()) != null) {
            if (area == null || result.id != id) continue;
            if (result.success) {
                AreaSelections.capture();
                if (AreaSelections.library().contains(area) && geometry.equals(geometry(area))) {
                    AreaSelections.library().moveEntire(area, delta.x, delta.y, delta.z); ToolManager.saveArea();
                }
            } else message("schematica.message.move.failed");
            area = null;
        }
    }
    private static String geometry(Area value) {
        StringBuilder text = new StringBuilder();
        for (SchematicRegion region : value.regions()) text.append(region.minX).append(',').append(region.minY).append(',').append(region.minZ)
            .append(',').append(region.maxX).append(',').append(region.maxY).append(',').append(region.maxZ).append(';');
        Vector3i origin = value.origin();
        return text.append(origin.x).append(',').append(origin.y).append(',').append(origin.z).toString();
    }
    private static void message(String key) { Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentTranslation(key)); }
}
