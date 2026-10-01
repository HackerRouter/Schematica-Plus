package com.github.lunatrius.schematica.client.verifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.task.TaskRegistry;
import com.github.lunatrius.schematica.util.MessageException;
import cpw.mods.fml.common.registry.GameData;

public final class VerificationManager {
    public static final VerificationManager INSTANCE = new VerificationManager();
    private final Map<SchematicWorld, Session> sessions = new LinkedHashMap<>();
    private int[] dirty;
    private int next;
    private Session focused;

    public Session get(SchematicWorld placement) {
        focused = sessions.computeIfAbsent(placement, Session::new);
        return focused;
    }

    public Session focused() { return focused; }

    public void clear() {
        for (Session session : sessions.values()) session.reset();
        sessions.clear();
        focused = null;
        takeChanges();
    }

    public synchronized void changed(int x0, int y0, int z0, int x1, int y1, int z1) {
        if (dirty == null) dirty = new int[] {x0, y0, z0, x1, y1, z1};
        else {
            dirty[0] = Math.min(dirty[0], x0); dirty[1] = Math.min(dirty[1], y0); dirty[2] = Math.min(dirty[2], z0);
            dirty[3] = Math.max(dirty[3], x1); dirty[4] = Math.max(dirty[4], y1); dirty[5] = Math.max(dirty[5], z1);
        }
    }

    private synchronized int[] takeChanges() { int[] changed = dirty; dirty = null; return changed; }

    public void tick(Minecraft mc) {
        int[] changes = takeChanges();
        Iterator<Session> iterator = sessions.values().iterator();
        while (iterator.hasNext()) {
            Session session = iterator.next();
            if (mc.theWorld == null || mc.thePlayer == null || !ClientProxy.loadedSchematics.contains(session.placement)) {
                session.reset();
                if (focused == session) focused = null;
                iterator.remove();
                continue;
            }
            if (session.scan != null && !session.valid(mc)) {
                session.reset();
                session.notice = "schematica.ui.verifier.changed";
            }
            if (changes != null && session.scan != null) session.scan.changed(changes[0], changes[1], changes[2], changes[3], changes[4], changes[5]);
            if (session.task != null && session.task.progress().cancelling) session.pause();
        }
        List<Session> pending = new ArrayList<>(sessions.values());
        long deadline = System.nanoTime() + 4_000_000L;
        for (int i = 0; i < pending.size(); i++) pending.get(Math.floorMod(next + i, pending.size())).step(mc, deadline);
        next = pending.isEmpty() ? 0 : Math.floorMod(next + 1, pending.size());
    }

    public static final class Session {
        public final SchematicWorld placement;
        private WorldClient world;
        private ISchematic source;
        private VerificationScan.Reader reader;
        private VerificationScan scan;
        private TaskRegistry.Task task;
        private int x, y, z, revision;
        private int[] bounds;
        private boolean running, layers;
        public boolean hud = true;
        public VerificationScan.Type filter = VerificationScan.Type.ALL;
        public int sortColumn = 2;
        public boolean reverse;
        private String notice = "";

        private Session(SchematicWorld placement) { this.placement = placement; }
        public VerificationScan scan() { return scan; }
        public boolean running() { return running; }
        public boolean layers() { return layers; }
        public String notice() { return notice; }
        public boolean available(Minecraft mc) {
            return mc.theWorld != null && mc.thePlayer != null && placement.isEnabled() && ClientProxy.loadedSchematics.contains(placement);
        }

        private int[] bounds() {
            return layers ? placement.renderBounds() : new int[] {0, 0, 0, placement.getWidth(), placement.getHeight(), placement.getLength()};
        }

        private boolean valid(Minecraft mc) {
            return available(mc) && world == mc.theWorld && source == placement.getSchematic()
                && x == placement.position.x && y == placement.position.y && z == placement.position.z
                && revision == placement.contentRevision() && Arrays.equals(bounds, bounds());
        }

        public void setLayers(boolean value) { reset(); layers = value; }

