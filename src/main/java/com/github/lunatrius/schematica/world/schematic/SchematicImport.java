package com.github.lunatrius.schematica.world.schematic;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.nbt.NBTHelper;
import com.github.lunatrius.schematica.reference.Reference;

/** Keep optional block entities only when their destination block can host them. */
final class SchematicImport {
    private SchematicImport() {}

    static boolean tile(ISchematic schematic, NBTTagCompound tag) {
        int x = tag.getInteger("x"), y = tag.getInteger("y"), z = tag.getInteger("z");
        try {
            Block block = schematic.getBlock(x, y, z);
            if (block != null && block.hasTileEntity(schematic.getBlockMetadata(x, y, z))) {
                TileEntity tile = NBTHelper.readTileEntityFromCompound(tag);
                if (tile != null) {
                    schematic.setTileEntity(x, y, z, tile);
                    return true;
                }
            }
        } catch (Exception | LinkageError e) {
            Reference.logger.debug("Skipped block entity '{}' at {} {} {}", tag.getString("id"), x, y, z, e);
        }
        ImportReport.omittedData("block entity: " + tag.getString("id"));
        return false;
    }
}
