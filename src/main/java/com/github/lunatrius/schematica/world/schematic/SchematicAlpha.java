package com.github.lunatrius.schematica.world.schematic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.api.event.PreSchematicSaveEvent;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.nbt.NBTHelper;
import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.world.WorldDummy;
import com.github.lunatrius.schematica.world.storage.Schematic;

import cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry;
import cpw.mods.fml.common.registry.GameData;

public class SchematicAlpha extends SchematicFormat {

    private static final FMLControlledNamespacedRegistry<Block> BLOCK_REGISTRY = GameData.getBlockRegistry();

    @Override
    public ISchematic readFromNBT(NBTTagCompound tagCompound) {
        ItemIdMaps.remap(tagCompound, ImportReport.items());
        ItemStack icon = SchematicUtil.getIconFromNBT(tagCompound);
        int width = tagCompound.getShort(Names.NBT.WIDTH);
        int height = tagCompound.getShort(Names.NBT.HEIGHT);
        int length = tagCompound.getShort(Names.NBT.LENGTH);
        int size = com.github.lunatrius.schematica.util.SchematicLimits.volume(width, height, length);
        int[] ids = SchematicBlockIds.read(tagCompound, size);
        byte[] metadata = tagCompound.getByteArray(Names.NBT.DATA);
        byte[] metadataHigh = tagCompound.getByteArray(DATA_HIGH);
        if (metadataHigh.length != 0 && metadataHigh.length != size) throw new IllegalArgumentException("Invalid extended metadata array");
        Map<Integer, Block> mapping = new HashMap<>();
        boolean hasMapping = tagCompound.hasKey(Names.NBT.MAPPING_SCHEMATICA);
        if (hasMapping) {
            NBTTagCompound names = tagCompound.getCompoundTag(Names.NBT.MAPPING_SCHEMATICA);
            for (Object key : names.func_150296_c()) {
                String name = (String) key;
                Block block = BLOCK_REGISTRY.containsKey(name) ? BLOCK_REGISTRY.getObject(name) : null;
                if (block == null) ImportReport.unknownBlock(name, "minecraft:air");
                mapping.put(names.getShort(name) & 65535, block == null ? Blocks.air : block);
            }
        }
        if (tagCompound.hasKey(REGION_DATA)) return readIndependent(tagCompound, icon, width, height, length);
        Schematic schematic = new Schematic(icon, width, height, length);
        schematic.setRegions(SchematicRegions.read(tagCompound, width, height, length));
        schematic.setOrigin(SchematicOrigins.read(tagCompound));
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    int index = x + (y * length + z) * width;
                    Block block = hasMapping ? mapping.get(ids[index]) : BLOCK_REGISTRY.getObjectById(ids[index]);
                    int meta = (metadata[index] & 255) | (metadataHigh.length == 0 ? 0 : (metadataHigh[index] & 255) << 8);
                    schematic.setBlock(x, y, z, block == null ? Blocks.air : block, meta);
                }
            }
        }
        NBTTagList tileEntitiesList = tagCompound.getTagList(Names.NBT.TILE_ENTITIES, Constants.NBT.TAG_COMPOUND);

        for (int i = 0; i < tileEntitiesList.tagCount(); i++) {
            try {
                TileEntity tileEntity = NBTHelper.readTileEntityFromCompound(tileEntitiesList.getCompoundTagAt(i));
                if (tileEntity != null) {
                    schematic.setTileEntity(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, tileEntity);
                }
            } catch (Exception e) {
                Reference.logger.error("TileEntity failed to load properly!", e);
            }
        }

        NBTTagList entitiesList = tagCompound.getTagList(Names.NBT.ENTITIES, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < entitiesList.tagCount(); i++) {
            try {
                NBTTagCompound entityCompound = entitiesList.getCompoundTagAt(i);
                Entity entity = EntityList.createEntityFromNBT(entityCompound, WorldDummy.instance());
                if (entity != null) {
                    schematic.addEntity(entity);
                }
            } catch (Exception e) {
                Reference.logger.error("Entity failed to load properly!", e);
            }
        }

        return schematic;
    }

    /** Independent (possibly overlapping) region contents of a .schemplus, each as a nested Alpha schematic. */
    static final String REGION_DATA = "SchematicaPlusRegionData";
    /** High byte of EndlessIDs extended metadata (above 255), .schemplus only; Data keeps the low byte. */
    static final String DATA_HIGH = "SchematicaPlusDataHigh";

    private ISchematic readIndependent(NBTTagCompound tag, ItemStack icon, int width, int height, int length) {
        if (tag.getInteger(REGION_DATA + "Version") != 1) throw new IllegalArgumentException("Unsupported region data");
        return independent(icon, width, height, length, SchematicOrigins.read(tag), SchematicRegions.read(tag, width, height, length),
            tag.getTagList(REGION_DATA, Constants.NBT.TAG_COMPOUND));
    }

    /** A multi-region schematic built from the region entries ({Name, Schematic}) in region order. */
    private ISchematic independent(ItemStack icon, int width, int height, int length,
        com.github.lunatrius.schematica.api.SchematicOrigin origin, List<com.github.lunatrius.schematica.api.SchematicRegion> regions, NBTTagList entries) {
        if (regions.isEmpty() || entries.tagCount() != regions.size()) throw new IllegalArgumentException("Invalid region data");
        com.github.lunatrius.schematica.world.storage.MultiRegionSchematic schematic =
            new com.github.lunatrius.schematica.world.storage.MultiRegionSchematic(icon, width, height, length);
        schematic.setOrigin(origin);
        schematic.setRegions(regions);
        for (int i = 0; i < entries.tagCount(); i++) {
            NBTTagCompound entry = entries.getCompoundTagAt(i);
            com.github.lunatrius.schematica.api.SchematicRegion region = regions.get(i);
            if (!region.name.equals(entry.getString("Name")) || entry.getCompoundTag("Schematic").hasKey(REGION_DATA)) {
                throw new IllegalArgumentException("Invalid region data");
            }
            schematic.addRegion(region, readFromNBT(entry.getCompoundTag("Schematic")));
        }
        return schematic;
    }

    /** The region entries of a schematic with independent region contents, or null when it has none. */
    NBTTagList regionEntries(ISchematic schematic, World backupWorld, boolean includeNBT, boolean includeEntities) {
        if (!com.github.lunatrius.schematica.world.storage.SchematicCopies.independent(schematic)) return null;
        NBTTagList entries = new NBTTagList();
        for (com.github.lunatrius.schematica.api.SchematicRegion region : schematic.getRegions()) {
            ISchematic part = schematic.getRegionSchematic(region.name);
            if (part == null) part = com.github.lunatrius.schematica.world.storage.SchematicCopies.extract(schematic, region);
            NBTTagCompound nested = new NBTTagCompound();
            if (!writeToNBT(nested, part, backupWorld, includeNBT, includeEntities, true)) throw new IllegalStateException("Region write failed");
            NBTTagCompound entry = new NBTTagCompound();
            entry.setString("Name", region.name);
            entry.setTag("Schematic", nested);
            entries.appendTag(entry);
        }
        return entries;
    }

    /** Download payload of the independent regions (compressed), or null for schematics without them. */
    public static byte[] regionPayload(ISchematic schematic) throws java.io.IOException {
        NBTTagList entries = new SchematicAlpha().regionEntries(schematic, null, true, true);
        if (entries == null) return null;
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Version", 1);
        tag.setTag("Regions", entries);
        return net.minecraft.nbt.CompressedStreamTools.compress(tag);
    }

    /** The downloaded flat schematic with its independent regions restored from a region payload. */
    public static ISchematic withRegionPayload(ISchematic merged, byte[] payload) throws java.io.IOException {
        NBTTagCompound tag = LitematicaNBTReader.readFromStream(new java.io.ByteArrayInputStream(payload));
        LitematicaNBTReader.clearLongArrayStore();
        if (tag.getInteger("Version") != 1) throw new java.io.IOException("Unsupported region payload");
        return new SchematicAlpha().independent(merged.getIcon(), merged.getWidth(), merged.getHeight(), merged.getLength(),
            merged.getOrigin(), merged.getRegions(), tag.getTagList("Regions", Constants.NBT.TAG_COMPOUND));
    }

    @Override
    public boolean writeToNBT(NBTTagCompound tagCompound, ISchematic schematic, World backupWorld) {
        return writeToNBT(tagCompound, schematic, backupWorld, SchematicFormat.saveNBT, SchematicFormat.saveEntities);
    }

    @Override
    public boolean writeToNBT(NBTTagCompound tagCompound, ISchematic schematic, World backupWorld,
        boolean includeNBT, boolean includeEntities) {
        return writeToNBT(tagCompound, schematic, backupWorld, includeNBT, includeEntities,
            ConfigurationHandler.useSchematicplusFormat);
    }

    boolean writeToNBT(NBTTagCompound tagCompound, ISchematic schematic, World backupWorld,
        boolean includeNBT, boolean includeEntities, boolean extended) {
        ItemStack icon = schematic.getIcon();
        if (icon != null) {
            NBTTagCompound tagCompoundIcon = new NBTTagCompound();
            icon.writeToNBT(tagCompoundIcon);
            tagCompound.setTag(Names.NBT.ICON, tagCompoundIcon);
        }

        tagCompound.setShort(Names.NBT.WIDTH, (short) schematic.getWidth());
        tagCompound.setShort(Names.NBT.LENGTH, (short) schematic.getLength());
        tagCompound.setShort(Names.NBT.HEIGHT, (short) schematic.getHeight());

        int size = schematic.getWidth() * schematic.getLength() * schematic.getHeight();
        byte[] localBlocks = new byte[size];
        byte[] localMetadata = new byte[size];
        byte[] metadataHigh = new byte[size];
        boolean highMetadata = false;
        byte[] extraBlocks = new byte[size];

        Map<String, Short> mappings = new HashMap<>();
        for (int x = 0; x < schematic.getWidth(); x++) {
            for (int y = 0; y < schematic.getHeight(); y++) {
                for (int z = 0; z < schematic.getLength(); z++) {
                    final int index = x + (y * schematic.getLength() + z) * schematic.getWidth();
                    final Block block = schematic.getBlock(x, y, z);
                    int blockId = BLOCK_REGISTRY.getId(block);
                    localBlocks[index] = BlockIdCodec.low(blockId);
                    extraBlocks[index] = BlockIdCodec.high(blockId);
                    int meta = schematic.getBlockMetadata(x, y, z);
                    localMetadata[index] = (byte) meta;
                    metadataHigh[index] = (byte) (meta >> 8);
                    highMetadata |= metadataHigh[index] != 0;
                    String name = BLOCK_REGISTRY.getNameForObject(block);
                    if (!mappings.containsKey(name)) {
                        mappings.put(name, (short) blockId);
                    }
                }
            }
        }

        int count = 20;
        NBTTagList tileEntitiesList = new NBTTagList();
        if (includeNBT) {
            for (TileEntity tileEntity : schematic.getTileEntities()) {
                try {
                    if (!tileEntity.hasWorldObj()) {
                        tileEntity.setWorldObj(backupWorld);
                    }
                    NBTTagCompound tileEntityTagCompound = NBTHelper.writeTileEntityToCompound(tileEntity);
                    tileEntitiesList.appendTag(tileEntityTagCompound);
                } catch (Exception e) {
                    if (--count > 0) {
                        Block block = schematic.getBlock(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
                        Reference.logger.error(
                            "Block {}[{}] with TileEntity {} failed to save its NBT",
                            block,
                            block != null ? BLOCK_REGISTRY.getNameForObject(block) : "?",
                            tileEntity.getClass()
                                .getName(),
                            e);
                    }
                }
            }
        }

        final NBTTagList entityList = new NBTTagList();
        if (includeEntities) {
            final List<Entity> entities = schematic.getEntities();
            for (Entity entity : entities) {
                try {
                    final NBTTagCompound entityCompound = NBTHelper.writeEntityToCompound(entity);
                    if (entityCompound != null) {
                        entityList.appendTag(entityCompound);
                    }
                } catch (Throwable t) {
                    Reference.logger.error("Entity {} failed to save, skipping!", entity, t);
                }
            }
        }

        PreSchematicSaveEvent event = new PreSchematicSaveEvent(schematic, mappings);
        MinecraftForge.EVENT_BUS.post(event);

        NBTTagCompound nbtMapping = new NBTTagCompound();
        for (Map.Entry<String, Short> entry : mappings.entrySet()) {
            nbtMapping.setShort(entry.getKey(), entry.getValue());
        }

        tagCompound.setString(Names.NBT.MATERIALS, Names.NBT.FORMAT_ALPHA);
        SchematicBlockIds.write(tagCompound, localBlocks, extraBlocks, extended || highMetadata || SchematicRegions.requiresExtended(schematic)
            || !schematic.getOrigin().isZero() || com.github.lunatrius.schematica.world.storage.SchematicCopies.independent(schematic));
        SchematicRegions.write(tagCompound, schematic);
        SchematicOrigins.write(tagCompound, schematic.getOrigin());
        tagCompound.setByteArray(Names.NBT.DATA, localMetadata);
        if (highMetadata) tagCompound.setByteArray(DATA_HIGH, metadataHigh);
        tagCompound.setTag(Names.NBT.ENTITIES, entityList);
        tagCompound.setTag(Names.NBT.TILE_ENTITIES, tileEntitiesList);
        tagCompound.setTag(Names.NBT.MAPPING_SCHEMATICA, nbtMapping);
        ItemIdMaps.writeMapping(tagCompound, ItemIdMaps.CURRENT);
        NBTTagList regionEntries = regionEntries(schematic, backupWorld, includeNBT, includeEntities);
        if (regionEntries != null) {
            tagCompound.setInteger(REGION_DATA + "Version", 1);
            tagCompound.setTag(REGION_DATA, regionEntries);
        }
        final NBTTagCompound extendedMetadata = event.extendedMetadata;
        if (!extendedMetadata.hasNoTags()) {
            tagCompound.setTag(Names.NBT.EXTENDED_METADATA, extendedMetadata);
        }

        return true;
    }
}
