package com.github.lunatrius.schematica.client.renderer;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;

import com.github.lunatrius.schematica.client.world.GridPlacements;
import com.github.lunatrius.schematica.client.world.Placements;
import com.github.lunatrius.schematica.client.world.RenderLayerRange;
import com.github.lunatrius.schematica.client.world.RenderLayerSettings;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.VisualSettings;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.client.verifier.VerifierSmoke;
import com.github.lunatrius.schematica.world.storage.Schematic;

import org.lwjgl.opengl.GL11;
import org.lwjgl.BufferUtils;

@Mod(modid="render_smoke", name="Render Smoke", version="1", dependencies="required-after:schematica_plus", acceptableRemoteVersions="*")
public class RenderSmoke {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Block custom = new Block(Material.rock) {
        @Override public IIcon getIcon(int side, int meta) { return Blocks.stone.getIcon(side, meta); }
    };
    private boolean started, prepared, finished;
    private int sortStage;
    private CountingChunk sortingChunk;
    private SchematicWorld sortingWorld;
    private SortedGeometryCache sortingCache;
    private int[] firstOrder;
    private VerifierSmoke verifier;
    private int ticks, x, z;
    private static final int Y = 161;

    @Mod.EventHandler public void prepare(FMLPreInitializationEvent event) {
        custom.setBlockName("render_smoke_stone").setBlockTextureName("stone");
        GameRegistry.registerBlock(custom, "custom_stone");
    }

