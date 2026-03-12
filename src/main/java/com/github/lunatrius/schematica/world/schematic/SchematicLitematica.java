package com.github.lunatrius.schematica.world.schematic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.compat.BlockMapping;
import com.github.lunatrius.schematica.compat.BlockStateTranslator;
import com.github.lunatrius.schematica.nbt.NBTHelper;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.world.storage.Schematic;

/**
 * Parser for the Litematica .litematic schematic format.
 * <p>
 * .litematic NBT structure:
 * <pre>
 *   Version: int (e.g. 5 or 6)
 *   MinecraftDataVersion: int
 *   Metadata:
 *     Name: string
 *     Author: string
 *     EnclosingSize: {x, y, z}
 *     TotalBlocks: int
 *     TotalVolume: int
 *     RegionCount: int
 *   Regions:
 *     [regionName]:
 *       Position: {x, y, z}
 *       Size: {x, y, z}
 *       BlockStatePalette: list of compounds {Name: "minecraft:stone", Properties: {variant: "granite"}}
 *       BlockStates: long[] (bit-packed palette indices)
 *       TileEntities: list of compounds
 *       Entities: list of compounds
 *       PendingBlockTicks: list
 *       PendingFluidTicks: list
 * </pre>
 * <p>
 * Multi-region schematics are merged into a single ISchematic by computing the
 * bounding box across all regions and offsetting each region's blocks accordingly.
 *
 * @author HackerRouter
 */
public class SchematicLitematica extends SchematicFormat {

    @Override
    public ISchematic readFromNBT(NBTTagCompound tagCompound) {
        try {
            return readLitematica(tagCompound);
        } catch (Exception e) {
            Reference.logger.error("Failed to parse .litematic schematic!", e);
            return null;
        }
    }

    @Override
    public boolean writeToNBT(NBTTagCompound tagCompound, ISchematic schematic, World backupWorld) {
        // Writing .litematic format is not supported — we only need read support for backport
        Reference.logger.warn("Writing .litematic format is not supported. Use .schematic format instead.");
        return false;
    }

    /**
     * Main parse entry point for .litematic files.
     */
    private ISchematic readLitematica(NBTTagCompound root) {
        int version = root.getInteger("Version");
        Reference.logger.info("Reading .litematic schematic, format version {}", version);

        NBTTagCompound metadata = root.getCompoundTag("Metadata");
        NBTTagCompound regions = root.getCompoundTag("Regions");

        if (regions.hasNoTags()) {
            Reference.logger.error("No regions found in .litematic file!");
            return null;
        }

        String name = metadata.hasKey("Name") ? metadata.getString("Name") : "Unknown";
        Reference.logger.info("Litematic schematic '{}' with {} region(s)", name, getRegionCount(regions));

        // Collect all region data to compute the global bounding box
        List<RegionData> regionDataList = new ArrayList<>();
        Set<String> regionNames = regions.func_150296_c();

        // First pass: parse region positions and sizes
        int globalMinX = Integer.MAX_VALUE, globalMinY = Integer.MAX_VALUE, globalMinZ = Integer.MAX_VALUE;
        int globalMaxX = Integer.MIN_VALUE, globalMaxY = Integer.MIN_VALUE, globalMaxZ = Integer.MIN_VALUE;

        for (String regionName : regionNames) {
            NBTTagCompound region = regions.getCompoundTag(regionName);
            NBTTagCompound posTag = region.getCompoundTag("Position");
            NBTTagCompound sizeTag = region.getCompoundTag("Size");

            int posX = posTag.getInteger("x");
            int posY = posTag.getInteger("y");
            int posZ = posTag.getInteger("z");
            int sizeX = sizeTag.getInteger("x");
            int sizeY = sizeTag.getInteger("y");
            int sizeZ = sizeTag.getInteger("z");

            // Size can be negative — compute actual min/max corners
            int minX = posX + Math.min(0, sizeX + (sizeX < 0 ? 1 : 0));
            int minY = posY + Math.min(0, sizeY + (sizeY < 0 ? 1 : 0));
            int minZ = posZ + Math.min(0, sizeZ + (sizeZ < 0 ? 1 : 0));
            int maxX = posX + Math.max(0, sizeX - (sizeX > 0 ? 1 : 0));
            int maxY = posY + Math.max(0, sizeY - (sizeY > 0 ? 1 : 0));
            int maxZ = posZ + Math.max(0, sizeZ - (sizeZ > 0 ? 1 : 0));

            globalMinX = Math.min(globalMinX, minX);
            globalMinY = Math.min(globalMinY, minY);
            globalMinZ = Math.min(globalMinZ, minZ);
            globalMaxX = Math.max(globalMaxX, maxX);
            globalMaxY = Math.max(globalMaxY, maxY);
            globalMaxZ = Math.max(globalMaxZ, maxZ);

            RegionData rd = new RegionData();
            rd.name = regionName;
            rd.region = region;
            rd.posX = posX;
            rd.posY = posY;
            rd.posZ = posZ;
            rd.sizeX = sizeX;
            rd.sizeY = sizeY;
            rd.sizeZ = sizeZ;
            regionDataList.add(rd);
        }

        int width = globalMaxX - globalMinX + 1;
        int height = globalMaxY - globalMinY + 1;
        int length = globalMaxZ - globalMinZ + 1;

        Reference.logger.info("Litematic bounding box: {}x{}x{} (offset: {},{},{})", width, height, length, globalMinX, globalMinY, globalMinZ);

        ItemStack icon = new ItemStack(Blocks.grass);
        ISchematic schematic = new Schematic(icon, width, height, length);

        BlockStateTranslator translator = BlockStateTranslator.instance();

        // Second pass: populate blocks from each region
        for (RegionData rd : regionDataList) {
            readRegion(rd, schematic, translator, globalMinX, globalMinY, globalMinZ);
        }

        return schematic;
    }

