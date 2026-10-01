// SPDX-License-Identifier: LGPL-3.0-only
// Litematica GridPlacementManager (maruohon, liteloader_1.12.2), adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import com.github.lunatrius.schematica.client.renderer.RendererSchematicGlobal;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;

/**
 * Keeps the repeated copies of placements with an enabled grid, only within the client's loaded area
 * (render distance plus one chunk, 512 blocks up and down), and recreates them when the base placement changes.
 */
public final class GridPlacements {
    public static final GridPlacements INSTANCE = new GridPlacements();

    /** What a base placement looked like when its copies were made; any change recreates them. */
    private static final class Signature {
        final Object schematic, source;
        final int[] box;
        final int x, y, z, placementRevision, contentRevision, layer;
        final boolean enabled, rendering, entities, layerMode;
        final GridSettings grid = new GridSettings();

        Signature(SchematicWorld base) {
            schematic = base.getSchematic();
            source = base.sourceData();
            box = base.enclosingBox();
            x = base.position.x; y = base.position.y; z = base.position.z;
            placementRevision = base.placementRevision();
            contentRevision = base.contentRevision();
            enabled = base.isEnabled();
            rendering = base.isRendering;
            entities = base.isRenderingEntities;
            layerMode = base.isRenderingLayer;
            layer = base.renderingLayer;
            grid.copyFrom(base.grid);
        }

        boolean same(Signature other) {
            return schematic == other.schematic && source == other.source && java.util.Arrays.equals(box, other.box)
                && x == other.x && y == other.y && z == other.z && placementRevision == other.placementRevision
                && contentRevision == other.contentRevision && enabled == other.enabled && rendering == other.rendering
                && entities == other.entities && layerMode == other.layerMode && layer == other.layer && grid.equals(other.grid);
        }
    }

    private final Map<SchematicWorld, Map<Long, SchematicWorld>> copies = new IdentityHashMap<>();
    private final Map<SchematicWorld, Signature> signatures = new IdentityHashMap<>();
    private final List<SchematicWorld> all = new ArrayList<>();
    private int lastChunkX = Integer.MIN_VALUE, lastChunkZ = Integer.MIN_VALUE, lastRenderDistance;

    private GridPlacements() {}

    /** Every repeated copy that currently exists. */
    public List<SchematicWorld> copies() { return Collections.unmodifiableList(all); }

    public List<SchematicWorld> copiesOf(SchematicWorld base) {
        Map<Long, SchematicWorld> map = copies.get(base);
        return map == null ? Collections.<SchematicWorld>emptyList() : new ArrayList<>(map.values());
    }

    static long key(int x, int y, int z) {
        return ((long) x & 0x1FFFFF) << 42 | ((long) y & 0x1FFFFF) << 21 | (long) z & 0x1FFFFF;
    }

    /** GridPlacementManager.getGridPointsWithinAreaFor: grid points (not 0,0,0) whose copies touch the area. */
    static List<int[]> points(int[] baseBox, GridSettings settings, int[] area) {
        List<int[]> points = new ArrayList<>();
        int[] size = settings.size(), negative = settings.repeatNegative(), positive = settings.repeatPositive();
        if (!settings.isEnabled() || size[0] < 1 || size[1] < 1 || size[2] < 1) return points;
        long[] repeat = new long[6];
        for (int i = 0; i < 3; i++) {
            repeat[i] = baseBox[i] - (long) negative[i] * size[i];
            repeat[i + 3] = baseBox[i + 3] + (long) positive[i] * size[i];
        }
        long[] intersect = new long[6];
        for (int i = 0; i < 3; i++) {
            intersect[i] = Math.max(repeat[i], area[i]);
            intersect[i + 3] = Math.min(repeat[i + 3], area[i + 3]);
            if (intersect[i] > intersect[i + 3]) return points;
        }
        int[] min = new int[3], max = new int[3];
        for (int i = 0; i < 3; i++) {
            min[i] = (int) Math.floorDiv(intersect[i] - baseBox[i], (long) size[i]);
            max[i] = (int) Math.floorDiv(intersect[i + 3] - baseBox[i], (long) size[i]);
            min[i] = Math.max(min[i], -negative[i]);
            max[i] = Math.min(max[i], positive[i]);
        }
        for (int y = min[1]; y <= max[1]; y++) {
            for (int z = min[2]; z <= max[2]; z++) {
                for (int x = min[0]; x <= max[0]; x++) {
                    if (x != 0 || y != 0 || z != 0) points.add(new int[] {x, y, z});
                }
            }
        }
        return points;
    }