    @Mod.EventHandler public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        try {
            if (!started) {
                started = true;
                mc.gameSettings.renderDistanceChunks = 3;
                mc.gameSettings.pauseOnLostFocus = false;
                mc.launchIntegratedServer("RenderSmoke", "RenderSmoke",
                    new WorldSettings(1, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
                return;
            }
            if (++ticks > 1200) throw new AssertionError("Timed out waiting for loaded chunks");
            if (mc.thePlayer == null || mc.theWorld == null) return;
            x = ((int) Math.floor(mc.thePlayer.posX) >> 4) * 16 + 2;
            z = ((int) Math.floor(mc.thePlayer.posZ) >> 4) * 16 + 2;
            if (!prepared) {
                if (!LoadedAirSections.containsOnlyAir(mc.theWorld, x - 1, Y - 1, z - 1, x + 48, Y + 40, z + 20)) return;
                prepared = true;
                RenderLayerSettings.RANGE.load(null);
                VisualSettings.fakeLighting = true;
                VisualSettings.fakeLightLevel = 15;
                while (GL11.glGetError() != GL11.GL_NO_ERROR) {}
                translations();
                invalidation();
                grids();
                com.github.lunatrius.schematica.client.printer.PrinterBatchSmoke.run(mc, x, (int) mc.thePlayer.boundingBox.minY + 8, z);
                com.github.lunatrius.schematica.client.printer.PrinterDestroySmoke.run(mc, x, Y, z + 8);
                sortingWorld = schematic(4, 4, 4, Blocks.stained_glass);
                for (int sx = 0; sx < 4; sx++) sortingWorld.setBlock(sx, 0, 0, Blocks.stained_glass, sx + 1, 0);
                sortingChunk = new CountingChunk(sortingWorld);
                check(sortingChunk.updateRenderer(), "sorted geometry initial build");
                sortingCache = (SortedGeometryCache) field(RendererSchematicChunk.class, sortingChunk, "sortedGeometry");
                verifier = new VerifierSmoke(mc, x, Y, z + 16);
                return;
            }
            boolean sorted = sorting();
            boolean verified = verifier.tick();
            if (!sorted || !verified) return;
            finished = true;
            check(GL11.glGetError() == GL11.GL_NO_ERROR, "OpenGL error");
            Files.write(Paths.get("render-smoke-result.txt"), "PASSED".getBytes(StandardCharsets.UTF_8));
            System.out.println("RENDER_SMOKE_PASSED: cached translation, air witnesses, lighting, layers, edits, custom blocks, dirty index, grid identities, background verification, native transparent sorting");
        } catch (Throwable failure) {
            finished = true;
            failure.printStackTrace();
            System.out.println("RENDER_SMOKE_FAILED");
        } finally {
            if (finished) {
                if (sortingChunk != null) sortingChunk.delete();
                if (verifier != null) verifier.close();
                GridPlacements.INSTANCE.clear();
                Placements.clear();
                mc.shutdown();
            }
        }
    }

    private boolean sorting() throws Exception {
        if (sortStage == 3) return true;
        SortedGeometryCache.beginFrame();
        double camera = sortStage == 0 ? 20 : -20;
        sortingChunk.updateSortPosition(x + camera, Y + camera, z + camera);
        SortedGeometryCache.update();
        Object[] entries = (Object[]) field(SortedGeometryCache.class, sortingCache, "passes");
        Object entry = entries[1];
        java.util.concurrent.Future<?> job = (java.util.concurrent.Future<?>) field(entry.getClass(), entry, "job");
        if (job != null) job.get(5, java.util.concurrent.TimeUnit.SECONDS);
        SortedGeometryCache.update();
        if (sortingCache.list(1, 0) == 0) return false;
        int[] order = (int[]) field(entry.getClass(), entry, "order");
        if (order == null) return false;
        if (sortStage == 0) {
            firstOrder = order.clone();
            pixels(sortingChunk, sortingWorld);
            sortStage = 1;
            return false;
        }
        if (sortStage == 1) {
            if (Arrays.equals(order, firstOrder)) return false;
            check(sortingChunk.passes == 3, "camera sorting must not call block renderers again");
            pixels(sortingChunk, sortingWorld);
            int list = sortingCache.list(1, 0);
            sortingChunk.setDirty(); sortingChunk.updateRenderer();
            check(entries[1] == entry && sortingCache.list(1, 0) == list, "identical geometry retains its sorted display list");
            SortedGeometryCache.beginFrame();
            sortingChunk.updateSortPosition(x + 40, Y + 40, z + 40);
            SortedGeometryCache.update();
            check(field(entry.getClass(), entry, "job") != null, "visible geometry requests sorting");
            SortedGeometryCache.beginFrame(); SortedGeometryCache.update();
            check(field(entry.getClass(), entry, "job") == null, "offscreen geometry releases background work slots");
            SortedGeometryCache.beginFrame();
            sortingChunk.updateSortPosition(x + 40, Y + 40, z + 40);
            SortedGeometryCache.update();
            check(field(entry.getClass(), entry, "job") != null, "visible sorting resumes after cancellation");
            SortedGeometryCache.suspend();
            check(field(entry.getClass(), entry, "job") == null, "globally hidden rendering releases background work slots");
            sortingWorld.setBlock(0, 0, 0, Blocks.stone, 0, 0);
            sortingChunk.setDirty(); sortingChunk.updateRenderer();
            check((Boolean) field(entry.getClass(), entry, "deleted"), "old sorted geometry is discarded after edits");
            check(field(entry.getClass(), entry, "geometry") == null, "discarded geometry releases its vertex storage");
            sortStage = 2;
            return false;
        }
        pixels(sortingChunk, sortingWorld);
        sortingChunk.delete(); sortingChunk = null;
        check(((Number) field(SortedGeometryCache.class, null, "retained")).longValue() == 0, "all geometry cache memory released");
        System.out.println("RENDER_SMOKE_SORTING: opposite camera orders, no model rebuild, stale geometry discarded, cache memory released");
        sortStage = 3;
        return true;
    }

    private static Object field(Class<?> type, Object instance, String name) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); return field.get(instance);
    }

    private SchematicWorld schematic(int width, int height, int length, Block block) {
        Schematic source = new Schematic(null, width, height, length);
        for (int sy = 0; sy < height; sy++) for (int sz = 0; sz < length; sz++) for (int sx = 0; sx < width; sx++) {
            source.setBlock(sx, sy, sz, block, 0);
        }
        SchematicWorld world = new SchematicWorld(source);
        world.name = "Render Smoke";
        world.position.set(x, Y, z);
        world.isRendering = true;
        return world;
    }

    private void translations() {
        SchematicWorld world = schematic(4, 4, 4, Blocks.stone);
        CountingChunk chunk = new CountingChunk(world);
        try {
            check(chunk.updateRenderer() && chunk.passes == 3, "initial compilation");
            for (int i = 0; i < 64; i++) {
                world.moveOriginTo(x + 1 + i % 8, Y, z);
                check(!chunk.updateRenderer() && !chunk.getDirty(), "translation reuse " + i);
                check(world.position.x == x + 1 + i % 8, "immediate logical position");
            }
            check(chunk.passes == 3, "64 moves must not rebuild geometry");
            System.out.println("RENDER_SMOKE_COUNTS: 64 air translations, 0 additional section builds, 3 total render passes");
            byte[] cached = pixels(chunk, world);
            chunk.setDirty();
            check(chunk.updateRenderer(), "explicit invalidation");
            check(Arrays.equals(cached, pixels(chunk, world)), "cached translation matches a fresh rendered image");
            world.moveOriginTo(x, Y, z);
            check(!chunk.updateRenderer(), "return to loaded air");
            mc.theWorld.setBlock(x + 1, Y, z, Blocks.stone, 0, 0);
            world.moveOriginTo(x + 1, Y, z);
            check(chunk.updateRenderer(), "destination obstruction");
            mc.theWorld.setBlockToAir(x + 1, Y, z);
            world.moveOriginTo(x, Y, z);
            check(chunk.updateRenderer(), "previous obstruction witness");
            world.moveOriginTo(x + 1, Y, z);
            check(!chunk.updateRenderer(), "air reuse resumes");
            VisualSettings.fakeLighting = false;
            world.moveOriginTo(x + 2, Y, z);
            check(chunk.updateRenderer(), "world lighting");
            world.moveOriginTo(x + 3, Y, z);
            check(chunk.updateRenderer(), "world lighting on subsequent moves");
            VisualSettings.fakeLighting = true;
            world.moveOriginTo(x, Y, z);
            check(chunk.updateRenderer(), "switch to fixed lighting");
            VisualSettings.fakeLightLevel = 7;
            world.moveOriginTo(x + 1, Y, z);
            check(chunk.updateRenderer(), "fixed light level change");
            VisualSettings.fakeLightLevel = 15;
            chunk.setDirty(); chunk.updateRenderer();
            RenderLayerSettings.RANGE.setMode(RenderLayerRange.Mode.SINGLE_LAYER);
            RenderLayerSettings.RANGE.setValue(false, Y + 1);
            check(chunk.updateRenderer(), "global slice change");
            world.moveOriginTo(x + 2, Y, z);
            check(!chunk.updateRenderer(), "translation parallel to slice");
            cached = pixels(chunk, world);
            chunk.setDirty(); chunk.updateRenderer();
            check(Arrays.equals(cached, pixels(chunk, world)), "cached slice matches a fresh rendered image");
            world.moveOriginTo(x + 2, Y + 1, z);
            check(chunk.updateRenderer(), "translation across fixed world slice");
            RenderLayerSettings.RANGE.load(null);
            chunk.updateRenderer();
            world.isRenderingLayer = true;
            world.renderingLayer = 2;
            check(chunk.updateRenderer(), "local slice");
            world.isRenderingLayer = false;
            chunk.updateRenderer();
            world.setBlock(0, 0, 0, Blocks.glass, 0, 0);
            world.moveOriginTo(x + 3, Y, z);
            check(chunk.updateRenderer(), "content revision");
            world.moveOriginTo(29000000, Y, 29000000);
            check(chunk.updateRenderer(), "unloaded destination");
            check(!LoadedAirSections.containsOnlyAir(mc.theWorld, 29000000, Y, 29000000, 29000004, Y + 4, 29000004), "empty chunk is not loaded air");
            world.moveOriginTo(x, Y, z);
            check(chunk.updateRenderer(), "unloaded previous witness");
        } finally { chunk.delete(); }
        for (Block block : new Block[] {custom, Blocks.chest}) {
            world = schematic(1, 1, 1, block);
            chunk = new CountingChunk(world);
            try {
                check(chunk.updateRenderer(), "special initial build");
                world.moveOriginTo(x + 1, Y, z);
                check(chunk.updateRenderer(), "custom and tile renderers retain normal rebuilding");
            } finally { chunk.delete(); }
        }
    }

    private void invalidation() throws Exception {
        SchematicWorld world = schematic(32, 1, 1, Blocks.stone);
        RendererSchematicGlobal global = RendererSchematicGlobal.INSTANCE;
        global.createRendererSchematicChunks(world);
        try {
            List<RendererSchematicChunk> chunks = chunks(data(world));
            RendererSchematicChunk first = chunks.get(0), second = chunks.get(1);
            first.updateRenderer(); second.updateRenderer();
            Collections.reverse(chunks);
            global.markDirtyAllSchematics(x + 16, Y, z, x + 16, Y, z);
            check(!first.getDirty() && second.getDirty(), "exact boundary after sorting");
            second.updateRenderer();
            global.markDirtyAllSchematics(x + 15, Y, z, x + 17, Y, z);
            check(first.getDirty() && second.getDirty(), "neighbor halo");
            first.updateRenderer(); second.updateRenderer();
            global.markDirtyAllSchematics(x - 100, Y, z, x - 50, Y, z);
            check(!first.getDirty() && !second.getDirty(), "unrelated update");
        } finally { global.removeRendererSchematicChunks(world); }
    }

    private void grids() throws Exception {
        SchematicWorld base = schematic(4, 4, 4, Blocks.stone);
        Placements.LIBRARY.create(Placements.LIBRARY.addMemory(new File("render-smoke.schemplus"), null), (source, previous) -> base);
        base.grid.setSize(new int[] {16, 16, 16});
        base.grid.setRepeatPositive(new int[] {2, 0, 0});
        base.grid.toggleEnabled();
        GridPlacements.INSTANCE.tick(mc);
        List<SchematicWorld> copies = GridPlacements.INSTANCE.copiesOf(base);
        check(copies.size() == 2, "initial grid");
        Map<SchematicWorld, Object> renderers = new IdentityHashMap<>();
        Map<SchematicWorld, int[]> origins = new IdentityHashMap<>();
        for (SchematicWorld copy : copies) {
            renderers.put(copy, data(copy));
            origins.put(copy, new int[] {copy.position.x, copy.position.y, copy.position.z});
        }
        base.moveOriginTo(x + 1, Y + 2, z + 3);
        GridPlacements.INSTANCE.tick(mc);
        List<SchematicWorld> moved = GridPlacements.INSTANCE.copiesOf(base);
        check(moved.size() == 2 && moved.containsAll(copies), "grid copy identities survive translation");
        for (SchematicWorld copy : moved) {
            int[] previous = origins.get(copy);
            check(data(copy) == renderers.get(copy), "grid render cache identity");
            check(copy.position.x == previous[0] + 1 && copy.position.y == previous[1] + 2 && copy.position.z == previous[2] + 3,
                "grid logical coordinates update in the same tick");
        }
        base.setBlock(0, 0, 0, Blocks.glass, 0, 0);
        GridPlacements.INSTANCE.tick(mc);
        for (SchematicWorld copy : GridPlacements.INSTANCE.copiesOf(base)) {
            check(!copies.contains(copy) && copy.getBlock(0, 0, 0) == Blocks.glass, "content edit rebuilds copies");
        }
        base.moveOriginTo(x + 10000, Y, z);
        GridPlacements.INSTANCE.tick(mc);
        check(GridPlacements.INSTANCE.copiesOf(base).isEmpty(), "copies leaving the loaded area are removed");
        base.moveOriginTo(x, Y, z);
        GridPlacements.INSTANCE.tick(mc);
        check(GridPlacements.INSTANCE.copiesOf(base).size() == 2, "copies entering the area are created");
        base.grid.toggleEnabled();
        GridPlacements.INSTANCE.tick(mc);
        check(GridPlacements.INSTANCE.copiesOf(base).isEmpty(), "disabled grid is removed");
        Placements.LIBRARY.remove(base);
    }

    private Object data(SchematicWorld world) throws Exception {
        Field field = RendererSchematicGlobal.class.getDeclaredField("renderDataMap");
        field.setAccessible(true);
        Object result = ((Map<?, ?>) field.get(RendererSchematicGlobal.INSTANCE)).get(world);
        check(result != null, "render data exists");
        return result;
    }

    private byte[] pixels(RendererSchematicChunk chunk, SchematicWorld world) {
        int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        Framebuffer target = new Framebuffer(128, 128, true);
        GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
        GL11.glLoadIdentity(); GL11.glOrtho(-12, 12, -12, 12, -100, 100);
        GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix(); GL11.glLoadIdentity();
        try {
            target.setFramebufferColor(0, 0, 0, 1);
            target.framebufferClear(); target.bindFramebuffer(true);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glRotatef(30, 1, 0, 0); GL11.glRotatef(-40, 0, 1, 0);
            GL11.glTranslated(world.position.x - x - 2, world.position.y - Y - 2, world.position.z - z - 2);
            VisualSettings.frameBlocks = true;
            VisualSettings.frameOverlay = true;
            VisualSettings.frameSchematic = true;
            chunk.isInFrustrum = true;
            for (int pass = 0; pass < 3; pass++) chunk.render(pass);
            ByteBuffer buffer = BufferUtils.createByteBuffer(128 * 128 * 4);
            GL11.glReadPixels(0, 0, 128, 128, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
            byte[] result = new byte[buffer.capacity()]; buffer.get(result);
            int visible = 0;
            for (int i = 0; i < result.length; i += 4) if (result[i] != 0 || result[i + 1] != 0 || result[i + 2] != 0) visible++;
            check(visible > 20, "nonempty rendered image");
            return result;
        } finally {
            target.deleteFramebuffer(); mc.getFramebuffer().bindFramebuffer(true);
            GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
            GL11.glMatrixMode(mode); GL11.glPopAttrib();
        }
    }

    @SuppressWarnings("unchecked") private List<RendererSchematicChunk> chunks(Object data) throws Exception {
        Field field = data.getClass().getDeclaredField("chunks");
        field.setAccessible(true);
        return (List<RendererSchematicChunk>) field.get(data);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class CountingChunk extends RendererSchematicChunk {
        int passes;
        CountingChunk(SchematicWorld world) { super(world, new RenderBlocks(world), 0, 0, 0); }
        @Override public void renderBlocks(int pass, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            passes++;
            super.renderBlocks(pass, minX, minY, minZ, maxX, maxY, maxZ);
        }
    }
}
