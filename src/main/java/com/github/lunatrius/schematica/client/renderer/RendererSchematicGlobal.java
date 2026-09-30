package com.github.lunatrius.schematica.client.renderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent17;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3d;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.nbt.TileEntitySnapshots;
import com.github.lunatrius.schematica.reference.Constants;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class RendererSchematicGlobal {

    public static final RendererSchematicGlobal INSTANCE = new RendererSchematicGlobal();

    private final Minecraft minecraft = Minecraft.getMinecraft();
    private final Profiler profiler = this.minecraft.mcProfiler;

    private final SchematicFrustum frustum = new SchematicFrustum();
    private final Vector3d cameraPosition = new Vector3d();
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
    private final float[] projection = new float[16];
    private final float[] modelView = new float[16];
    /** Renderer chunks for the active/selected schematic (used by printer, tools). */
    public RenderBlocks renderBlocks = null;
    public final List<RendererSchematicChunk> sortedRendererSchematicChunk = new ArrayList<>();
    private final RendererSchematicChunkComparator rendererSchematicChunkComparator = new RendererSchematicChunkComparator();
    private final RenderUpdateScheduler<RendererSchematicChunk> updateScheduler = new RenderUpdateScheduler<>();

    /** Per-schematic renderer data for multi-schematic rendering. */
    private final Map<SchematicWorld, SchematicRenderData> renderDataMap = new HashMap<>();

    private RendererSchematicGlobal() {}

    private static class SchematicRenderData {
        RenderBlocks renderBlocks;
        final List<RendererSchematicChunk> chunks = new ArrayList<>();
    }

    @SubscribeEvent(priority = cpw.mods.fml.common.eventhandler.EventPriority.LOWEST)
    public void onPlaySound(PlaySoundEvent17 event) {
        if (TileEntitySnapshots.isRestoring()) event.result = null;
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        EntityPlayerSP player = this.minecraft.thePlayer;
        if (player != null) {
            ClientProxy.setPlayerData(player, event.partialTicks);

            this.profiler.startSection("schematica");

            boolean anyRendering = false;
            for (SchematicWorld sw : ClientProxy.loadedSchematics) {
                if (sw.isRendering) {
                    anyRendering = true;
                    break;
                }
            }

            if (anyRendering || ClientProxy.isRenderingGuide) {
                EntityLivingBase camera = this.minecraft.renderViewEntity;
                if (camera == null) camera = player;
                this.cameraPosition.set(
                    camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * event.partialTicks,
                    camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * event.partialTicks,
                    camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * event.partialTicks);
                captureMatrix(GL11.GL_PROJECTION_MATRIX, this.projection);
                captureMatrix(GL11.GL_MODELVIEW_MATRIX, this.modelView);
                this.frustum.update(this.projection, this.modelView);
                renderAll(event.partialTicks);
            }

            this.profiler.endSection();
        }
    }

    /** Renders all loaded schematics plus the guide overlay. */
    public void renderAll() {
        renderAll(0);
    }

    public void renderAll(float partialTicks) {
        GL11.glPushMatrix();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_BLEND);

        this.profiler.startSection("schematic");

        for (SchematicWorld sw : ClientProxy.loadedSchematics) {
            sw.setWorldTime(this.minecraft.theWorld.getWorldTime());
            sw.func_82738_a(this.minecraft.theWorld.getTotalWorldTime());
        }
        updateRenderers();

        // Render each loaded schematic
        for (SchematicWorld sw : ClientProxy.loadedSchematics) {
            if (!sw.isRendering) continue;

            SchematicRenderData data = renderDataMap.get(sw);
            if (data == null || data.chunks.isEmpty()) continue;

            GL11.glPushMatrix();

            Vector3d playerPos = this.cameraPosition.clone();
            playerPos.sub(sw.position.toVector3d());
            GL11.glTranslated(-playerPos.x, -playerPos.y, -playerPos.z);

            // Render passes
            for (int pass = 0; pass < 3; pass++) {
                boolean reverse = pass == 1 || (pass == 0 &&
                    com.github.lunatrius.schematica.handler.ConfigurationHandler.enableAlpha);
                for (int i = 0; i < data.chunks.size(); i++) {
                    int index = reverse ? data.chunks.size() - 1 - i : i;
                    data.chunks.get(index).render(pass, partialTicks);
                }
            }

            // Render entities if enabled
            if (sw.isRenderingEntities) {
                renderEntities(sw);
            }

            // Draw bounding box outline
            RenderHelper.createBuffers();
            boolean isActive = (sw == ClientProxy.schematic);
            float r = isActive ? 0.75f : 0.25f;
            float g = isActive ? 0.0f : 0.5f;
            float b = isActive ? 0.75f : 0.25f;
            RenderHelper.drawCuboidOutline(
                RenderHelper.VEC_ZERO,
                sw.dimensions(),
                RenderHelper.LINE_ALL,
                r, g, b, 0.5f);

            int quadCount = RenderHelper.getQuadCount();
            int lineCount = RenderHelper.getLineCount();
            if (quadCount > 0 || lineCount > 0) {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glLineWidth(3.0f);
                GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
                GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
                if (quadCount > 0) {
                    GL11.glVertexPointer(3, 0, RenderHelper.getQuadVertexBuffer());
                    GL11.glColorPointer(4, 0, RenderHelper.getQuadColorBuffer());
                    GL11.glDrawArrays(GL11.GL_QUADS, 0, quadCount);
                }
                if (lineCount > 0) {
                    GL11.glVertexPointer(3, 0, RenderHelper.getLineVertexBuffer());
                    GL11.glColorPointer(4, 0, RenderHelper.getLineColorBuffer());
                    GL11.glDrawArrays(GL11.GL_LINES, 0, lineCount);
                }
                GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
                GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
            }

            GL11.glPopMatrix();
        }

        this.profiler.endStartSection("guide");

        // Render guide overlay (selection box)
        if (ClientProxy.isRenderingGuide) {
            // Re-establish GL state after schematic chunk/entity rendering may have changed it
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDepthMask(true);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

            SchematicWorld activeSchematic = ClientProxy.schematic;
            Vector3d extra = new Vector3d();
            if (activeSchematic != null) {
                extra.add(activeSchematic.position.toVector3d());
            }

            GL11.glPushMatrix();
            Vector3d playerPos = this.cameraPosition.clone();
            playerPos.sub(extra);
            GL11.glTranslated(-playerPos.x, -playerPos.y, -playerPos.z);

            Vector3d start = new Vector3d();
            Vector3d end = new Vector3d();

            // --- Pass 1: Green selection box (depth-tested, occluded by blocks) ---
            RenderHelper.createBuffers();

            ClientProxy.pointMin.toVector3d(start).sub(extra);
            ClientProxy.pointMax.toVector3d(end).sub(extra).add(1, 1, 1);
            RenderHelper.drawCuboidOutline(start.toVector3f(), end.toVector3f(),
                RenderHelper.LINE_ALL, 0.0f, 0.75f, 0.0f, 0.5f);
            RenderHelper.drawCuboidSurface(start.toVector3f(), end.toVector3f(),
                RenderHelper.QUAD_ALL, 1.0f, 1.0f, 1.0f, 0.2f);

            int quadCount = RenderHelper.getQuadCount();
            int lineCount = RenderHelper.getLineCount();
            if (quadCount > 0 || lineCount > 0) {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glLineWidth(3.0f);
                GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
                GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
                if (quadCount > 0) {
                    GL11.glDepthMask(false);
                    GL11.glVertexPointer(3, 0, RenderHelper.getQuadVertexBuffer());
                    GL11.glColorPointer(4, 0, RenderHelper.getQuadColorBuffer());
                    GL11.glDrawArrays(GL11.GL_QUADS, 0, quadCount);
                    GL11.glDepthMask(true);
                }
                if (lineCount > 0) {
                    // Green lines WITH depth test — occluded by world blocks
                    GL11.glVertexPointer(3, 0, RenderHelper.getLineVertexBuffer());
                    GL11.glColorPointer(4, 0, RenderHelper.getLineColorBuffer());
                    GL11.glDrawArrays(GL11.GL_LINES, 0, lineCount);
                }
                GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
                GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
            }

            // --- Pass 2: Red (pointA) and Blue (pointB) boxes (no depth test, always visible) ---
            RenderHelper.createBuffers();

            ClientProxy.pointA.toVector3d(start).sub(extra);
            end.set(start).add(1, 1, 1);
            RenderHelper.drawCuboidOutline(start.toVector3f(), end.toVector3f(),
                RenderHelper.LINE_ALL, 0.75f, 0.0f, 0.0f, 0.5f);
            RenderHelper.drawCuboidSurface(start.toVector3f(), end.toVector3f(),
                RenderHelper.QUAD_ALL, 0.75f, 0.0f, 0.0f, 0.25f);

            ClientProxy.pointB.toVector3d(start).sub(extra);
            end.set(start).add(1, 1, 1);
            RenderHelper.drawCuboidOutline(start.toVector3f(), end.toVector3f(),
                RenderHelper.LINE_ALL, 0.0f, 0.0f, 0.75f, 0.5f);
            RenderHelper.drawCuboidSurface(start.toVector3f(), end.toVector3f(),
                RenderHelper.QUAD_ALL, 0.0f, 0.0f, 0.75f, 0.25f);

            quadCount = RenderHelper.getQuadCount();
            lineCount = RenderHelper.getLineCount();
            if (quadCount > 0 || lineCount > 0) {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glLineWidth(3.0f);
                GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
                GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
                if (quadCount > 0) {
                    GL11.glDepthMask(false);
                    GL11.glVertexPointer(3, 0, RenderHelper.getQuadVertexBuffer());
                    GL11.glColorPointer(4, 0, RenderHelper.getQuadColorBuffer());
                    GL11.glDrawArrays(GL11.GL_QUADS, 0, quadCount);
                    GL11.glDepthMask(true);
                }
                if (lineCount > 0) {
                    // Red/Blue lines WITHOUT depth test — always visible
                    GL11.glDisable(GL11.GL_DEPTH_TEST);
                    GL11.glVertexPointer(3, 0, RenderHelper.getLineVertexBuffer());
                    GL11.glColorPointer(4, 0, RenderHelper.getLineColorBuffer());
                    GL11.glDrawArrays(GL11.GL_LINES, 0, lineCount);
                    GL11.glEnable(GL11.GL_DEPTH_TEST);
                }
                GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
                GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
            }

            GL11.glPopMatrix();
        }

        this.profiler.endSection();

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glPopMatrix();
    }

    private void updateRenderers() {
        List<List<RendererSchematicChunk>> groups = new ArrayList<>();
        for (SchematicWorld schematic : ClientProxy.loadedSchematics) {
            if (!schematic.isRendering) continue;
            SchematicRenderData data = renderDataMap.get(schematic);
            if (data == null) continue;
            double offsetX = schematic.position.x - this.cameraPosition.x;
            double offsetY = schematic.position.y - this.cameraPosition.y;
            double offsetZ = schematic.position.z - this.cameraPosition.z;
            for (RendererSchematicChunk chunk : data.chunks) {
                AxisAlignedBB box = chunk.getBoundingBox();
                chunk.isInFrustrum = this.frustum.isVisible(
                    box.minX + offsetX, box.minY + offsetY, box.minZ + offsetZ,
                    box.maxX + offsetX, box.maxY + offsetY, box.maxZ + offsetZ);
            }
            this.rendererSchematicChunkComparator.setPosition(schematic.position, this.cameraPosition);
            data.chunks.sort(this.rendererSchematicChunkComparator);
            groups.add(data.chunks);
        }
        long deadline = System.nanoTime() + 4_000_000L;
        updateScheduler.update(groups, 3, () -> System.nanoTime() < deadline,
            RendererSchematicChunk::getDirty, RendererSchematicChunk::updateRenderer);
    }

    public void selectSchematic(SchematicWorld schematic) {
        if (schematic != null && !renderDataMap.containsKey(schematic)) {
            createRendererSchematicChunks(schematic);
        } else {
            rebuildLegacyChunkList();
        }
    }

    public void createRendererSchematicChunks(SchematicWorld schematic) {
        int width = (schematic.getWidth() - 1) / Constants.SchematicChunk.WIDTH + 1;
        int height = (schematic.getHeight() - 1) / Constants.SchematicChunk.HEIGHT + 1;
        int length = (schematic.getLength() - 1) / Constants.SchematicChunk.LENGTH + 1;

        // Remove old render data for this schematic
        destroyRendererSchematicChunksFor(schematic);

        SchematicRenderData data = new SchematicRenderData();
        data.renderBlocks = new RenderBlocks(schematic);
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    data.chunks.add(new RendererSchematicChunk(schematic, data.renderBlocks, x, y, z));
                }
            }
        }
        renderDataMap.put(schematic, data);

        // Also update the legacy fields for the active schematic
        if (schematic == ClientProxy.schematic) {
            rebuildLegacyChunkList();
        }
    }

    /** Public method to remove render data for a single schematic (used by Instances GUI). */
    public void removeRendererSchematicChunks(SchematicWorld schematic) {
        destroyRendererSchematicChunksFor(schematic);
    }

    private void destroyRendererSchematicChunksFor(SchematicWorld schematic) {
        SchematicRenderData data = renderDataMap.remove(schematic);
        if (data != null) {
            for (RendererSchematicChunk chunk : data.chunks) {
                chunk.delete();
            }
            data.chunks.clear();
            data.renderBlocks = null;
        }
        rebuildLegacyChunkList();
    }

    public void destroyRendererSchematicChunks() {
        for (SchematicRenderData data : renderDataMap.values()) {
            for (RendererSchematicChunk chunk : data.chunks) {
                chunk.delete();
            }
            data.chunks.clear();
        }
        renderDataMap.clear();
        this.renderBlocks = null;
        this.sortedRendererSchematicChunk.clear();
    }

    /** Rebuilds the legacy sortedRendererSchematicChunk list from the active schematic's data. */
    private void rebuildLegacyChunkList() {
        this.sortedRendererSchematicChunk.clear();
        this.renderBlocks = null;
        SchematicWorld active = ClientProxy.schematic;
        if (active != null) {
            SchematicRenderData data = renderDataMap.get(active);
            if (data != null) {
                this.sortedRendererSchematicChunk.addAll(data.chunks);
                this.renderBlocks = data.renderBlocks;
            }
        }
    }

    public void refresh() {
        for (SchematicRenderData data : renderDataMap.values()) {
            for (RendererSchematicChunk chunk : data.chunks) {
                chunk.setDirty();
            }
        }
    }

    public void markDirtyAllSchematics(final int wx0, final int wy0, final int wz0, final int wx1, final int wy1, final int wz1) {
        for (Map.Entry<SchematicWorld, SchematicRenderData> entry : renderDataMap.entrySet()) {
            SchematicWorld sw = entry.getKey();
            SchematicRenderData data = entry.getValue();
            final AxisAlignedBB boundingBox = AxisAlignedBB.getBoundingBox(
                wx0 - sw.position.x,
                wy0 - sw.position.y,
                wz0 - sw.position.z,
                wx1 - sw.position.x,
                wy1 - sw.position.y,
                wz1 - sw.position.z);
            for (RendererSchematicChunk renderer : data.chunks) {
                if (!renderer.getDirty() && renderer.getBoundingBox().intersectsWith(boundingBox)) {
                    renderer.setDirty();
                }
            }
        }
    }

    /** Refreshes only the chunks for a specific schematic. */
    public void refresh(SchematicWorld schematic) {
        SchematicRenderData data = renderDataMap.get(schematic);
        if (data != null) {
            for (RendererSchematicChunk chunk : data.chunks) {
                chunk.setDirty();
            }
        }
    }

    private void captureMatrix(int matrix, float[] values) {
        this.matrixBuffer.clear();
        GL11.glGetFloat(matrix, this.matrixBuffer);
        this.matrixBuffer.position(0);
        this.matrixBuffer.get(values);
    }

    private void renderEntities(SchematicWorld schematic) {
        RenderManager renderManager = RenderManager.instance;
        for (Entity entity : schematic.getEntities()) {
            net.minecraft.world.World originalWorld = entity.worldObj;
            GL11.glPushMatrix();
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            try {
                entity.worldObj = schematic;
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
                GL11.glColor4f(1, 1, 1, 1);
                renderManager.renderEntityWithPosYaw(entity, entity.posX, entity.posY, entity.posZ,
                    entity.rotationYaw, 1.0f);
            } catch (Exception ignored) {
            } finally {
                entity.worldObj = originalWorld;
                GL11.glPopAttrib();
                GL11.glPopMatrix();
            }
        }
    }
}
