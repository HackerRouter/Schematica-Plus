package com.github.lunatrius.schematica.client.renderer;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import com.github.lunatrius.schematica.client.world.RenderLayerSettings;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3d;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3f;
import com.github.lunatrius.schematica.client.renderer.shader.ShaderProgram;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.RenderColors;
import com.github.lunatrius.schematica.reference.Constants;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.compat.VisualAdapters;

public class RendererSchematicChunk {

    private static final ShaderProgram SHADER_ALPHA = new ShaderProgram("schematica", null, "shaders/alpha.frag");

    public boolean isInFrustrum = false;

    public final Vector3d centerPosition = new Vector3d();

    private final Minecraft minecraft = Minecraft.getMinecraft();
    private final Profiler profiler = this.minecraft.mcProfiler;
    private final SchematicWorld schematic;
    private final RenderBlocks ownRenderBlocks;
    private final List<TileEntity> tileEntities = new ArrayList<>();

    private final AxisAlignedBB boundingBox = AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);

    private boolean needsUpdate = true;
    private long layerRevision = -1;
    private int originX, originY, originZ, localLayer;
    private boolean localLayerEnabled;
    private int glList = -1;
    // TODO: move this away from GL lists
    private int glListHighlight = -1;

    public RendererSchematicChunk(SchematicWorld schematicWorld, RenderBlocks renderBlocks, int baseX, int baseY, int baseZ) {
        this.schematic = schematicWorld;
        this.ownRenderBlocks = renderBlocks;
        this.boundingBox.setBounds(
            baseX * Constants.SchematicChunk.WIDTH,
            baseY * Constants.SchematicChunk.HEIGHT,
            baseZ * Constants.SchematicChunk.LENGTH,
            (baseX + 1) * Constants.SchematicChunk.WIDTH,
            (baseY + 1) * Constants.SchematicChunk.HEIGHT,
            (baseZ + 1) * Constants.SchematicChunk.LENGTH);

        this.centerPosition.x = (int) ((baseX + 0.5) * Constants.SchematicChunk.WIDTH);
        this.centerPosition.y = (int) ((baseY + 0.5) * Constants.SchematicChunk.HEIGHT);
        this.centerPosition.z = (int) ((baseZ + 0.5) * Constants.SchematicChunk.LENGTH);

        int x, y, z;
        for (TileEntity tileEntity : this.schematic.getTileEntities()) {
            x = tileEntity.xCoord;
            y = tileEntity.yCoord;
            z = tileEntity.zCoord;

            if (x < this.boundingBox.minX || x >= this.boundingBox.maxX) {
                continue;
            } else if (z < this.boundingBox.minZ || z >= this.boundingBox.maxZ) {
                continue;
            } else if (y < this.boundingBox.minY || y >= this.boundingBox.maxY) {
                continue;
            }

            this.tileEntities.add(tileEntity);
        }

        this.glList = GL11.glGenLists(3);
        this.glListHighlight = GL11.glGenLists(3);
    }

    public void delete() {
        if (this.glList != -1) {
            GL11.glDeleteLists(this.glList, 3);
        }
        if (this.glListHighlight != -1) {
            GL11.glDeleteLists(this.glListHighlight, 3);
        }
    }

    public AxisAlignedBB getBoundingBox() {
        return this.boundingBox;
    }

    public void setDirty() {
        this.needsUpdate = true;
    }

    public boolean getDirty() {
        return this.needsUpdate
            || layerRevision != RenderLayerSettings.RANGE.revision()
            || originX != schematic.position.x || originY != schematic.position.y || originZ != schematic.position.z
            || localLayer != schematic.renderingLayer || localLayerEnabled != schematic.isRenderingLayer;
    }

    public void updateRenderer() {
        if (getDirty()) {
            this.needsUpdate = false;
            layerRevision = RenderLayerSettings.RANGE.revision();
            originX = schematic.position.x; originY = schematic.position.y; originZ = schematic.position.z;
            localLayer = schematic.renderingLayer; localLayerEnabled = schematic.isRenderingLayer;

            RenderHelper.createBuffers();

            for (int pass = 0; pass < 3; pass++) {
                RenderHelper.initBuffers();

                int minX, maxX, minY, maxY, minZ, maxZ;

                minX = (int) this.boundingBox.minX;
                maxX = Math.min((int) this.boundingBox.maxX, this.schematic.getWidth());
                minY = (int) this.boundingBox.minY;
                maxY = Math.min((int) this.boundingBox.maxY, this.schematic.getHeight());
                minZ = (int) this.boundingBox.minZ;
                maxZ = Math.min((int) this.boundingBox.maxZ, this.schematic.getLength());

                int[] limits = schematic.renderBounds();
                minX = Math.max(minX, limits[0]); maxX = Math.min(maxX, limits[3]);
                minY = Math.max(minY, limits[1]); maxY = Math.min(maxY, limits[4]);
                minZ = Math.max(minZ, limits[2]); maxZ = Math.min(maxZ, limits[5]);

                GL11.glNewList(this.glList + pass, GL11.GL_COMPILE);
                try (SchematicRenderPass context = new SchematicRenderPass(pass)) {
                    renderBlocks(pass, minX, minY, minZ, maxX, maxY, maxZ);
                } finally {
                    GL11.glEndList();
                }

                GL11.glNewList(this.glListHighlight + pass, GL11.GL_COMPILE);
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

                GL11.glEndList();
            }

            RenderHelper.destroyBuffers();
        }
    }

    public void render(int renderPass) {
        render(renderPass, 0);
    }

    public void render(int renderPass, float partialTicks) {
        if (!this.isInFrustrum) {
            return;
        }

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        int previousProgram = OpenGlHelper.shadersSupported ? GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) : 0;
        this.profiler.startSection("blocks");
        try (SchematicRenderPass context = new SchematicRenderPass(renderPass)) {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glCullFace(GL11.GL_BACK);
            GL11.glColor4f(1, 1, 1, 1);
            boolean alpha = OpenGlHelper.shadersSupported && ConfigurationHandler.enableAlpha;
            GL11.glDepthMask(renderPass == 0 && (!alpha || ConfigurationHandler.alpha >= 1));
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.001f);
            this.minecraft.renderEngine.bindTexture(TextureMap.locationBlocksTexture);
            if (alpha) {
                GL20.glUseProgram(SHADER_ALPHA.getProgram());
                GL20.glUniform1f(GL20.glGetUniformLocation(SHADER_ALPHA.getProgram(), "alpha_multiplier"),
                    ConfigurationHandler.alpha);
            }
            GL11.glCallList(this.glList + renderPass);
            if (alpha) GL20.glUseProgram(previousProgram);
            this.profiler.endStartSection("highlight");
            GL11.glDepthMask(false);
            GL11.glCallList(this.glListHighlight + renderPass);
            this.profiler.endStartSection("tileEntities");
            renderTileEntities(renderPass, partialTicks);
        } finally {
            if (OpenGlHelper.shadersSupported) GL20.glUseProgram(previousProgram);
            GL11.glPopAttrib();
            this.profiler.endSection();
        }
    }

    public void renderBlocks(int renderPass, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        IBlockAccess mcWorld = this.minecraft.theWorld;
        RenderBlocks renderBlocks = this.ownRenderBlocks;

        int x, y, z, wx, wy, wz;
        int sides;
        Block block, mcBlock;
        Vector3f zero = new Vector3f();
        Vector3f size = new Vector3f();

        int ambientOcclusion = this.minecraft.gameSettings.ambientOcclusion;
        this.minecraft.gameSettings.ambientOcclusion = 0;

        Tessellator.instance.startDrawingQuads();
        try {
        for (y = minY; y < maxY; y++) {
            for (z = minZ; z < maxZ; z++) {
                for (x = minX; x < maxX; x++) {
                    if (!schematic.isBlockRendered(x, y, z)) continue;
                    try {
                        block = this.schematic.getBlock(x, y, z);

                        wx = this.schematic.position.x + x;
                        wy = this.schematic.position.y + y;
                        wz = this.schematic.position.z + z;

                        mcBlock = mcWorld.getBlock(wx, wy, wz);

                        sides = 0;
                        if (block != null) {
                            if (!schematic.isBlockRendered(x, y - 1, z) || block.shouldSideBeRendered(this.schematic, x, y - 1, z, 0)) {
                                sides |= RenderHelper.QUAD_DOWN;
                            }

                            if (!schematic.isBlockRendered(x, y + 1, z) || block.shouldSideBeRendered(this.schematic, x, y + 1, z, 1)) {
                                sides |= RenderHelper.QUAD_UP;
                            }

                            if (!schematic.isBlockRendered(x, y, z - 1) || block.shouldSideBeRendered(this.schematic, x, y, z - 1, 2)) {
                                sides |= RenderHelper.QUAD_NORTH;
                            }

                            if (!schematic.isBlockRendered(x, y, z + 1) || block.shouldSideBeRendered(this.schematic, x, y, z + 1, 3)) {
                                sides |= RenderHelper.QUAD_SOUTH;
                            }

                            if (!schematic.isBlockRendered(x - 1, y, z) || block.shouldSideBeRendered(this.schematic, x - 1, y, z, 4)) {
                                sides |= RenderHelper.QUAD_WEST;
                            }

                            if (!schematic.isBlockRendered(x + 1, y, z) || block.shouldSideBeRendered(this.schematic, x + 1, y, z, 5)) {
                                sides |= RenderHelper.QUAD_EAST;
                            }
                        }

                        boolean isAirBlock = this.schematic.isAirBlock(x, y, z);
                        boolean isMcAirBlock = mcWorld.isAirBlock(wx, wy, wz)
                            || ConfigurationHandler.isExtraAirBlock(mcBlock);

                        if (!isMcAirBlock) {
                            if (ConfigurationHandler.highlight && renderPass == 2) {
                                if (isAirBlock && ConfigurationHandler.highlightAir) {
                                    zero.set(x, y, z);
                                    size.set(x + 1, y + 1, z + 1);
                                    if (ConfigurationHandler.drawQuads) {
                                        RenderHelper.drawCuboidSurface(
                                            zero,
                                            size,
                                            RenderHelper.QUAD_ALL,
                                            RenderColors.EXTRA.color());
                                    }
                                    if (ConfigurationHandler.drawLines) {
                                        RenderHelper.drawCuboidOutline(
                                            zero,
                                            size,
                                            RenderHelper.LINE_ALL,
                                            RenderColors.EXTRA.color());
                                    }
                                } else if (block != mcBlock) {
                                    zero.set(x, y, z);
                                    size.set(x + 1, y + 1, z + 1);
                                    if (ConfigurationHandler.drawQuads) {
                                        RenderHelper.drawCuboidSurface(zero, size, sides, RenderColors.WRONG_BLOCK.color());
                                    }
                                    if (ConfigurationHandler.drawLines) {
                                        RenderHelper.drawCuboidOutline(zero, size, sides, RenderColors.WRONG_BLOCK.color());
                                    }
                                } else if (this.schematic.getBlockMetadata(x, y, z)
                                    != mcWorld.getBlockMetadata(wx, wy, wz)) {
                                        zero.set(x, y, z);
                                        size.set(x + 1, y + 1, z + 1);
                                        if (ConfigurationHandler.drawQuads) {
                                            RenderHelper
                                                .drawCuboidSurface(zero, size, sides, RenderColors.WRONG_STATE.color());
                                        }
                                        if (ConfigurationHandler.drawLines) {
                                            RenderHelper
                                                .drawCuboidOutline(zero, size, sides, RenderColors.WRONG_STATE.color());
                                        }
                                    }
                            }
                        } else if (!isAirBlock) {
                            if (ConfigurationHandler.highlight && renderPass == 2) {
                                zero.set(x, y, z);
                                size.set(x + 1, y + 1, z + 1);
                                if (ConfigurationHandler.drawQuads) {
                                    RenderHelper.drawCuboidSurface(zero, size, sides, RenderColors.MISSING.color());
                                }
                                if (ConfigurationHandler.drawLines) {
                                    RenderHelper.drawCuboidOutline(zero, size, sides, RenderColors.MISSING.color());
                                }
                            }

                            if (renderPass < 2 && block != null && block.canRenderInPass(renderPass)) {
                                resetRenderBlocks(renderBlocks);
                                renderBlocks.renderAllFaces = !schematic.isBlockRendered(x - 1, y, z)
                                    || !schematic.isBlockRendered(x + 1, y, z) || !schematic.isBlockRendered(x, y - 1, z)
                                    || !schematic.isBlockRendered(x, y + 1, z) || !schematic.isBlockRendered(x, y, z - 1)
                                    || !schematic.isBlockRendered(x, y, z + 1);
                                renderBlocks.renderBlockByRenderType(block, x, y, z);
                            }
                        }
                    } catch (Exception e) {
                        Reference.logger.error("Failed to render block!", e);
                    }
                }
            }
        }

        } finally {
            this.minecraft.gameSettings.ambientOcclusion = ambientOcclusion;
            Tessellator.instance.draw();
        }
    }

    private void resetRenderBlocks(RenderBlocks renderer) {
        renderer.blockAccess = this.schematic;
        renderer.clearOverrideBlockTexture();
        renderer.lockBlockBounds = false;
        renderer.renderAllFaces = false;
        renderer.renderFromInside = false;
        renderer.flipTexture = false;
        renderer.enableAO = false;
        renderer.uvRotateTop = renderer.uvRotateBottom = renderer.uvRotateEast = renderer.uvRotateWest = 0;
        renderer.uvRotateNorth = renderer.uvRotateSouth = 0;
    }

    public void renderTileEntities(int renderPass) {
        renderTileEntities(renderPass, 0);
    }

    public void renderTileEntities(int renderPass, float partialTicks) {
        if (renderPass > 1) {
            return;
        }

        IBlockAccess mcWorld = this.minecraft.theWorld;

        int x, y, z;

        GL11.glColor4f(1.0f, 1.0f, 1.0f, ConfigurationHandler.alpha);

        try (SchematicTileRenderContext context = new SchematicTileRenderContext(this.schematic)) {
            for (TileEntity tileEntity : this.tileEntities) {
                if (!tileEntity.shouldRenderInPass(renderPass)) continue;
                x = tileEntity.xCoord;
                y = tileEntity.yCoord;
                z = tileEntity.zCoord;

                if (!this.schematic.isBlockRendered(x, y, z)) {
                    continue;
                }

                final boolean isAirBlock = mcWorld.isAirBlock(
                    x + this.schematic.position.x,
                    y + this.schematic.position.y,
                    z + this.schematic.position.z);

                if (isAirBlock) {
                    TileEntitySpecialRenderer tileEntitySpecialRenderer = TileEntityRendererDispatcher.instance
                        .getSpecialRenderer(tileEntity);
                    if (tileEntitySpecialRenderer != null) {
                        GL11.glPushMatrix();
                        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
                        try {
                            VisualAdapters.beforeRender(tileEntity, partialTicks);
                            tileEntitySpecialRenderer.renderTileEntityAt(tileEntity, x, y, z, partialTicks);

                            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
                            GL11.glDisable(GL11.GL_TEXTURE_2D);
                            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
                        } catch (Exception e) {
                            Reference.logger.error("Failed to render a tile entity!", e);
                        } finally {
                            GL11.glPopAttrib();
                            GL11.glPopMatrix();
                            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
                        }
                        GL11.glColor4f(1.0f, 1.0f, 1.0f, ConfigurationHandler.alpha);
                    }
                }
            }
        } catch (Exception ex) {
            Reference.logger.error("Failed to render tile entities!", ex);
        }
    }
}