    /** The loaded area around the player, as GridPlacementManager.getCurrentLoadedArea(1). */
    private static int[] loadedArea(Minecraft mc) {
        EntityPlayer player = mc.thePlayer;
        if (player == null) return null;
        int chunkX = (int) Math.floor(player.posX) >> 4, chunkZ = (int) Math.floor(player.posZ) >> 4;
        int radius = mc.gameSettings.renderDistanceChunks + 1, y = (int) player.posY;
        return new int[] {(chunkX - radius) << 4, y - 512, (chunkZ - radius) << 4,
            ((chunkX + radius) << 4) + 15, y + 512, ((chunkZ + radius) << 4) + 15};
    }

    /** Client tick: follows base placement changes, player movement across chunks and removed placements. */
    public void tick(Minecraft mc) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            clear();
            return;
        }
        Set<SchematicWorld> bases = new HashSet<>();
        for (SchematicWorld placement : ClientProxy.loadedSchematics) {
            if (placement.isRepeatedPlacement()) continue;
            placement.grid.setDefaultSize(size(placement.enclosingBox()));
            if (placement.grid.isEnabled() || copies.containsKey(placement)) bases.add(placement);
        }
        for (SchematicWorld base : new ArrayList<>(copies.keySet())) if (!bases.contains(base)) remove(base);
        int chunkX = (int) Math.floor(mc.thePlayer.posX) >> 4, chunkZ = (int) Math.floor(mc.thePlayer.posZ) >> 4;
        boolean moved = chunkX != lastChunkX || chunkZ != lastChunkZ || mc.gameSettings.renderDistanceChunks != lastRenderDistance;
        lastChunkX = chunkX; lastChunkZ = chunkZ; lastRenderDistance = mc.gameSettings.renderDistanceChunks;
        int[] area = loadedArea(mc);
        for (SchematicWorld base : bases) {
            Signature now = new Signature(base);
            Signature before = signatures.get(base);
            if (before == null || !before.same(now)) {
                remove(base);
                signatures.put(base, now);
                update(base, area);
            } else if (moved) {
                update(base, area);
            }
        }
    }

    private static int[] size(int[] box) {
        return new int[] {box[3] - box[0] + 1, box[4] - box[1] + 1, box[5] - box[2] + 1};
    }

    /** Adds the copies that entered the area and removes those that left it. */
    private void update(SchematicWorld base, int[] area) {
        Map<Long, SchematicWorld> existing = copies.computeIfAbsent(base, ignored -> new HashMap<>());
        Set<Long> wanted = new HashSet<>();
        if (base.isEnabled() && area != null) {
            int[] box = base.enclosingBox(), size = base.grid.size();
            for (int[] point : points(box, base.grid, area)) {
                long key = key(point[0], point[1], point[2]);
                wanted.add(key);
                if (existing.containsKey(key)) continue;
                try {
                    SchematicWorld copy = SchematicWorld.repeatedCopy(base, base.position.x + point[0] * size[0],
                        base.position.y + point[1] * size[1], base.position.z + point[2] * size[2]);
                    RendererSchematicGlobal.INSTANCE.createRendererSchematicChunks(copy);
                    existing.put(key, copy);
                    all.add(copy);
                } catch (RuntimeException e) {
                    Reference.logger.warn("Could not create a grid copy of {}", base.name, e);
                }
            }
        }
        for (Long key : new ArrayList<>(existing.keySet())) {
            if (wanted.contains(key)) continue;
            discard(existing.remove(key));
        }
    }

    private void discard(SchematicWorld copy) {
        if (copy == null) return;
        RendererSchematicGlobal.INSTANCE.removeRendererSchematicChunks(copy);
        all.remove(copy);
    }

    /** Removes the copies of a base placement (it was removed, its grid disabled or it changed). */
    public void remove(SchematicWorld base) {
        Map<Long, SchematicWorld> map = copies.remove(base);
        signatures.remove(base);
        if (map != null) for (SchematicWorld copy : map.values()) discard(copy);
    }

    public void clear() {
        for (SchematicWorld base : new ArrayList<>(copies.keySet())) remove(base);
        signatures.clear();
        lastChunkX = lastChunkZ = Integer.MIN_VALUE;
    }
}
