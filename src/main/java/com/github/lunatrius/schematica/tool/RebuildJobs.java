// SPDX-License-Identifier: LGPL-3.0-only
// Litematica schematic rebuild bulk operations, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.tool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.client.renderer.RendererSchematicGlobal;
import com.github.lunatrius.schematica.client.world.CellState;
import com.github.lunatrius.schematica.client.world.SchematicLibrary;
import com.github.lunatrius.schematica.client.world.SchematicSourceData;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.client.world.SourceBlockPosition;
import com.github.lunatrius.schematica.client.world.SourceEditor;
import com.github.lunatrius.schematica.client.world.SourceEditor.Cell;
import com.github.lunatrius.schematica.client.world.SubRegionPlacements;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.task.TaskRegistry;

/** Runs schematic edits of unbounded size in per-tick time slices; the first slice runs immediately. */
public final class RebuildJobs {
    private static final long SLICE_NANOS = 6_000_000L;
    private static final int FLUSH_CELLS = 65536;
    private static Job running;

    private RebuildJobs() {}

    public static boolean busy() { return running != null; }

    static boolean start(Job job) {
        if (running != null) {
            SchematicRebuild.message(EnumChatFormatting.RED, "schematica.message.rebuild.busy");
            return false;
        }
        running = job;
        run(job, System.nanoTime() + SLICE_NANOS);
        if (running == job) {
            Minecraft mc = Minecraft.getMinecraft();
            job.task = TaskRegistry.INSTANCE.start(mc.thePlayer.getUniqueID(), mc.thePlayer.dimension, TaskRegistry.Kind.REBUILD,
                TaskRegistry.Backend.MEMORY, job.detail);
            job.publish();
        }
        return true;
    }

    public static void tick(Minecraft mc) {
        Job job = running;
        if (job == null) return;
        if (job.task != null && job.task.progress().cancelling) { end(job, false); return; }
        run(job, System.nanoTime() + SLICE_NANOS);
    }

    private static void run(Job job, long deadline) {
        try {
            if (!job.valid()) { end(job, false); return; }
            boolean done = job.step(deadline);
            job.edits.flush(job.valid());
            if (done) end(job, true);
            else job.publish();
        } catch (IOException | RuntimeException error) {
            Reference.logger.error("Schematic edit task failed", error);
            SchematicRebuild.message(EnumChatFormatting.RED, "schematica.message.rebuild.failed");
            end(job, false);
        }
    }

    private static void end(Job job, boolean completed) {
        if (running == job) running = null;
        try {
            job.edits.flush(job.valid());
            job.edits.publish();
            if (completed && job.valid()) job.completed();
        } catch (IOException | RuntimeException error) {
            Reference.logger.error("Could not refresh placements after a schematic edit", error);
            SchematicRebuild.message(EnumChatFormatting.RED, "schematica.message.rebuild.failed");
        } finally {
            if (job.task != null) job.task.finish();
        }
    }

    /** Source edits applied in bounded batches; placements are refreshed once at the end. */
    static final class Edits {
        private final Map<SchematicLibrary.Source<SchematicSourceData>, Pending> sources = new IdentityHashMap<>();
        private long changed;

        private static final class Pending {
            final SchematicSourceData data;
            final Map<Cell, CellState> queued = new LinkedHashMap<>();
            final List<Cell> applied = new ArrayList<>();
            boolean recompose;

            Pending(SchematicSourceData data) { this.data = data; }
        }

        void put(SchematicLibrary.Source<SchematicSourceData> source, Cell cell, CellState state) throws IOException {
            Pending pending = sources.computeIfAbsent(source, key -> new Pending(key.data()));
            pending.queued.put(cell, state);
            if (pending.queued.size() >= FLUSH_CELLS) flush(source, pending, true);
        }

        long changed() { return changed; }

        void flush(boolean valid) throws IOException {
            for (Map.Entry<SchematicLibrary.Source<SchematicSourceData>, Pending> entry : sources.entrySet()) flush(entry.getKey(), entry.getValue(), valid);
        }

