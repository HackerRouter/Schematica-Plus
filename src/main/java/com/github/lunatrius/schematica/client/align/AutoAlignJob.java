// Auto align, searched a few milliseconds per client tick, by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.align;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.client.printer.SchematicPrinter;
import com.github.lunatrius.schematica.client.renderer.RendererSchematicGlobal;
import com.github.lunatrius.schematica.client.world.PlacementSettings;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.client.WorldHandler;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;

/** One auto align search at a time: planning and voting on a worker thread, world reads on the client thread. */
public final class AutoAlignJob {
    private static final long BUDGET_NANOS = 3_000_000L;
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Schematica Plus auto align");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });
    private static AutoAlignJob current;

    private enum Phase { PLANNING, SCANNING, VOTING, CHECKING }

    private final SchematicWorld placement;
    private final World world;
    private final String name;
    private final AutoAlign.Source source;
    private final AutoAlign.Target target;
    private final int revision;
    private Phase phase = Phase.PLANNING;
    private Future<AutoAlign.Plan> planning;
    private Future<List<AutoAlign.Candidate>> voting;
    private AutoAlign.Plan plan;
    private final Map<Block, Integer> index = new IdentityHashMap<>();
    private final List<List<Long>> hits = new ArrayList<>();
    private final List<int[]> chunks = new ArrayList<>();
    private int nextChunk, nextCandidate;
    private List<AutoAlign.Candidate> candidates;
    private AutoAlign.Result best, baseline;

