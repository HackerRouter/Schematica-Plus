package com.github.lunatrius.schematica.compat;

import net.minecraft.block.Block;

/**
 * Immutable data class representing a resolved 1.7.10 block + metadata pair.
 *
 * @author HackerRouter
 */
public final class BlockMapping {

    public final Block block;
    public final int metadata;

    public BlockMapping(Block block, int metadata) {
        this.block = block;
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "BlockMapping{block=" + block + ", meta=" + metadata + "}";
    }
}