        public void start(Minecraft mc) {
            if (!available(mc)) throw new MessageException("schematica.ui.verifier.unavailable");
            if (scan != null && !running && valid(mc)) {
                running = true;
                notice = "";
                ensureTask(mc);
                return;
            }
            java.util.Set<VerificationScan.Pair> ignored = scan == null ? java.util.Collections.emptySet()
                : new java.util.HashSet<>(scan.ignored());
            reset();
            world = mc.theWorld;
            source = placement.getSchematic();
            x = placement.position.x; y = placement.position.y; z = placement.position.z;
            revision = placement.contentRevision();
            bounds = bounds();
            try {
                scan = new VerificationScan(x, y, z, placement.getWidth(), placement.getHeight(), placement.getLength(), bounds);
            } catch (IllegalArgumentException error) {
                reset();
                throw new MessageException("schematica.ui.verifier.bounds");
            }
            scan.addIgnored(ignored);
            reader = new VerificationScan.Reader() {
                private final Map<Long, VerificationScan.State> states = new HashMap<>();
                private int logged;
                @Override public boolean included(int x, int y, int z) { return source.containsBlock(x, y, z); }
                @Override public boolean loaded(int cx, int cz) {
                    if (!world.getChunkProvider().chunkExists(cx, cz)) return false;
                    Chunk chunk = world.getChunkFromChunkCoords(cx, cz);
                    return chunk != null && chunk.isChunkLoaded && !(chunk instanceof net.minecraft.world.chunk.EmptyChunk);
                }
                @Override public VerificationScan.State expected(int x, int y, int z) { return state(placement, x, y, z); }
                @Override public VerificationScan.State found(int x, int y, int z) { return state(world, x, y, z); }
                @Override public void failed(int x, int y, int z, RuntimeException error) {
                    if (logged++ < 3) Reference.logger.debug("Could not verify block at {}, {}, {}", x, y, z, error);
                }
                private VerificationScan.State state(World target, int x, int y, int z) {
                    Block block = target.getBlock(x, y, z);
                    if (block.isAir(target, x, y, z)) return VerificationScan.State.AIR;
                    int meta = target.getBlockMetadata(x, y, z);
                    long id = ((long) Block.getIdFromBlock(block) << 32) | (meta & 0xffffffffL);
                    return states.computeIfAbsent(id, ignored -> {
                        String name = GameData.getBlockRegistry().getNameForObject(block);
                        return new VerificationScan.State(name == null ? "#" + Block.getIdFromBlock(block) : name, meta);
                    });
                }
            };
            running = true;
            ensureTask(mc);
        }

        private void ensureTask(Minecraft mc) {
            if (task == null && scan != null && !scan.done()) {
                task = TaskRegistry.INSTANCE.start(mc.thePlayer.getUniqueID(), mc.thePlayer.dimension, TaskRegistry.Kind.VERIFIER,
                    TaskRegistry.Backend.ANALYSIS, placement.name);
            }
        }

        private void step(Minecraft mc, long deadline) {
            if (!running || scan == null) return;
            if (System.nanoTime() >= deadline) return;
            ensureTask(mc);
            try {
                scan.step(reader, 4096, deadline);
                if (task != null) {
                    task.update(TaskRegistry.Stage.VERIFY, scan.totalChunks() - scan.remainingChunks(), scan.totalChunks(), 0, 0);
                    if (scan.done()) finishTask();
                }
            } catch (RuntimeException error) {
                Reference.logger.warn("Schematic verification stopped", error);
                pause();
                notice = "schematica.ui.verifier.failed";
            }
        }

        private void finishTask() { if (task != null) task.finish(); task = null; }
        public void pause() { running = false; finishTask(); }
        public void reset() {
            pause(); scan = null; reader = null; world = null; source = null; bounds = null; notice = "";
        }

        public String progressText() {
            if (!notice.isEmpty()) return UiTranslations.format(notice);
            if (scan == null) return UiTranslations.format("schematica.ui.verifier.ready");
            if (!running) return UiTranslations.format("schematica.ui.verifier.paused", scan.checked(), scan.total());
            if (!scan.done()) return UiTranslations.format("litematica.gui.label.schematic_verifier.status.verifying", scan.remainingChunks(), scan.totalChunks());
            return UiTranslations.format("litematica.gui.label.schematic_verifier.status.done_errors.no_diff",
                scan.count(VerificationScan.Type.WRONG_BLOCK), scan.count(VerificationScan.Type.WRONG_STATE),
                scan.count(VerificationScan.Type.MISSING), scan.count(VerificationScan.Type.EXTRA));
        }

        public String countsText() {
            if (scan == null) return "";
            String counts = UiTranslations.format("litematica.gui.label.schematic_verifier.status.done_correct_total",
                scan.count(VerificationScan.Type.CORRECT), scan.expectedBlocks());
            return scan.skipped() == 0 ? counts : counts + "  " + UiTranslations.format("schematica.ui.verifier.skipped", scan.skipped());
        }
    }
}