    private AutoAlignJob(SchematicWorld placement, World world, int x, int z) {
        this.placement = placement;
        this.world = world;
        this.name = placement.name;
        this.revision = placement.placementRevision();
        ISchematic schematic = placement.getSchematic();
        int width = schematic.getWidth(), height = schematic.getHeight(), length = schematic.getLength();
        this.source = new AutoAlign.Source() {
            @Override public int width() { return width; }
            @Override public int height() { return height; }
            @Override public int length() { return length; }
            @Override public Object block(int bx, int by, int bz) {
                Block block = schematic.getBlock(bx, by, bz);
                return block == null || block == Blocks.air ? null : block;
            }
        };
        this.target = new AutoAlign.Target() {
            @Override public Object block(int bx, int by, int bz) { return world.getBlock(bx, by, bz); }
            @Override public boolean loaded(int bx, int bz) { return !(world.getChunkFromChunkCoords(bx >> 4, bz >> 4) instanceof EmptyChunk); }
        };
        int radius = Math.max(4, (Math.max(width, length) + 15) / 16 + 1), cx = x >> 4, cz = z >> 4;
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) chunks.add(new int[] {cx + dx, cz + dz});
        chunks.sort((a, b) -> Integer.compare((a[0] - cx) * (a[0] - cx) + (a[1] - cz) * (a[1] - cz), (b[0] - cx) * (b[0] - cx) + (b[1] - cz) * (b[1] - cz)));
        planning = WORKER.submit(() -> AutoAlign.plan(source, terrain()));
    }

    private static Set<Object> terrain() {
        return new HashSet<>(Arrays.<Object>asList(Blocks.stone, Blocks.dirt, Blocks.grass, Blocks.mycelium, Blocks.gravel, Blocks.sand,
            Blocks.sandstone, Blocks.clay, Blocks.water, Blocks.flowing_water, Blocks.lava, Blocks.flowing_lava, Blocks.netherrack,
            Blocks.end_stone, Blocks.snow, Blocks.snow_layer, Blocks.ice, Blocks.packed_ice, Blocks.tallgrass, Blocks.double_plant,
            Blocks.deadbush, Blocks.bedrock, Blocks.hardened_clay, Blocks.stained_hardened_clay));
    }

    /** Starts searching for the placement's build around the player, cancelling a running search. */
    public static void start(SchematicWorld placement) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || placement == null) return;
        if (placement.placementSettings().locked) {
            message(EnumChatFormatting.RED, PlacementSettings.LOCKED_MESSAGE);
            return;
        }
        current = new AutoAlignJob(placement, mc.theWorld, MathHelper.floor_double(mc.thePlayer.posX), MathHelper.floor_double(mc.thePlayer.posZ));
        message(EnumChatFormatting.GRAY, "schematica.message.auto_align.started", placement.name);
    }

    public static boolean running(SchematicWorld placement) { return current != null && current.placement == placement; }

    public static void tick(Minecraft mc) {
        AutoAlignJob job = current;
        if (job == null) return;
        if (mc.theWorld != job.world || !ClientProxy.loadedSchematics.contains(job.placement) || job.placement.placementRevision() != job.revision) {
            current = null;
            return;
        }
        try {
            if (job.step()) current = null;
        } catch (RuntimeException e) {
            Reference.logger.error("Auto align failed", e);
            current = null;
            message(EnumChatFormatting.RED, "schematica.message.auto_align.failed", job.name);
        }
    }

    /** Runs a slice of the search; true when it is finished. */
    private boolean step() throws RuntimeException {
        long deadline = System.nanoTime() + BUDGET_NANOS;
        switch (phase) {
            case PLANNING:
                if (!planning.isDone()) return false;
                plan = take(planning);
                if (plan == null || plan.isEmpty()) return fail();
                for (int i = 0; i < plan.ids.size(); i++) {
                    index.put((Block) plan.ids.get(i), i);
                    hits.add(new ArrayList<>());
                }
                phase = Phase.SCANNING;
                return false;
            case SCANNING:
                while (nextChunk < chunks.size() && System.nanoTime() < deadline) scan(chunks.get(nextChunk++));
                if (nextChunk < chunks.size()) return false;
                List<long[]> found = new ArrayList<>();
                for (List<Long> list : hits) {
                    long[] array = new long[list.size()];
                    for (int i = 0; i < array.length; i++) array[i] = list.get(i);
                    found.add(array);
                }
                voting = WORKER.submit(() -> AutoAlign.vote(source.width(), source.length(), plan, found));
                phase = Phase.VOTING;
                return false;
            case VOTING:
                if (!voting.isDone()) return false;
                candidates = take(voting);
                if (candidates == null) candidates = Collections.emptyList();
                baseline = AutoAlign.score(source, target, plan, AutoAlign.current(placement.position.x, placement.position.y, placement.position.z));
                phase = Phase.CHECKING;
                return false;
            default:
                while (nextCandidate < candidates.size() && System.nanoTime() < deadline) {
                    AutoAlign.Result result = AutoAlign.score(source, target, plan, candidates.get(nextCandidate++));
                    if (result.better(best)) best = result;
                }
                if (nextCandidate < candidates.size()) return false;
                return finish();
        }
    }

    private void scan(int[] column) {
        Chunk chunk = world.getChunkFromChunkCoords(column[0], column[1]);
        if (chunk instanceof EmptyChunk) return;
        int bx = column[0] << 4, bz = column[1] << 4;
        for (ExtendedBlockStorage storage : chunk.getBlockStorageArray()) {
            if (storage == null || storage.isEmpty()) continue;
            int by = storage.getYLocation();
            for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                Integer k = index.get(storage.getBlockByExtId(x, y, z));
                if (k != null && hits.get(k).size() < AutoAlign.MAX_HITS_PER_ID) hits.get(k).add(AutoAlign.pack(bx + x, by + y, bz + z));
            }
        }
    }

    private boolean finish() {
        if (best == null || !best.confident()) return fail();
        if (baseline.confident() && !best.better(baseline)) {
            message(EnumChatFormatting.GREEN, "schematica.message.auto_align.unchanged", name, baseline.percent());
            return true;
        }
        AutoAlign.Candidate found = best.candidate;
        try {
            for (int i = 0; i < found.steps.length(); i++) {
                if (found.steps.charAt(i) == 'Y') placement.rotate(ForgeDirection.UP);
                else placement.flip(ForgeDirection.EAST);
            }
            placement.moveMinimumTo(found.x, found.y, found.z);
        } finally {
            RendererSchematicGlobal.INSTANCE.createRendererSchematicChunks(placement);
            if (ClientProxy.schematic == placement) SchematicPrinter.INSTANCE.refresh();
            WorldHandler.INSTANCE.saveSession();
        }
        boolean moved = placement.position.x == found.x && placement.position.y == found.y && placement.position.z == found.z;
        message(moved ? EnumChatFormatting.GREEN : EnumChatFormatting.GOLD,
            moved ? "schematica.message.auto_align.done" : "schematica.message.auto_align.coordinate_locks", name, best.percent());
        return true;
    }

    private boolean fail() {
        message(EnumChatFormatting.RED, "schematica.message.auto_align.failed", name);
        return true;
    }

    private static <T> T take(Future<T> future) {
        try {
            return future.get();
        } catch (Exception e) {
            Reference.logger.error("Auto align failed", e);
            return null;
        }
    }

    private static void message(EnumChatFormatting color, String key, Object... arguments) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        ChatComponentTranslation text = new ChatComponentTranslation(key, arguments);
        text.getChatStyle().setColor(color);
        mc.thePlayer.addChatMessage(text);
    }
}