        private void flush(SchematicLibrary.Source<SchematicSourceData> source, Pending pending, boolean valid) throws IOException {
            if (pending.queued.isEmpty()) return;
            if (!valid || !current(source, pending)) { pending.queued.clear(); return; }
            List<Cell> applied = SourceEditor.apply(pending.data, pending.queued);
            pending.queued.clear();
            changed += applied.size();
            if (pending.recompose) return;
            pending.applied.addAll(applied);
            if (pending.applied.size() > SourceEditor.PATCH_LIMIT) { pending.recompose = true; pending.applied.clear(); }
        }

        void publish() throws IOException {
            for (Map.Entry<SchematicLibrary.Source<SchematicSourceData>, Pending> entry : sources.entrySet()) {
                SchematicLibrary.Source<SchematicSourceData> source = entry.getKey();
                Pending pending = entry.getValue();
                if (!current(source, pending) || !pending.recompose && pending.applied.isEmpty()) continue;
                if (pending.recompose) { ClientProxy.refreshSource(source); continue; }
                Map<String, CellState> cache = new HashMap<>();
                for (SchematicWorld world : new ArrayList<>(ClientProxy.loadedSchematics)) {
                    if (ClientProxy.SCHEMATICS.sourceOf(world) != source) continue;
                    int[] bounds = SourceEditor.patch(world, pending.data.editable(), pending.applied, cache);
                    if (bounds != null) RendererSchematicGlobal.INSTANCE.markDirtyAllSchematics(bounds[0] - 1, bounds[1] - 1, bounds[2] - 1,
                        bounds[3] + 1, bounds[4] + 1, bounds[5] + 1);
                }
            }
            sources.clear();
        }

        private static boolean current(SchematicLibrary.Source<SchematicSourceData> source, Pending pending) {
            return ClientProxy.SCHEMATICS.sources().contains(source) && source.data() == pending.data;
        }
    }

    abstract static class Job {
        final Edits edits = new Edits();
        final String detail;
        TaskRegistry.Task task;
        long completed, total;

        Job(String detail) { this.detail = detail; }

        abstract boolean valid();
        /** Advances until the deadline; returns true when every cell was visited. */
        abstract boolean step(long deadline) throws IOException;
        void completed() {}

        void publish() {
            if (task != null) task.update(TaskRegistry.Stage.EDIT, completed, total, edits.changed(), 0);
        }
    }

    /** Applies a rule to every cell of the scoped source regions inside the layer range at the start of the edit. */
    static final class Bulk extends Job {
        private final SchematicLibrary.Source<SchematicSourceData> source;
        private final SchematicSourceData data;
        private final List<Part> parts = new ArrayList<>();
        private final SchematicOrigin origin;
        private final List<String> global;
        private final boolean limited;
        private int part, x, y, z;

        private static final class Part {
            final SubRegionPlacements.Region region;
            final SchematicRebuild.CellRule rule;
            final ISchematic container;
            final boolean independent;

            Part(SubRegionPlacements.Region region, SchematicRebuild.CellRule rule, ISchematic container, boolean independent) {
                this.region = region; this.rule = rule; this.container = container; this.independent = independent;
            }
        }

        Bulk(SchematicRebuild.Owner owner, List<SubRegionPlacements.Region> scope, SchematicRebuild.RegionRule factory, boolean limited) throws IOException {
            super(owner.world.name);
            source = owner.source;
            data = owner.source.data();
            origin = owner.world.originPosition();
            global = new ArrayList<>(owner.world.transformOperations);
            this.limited = limited;
            ISchematic edited = data.editable();
            for (SubRegionPlacements.Region region : scope) {
                if (!region.enabled) continue;
                ISchematic payload = edited.getRegionSchematic(region.name());
                SchematicRebuild.CellRule rule = factory.create(new SchematicRebuild.RegionScope(region, SourceBlockPosition.combined(region, global)));
                parts.add(new Part(region, rule, payload == null ? edited : payload, payload != null));
                SchematicRegion box = region.box;
                total += (long) (box.maxX - box.minX + 1) * (box.maxY - box.minY + 1) * (box.maxZ - box.minZ + 1);
            }
        }

