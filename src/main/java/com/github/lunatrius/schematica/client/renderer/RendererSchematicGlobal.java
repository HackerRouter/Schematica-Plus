package com.github.lunatrius.schematica.client.renderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.culling.Frustrum;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.lwjgl.opengl.GL11;

import com.github.lunatrius.core.util.vector.Vector3d;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Constants;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class RendererSchematicGlobal {

    public static final RendererSchematicGlobal INSTANCE = new RendererSchematicGlobal();

    private final Minecraft minecraft = Minecraft.getMinecraft();
    private final Profiler profiler = this.minecraft.mcProfiler;

    private final Frustrum frustrum = new Frustrum();
    /** Renderer chunks for the active/selected schematic (used by printer, tools). */
    public RenderBlocks renderBlocks = null;
    public final List<RendererSchematicChunk> sortedRendererSchematicChunk = new ArrayList<>();
    private final RendererSchematicChunkComparator rendererSchematicChunkComparator = new RendererSchematicChunkComparator();

    /** Per-schematic renderer data for multi-schematic rendering. */
    private final Map<SchematicWorld, SchematicRenderData> renderDataMap = new HashMap<>();

    private RendererSchematicGlobal() {}

    private static class SchematicRenderData {
        RenderBlocks renderBlocks;
        final List<RendererSchematicChunk> chunks = new ArrayList<>();
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
                renderAll();
            }

            this.profiler.endSection();
        }
    }

    /** Renders all loaded schematics plus the guide overlay. */
    public void renderAll() {
        GL11.glPushMatrix();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_BLEND);

        this.profiler.startSection("schematic");

        // Render each loaded schematic
        for (SchematicWorld sw : ClientProxy.loadedSchematics) {
            if (!sw.isRendering) continue;

            SchematicRenderData data = renderDataMap.get(sw);
            if (data == null || data.chunks.isEmpty()) continue;

            GL11.glPushMatrix();

            Vector3d playerPos = ClientProxy.playerPosition.clone();
            playerPos.sub(sw.position.toVector3d());
            GL11.glTranslated(-playerPos.x, -playerPos.y, -playerPos.z);

            // Update frustrum for this schematic
            this.frustrum.setPosition(
                ClientProxy.playerPosition.x - sw.position.x,
                ClientProxy.playerPosition.y - sw.position.y,
                ClientProxy.playerPosition.z - sw.position.z);
            for (RendererSchematicChunk chunk : data.chunks) {
                chunk.isInFrustrum = this.frustrum.isBoundingBoxInFrustum(chunk.getBoundingBox());
            }

            // Sort and update dirty chunks
            if (RendererSchematicChunk.getCanUpdate()) {
                this.rendererSchematicChunkComparator.setPosition(sw.position);
                data.chunks.sort(this.rendererSchematicChunkComparator);
                for (RendererSchematicChunk chunk : data.chunks) {
                    if (chunk.getDirty()) {
                        chunk.updateRenderer();
                        break;
                    }
                }
            }

            // Render passes
            for (int pass = 0; pass < 3; pass++) {
                for (RendererSchematicChunk chunk : data.chunks) {
                    chunk.render(pass);
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
                r, g, b, 0.25f);

            int quadCount = RenderHelper.getQuadCount();
            int lineCount = RenderHelper.getLineCount();
            if (quadCount > 0 || lineCount > 0) {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glLineWidth(1.5f);
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
            SchematicWorld activeSchematic = ClientProxy.schematic;
            Vector3d extra = new Vector3d();
            if (activeSchematic != null) {
                extra.add(activeSchematic.position.toVector3d());
            }

            GL11.glPushMatrix();
            Vector3d playerPos = ClientProxy.playerPosition.clone();
            playerPos.sub(extra);
            GL11.glTranslated(-playerPos.x, -playerPos.y, -playerPos.z);

            RenderHelper.createBuffers();

            Vector3d start = new Vector3d();
            Vector3d end = new Vector3d();

            ClientProxy.pointMin.toVector3d(start).sub(extra);
            ClientProxy.pointMax.toVector3d(end).sub(extra).add(1, 1, 1);
            RenderHelper.drawCuboidOutline(start.toVector3f(), end.toVector3f(),
                RenderHelper.LINE_ALL, 0.0f, 0.75f, 0.0f, 0.25f);

            ClientProxy.pointA.toVector3d(start).sub(extra);
            end.set(start).add(1, 1, 1);
            RenderHelper.drawCuboidOutline(start.toVector3f(), end.toVector3f(),
                RenderHelper.LINE_ALL, 0.75f, 0.0f, 0.0f, 0.25f);
            RenderHelper.drawCuboidSurface(start.toVector3f(), end.toVector3f(),
                RenderHelper.QUAD_ALL, 0.75f, 0.0f, 0.0f, 0.25f);

            ClientProxy.pointB.toVector3d(start).sub(extra);
            end.set(start).add(1, 1, 1);
            RenderHelper.drawCuboidOutline(start.toVector3f(), end.toVector3f(),
                RenderHelper.LINE_ALL, 0.0f, 0.0f, 0.75f, 0.25f);
            RenderHelper.drawCuboidSurface(start.toVector3f(), end.toVector3f(),
                RenderHelper.QUAD_ALL, 0.0f, 0.0f, 0.75f, 0.25f);

            int quadCount = RenderHelper.getQuadCount();
            int lineCount = RenderHelper.getLineCount();
            if (quadCount > 0 || lineCount > 0) {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glLineWidth(1.5f);
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

        this.profiler.endSection();

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glPopMatrix();
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
                    data.chunks.add(new RendererSchematicChunk(schematic, x, y, z));
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

    /** Refreshes only the chunks for a specific schematic. */
    public void refresh(SchematicWorld schematic) {
        SchematicRenderData data = renderDataMap.get(schematic);
        if (data != null) {
            for (RendererSchematicChunk chunk : data.chunks) {
                chunk.setDirty();
            }
        }
    }

    private void renderEntities(SchematicWorld schematic) {
        RenderManager renderManager = RenderManager.instance;

        // The GL matrix is translated by (sw.position - playerPosition).
        // renderEntitySimple renders at (entity.pos - renderManager.renderPos).
        // Final screen position = GL_translate + render_offset
        //   = (sw.position - playerPos) + (entity.pos - renderPosX/Y/Z)
        // Since renderPosX/Y/Z == playerPos, this gives:
        //   sw.position + entity.pos - 2*playerPos  (WRONG)
        // We want: sw.position + entity.pos - playerPos
        // Fix: temporarily offset entity.pos by +playerPos so the -renderPos cancels correctly.

        for (Entity entity : schematic.getEntities()) {
            try {
                // Save original position
                double origX = entity.posX;
                double origY = entity.posY;
                double origZ = entity.posZ;
                double origPrevX = entity.prevPosX;
                double origPrevY = entity.prevPosY;
                double origPrevZ = entity.prevPosZ;
                double origLastX = entity.lastTickPosX;
                double origLastY = entity.lastTickPosY;
                double origLastZ = entity.lastTickPosZ;

                // Offset entity position so renderEntitySimple places it correctly
                // in the already-translated GL coordinate space
                double offsetX = ClientProxy.playerPosition.x;
                double offsetY = ClientProxy.playerPosition.y;
                double offsetZ = ClientProxy.playerPosition.z;
                entity.posX = origX + offsetX;
                entity.posY = origY + offsetY;
                entity.posZ = origZ + offsetZ;
                entity.prevPosX = origX + offsetX;
                entity.prevPosY = origY + offsetY;
                entity.prevPosZ = origZ + offsetZ;
                entity.lastTickPosX = origX + offsetX;
                entity.lastTickPosY = origY + offsetY;
                entity.lastTickPosZ = origZ + offsetZ;

                // Temporarily set the entity's world to the client world so the renderer can access textures
                net.minecraft.world.World originalWorld = entity.worldObj;
                entity.worldObj = this.minecraft.theWorld;

                renderManager.renderEntitySimple(entity, 0.0f);

                // Restore original state
                entity.worldObj = originalWorld;
                entity.posX = origX;
                entity.posY = origY;
                entity.posZ = origZ;
                entity.prevPosX = origPrevX;
                entity.prevPosY = origPrevY;
                entity.prevPosZ = origPrevZ;
                entity.lastTickPosX = origLastX;
                entity.lastTickPosY = origLastY;
                entity.lastTickPosZ = origLastZ;
            } catch (Exception e) {
                // Silently ignore rendering errors for unsupported entities
            }
        }
    }
}
