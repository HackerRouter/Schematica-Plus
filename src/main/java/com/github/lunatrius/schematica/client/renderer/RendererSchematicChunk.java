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
import com.github.lunatrius.schematica.handler.VisualSettings;
import com.github.lunatrius.schematica.handler.RenderColors;
import com.github.lunatrius.schematica.reference.Constants;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.compat.VisualAdapters;

public class RendererSchematicChunk {

    private static final ShaderProgram SHADER_ALPHA = new ShaderProgram("schematica", null, "shaders/alpha.frag");

    public boolean isInFrustrum = false;
    /** Within the render distance (RenderBudget); chunks outside are neither drawn nor rebuilt. */
    public boolean inRange = true;

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
            if (VisualSettings.frameBlocks) {
                if (alpha) {
                    GL20.glUseProgram(SHADER_ALPHA.getProgram());
                    GL20.glUniform1f(GL20.glGetUniformLocation(SHADER_ALPHA.getProgram(), "alpha_multiplier"),
                        ConfigurationHandler.alpha);
                }
                GL11.glCallList(this.glList + renderPass);
                if (alpha) GL20.glUseProgram(previousProgram);
            }
            this.profiler.endStartSection("highlight");
            GL11.glDepthMask(false);
            if (VisualSettings.frameOverlay) {
                if (VisualSettings.frameThrough) GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glLineWidth(VisualSettings.frameOutlineWidth());
                GL11.glCallList(this.glListHighlight + renderPass);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
            }
            this.profiler.endStartSection("tileEntities");
            if (VisualSettings.frameSchematic && VisualSettings.renderTileEntities) renderTileEntities(renderPass, partialTicks);
        } finally {
            if (OpenGlHelper.shadersSupported) GL20.glUseProgram(previousProgram);
            GL11.glPopAttrib();
            this.profiler.endSection();
        }
    }

    public static boolean isFluid(Block block) {
        return block instanceof net.minecraft.block.BlockLiquid || block instanceof net.minecraftforge.fluids.IFluidBlock;
    }

    /** Air in the world for the overlay: air, an extra air block, or a fluid with ignoreExistingFluids. */
    private static boolean worldAir(IBlockAccess world, Block block, int x, int y, int z) {
        return world.isAirBlock(x, y, z) || ConfigurationHandler.isExtraAirBlock(block) || VisualSettings.ignoreExistingFluids && isFluid(block);
    }

    /** The overlay at a schematic position, or null. */
    private RenderColors overlayAt(IBlockAccess world, int x, int y, int z) {
        if (!schematic.isBlockRendered(x, y, z)) return null;
        boolean air = schematic.isAirBlock(x, y, z);
        Block block = schematic.getBlock(x, y, z);
        if (!air && !VisualSettings.fluids && isFluid(block)) return null;
        int wx = schematic.position.x + x, wy = schematic.position.y + y, wz = schematic.position.z + z;
        Block real = world.getBlock(wx, wy, wz);
        if (worldAir(world, real, wx, wy, wz)) return air || !VisualSettings.overlayMissing ? null : RenderColors.MISSING;
        if (air) return ConfigurationHandler.highlightAir ? RenderColors.EXTRA : null;
        return overlayColor(block, schematic.getBlockMetadata(x, y, z), real, world.getBlockMetadata(wx, wy, wz));
    }

    private static final int[][] SIDES = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
    private static final int[] QUADS = {RenderHelper.QUAD_DOWN, RenderHelper.QUAD_UP, RenderHelper.QUAD_NORTH, RenderHelper.QUAD_SOUTH,
        RenderHelper.QUAD_WEST, RenderHelper.QUAD_EAST};

    /**
     * The overlay box: the block's model bounds with schematicOverlayModelSides/Outline, else the full block; faces hidden by
     * the schematic's neighbours are culled (enableSchematicOverlayCulling), and with overlayReducedInnerSides also the faces
     * between two overlays of the same type.
     */
    private void drawOverlay(IBlockAccess world, RenderColors color, Block block, boolean air, int x, int y, int z, Vector3f zero, Vector3f size) {
        int sides = 0;
        for (int i = 0; i < 6; i++) {
            int nx = x + SIDES[i][0], ny = y + SIDES[i][1], nz = z + SIDES[i][2];
            boolean visible = air || !VisualSettings.overlayCulling || !schematic.isBlockRendered(nx, ny, nz)
                || block.shouldSideBeRendered(this.schematic, nx, ny, nz, i);
            if (visible && VisualSettings.reducedInnerSides && color == overlayAt(world, nx, ny, nz)) visible = false;
            if (visible) sides |= QUADS[i];
        }
        float[] full = {x, y, z, x + 1, y + 1, z + 1}, model = full;
        if (!air && (VisualSettings.modelSides || VisualSettings.modelOutline)) {
            try {
                block.setBlockBoundsBasedOnState(this.schematic, x, y, z);
                float[] bounds = {x + (float) block.getBlockBoundsMinX(), y + (float) block.getBlockBoundsMinY(), z + (float) block.getBlockBoundsMinZ(),
                    x + (float) block.getBlockBoundsMaxX(), y + (float) block.getBlockBoundsMaxY(), z + (float) block.getBlockBoundsMaxZ()};
                if (bounds[3] > bounds[0] && bounds[4] > bounds[1] && bounds[5] > bounds[2]) model = bounds;
            } catch (RuntimeException ignored) {}
        }
        if (ConfigurationHandler.drawQuads) {
            float[] box = VisualSettings.modelSides ? model : full;
            zero.set(box[0], box[1], box[2]);
            size.set(box[3], box[4], box[5]);
            RenderHelper.drawCuboidSurface(zero, size, box == full ? sides : RenderHelper.QUAD_ALL, color.color());
        }
        if (ConfigurationHandler.drawLines) {
            float[] box = VisualSettings.modelOutline ? model : full;
            zero.set(box[0], box[1], box[2]);
            size.set(box[3], box[4], box[5]);
            RenderHelper.drawCuboidOutline(zero, size, air ? RenderHelper.LINE_ALL : box == full ? sides : RenderHelper.LINE_ALL, color.color());
        }
    }

    /** The overlay of a schematic block standing where another block is, or null when that overlay type is off. */
    static RenderColors overlayColor(Block expected, int expectedMeta, Block found, int foundMeta) {
        switch (com.github.lunatrius.schematica.util.BlockGroups.compare(expected, expectedMeta, found, foundMeta, com.github.lunatrius.schematica.util.BlockGroups.enabled)) {
            case WRONG_BLOCK: return VisualSettings.overlayWrongBlock ? RenderColors.WRONG_BLOCK : null;
            case WRONG_STATE: return VisualSettings.overlayWrongState ? RenderColors.WRONG_STATE : null;
            case DIFFERENT_BLOCK: return VisualSettings.overlayDiffBlock ? RenderColors.DIFFERENT_TYPE : null;
            default: return null;
        }
    }

    public void renderBlocks(int renderPass, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        IBlockAccess mcWorld = this.minecraft.theWorld;
        RenderBlocks renderBlocks = this.ownRenderBlocks;

        int x, y, z, wx, wy, wz;
        Block block, mcBlock;
        Vector3f zero = new Vector3f();
        Vector3f size = new Vector3f();

        int ambientOcclusion = this.minecraft.gameSettings.ambientOcclusion;
        if (!VisualSettings.aoModern) this.minecraft.gameSettings.ambientOcclusion = 0;

        Tessellator.instance.startDrawingQuads();
        try {
        for (y = minY; y < maxY; y++) {
            for (z = minZ; z < maxZ; z++) {
                for (x = minX; x < maxX; x++) {
                    if (!schematic.isBlockRendered(x, y, z)) continue;
                    try {
                        block = this.schematic.getBlock(x, y, z);
                        boolean isAirBlock = this.schematic.isAirBlock(x, y, z);
                        // enableSchematicFluidRendering off: no fluid and no overlay where the fluid is
                        if (!isAirBlock && !VisualSettings.fluids && isFluid(block)) continue;

                        wx = this.schematic.position.x + x;
                        wy = this.schematic.position.y + y;
                        wz = this.schematic.position.z + z;
                        mcBlock = mcWorld.getBlock(wx, wy, wz);
                        boolean isMcAirBlock = worldAir(mcWorld, mcBlock, wx, wy, wz);

                        if (ConfigurationHandler.highlight && renderPass == 2) {
                            RenderColors color = overlayAt(mcWorld, x, y, z);
                            if (color != null) drawOverlay(mcWorld, color, block, isAirBlock, x, y, z, zero, size);
                        }

                        if (renderPass < 2 && !isAirBlock && block != null && block.canRenderInPass(renderPass)
                            && (isMcAirBlock || VisualSettings.renderColliding && (block != mcBlock
                                || this.schematic.getBlockMetadata(x, y, z) != mcWorld.getBlockMetadata(wx, wy, wz)))) {
                            resetRenderBlocks(renderBlocks);
                            renderBlocks.renderAllFaces = !schematic.isBlockRendered(x - 1, y, z)
                                || !schematic.isBlockRendered(x + 1, y, z) || !schematic.isBlockRendered(x, y - 1, z)
                                || !schematic.isBlockRendered(x, y + 1, z) || !schematic.isBlockRendered(x, y, z - 1)
                                || !schematic.isBlockRendered(x, y, z + 1);
                            renderBlocks.renderBlockByRenderType(block, x, y, z);
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
                            // Opaque models need depth writes; transparent chest faces must not hide their contents.
                            GL11.glDepthMask(renderPass == 0 && !VisualAdapters.usesTransparentDepth(tileEntity));
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
