package com.github.lunatrius.schematica.world.schematic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
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
import com.github.lunatrius.schematica.compat.EntityTranslator;
import com.github.lunatrius.schematica.compat.TileEntityTranslator;
import com.github.lunatrius.schematica.nbt.NBTHelper;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.world.WorldDummy;
import com.github.lunatrius.schematica.world.storage.Schematic;

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
        Reference.logger.warn("Writing .litematic format is not supported.");
        return false;
    }

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

        List<RegionData> regionDataList = new ArrayList<>();
        Set<String> regionNames = regions.func_150296_c();

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

        Reference.logger.info("Litematic bounding box: {}x{}x{} (offset: {},{},{})",
            width, height, length, globalMinX, globalMinY, globalMinZ);

        ItemStack icon = new ItemStack(Blocks.grass);
        ISchematic schematic = new Schematic(icon, width, height, length);
        BlockStateTranslator translator = BlockStateTranslator.instance();

        for (RegionData rd : regionDataList) {
            readRegion(rd, schematic, translator, globalMinX, globalMinY, globalMinZ);
        }

        return schematic;
    }

    private void readRegion(RegionData rd, ISchematic schematic, BlockStateTranslator translator,
                            int offsetX, int offsetY, int offsetZ) {
        NBTTagCompound region = rd.region;

        int absSizeX = Math.abs(rd.sizeX);
        int absSizeY = Math.abs(rd.sizeY);
        int absSizeZ = Math.abs(rd.sizeZ);

        NBTTagList paletteList = region.getTagList("BlockStatePalette", Constants.NBT.TAG_COMPOUND);
        int paletteSize = paletteList.tagCount();

        if (paletteSize == 0) {
            Reference.logger.warn("Region '{}' has empty palette, skipping", rd.name);
            return;
        }

        // Build palette and keep original state strings for TE translation
        BlockMapping[] palette = new BlockMapping[paletteSize];
        String[] paletteStateStrings = new String[paletteSize];
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
            paletteStateStrings[i] = fullState;
        }

        // Unpack bit-packed block states
        long[] blockStatesArray = LitematicaNBTReader.getLongArray(region, "BlockStates");
        if (blockStatesArray == null || blockStatesArray.length == 0) {
            Reference.logger.warn("Region '{}' has no BlockStates data, skipping", rd.name);
            return;
        }

        long totalBlocks = (long) absSizeX * absSizeY * absSizeZ;
        int bitsPerEntry = LitematicBitArray.getRequiredBits(paletteSize);
        LitematicBitArray bitArray = new LitematicBitArray(bitsPerEntry, totalBlocks, blockStatesArray);

        // Build a map of schematic-local position -> block state string for TE translation
        Map<Long, String> posToBlockState = new HashMap<>();

        int regionOriginX = rd.sizeX < 0 ? rd.posX + rd.sizeX + 1 : rd.posX;
        int regionOriginY = rd.sizeY < 0 ? rd.posY + rd.sizeY + 1 : rd.posY;
        int regionOriginZ = rd.sizeZ < 0 ? rd.posZ + rd.sizeZ + 1 : rd.posZ;

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
                        continue;
                    }

                    int worldX = rd.sizeX < 0 ? rd.posX + rd.sizeX + 1 + x : rd.posX + x;
                    int worldY = rd.sizeY < 0 ? rd.posY + rd.sizeY + 1 + y : rd.posY + y;
                    int worldZ = rd.sizeZ < 0 ? rd.posZ + rd.sizeZ + 1 + z : rd.posZ + z;

                    int localX = worldX - offsetX;
                    int localY = worldY - offsetY;
                    int localZ = worldZ - offsetZ;

                    if (localX >= 0 && localX < schematic.getWidth()
                        && localY >= 0 && localY < schematic.getHeight()
                        && localZ >= 0 && localZ < schematic.getLength()) {
                        schematic.setBlock(localX, localY, localZ, mapping.block, mapping.metadata);

                        // Store block state string for TE translation (skulls, signs, etc.)
                        String stateStr = paletteStateStrings[paletteIndex];
                        if (stateStr != null && needsBlockStateForTE(stateStr)) {
                            long posKey = ((long) localX & 0xFFFFL) | (((long) localY & 0xFFFFL) << 16) | (((long) localZ & 0xFFFFL) << 32);
                            posToBlockState.put(posKey, stateStr);
                        }
                    }
                }
            }
        }

        // Read tile entities
        TileEntityTranslator teTranslator = TileEntityTranslator.instance();
        if (region.hasKey("TileEntities", Constants.NBT.TAG_LIST)) {
            NBTTagList tileEntitiesList = region.getTagList("TileEntities", Constants.NBT.TAG_COMPOUND);
            int teLoaded = 0;
            int teSkipped = 0;
            for (int i = 0; i < tileEntitiesList.tagCount(); i++) {
                try {
                    NBTTagCompound teTag = tileEntitiesList.getCompoundTagAt(i);
                    if (teTag.hasKey("x") && teTag.hasKey("y") && teTag.hasKey("z")) {
                        int teX = teTag.getInteger("x") + regionOriginX - offsetX;
                        int teY = teTag.getInteger("y") + regionOriginY - offsetY;
                        int teZ = teTag.getInteger("z") + regionOriginZ - offsetZ;
                        teTag.setInteger("x", teX);
                        teTag.setInteger("y", teY);
                        teTag.setInteger("z", teZ);

                        // Look up block state string at this position for TE-specific translation
                        long posKey = ((long) teX & 0xFFFFL) | (((long) teY & 0xFFFFL) << 16) | (((long) teZ & 0xFFFFL) << 32);
                        String blockStateStr = posToBlockState.get(posKey);

                        String originalId = teTag.hasKey("id") ? teTag.getString("id") : "unknown";
                        if (!teTranslator.translate(teTag, blockStateStr)) {
                            Reference.logger.debug("TileEntity '{}' could not be translated, skipping", originalId);
                            teSkipped++;
                            continue;
                        }

                        TileEntity te = NBTHelper.readTileEntityFromCompound(teTag);
                        if (te != null) {
                            schematic.setTileEntity(te.xCoord, te.yCoord, te.zCoord, te);
                            teLoaded++;
                        } else {
                            Reference.logger.debug("TileEntity '{}' translated to '{}' but failed to instantiate",
                                originalId, teTag.getString("id"));
                            teSkipped++;
                        }
                    }
                } catch (Exception e) {
                    Reference.logger.debug("Failed to load TileEntity from litematic region '{}': {}", rd.name, e.getMessage());
                    teSkipped++;
                }
            }
            if (teLoaded > 0 || teSkipped > 0) {
                Reference.logger.info("Region '{}': loaded {} TileEntities, skipped {}", rd.name, teLoaded, teSkipped);
            }
        }

        // Read entities
        EntityTranslator entityTranslator = EntityTranslator.instance();
        if (region.hasKey("Entities", Constants.NBT.TAG_LIST)) {
            NBTTagList entitiesList = region.getTagList("Entities", Constants.NBT.TAG_COMPOUND);
            int entLoaded = 0;
            int entSkipped = 0;
            for (int i = 0; i < entitiesList.tagCount(); i++) {
                try {
                    NBTTagCompound entityTag = entitiesList.getCompoundTagAt(i);

                    if (entityTag.hasKey("Pos", Constants.NBT.TAG_LIST)) {
                        NBTTagList posList = entityTag.getTagList("Pos", Constants.NBT.TAG_DOUBLE);
                        if (posList.tagCount() == 3) {
                            double ex = posList.func_150309_d(0) + rd.posX - offsetX;
                            double ey = posList.func_150309_d(1) + rd.posY - offsetY;
                            double ez = posList.func_150309_d(2) + rd.posZ - offsetZ;
                            NBTTagList newPos = new NBTTagList();
                            newPos.appendTag(new net.minecraft.nbt.NBTTagDouble(ex));
                            newPos.appendTag(new net.minecraft.nbt.NBTTagDouble(ey));
                            newPos.appendTag(new net.minecraft.nbt.NBTTagDouble(ez));
                            entityTag.setTag("Pos", newPos);
                        }
                    }

                    String originalId = entityTag.hasKey("id") ? entityTag.getString("id") : "unknown";
                    if (!entityTranslator.translate(entityTag)) {
                        Reference.logger.debug("Entity '{}' could not be translated, skipping", originalId);
                        entSkipped++;
                        continue;
                    }

                    Entity entity = EntityList.createEntityFromNBT(entityTag, WorldDummy.instance());
                    if (entity != null) {
                        // Sync prev values so rendering with partialTicks=0 uses the correct state
                        entity.prevRotationYaw = entity.rotationYaw;
                        entity.prevRotationPitch = entity.rotationPitch;
                        entity.prevPosX = entity.posX;
                        entity.prevPosY = entity.posY;
                        entity.prevPosZ = entity.posZ;
                        entity.lastTickPosX = entity.posX;
                        entity.lastTickPosY = entity.posY;
                        entity.lastTickPosZ = entity.posZ;

                        // Sync body/head yaw for living entities so renderers show correct orientation
                        if (entity instanceof EntityLivingBase) {
                            EntityLivingBase living = (EntityLivingBase) entity;
                            living.renderYawOffset = entity.rotationYaw;
                            living.prevRenderYawOffset = entity.rotationYaw;
                            living.rotationYawHead = entity.rotationYaw;
                            living.prevRotationYawHead = entity.rotationYaw;
                        }

                        schematic.addEntity(entity);
                        entLoaded++;
                    } else {
                        entSkipped++;
                    }
                } catch (Exception e) {
                    Reference.logger.debug("Failed to load Entity from litematic region '{}': {}", rd.name, e.getMessage());
                    entSkipped++;
                }
            }
            if (entLoaded > 0 || entSkipped > 0) {
                Reference.logger.info("Region '{}': loaded {} Entities, skipped {}", rd.name, entLoaded, entSkipped);
            }
        }

        Reference.logger.info("Region '{}': {}x{}x{}, palette size {}, loaded successfully",
            rd.name, absSizeX, absSizeY, absSizeZ, paletteSize);
    }

    /**
     * Returns true if the block state string is for a block type that needs
     * its state info passed to the TileEntity translator (skulls, signs, etc.)
     */
    private boolean needsBlockStateForTE(String stateStr) {
        return stateStr.contains("skull") || stateStr.contains("head")
            || stateStr.contains("sign") || stateStr.contains("banner");
    }

    private String buildPropertiesString(NBTTagCompound props) {
        Set<String> keys = props.func_150296_c();
        if (keys.isEmpty()) return "";

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

    public static boolean isLitematicFormat(NBTTagCompound tagCompound) {
        return tagCompound.hasKey("Version")
            && tagCompound.hasKey("Regions")
            && tagCompound.hasKey("Metadata");
    }

    private static class RegionData {
        String name;
        NBTTagCompound region;
        int posX, posY, posZ;
        int sizeX, sizeY, sizeZ;
    }
}