    /**
     * Reads a single region and places its blocks into the schematic.
     */
    private void readRegion(RegionData rd, ISchematic schematic, BlockStateTranslator translator,
                            int offsetX, int offsetY, int offsetZ) {
        NBTTagCompound region = rd.region;

        // Absolute sizes (region dimensions can be negative)
        int absSizeX = Math.abs(rd.sizeX);
        int absSizeY = Math.abs(rd.sizeY);
        int absSizeZ = Math.abs(rd.sizeZ);

        // Parse the block state palette
        NBTTagList paletteList = region.getTagList("BlockStatePalette", Constants.NBT.TAG_COMPOUND);
        int paletteSize = paletteList.tagCount();

        if (paletteSize == 0) {
            Reference.logger.warn("Region '{}' has empty palette, skipping", rd.name);
            return;
        }

        // Build palette: index -> translated BlockMapping
        BlockMapping[] palette = new BlockMapping[paletteSize];
        for (int i = 0; i < paletteSize; i++) {
            NBTTagCompound entry = paletteList.getCompoundTagAt(i);
            String blockName = entry.getString("Name");

            String propsString = "";
            if (entry.hasKey("Properties", Constants.NBT.TAG_COMPOUND)) {
                NBTTagCompound props = entry.getCompoundTag("Properties");
                propsString = buildPropertiesString(props);
            }

            String fullState = propsString.isEmpty() ? blockName : blockName + "[" + propsString + "]";
            palette[i] = translator.translate(fullState);
        }

        // Unpack bit-packed block states using our custom long array reader
        long[] blockStatesArray = LitematicaNBTReader.getLongArray(region, "BlockStates");
        if (blockStatesArray == null || blockStatesArray.length == 0) {
            Reference.logger.warn("Region '{}' has no BlockStates data, skipping", rd.name);
            return;
        }

        long totalBlocks = (long) absSizeX * absSizeY * absSizeZ;
        int bitsPerEntry = LitematicBitArray.getRequiredBits(paletteSize);
        LitematicBitArray bitArray = new LitematicBitArray(bitsPerEntry, totalBlocks, blockStatesArray);

        // Litematica index order: x + z * sizeX + y * sizeX * sizeZ
        for (int y = 0; y < absSizeY; y++) {
            for (int z = 0; z < absSizeZ; z++) {
                for (int x = 0; x < absSizeX; x++) {
                    long index = x + (long) z * absSizeX + (long) y * absSizeX * absSizeZ;
                    int paletteIndex = bitArray.getAt(index);

                    if (paletteIndex < 0 || paletteIndex >= paletteSize) {
                        continue;
                    }

                    BlockMapping mapping = palette[paletteIndex];
                    if (mapping.block == Blocks.air) {
                        continue; // Skip air blocks
                    }

                    // Compute world-relative position, accounting for negative region sizes
                    int worldX = rd.posX + (rd.sizeX < 0 ? -(x) + rd.sizeX + 1 : x);
                    int worldY = rd.posY + (rd.sizeY < 0 ? -(y) + rd.sizeY + 1 : y);
                    int worldZ = rd.posZ + (rd.sizeZ < 0 ? -(z) + rd.sizeZ + 1 : z);

                    // Translate to schematic-local coordinates
                    int localX = worldX - offsetX;
                    int localY = worldY - offsetY;
                    int localZ = worldZ - offsetZ;

                    if (localX >= 0 && localX < schematic.getWidth()
                        && localY >= 0 && localY < schematic.getHeight()
                        && localZ >= 0 && localZ < schematic.getLength()) {
                        schematic.setBlock(localX, localY, localZ, mapping.block, mapping.metadata);
                    }
                }
            }
        }

        // Read tile entities
        if (region.hasKey("TileEntities", Constants.NBT.TAG_LIST)) {
            NBTTagList tileEntitiesList = region.getTagList("TileEntities", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < tileEntitiesList.tagCount(); i++) {
                try {
                    NBTTagCompound teTag = tileEntitiesList.getCompoundTagAt(i);
                    // Litematica stores TE positions relative to the region
                    // We need to adjust to schematic-local coordinates
                    if (teTag.hasKey("x") && teTag.hasKey("y") && teTag.hasKey("z")) {
                        int teX = teTag.getInteger("x") + rd.posX - offsetX;
                        int teY = teTag.getInteger("y") + rd.posY - offsetY;
                        int teZ = teTag.getInteger("z") + rd.posZ - offsetZ;
                        teTag.setInteger("x", teX);
                        teTag.setInteger("y", teY);
                        teTag.setInteger("z", teZ);
                    }
                    TileEntity te = NBTHelper.readTileEntityFromCompound(teTag);
                    if (te != null) {
                        schematic.setTileEntity(te.xCoord, te.yCoord, te.zCoord, te);
                    }
                } catch (Exception e) {
                    Reference.logger.debug("Failed to load TileEntity from litematic region '{}': {}", rd.name, e.getMessage());
                }
            }
        }

        Reference.logger.info("Region '{}': {}x{}x{}, palette size {}, loaded successfully",
            rd.name, absSizeX, absSizeY, absSizeZ, paletteSize);
    }

