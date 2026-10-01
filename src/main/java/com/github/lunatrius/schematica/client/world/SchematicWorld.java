package com.github.lunatrius.schematica.client.world;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.util.Direction;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3f;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.nbt.TileEntitySnapshots;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.SchematicTransform;
import com.github.lunatrius.schematica.world.chunk.ChunkProviderSchematic;
import com.github.lunatrius.schematica.world.storage.SaveHandlerSchematic;
import com.github.lunatrius.schematica.world.storage.Schematic;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class SchematicWorld extends World {

    private static final WorldSettings WORLD_SETTINGS = new WorldSettings(
        0,
        WorldSettings.GameType.CREATIVE,
        false,
        false,
        WorldType.FLAT);

    public String name = "";
    public static final ItemStack DEFAULT_ICON = new ItemStack(Blocks.grass);

    private ISchematic schematic;
    public final List<String> transformOperations = new ArrayList<>();
    private SchematicSourceData placementSource;
    private SubRegionPlacements subregions;
    private java.util.BitSet visibleRegionBlocks;
    private int placementRevision;
    private int contentRevision;
    private PlacementSettings placementSettings = PlacementSettings.DEFAULT;

    public PlacementSettings placementSettings() { return placementSettings; }
    public void setPlacementSettings(PlacementSettings settings) {
        placementSettings = java.util.Objects.requireNonNull(settings);
        placementRevision++;
    }
    public boolean isEnabled() { return placementSettings.enabled; }
    public boolean isRenderingEnabled() { return placementSettings.renders(isRendering); }

    public void setPlacementSource(SchematicSourceData source) {
        placementSource = source;
        subregions = SubRegionPlacements.create(schematic);
    }

    public SubRegionPlacements subregions() { return subregions; }
    public SchematicSourceData sourceData() { return placementSource; }

    /** Replaces one composed cell after its source cell was edited; the placement geometry stays unchanged. */
    public void writeCell(int x, int y, int z, CellState state) {
        if (!schematic.containsBlock(x, y, z)) return;
        Block previous = schematic.getBlock(x, y, z);
        setBlock(x, y, z, state.block, state.meta, 0);
        if (state.tile != null) setTileEntity(x, y, z, com.github.lunatrius.schematica.world.storage.SchematicCopies.tile(state.tile, -x, -y, -z));
        else if (previous != state.block || state.isAir()) removeTileEntity(x, y, z);
        contentRevision++;
    }
    public int placementRevision() { return placementRevision; }
    public int contentRevision() { return contentRevision; }
    public boolean hasEnabledRegions() { return subregions == null || subregions.hasEnabled(); }

    public void selectSubregion(String name) {
        subregions = subregions.select(name);
        placementRevision++;
    }

    public com.github.lunatrius.schematica.api.SchematicOrigin subregionPosition(String name) {
        com.github.lunatrius.schematica.api.SchematicOrigin relative = SubRegionPlacements.vector(subregions.get(name).position, transformOperations, false);
        com.github.lunatrius.schematica.api.SchematicOrigin origin = originPosition();
        return relative.atMinimum(origin.x, origin.y, origin.z);
    }

    public com.github.lunatrius.schematica.api.SchematicRegion subregionBounds(String name) {
        com.github.lunatrius.schematica.api.SchematicRegion box = subregions.get(name).bounds();
        com.github.lunatrius.schematica.api.SchematicOrigin origin = originPosition();
        com.github.lunatrius.schematica.api.SchematicOrigin a = SubRegionPlacements.vector(
            new com.github.lunatrius.schematica.api.SchematicOrigin(box.minX, box.minY, box.minZ), transformOperations, false).atMinimum(origin.x, origin.y, origin.z);
        com.github.lunatrius.schematica.api.SchematicOrigin b = SubRegionPlacements.vector(
            new com.github.lunatrius.schematica.api.SchematicOrigin(box.maxX, box.maxY, box.maxZ), transformOperations, false).atMinimum(origin.x, origin.y, origin.z);
        return new com.github.lunatrius.schematica.api.SchematicRegion(name, a.x, a.y, a.z, b.x, b.y, b.z);
    }

    public void moveSubregionTo(String name, int x, int y, int z) {
        com.github.lunatrius.schematica.api.SchematicOrigin origin = originPosition();
        com.github.lunatrius.schematica.api.SchematicOrigin relative = SubRegionPlacements.vector(
            new com.github.lunatrius.schematica.api.SchematicOrigin(Math.subtractExact(x, origin.x), Math.subtractExact(y, origin.y), Math.subtractExact(z, origin.z)),
            transformOperations, true);
        changeSubregions(subregions.replace(subregions.get(name).position(relative)));
    }

    public void changeSubregions(SubRegionPlacements next) {
        if (!placementSettings.allowsRegionChange(subregions, next)) throw new IllegalStateException(PlacementSettings.LOCKED_MESSAGE);
        rebuildRegions(next, new ArrayList<>(transformOperations));
    }

    public void resetSubregions(String name) {
        if (placementSettings.locked) throw new IllegalStateException(PlacementSettings.LOCKED_MESSAGE);
        changeSubregions(name == null ? subregions.reset() : subregions.replace(subregions.get(name).reset()));
    }

    public void restoreSubregions(com.google.gson.JsonObject saved) {
        if (saved == null) return;
        SubRegionPlacements next = subregions.restore(saved);
        if (!subregions.modified() && !next.modified()) {
            subregions = next;
            placementRevision++;
        } else changeSubregions(next);
    }

    private void rebuildRegions(SubRegionPlacements next, List<String> operations) {
        com.github.lunatrius.schematica.api.SchematicOrigin origin = originPosition();
        SubRegionPlacements.Layout layout = next.layout();
        com.github.lunatrius.schematica.api.SchematicOrigin offset = com.github.lunatrius.schematica.client.gui.placement.PlacementTransform.transformOrigin(
            new com.github.lunatrius.schematica.api.SchematicOrigin(-layout.minimum.x, -layout.minimum.y, -layout.minimum.z),
            layout.width, layout.height, layout.length, String.join("", operations));
        com.github.lunatrius.schematica.api.SchematicOrigin minimum = offset.minimumAt(origin);
        int[] size = com.github.lunatrius.schematica.client.gui.placement.PlacementTransform.transformedSize(layout.width, layout.height, layout.length, String.join("", operations));
        for (int i = 0; i < 3; i++) if (minimum.coordinates()[i] < -30000000 || (long) minimum.coordinates()[i] + size[i] > 30000000) {
            throw new IllegalArgumentException("Subregion placement outside coordinate limits");
        }
        RegionComposer composed;
        try { composed = RegionComposer.build(placementSource.instantiate(), next, operations); }
        catch (java.io.IOException e) { throw new IllegalArgumentException("Unable to restore schematic source", e); }
        this.schematic = composed.world.getSchematic();
        this.visibleRegionBlocks = composed.visible;
        this.subregions = next;
        this.position.set(minimum.x, minimum.y, minimum.z);
        this.transformOperations.clear();
        this.transformOperations.addAll(operations);
        rotationStateX = composed.world.rotationStateX; rotationStateY = composed.world.rotationStateY; rotationStateZ = composed.world.rotationStateZ;
        flipStateX = composed.world.flipStateX; flipStateY = composed.world.flipStateY; flipStateZ = composed.world.flipStateZ;
        renderingLayer = Math.min(renderingLayer, getHeight() - 1);
        for (TileEntity tile : schematic.getTileEntities()) bindTileEntity(tile);
        for (TileEntity tile : schematic.getTileEntities()) validateTileEntity(tile);
        refreshChests();
        placementRevision++;
    }

    public ISchematic getSchematic() { return this.schematic; }

    public com.github.lunatrius.schematica.api.SchematicOrigin originPosition() {
        return schematic.getOrigin().atMinimum(position.x, position.y, position.z);
    }

    public void moveOriginTo(int x, int y, int z) {
        com.github.lunatrius.schematica.api.SchematicOrigin minimum = schematic.getOrigin().minimumAt(
            placementSettings.constrainOrigin(originPosition(), new com.github.lunatrius.schematica.api.SchematicOrigin(x, y, z)));
        position.set(minimum.x, minimum.y, minimum.z);
    }

    public void moveMinimumTo(int x, int y, int z) {
        com.github.lunatrius.schematica.api.SchematicOrigin origin = schematic.getOrigin().atMinimum(x, y, z);
        moveOriginTo(origin.x, origin.y, origin.z);
    }

    public final Vector3i position = new Vector3i();
    public boolean isRendering;
    public boolean isRenderingLayer;
    public int renderingLayer;
    public boolean isRenderingEntities = false;
    /** Whether to paste block NBT (tile entity data) when pasting this schematic. */
    public boolean isPastingBlockNBT = true;
    public int rotationState;
    public int rotationStateX;
    public int rotationStateY;
    public int rotationStateZ;
    public int flipStateX;
    public int flipStateY;
    public int flipStateZ;

    /** The directory this schematic was loaded from (for persistence). */
    public File sourceDirectory;
    /** The filename this schematic was loaded from (for persistence). */
    public String sourceFilename;

    public SchematicWorld(ISchematic schematic) {
        super(new SaveHandlerSchematic(), "Schematica", WORLD_SETTINGS, null, new Profiler());
        this.schematic = schematic;
        this.isRemote = true;

        for (TileEntity tileEntity : schematic.getTileEntities()) bindTileEntity(tileEntity);
        for (TileEntity tileEntity : schematic.getTileEntities()) validateTileEntity(tileEntity);
        for (TileEntity tileEntity : new ArrayList<>(schematic.getTileEntities())) restoreTileEntity(tileEntity);

        this.isRendering = false;
        this.isRenderingLayer = false;
        this.renderingLayer = 0;

        // Auto-enable entity rendering if the schematic contains entities
        if (!schematic.getEntities().isEmpty()) {
            this.isRenderingEntities = true;
        }
    }

    public SchematicWorld(ISchematic schematic, String filename) {
        this(schematic);
        // Strip any known schematic extension from the display name
        this.name = filename.replaceAll("(?i)\\.(schematic|litematic|schemplus)$", "");
    }

    private boolean tracingRenderedBlocks;

    public boolean isBlockRendered(int x, int y, int z) {
        return isBlockInRange(x, y, z) && (visibleRegionBlocks == null || visibleRegionBlocks.get(x + getWidth() * (z + getLength() * y)));
    }

    public boolean isBlockInRange(int x, int y, int z) {
        return isEnabled() && schematic.containsBlock(x, y, z) && (!isRenderingLayer || renderingLayer == y)
            && RenderLayerSettings.RANGE.contains((long) position.x + x, (long) position.y + y, (long) position.z + z);
    }

    public int[] renderBounds() {
        return RenderLayerSettings.RANGE.localBounds(position.x, position.y, position.z,
            getWidth(), getHeight(), getLength(), isRenderingLayer, renderingLayer);
    }

    public net.minecraft.util.MovingObjectPosition rayTraceRendered(net.minecraft.util.Vec3 start, net.minecraft.util.Vec3 end) {
        return rayTraceRendered(start, end, false);
    }

    public net.minecraft.util.MovingObjectPosition rayTraceRendered(net.minecraft.util.Vec3 start, net.minecraft.util.Vec3 end, boolean fluids) {
        if (!isRenderingEnabled()) return null;
        boolean previous = tracingRenderedBlocks;
        tracingRenderedBlocks = true;
        try { return func_147447_a(start, end, fluids, false, false); }
        finally { tracingRenderedBlocks = previous; }
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        if (tracingRenderedBlocks && !isBlockRendered(x, y, z)) {
            return Blocks.air;
        }

        return this.schematic.getBlock(x, y, z);
    }

    @Override
    public boolean setBlock(int x, int y, int z, Block block, int metadata, int flags) {
        boolean different = this.schematic.getBlock(x, y, z) != block || this.schematic.getBlockMetadata(x, y, z) != metadata;
        boolean changed = this.schematic.setBlock(x, y, z, block, metadata);
        if (changed && different) { placementRevision++; contentRevision++; }
        return changed;
    }

    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        return this.schematic.getTileEntity(x, y, z);
    }

    @Override
    public void setTileEntity(int x, int y, int z, TileEntity tileEntity) {
        TileEntitySnapshots.replacePreview(getTileEntity(x, y, z), tileEntity);
        if (tileEntity != null) {
            tileEntity.xCoord = x;
            tileEntity.yCoord = y;
            tileEntity.zCoord = z;
        }
        this.schematic.setTileEntity(x, y, z, tileEntity);
        initializeTileEntity(tileEntity);
    }

    @Override
    public void removeTileEntity(int x, int y, int z) {
        this.schematic.removeTileEntity(x, y, z);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getSkyBlockTypeBrightness(EnumSkyBlock skyBlock, int x, int y, int z) {
        return 15;
    }

    @Override
    public float getLightBrightness(int x, int y, int z) {
        return 1.0f;
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        return this.schematic.getBlockMetadata(x, y, z);
    }

    @Override
    public boolean isBlockNormalCubeDefault(int x, int y, int z, boolean _default) {
        return getBlock(x, y, z).isNormalCube();
    }

    @Override
    protected int func_152379_p() {
        return 0;
    }

    @Override
    public boolean isAirBlock(int x, int y, int z) {
        return getBlock(x, y, z).isAir(this, x, y, z);
    }

    @Override
    public BiomeGenBase getBiomeGenForCoords(int x, int z) {
        return BiomeGenBase.jungle;
    }

    public int getWidth() {
        return this.schematic.getWidth();
    }

    public int getLength() {
        return this.schematic.getLength();
    }

    @Override
    public int getHeight() {
        return this.schematic.getHeight();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean extendedLevelsInChunkCache() {
        return false;
    }

    @Override
    protected IChunkProvider createChunkProvider() {
        return new ChunkProviderSchematic(this);
    }

    @Override
    public Entity getEntityByID(int id) {
        return null;
    }

    @Override
    public boolean blockExists(int x, int y, int z) {
        return this.schematic != null && x >= 0 && x < getWidth() && y >= 0 && y < getHeight()
            && z >= 0 && z < getLength();
    }

    @Override
    public boolean setBlockMetadataWithNotify(int x, int y, int z, int metadata, int flag) {
        return this.schematic.setBlockMetadata(x, y, z, metadata);
    }

    @Override
    public boolean isSideSolid(int x, int y, int z, ForgeDirection side) {
        return isSideSolid(x, y, z, side, false);
    }

    @Override
    public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean _default) {
        return getBlock(x, y, z).isSideSolid(this, x, y, z, side);
    }

    public void initializeTileEntity(TileEntity tileEntity) {
        if (tileEntity == null) return;
        bindTileEntity(tileEntity);
        validateTileEntity(tileEntity);
        restoreTileEntity(tileEntity);
    }

    private void bindTileEntity(TileEntity tileEntity) {
        tileEntity.setWorldObj(this);
        tileEntity.updateContainingBlockInfo();
        tileEntity.getBlockType();
    }

    private void validateTileEntity(TileEntity tileEntity) {
        try {
            tileEntity.validate();
        } catch (Exception e) {
            Reference.logger.error("TileEntity validation for {} failed!", tileEntity.getClass(), e);
        }
    }

    private void restoreTileEntity(TileEntity tileEntity) {
        try {
            TileEntitySnapshots.restorePreview(tileEntity);
        } catch (Exception | LinkageError e) {
            Reference.logger.warn("TileEntity preview for {} failed", tileEntity.getClass().getName(), e);
        }
    }

    public void setIcon(ItemStack icon) {
        this.schematic.setIcon(icon);
    }

    public ItemStack getIcon() {
        return this.schematic.getIcon();
    }

    public List<TileEntity> getTileEntities() {
        return this.schematic.getTileEntities();
    }

    public List<Entity> getEntities() {
        return this.schematic.getEntities();
    }

    public boolean toggleRendering() {
        this.isRendering = !this.isRendering;
        return this.isRendering;
    }

    public void refreshChests() {
        for (TileEntity tileEntity : this.schematic.getTileEntities()) {
            if (tileEntity instanceof TileEntityChest) {
                ((TileEntityChest) tileEntity).adjacentChestChecked = false;
                ((TileEntityChest) tileEntity).checkForAdjacentChests();
            }
        }
    }

    public void flip(ForgeDirection direction) {
        transform(direction, true);
    }

    public void rotate(ForgeDirection direction) {
        transform(direction, false);
    }

    private void transform(ForgeDirection direction, boolean mirror) {
        if (placementSettings.locked) return;
        char operation;
        switch (direction) {
            case EAST: operation = 'X'; break;
            case UP: operation = 'Y'; break;
            case SOUTH: operation = 'Z'; break;
            default: throw new IllegalArgumentException("Unsupported transform axis");
        }
        if (mirror) operation = Character.toLowerCase(operation);
        if (subregions != null && subregions.modified()) {
            List<String> operations = new ArrayList<>(transformOperations);
            operations.add(String.valueOf(operation));
            rebuildRegions(subregions, operations);
            return;
        }
        List<Entity> entities = new ArrayList<>(this.schematic.getEntities());
        for (Entity entity : entities) {
            if (entity instanceof EntityHanging) {
                int facing = ((EntityHanging) entity).hangingDirection;
                double[] normal = SchematicTransform.point(operation, Direction.offsetX[facing], 0,
                    Direction.offsetZ[facing], 0, 0, 0);
                if (normal[1] != 0) {
                    // 1.7.10 has no floor/ceiling direction for paintings and item frames.
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
                    if (mc != null && mc.thePlayer != null) mc.thePlayer.addChatMessage(
                        new net.minecraft.util.ChatComponentTranslation("schematica.message.transform.hanging_entities"));
                    return;
                }
            }
        }
        java.util.List<com.github.lunatrius.schematica.api.SchematicRegion> regions = this.schematic.getRegions();
        int w = getWidth(), h = getHeight(), l = getLength();
        com.github.lunatrius.schematica.api.SchematicOrigin transformedOrigin = schematic.getOrigin().transform(operation, w, h, l);
        com.github.lunatrius.schematica.api.SchematicOrigin minimum = transformedOrigin.minimumAt(originPosition());
        boolean layerMode = this.isRenderingLayer;
        this.isRenderingLayer = false;
        try {
            if (mirror) flipContents(direction);
            else rotateContents(direction);
            java.util.List<com.github.lunatrius.schematica.api.SchematicRegion> transformed = new ArrayList<>();
            for (com.github.lunatrius.schematica.api.SchematicRegion region : regions) transformed.add(region.transform(operation, w, h, l));
            ((Schematic) this.schematic).setRegions(transformed);
            ((Schematic) this.schematic).setOrigin(transformedOrigin);
            position.set(minimum.x, minimum.y, minimum.z);
            for (TileEntity tileEntity : this.schematic.getTileEntities()) {
                TileEntitySnapshots.transformPreview(tileEntity, operation);
            }
            for (Entity entity : entities) {
                double[] p = SchematicTransform.point(operation, entity.posX, entity.posY, entity.posZ, w, h, l);
                double yaw = Math.toRadians(entity.rotationYaw), pitch = Math.toRadians(entity.rotationPitch);
                double[] look = SchematicTransform.point(operation, -Math.sin(yaw) * Math.cos(pitch),
                    -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch), 0, 0, 0);
                entity.setLocationAndAngles(p[0], p[1], p[2],
                    (float) Math.toDegrees(Math.atan2(-look[0], look[2])),
                    (float) Math.toDegrees(Math.atan2(-look[1], Math.hypot(look[0], look[2]))));
                double[] motion = SchematicTransform.point(operation, entity.motionX, entity.motionY, entity.motionZ, 0, 0, 0);
                entity.motionX = motion[0]; entity.motionY = motion[1]; entity.motionZ = motion[2];
                if (entity instanceof EntityHanging) {
                    EntityHanging hanging = (EntityHanging) entity;
                    double[] anchor = SchematicTransform.point(operation, hanging.field_146063_b, hanging.field_146064_c,
                        hanging.field_146062_d, w - 1, h - 1, l - 1);
                    hanging.field_146063_b = (int) anchor[0];
                    hanging.field_146064_c = (int) anchor[1];
                    hanging.field_146062_d = (int) anchor[2];
                    double[] normal = SchematicTransform.point(operation, Direction.offsetX[hanging.hangingDirection], 0,
                        Direction.offsetZ[hanging.hangingDirection], 0, 0, 0);
                    for (int i = 0; i < 4; i++) {
                        if (normal[0] == Direction.offsetX[i] && normal[2] == Direction.offsetZ[i]) {
                            hanging.setDirection(i);
                            break;
                        }
                    }
                }
                entity.prevPosX = entity.lastTickPosX = entity.posX;
                entity.prevPosY = entity.lastTickPosY = entity.posY;
                entity.prevPosZ = entity.lastTickPosZ = entity.posZ;
                entity.prevRotationYaw = entity.rotationYaw;
                entity.prevRotationPitch = entity.rotationPitch;
                this.schematic.addEntity(entity);
            }
            this.transformOperations.add(String.valueOf(operation));
            this.visibleRegionBlocks = null;
        } finally {
            this.isRenderingLayer = layerMode;
            this.renderingLayer = Math.min(this.renderingLayer, getHeight() - 1);
        }
    }

    private void flipContents(ForgeDirection direction) {
        final ItemStack icon = this.schematic.getIcon();
        final int width = this.schematic.getWidth();
        final int height = this.schematic.getHeight();
        final int length = this.schematic.getLength();

        final ISchematic schematicFlipped = new Schematic(icon, width, height, length);

        switch (direction) {
            case EAST:
                for (int y = 0; y < height; y++) {
                    for (int z = 0; z < length; z++) {
                        for (int x = 0; x < width; x++) {
                            final Block block = getBlock(width - 1 - x, y, z);
                            final int metadata = getBlockMetadata(width - 1 - x, y, z);
                            schematicFlipped.setBlock(x, y, z, block, metadata);
                        }
                    }
                }
                for (TileEntity tileEntity : this.schematic.getTileEntities()) {
                    tileEntity.xCoord = width - 1 - tileEntity.xCoord;
                    tileEntity.blockMetadata = schematicFlipped
                        .getBlockMetadata(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
                    schematicFlipped.setTileEntity(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, tileEntity);
                }
                flipStateX++;
                if (flipStateX > 1) {
                    flipStateX = 0;
                }

                break;
            case SOUTH: {
                for (int y = 0; y < height; y++) {
                    for (int z = 0; z < length; z++) {
                        for (int x = 0; x < width; x++) {
                            final Block block = getBlock(x, y, length - 1 - z);
                            final int metadata = getBlockMetadata(x, y, length - 1 - z);
                            schematicFlipped.setBlock(x, y, z, block, metadata);
                        }
                    }
                }
                for (TileEntity tileEntity : this.schematic.getTileEntities()) {
                    tileEntity.zCoord = length - 1 - tileEntity.zCoord;
                    tileEntity.blockMetadata = schematicFlipped
                        .getBlockMetadata(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
                    schematicFlipped.setTileEntity(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, tileEntity);
                }
                flipStateZ++;
                if (flipStateZ > 1) {
                    flipStateZ = 0;
                }

                break;
            }
            case UP:
                for (int y = 0; y < height; y++) {
                    for (int z = 0; z < length; z++) {
                        for (int x = 0; x < width; x++) {
                            final Block block = getBlock(x, height - 1 - y, z);
                            final int metadata = getBlockMetadata(x, height - 1 - y, z);
                            schematicFlipped.setBlock(x, y, z, block, metadata);
                        }
                    }
                }
                for (TileEntity tileEntity : this.schematic.getTileEntities()) {
                    tileEntity.yCoord = height - 1 - tileEntity.yCoord;
                    tileEntity.blockMetadata = schematicFlipped
                        .getBlockMetadata(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
                    schematicFlipped.setTileEntity(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, tileEntity);
                }
                flipStateY++;
                if (flipStateY > 1) {
                    flipStateY = 0;
                }

                break;
            default:
                Reference.logger.debug("Incorrect direction given to flip function!");
        }

        this.schematic = schematicFlipped;
        refreshChests();
    }

    private void rotateContents(ForgeDirection direction) {
        final ItemStack icon = this.schematic.getIcon();
        final int width = this.schematic.getWidth();
        final int height = this.schematic.getHeight();
        final int length = this.schematic.getLength();

        final ISchematic schematicRotated;
        switch (direction) {
            case EAST:
                schematicRotated = new Schematic(icon, width, length, height);

                for (int y = 0; y < height; y++) {
                    for (int z = 0; z < length; z++) {
                        for (int x = 0; x < width; x++) {
                            try {
                                getBlock(x, height - 1 - y, z)
                                    .rotateBlock(this, x, height - 1 - y, z, ForgeDirection.EAST);
                            } catch (Exception e) {
                                Reference.logger.debug("Failed to rotate block!", e);
                            }
                            final Block block = getBlock(x, height - 1 - y, z);
                            final int metadata = getBlockMetadata(x, height - 1 - y, z);
                            schematicRotated.setBlock(x, z, y, block, metadata);
                        }
                    }
                }

                for (TileEntity tileEntity : this.schematic.getTileEntities()) {
                    final int coord = tileEntity.yCoord;
                    tileEntity.yCoord = tileEntity.zCoord;
                    tileEntity.zCoord = height - 1 - coord;
                    tileEntity.blockMetadata = schematicRotated
                        .getBlockMetadata(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
                    schematicRotated.setTileEntity(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, tileEntity);
                }

                rotationStateX++;
                if (rotationStateX > 3) {
                    rotationStateX = 0;
                }

                this.schematic = schematicRotated;

                refreshChests();
                break;
            case UP: // Rotation only really works when moving around the UP direction for most blocks
                schematicRotated = new Schematic(icon, length, height, width);

                for (int y = 0; y < height; y++) {
                    for (int z = 0; z < length; z++) {
                        for (int x = 0; x < width; x++) {
                            try {
                                getBlock(x, y, length - 1 - z)
                                    .rotateBlock(this, x, y, length - 1 - z, ForgeDirection.UP);
                            } catch (Exception e) {
                                Reference.logger.debug("Failed to rotate block!", e);
                            }

                            final Block block = getBlock(x, y, length - 1 - z);
                            final int metadata = getBlockMetadata(x, y, length - 1 - z);
                            schematicRotated.setBlock(z, y, x, block, metadata);
                        }
                    }
                }

                for (TileEntity tileEntity : this.schematic.getTileEntities()) {
                    final int coord = tileEntity.zCoord;
                    tileEntity.zCoord = tileEntity.xCoord;
                    tileEntity.xCoord = length - 1 - coord;
                    tileEntity.blockMetadata = schematicRotated
                        .getBlockMetadata(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);

                    if (tileEntity instanceof TileEntitySkull && tileEntity.blockMetadata == 0x1) {
                        ((TileEntitySkull) tileEntity).func_145903_a((((TileEntitySkull) tileEntity).func_145906_b() + 12) & 15);
                    }

                    schematicRotated.setTileEntity(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, tileEntity);
                }

                rotationStateY++;
                if (rotationStateY > 3) {
                    rotationStateY = 0;
                }

                this.schematic = schematicRotated;

                refreshChests();
                break;
            case SOUTH:
                schematicRotated = new Schematic(icon, height, width, length);

                for (int y = 0; y < height; y++) {
                    for (int z = 0; z < length; z++) {
                        for (int x = 0; x < width; x++) {
                            try {
                                getBlock(width - 1 - x, y, z)
                                    .rotateBlock(this, width - 1 - x, y, z, ForgeDirection.SOUTH);
                            } catch (Exception e) {
                                Reference.logger.debug("Failed to rotate block!", e);
                            }
                            final Block block = getBlock(width - 1 - x, y, z);
                            final int metadata = getBlockMetadata(width - 1 - x, y, z);
                            schematicRotated.setBlock(y, x, z, block, metadata);
                        }
                    }
                }

                for (TileEntity tileEntity : this.schematic.getTileEntities()) {
                    final int coord = tileEntity.xCoord;
                    tileEntity.xCoord = tileEntity.yCoord;
                    tileEntity.yCoord = width - 1 - coord;
                    tileEntity.blockMetadata = schematicRotated
                        .getBlockMetadata(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
                    schematicRotated.setTileEntity(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, tileEntity);
                }

                rotationStateZ++;
                if (rotationStateZ > 3) {
                    rotationStateZ = 0;
                }
                this.schematic = schematicRotated;

                refreshChests();
                break;
            default:
                Reference.logger.debug("Incorrect direction given to rotate function!");
        }
    }

    public Vector3f dimensions() {
        return new Vector3f(this.schematic.getWidth(), this.schematic.getHeight(), this.schematic.getLength());
    }

    public String getDebugDimensions() {
        return "WHL: " + getWidth() + " / " + getHeight() + " / " + getLength();
    }
}