        @Override boolean valid() { return ClientProxy.SCHEMATICS.sources().contains(source) && source.data() == data; }

        @Override boolean step(long deadline) throws IOException {
            while (part < parts.size()) {
                Part current = parts.get(part);
                SchematicRegion box = current.region.box;
                int width = box.maxX - box.minX + 1, height = box.maxY - box.minY + 1, length = box.maxZ - box.minZ + 1;
                while (y < height) {
                    int cx = current.independent ? x : box.minX + x, cy = current.independent ? y : box.minY + y, cz = current.independent ? z : box.minZ + z;
                    if (current.container.containsBlock(cx, cy, cz) && (!limited || inRange(current.region, x, y, z))) {
                        CellState next = current.rule.apply(current.container.getBlock(cx, cy, cz), current.container.getBlockMetadata(cx, cy, cz));
                        if (next != null) edits.put(source, new Cell(current.independent ? current.region.name() : null, cx, cy, cz), next);
                    }
                    completed++;
                    if (++x == width) { x = 0; if (++z == length) { z = 0; y++; } }
                    if ((completed & 1023) == 0 && System.nanoTime() >= deadline) return false;
                }
                part++; x = 0; y = 0; z = 0;
            }
            return true;
        }

        private boolean inRange(SubRegionPlacements.Region region, int x, int y, int z) {
            SchematicOrigin world = SourceBlockPosition.world(region, new SchematicOrigin(x, y, z), origin, global);
            return SchematicRebuild.inRange(world.x, world.y, world.z);
        }
    }

    /** Copies loaded real-world blocks that differ from the schematic shown at the same position. */
    static final class Selection extends Job {
        private final World world;
        private final List<SchematicRegion> boxes;
        private final String origin;
        private final Map<String, CellState> states = new HashMap<>(), cache = new HashMap<>();
        private int box, x, y, z;

        Selection(World world, List<SchematicRegion> boxes, String name, String origin) {
            super(name);
            this.world = world;
            this.boxes = new ArrayList<>(boxes);
            this.origin = origin;
            for (SchematicRegion region : boxes) total += (long) (region.maxX - region.minX + 1) * (region.maxY - region.minY + 1) * (region.maxZ - region.minZ + 1);
        }

        @Override boolean valid() { return Minecraft.getMinecraft().theWorld == world && !ClientProxy.loadedSchematics.isEmpty(); }

        @Override boolean step(long deadline) throws IOException {
            while (box < boxes.size()) {
                SchematicRegion region = boxes.get(box);
                int width = region.maxX - region.minX + 1, height = region.maxY - region.minY + 1, length = region.maxZ - region.minZ + 1;
                while (y < height) {
                    visit(region.minX + x, region.minY + y, region.minZ + z);
                    completed++;
                    if (++x == width) { x = 0; if (++z == length) { z = 0; y++; } }
                    if ((completed & 255) == 0 && System.nanoTime() >= deadline) return false;
                }
                box++; x = 0; y = 0; z = 0;
            }
            return true;
        }

        private void visit(int px, int py, int pz) throws IOException {
            if (!world.blockExists(px, py, pz)) return;
            SchematicRebuild.Owner owner = SchematicRebuild.owner(ClientProxy.schematic, px, py, pz);
            if (owner == null) return;
            Block block = world.getBlock(px, py, pz);
            int meta = world.getBlockMetadata(px, py, pz);
            if (SchematicRebuild.composed(owner.world, px, py, pz).same(new CellState(block, meta, null))) return;
            CellState state = states.computeIfAbsent(Block.getIdFromBlock(block) + ":" + meta, key -> CellState.of(block, meta));
            edits.put(owner.source, owner.cell, state.transform(SourceBlockPosition.inverse(owner.operations()), cache));
        }

        @Override void completed() {
            SchematicRebuild.message(EnumChatFormatting.GREEN, "litematica.message.schematic_edit_replace_selection", origin);
        }
    }
}