    /**
     * Builds a sorted "key=value,key=value" string from an NBT Properties compound.
     */
    private String buildPropertiesString(NBTTagCompound props) {
        Set<String> keys = props.func_150296_c();
        if (keys.isEmpty()) return "";

        // Sort keys for consistent lookup
        List<String> sortedKeys = new ArrayList<>(keys);
        java.util.Collections.sort(sortedKeys);

        StringBuilder sb = new StringBuilder();
        for (String key : sortedKeys) {
            if (sb.length() > 0) sb.append(',');
            sb.append(key).append('=').append(props.getString(key));
        }
        return sb.toString();
    }

    private int getRegionCount(NBTTagCompound regions) {
        return regions.func_150296_c().size();
    }

    /**
     * Checks if an NBT compound looks like a .litematic file.
     * .litematic files have "Version", "Metadata", and "Regions" tags at root level.
     */
    public static boolean isLitematicFormat(NBTTagCompound tagCompound) {
        return tagCompound.hasKey("Version")
            && tagCompound.hasKey("Regions")
            && tagCompound.hasKey("Metadata");
    }

    /** Internal data holder for region parsing */
    private static class RegionData {
        String name;
        NBTTagCompound region;
        int posX, posY, posZ;
        int sizeX, sizeY, sizeZ;
    }
}
