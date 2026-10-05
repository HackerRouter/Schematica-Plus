// Railcraft track directions turned with the schematic, by HackerRouter, 2026.
package com.github.lunatrius.schematica.compat;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.lunatrius.schematica.api.ISchematicVisualAdapter;
import com.github.lunatrius.schematica.util.SchematicTransform;

/**
 * Railcraft's reversible tracks (one way, buffer stop, boarding and holding, direction detector: "direction";
 * control, gated, speed transition: "reversed") point north on north-south rails and east on east-west rails
 * unless reversed. The flag is recomputed for the turned rail, whose metadata the schematic world already turned.
 * Switches change hands ("Direction") when mirrored.
 */
final class RailcraftTrackAdapter implements ISchematicVisualAdapter {
    private static final String[] FLAGS = {"direction", "reversed"};

    /** Whether a rail shape runs north-south (flat or ascending north or south). */
    static boolean northSouth(int shape) { return shape == 0 || shape == 4 || shape == 5; }

    /** The data of a track whose rail now has the shape newShape (power bit removed). */
    static void transform(NBTTagCompound tag, int newShape, char operation) {
        if (newShape < 6 && (operation == 'Y' || operation == 'x' || operation == 'z')) {
            boolean nowNorthSouth = northSouth(newShape), wasNorthSouth = operation == 'Y' ? !nowNorthSouth : nowNorthSouth;
            for (String key : FLAGS) {
                if (!tag.hasKey(key, NBT.TAG_BYTE)) continue;
                boolean reversed = tag.getBoolean(key);
                ForgeDirection forward = wasNorthSouth ? (reversed ? ForgeDirection.SOUTH : ForgeDirection.NORTH)
                    : (reversed ? ForgeDirection.WEST : ForgeDirection.EAST);
                ForgeDirection turned = SchematicTransform.direction(operation, forward);
                tag.setBoolean(key, nowNorthSouth ? turned == ForgeDirection.SOUTH : turned == ForgeDirection.WEST);
            }
        }
        if (Character.isLowerCase(operation) && operation != 'y' && tag.hasKey("Direction", NBT.TAG_BYTE)) {
            tag.setBoolean("Direction", !tag.getBoolean("Direction"));
        }
    }

    private static int shape(TileEntity tile) {
        int meta = tile.getWorldObj().getBlockMetadata(tile.xCoord, tile.yCoord, tile.zCoord);
        boolean flexible = ((net.minecraft.block.BlockRailBase) tile.getWorldObj().getBlock(tile.xCoord, tile.yCoord, tile.zCoord))
            .isFlexibleRail(tile.getWorldObj(), tile.xCoord, tile.yCoord, tile.zCoord);
        return flexible ? meta : meta & 7;
    }

    @Override public String id() { return "plus:railcraft_tracks"; }
    @Override public boolean supports(TileEntity tile) { return Reflect.is(tile, "mods.railcraft.common.blocks.tracks.TileTrack"); }
    @Override public NBTTagCompound capture(TileEntity tile) { return null; }
    @Override public void restore(TileEntity tile, NBTTagCompound data) {}
    @Override public boolean replacesDescriptionPacket(TileEntity tile) { return false; }
    @Override public boolean transformsNBT(TileEntity tile) { return true; }

    @Override public void transformNBT(TileEntity tile, NBTTagCompound data, char operation) {
        transform(data, shape(tile), operation);
    }

    @Override public void transformPreview(TileEntity tile, char operation) {
        NBTTagCompound data = new NBTTagCompound();
        tile.writeToNBT(data);
        transform(data, shape(tile), operation);
        tile.readFromNBT(data);
    }
}
