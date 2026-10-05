package com.github.lunatrius.schematica.tool;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldServer;

import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.nbt.NBTHelper;
import com.github.lunatrius.schematica.task.TaskRegistry;
import com.github.lunatrius.schematica.util.SchematicLimits;
import com.github.lunatrius.schematica.world.storage.RegionMask;
import com.github.lunatrius.schematica.world.storage.RegionSelection;

public final class WorldMoveJob extends WorldEditTask {
    private final RegionSelection selection;
    private final int dx, dy, dz;
    private final BitSet mask;
    private final BlockMoveTransaction<Cell> transaction;
    private final SilentBlockPlacement writer = new SilentBlockPlacement();
    private final List<Entity> entities = new ArrayList<>();
    private boolean capturedEntities;

    public WorldMoveJob(UUID player, int dimension, List<SchematicRegion> regions, int dx, int dy, int dz) {
        this(player, dimension, new RegionSelection(regions), dx, dy, dz);
    }
    private WorldMoveJob(UUID player, int dimension, RegionSelection selection, int dx, int dy, int dz) {
        super(player, dimension, Math.addExact(selection.minX, dx), Math.addExact(selection.minY, dy), Math.addExact(selection.minZ, dz));
        this.selection = selection; this.dx = dx; this.dy = dy; this.dz = dz;
        SchematicLimits.worldBounds(selection.minX, selection.minY, selection.minZ, selection.maxX, selection.maxY, selection.maxZ);
        SchematicLimits.worldBounds(x, y, z, (long) selection.maxX + dx, (long) selection.maxY + dy, (long) selection.maxZ + dz);
        int width = selection.maxX - selection.minX + 1, height = selection.maxY - selection.minY + 1, length = selection.maxZ - selection.minZ + 1;
        mask = RegionMask.create(selection.localRegions, width, height, length);
        transaction = new BlockMoveTransaction<>(selection.minX, selection.minY, selection.minZ, width, height, length, dx, dy, dz, mask, new Cell(Blocks.air, 0, null, null));
    }
    @Override public boolean step(WorldServer world) {
        boolean done = transaction.step(new BlockMoveTransaction.Access<Cell>() {
            @Override public Cell read(int x, int y, int z) {
                if (!world.blockExists(x, y, z)) throw new IllegalArgumentException("Move requires loaded source and target chunks");
                Block block = world.getBlock(x, y, z); int meta = world.getBlockMetadata(x, y, z);
                TileEntity tile = world.getTileEntity(x, y, z);
                if (block.hasTileEntity(meta) && tile == null) throw new IllegalArgumentException("Missing tile entity at " + x + ", " + y + ", " + z);
                return new Cell(block, meta, tile == null ? null : NBTHelper.writeTileEntityToCompound(tile), tile);
            }
            @Override public void write(int x, int y, int z, Cell cell) {
                writer.setBlock(world, x, y, z, cell.block, cell.meta);
                if (cell.tag != null) {
                    NBTTagCompound tag = (NBTTagCompound) cell.tag.copy();
                    boolean restoring = transaction.phase() == BlockMoveTransaction.Phase.RESTORE_SOURCE || transaction.phase() == BlockMoveTransaction.Phase.RESTORE_TARGET;
                    if (!restoring && cell.original != null) com.github.lunatrius.schematica.compat.CoordinateLinks.move(cell.original, tag, x, y, z);
                    tag.setInteger("x", x); tag.setInteger("y", y); tag.setInteger("z", z);
                    TileEntity tile = restoring ? cell.original : "savedMultipart".equals(tag.getString("id"))
                        ? com.github.lunatrius.schematica.nbt.ForgeMultipart.createFromNBT(tag, false) : TileEntity.createAndLoadEntity(tag);
                    if (tile == null) throw new IllegalArgumentException("Unable to restore moved tile at " + x + ", " + y + ", " + z);
                    if (restoring) tile.readFromNBT(tag);
                    tile.xCoord = x; tile.yCoord = y; tile.zCoord = z; writer.setTile(world, tile);
                }
            }
            @Override public void changed(int x, int y, int z) { world.notifyBlocksOfNeighborChange(x, y, z, world.getBlock(x, y, z)); }
        }, cancelled);
        if (!capturedEntities && transaction.phase() == BlockMoveTransaction.Phase.CLEAR) {
            capturedEntities = true;
            for (Object value : world.getEntitiesWithinAABBExcludingEntity(null, AxisAlignedBB.getBoundingBox(selection.minX, selection.minY, selection.minZ,
                selection.maxX + 1.0, selection.maxY + 1.0, selection.maxZ + 1.0))) {
                Entity entity = (Entity) value;
                if (!(entity instanceof EntityPlayer) && contains(entity)) entities.add(entity);
            }
        }
        if (done && transaction.successful()) {
            blockCount = transaction.affected();
            for (Entity entity : entities) if (!entity.isDead && entity.worldObj == world && contains(entity)) {
                if (entity instanceof net.minecraft.entity.EntityHanging) {
                    net.minecraft.entity.EntityHanging hanging = (net.minecraft.entity.EntityHanging) entity;
                    hanging.field_146063_b += dx; hanging.field_146064_c += dy; hanging.field_146062_d += dz;
                    hanging.setDirection(hanging.hangingDirection);
                } else entity.setPositionAndRotation(entity.posX + dx, entity.posY + dy, entity.posZ + dz, entity.rotationYaw, entity.rotationPitch);
                entityCount++;
            }
        }
        return done;
    }
    private boolean contains(Entity entity) {
        int x = (int) Math.floor(entity.posX) - selection.minX, y = (int) Math.floor(entity.posY) - selection.minY, z = (int) Math.floor(entity.posZ) - selection.minZ;
        int width = selection.maxX - selection.minX + 1, length = selection.maxZ - selection.minZ + 1;
        return x >= 0 && x < width && y >= 0 && y <= selection.maxY - selection.minY && z >= 0 && z < length && mask.get(x + width * (z + length * y));
    }
    @Override public void flushBlockChanges(WorldServer world) { writer.flush(world); }
    @Override public TaskRegistry.Kind taskKind() { return TaskRegistry.Kind.MOVE; }
    @Override public boolean needsRollback() { return transaction.mutated() && transaction.phase() != BlockMoveTransaction.Phase.DONE; }
    @Override public RuntimeException failure() { return transaction.failure(); }
    @Override public void publishProgress(TaskRegistry.Task task) {
        task.update(transaction.phase() == BlockMoveTransaction.Phase.CAPTURE ? TaskRegistry.Stage.CAPTURE : TaskRegistry.Stage.WRITE,
            transaction.completed(), transaction.volume(), blockCount, entityCount);
    }
    private static final class Cell {
        final Block block;
        final int meta;
        final NBTTagCompound tag;
        final TileEntity original;
        Cell(Block block, int meta, NBTTagCompound tag, TileEntity original) { this.block = block; this.meta = meta; this.tag = tag; this.original = original; }
    }
}
