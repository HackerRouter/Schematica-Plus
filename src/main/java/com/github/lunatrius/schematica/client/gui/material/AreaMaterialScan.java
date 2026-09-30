package com.github.lunatrius.schematica.client.gui.material;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.client.selection.AreaBlockCounter;
import com.github.lunatrius.schematica.reference.Reference;
import cpw.mods.fml.common.registry.GameData;

public final class AreaMaterialScan implements MaterialScanner {
    private final AreaBlockCounter<MaterialItemKey> counter;
    private final EntityPlayer player;
    private final AreaBlockCounter.Reader<MaterialItemKey> reader;
    private int logged;

    public AreaMaterialScan(List<SchematicRegion> regions, WorldClient world, EntityPlayer player, boolean renderLayers) {
        this.player = player;
        counter = new AreaBlockCounter<>(regions, renderLayers ? com.github.lunatrius.schematica.client.world.RenderLayerSettings.RANGE : null);
        reader = new AreaBlockCounter.Reader<MaterialItemKey>() {
            @Override public boolean loaded(int x, int y, int z) {
                return world.getChunkProvider().chunkExists(x >> 4, z >> 4)
                    && MaterialScan.verifiedChunk(world.getChunkFromChunkCoords(x >> 4, z >> 4), y);
            }
            @Override public MaterialItemKey read(int x, int y, int z) {
                try {
                    Block block = world.getBlock(x, y, z);
                    if (block.isAir(world, x, y, z)) return null;
                    ItemStack stack = block.getPickBlock(new MovingObjectPosition(x, y, z, 1,
                        Vec3.createVectorHelper(x + 0.5, y + 0.5, z + 0.5)), world, x, y, z, player);
                    if (stack == null || stack.getItem() == null) throw new IllegalArgumentException("Block has no item representation");
                    return new MaterialItemKey(stack);
                } catch (RuntimeException error) {
                    if (logged++ < 3) Reference.logger.debug("Could not analyze selection block at {}, {}, {}", x, y, z, error);
                    throw error;
                }
            }
        };
    }

    @Override public void step() { counter.step(reader, 4096, System.nanoTime() + 4_000_000L); }
    @Override public boolean done() { return counter.done(); }
    @Override public int percent() { return counter.percent(); }
    @Override public int skipped() { return counter.skipped(); }
    @Override public int unverified() { return counter.unverified(); }

    @Override public List<MaterialListModel.Entry<MaterialItemKey>> result() {
        if (!done()) throw new IllegalStateException("Area analysis is incomplete");
        List<MaterialListModel.Entry<MaterialItemKey>> result = new ArrayList<>();
        for (Map.Entry<MaterialItemKey, Integer> entry : counter.counts().entrySet()) {
            ItemStack stack = entry.getKey().stack();
            String registry = String.valueOf(GameData.getItemRegistry().getNameForObject(stack.getItem()));
            String name;
            try { name = stack.getDisplayName(); }
            catch (RuntimeException error) { name = registry; }
            result.add(new MaterialListModel.Entry<>(entry.getKey(), name, registry, entry.getValue(), 0, 0, 0));
        }
        MaterialScan.updateAvailable(result, player);
        return result;
    }
}
