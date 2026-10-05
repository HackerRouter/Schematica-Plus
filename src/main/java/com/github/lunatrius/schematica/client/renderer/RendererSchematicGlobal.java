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
import com.github.lunatrius.schematica.handler.VisualSettings;
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
    private final RenderBudget budget = new RenderBudget();

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
            for (SchematicWorld sw : ClientProxy.visiblePlacements()) {
                if (sw.isEnabled()) {
                    anyRendering = true;
                    break;
                }
            }

            if (anyRendering && this.budget.frame(System.nanoTime()) && this.minecraft.ingameGUI != null) {
                this.minecraft.ingameGUI.func_110326_a(com.github.lunatrius.schematica.client.gui.framework.UiTranslations.format(
                    "schematica.message.render_distance_reduced", RenderBudget.minFps), false);
            }
            if (VisualSettings.rendering && (anyRendering || ClientProxy.isRenderingGuide)) {
                VisualSettings.beginFrame();
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
                VerifierOverlayRenderer.render(this.minecraft, event.partialTicks, this.cameraPosition.x, this.cameraPosition.y, this.cameraPosition.z);
                RebuildOverlayRenderer.render(this.minecraft, event.partialTicks, this.cameraPosition.x, this.cameraPosition.y, this.cameraPosition.z);
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

        for (SchematicWorld sw : ClientProxy.visiblePlacements()) {
            sw.setWorldTime(this.minecraft.theWorld.getWorldTime());
            sw.func_82738_a(this.minecraft.theWorld.getTotalWorldTime());
        }
        updateRenderers();

        // Render each loaded schematic
        for (SchematicWorld sw : ClientProxy.visiblePlacements()) {
            if (!sw.isEnabled()) continue;

            SchematicRenderData data = renderDataMap.get(sw);
            if (data == null || data.chunks.isEmpty()) continue;

            GL11.glPushMatrix();

            Vector3d playerPos = this.cameraPosition.clone();
            playerPos.sub(sw.position.toVector3d());
            GL11.glTranslated(-playerPos.x, -playerPos.y, -playerPos.z);

            // Render passes
            for (int pass = 0; sw.isRenderingEnabled() && pass < 3; pass++) {
                boolean reverse = pass == 1 || (pass == 0 &&
                    com.github.lunatrius.schematica.handler.ConfigurationHandler.enableAlpha);
                for (int i = 0; i < data.chunks.size(); i++) {
                    int index = reverse ? data.chunks.size() - 1 - i : i;
                    data.chunks.get(index).render(pass, partialTicks);
                }
            }

            // Render entities if enabled
            if (sw.isRenderingEnabled() && sw.isRenderingEntities && VisualSettings.frameSchematic && VisualSettings.renderEntities) {
                renderEntities(sw);
            }

            GL11.glPopMatrix();
        }

        this.profiler.endStartSection("boxes");
        new BoxRenderer(this.cameraPosition.x, this.cameraPosition.y, this.cameraPosition.z).render();
        com.github.lunatrius.schematica.client.printer.PrinterHighlights.render(this.cameraPosition.x, this.cameraPosition.y, this.cameraPosition.z);
        com.github.lunatrius.schematica.client.printer.NextBlockHint.render(this.cameraPosition.x, this.cameraPosition.y, this.cameraPosition.z);
        com.github.lunatrius.schematica.client.container.ContainerLabels.render(this.cameraPosition.x, this.cameraPosition.y, this.cameraPosition.z);

        this.profiler.endStartSection("projects");
        renderProjectOrigin();
        this.profiler.endSection();

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glPopMatrix();
    }

    /** OverlayRenderer render_projects: the open project's origin, whether or not area boxes are shown. */
    private void renderProjectOrigin() {
        com.github.lunatrius.schematica.client.projects.SchematicProject project = com.github.lunatrius.schematica.client.projects.SchematicProjects.current();
        if (project == null) return;
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glPushMatrix();
        GL11.glTranslated(-this.cameraPosition.x, -this.cameraPosition.y, -this.cameraPosition.z);
        RenderHelper.createBuffers();
        Vector3d start = new Vector3d(), end = new Vector3d();
        project.origin().toVector3d(start).sub(0.001, 0.001, 0.001);
        end.set(start).add(1.002, 1.002, 1.002);
        RenderHelper.drawCuboidOutline(start.toVector3f(), end.toVector3f(), RenderHelper.LINE_ALL, 1, 0.0625f, 1, 1);
        int lineCount = RenderHelper.getLineCount();
        if (lineCount > 0) {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glLineWidth(4.0f);
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
            GL11.glVertexPointer(3, 0, RenderHelper.getLineVertexBuffer());
            GL11.glColorPointer(4, 0, RenderHelper.getLineColorBuffer());
            GL11.glDrawArrays(GL11.GL_LINES, 0, lineCount);
            GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
            GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }
        GL11.glPopMatrix();
    }

    private void updateRenderers() {
        List<List<RendererSchematicChunk>> groups = new ArrayList<>();
        for (SchematicWorld schematic : ClientProxy.visiblePlacements()) {
            if (!schematic.isRenderingEnabled()) continue;
            SchematicRenderData data = renderDataMap.get(schematic);
            if (data == null) continue;
            double offsetX = schematic.position.x - this.cameraPosition.x;
            double offsetY = schematic.position.y - this.cameraPosition.y;
            double offsetZ = schematic.position.z - this.cameraPosition.z;
            double limit = this.budget.limit();
            for (RendererSchematicChunk chunk : data.chunks) {
                AxisAlignedBB box = chunk.getBoundingBox();
                chunk.inRange = limit == RenderBudget.UNLIMITED || RenderBudget.distance(0, 0, 0, box.minX + offsetX, box.minY + offsetY,
                    box.minZ + offsetZ, box.maxX + offsetX, box.maxY + offsetY, box.maxZ + offsetZ) <= limit;
                chunk.isInFrustrum = chunk.inRange && this.frustum.isVisible(
                    box.minX + offsetX, box.minY + offsetY, box.minZ + offsetZ,
                    box.maxX + offsetX, box.maxY + offsetY, box.maxZ + offsetZ);
            }
            this.rendererSchematicChunkComparator.setPosition(schematic.position, this.cameraPosition);
            data.chunks.sort(this.rendererSchematicChunkComparator);
            groups.add(data.chunks);
        }
        long deadline = System.nanoTime() + 4_000_000L;
        updateScheduler.update(groups, 3, () -> System.nanoTime() < deadline,
            chunk -> chunk.inRange && chunk.getDirty(), RendererSchematicChunk::updateRenderer);
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
        try {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++) {
                        data.chunks.add(new RendererSchematicChunk(schematic, data.renderBlocks, x, y, z));
                    }
                }
            }
        } catch (RuntimeException e) {
            for (RendererSchematicChunk chunk : data.chunks) chunk.delete();
            throw e;
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
        boolean hitboxes = RenderManager.debugBoundingBox;
        // enableSchematicEntityHitboxes: schematic entities show their hitboxes only while the hitbox renderer is on
        RenderManager.debugBoundingBox = hitboxes && VisualSettings.entityHitboxes;
        try {
        for (Entity entity : schematic.getEntities()) {
            if (!schematic.isBlockRendered(net.minecraft.util.MathHelper.floor_double(entity.posX),
                net.minecraft.util.MathHelper.floor_double(entity.posY), net.minecraft.util.MathHelper.floor_double(entity.posZ))) continue;
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
        } finally {
            RenderManager.debugBoundingBox = hitboxes;
        }
    }
}
