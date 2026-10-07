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
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.api.SchematicOrigin;
import com.github.lunatrius.schematica.world.storage.MultiRegionSchematic;

import cpw.mods.fml.common.registry.GameData;

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

    /** .litematic files need long arrays, so they are written by SchematicFormat.saveToFile through LitematicExport. */
    @Override
    public boolean writeToNBT(NBTTagCompound tagCompound, ISchematic schematic, World backupWorld) {
        return false;
    }

    private ISchematic readLitematica(NBTTagCompound root) {
        LitematicRegions document = new LitematicRegions(root);
        MultiRegionSchematic schematic = new MultiRegionSchematic(new ItemStack(Blocks.grass), document.width, document.height, document.length);
        SchematicOrigin min = document.minimum;
        schematic.setOrigin(new SchematicOrigin(-min.x, -min.y, -min.z));
        List<SchematicRegion> bounds = new ArrayList<>();
        for (LitematicRegions.Region region : document.regions) bounds.add(region.box.offset(-min.x, -min.y, -min.z));
        schematic.setRegions(bounds);
        BlockStateTranslator translator = BlockStateTranslator.instance();
        boolean legacy = isLegacy(root);
        for (LitematicRegions.Region region : document.regions) {
            Schematic part = new Schematic(schematic.getIcon(), region.width, region.height, region.length);
            part.setOrigin(region.origin);
            readRegion(region, part, translator, legacy);
            schematic.addRegion(region.box.offset(-min.x, -min.y, -min.z), part);
        }
        return schematic;
    }

    /** Files before version 5 (or data version 1631, 1.13.2) hold 1.12 block states, as in Litematica's own check. */
    static boolean isLegacy(NBTTagCompound root) {
        int dataVersion = root.getInteger("MinecraftDataVersion");
        return root.getInteger("Version") < 5 || dataVersion > 0 && dataVersion < 1631;
    }

    private static Block registered(String name) {
        return GameData.getBlockRegistry().containsKey(name) ? GameData.getBlockRegistry().getObject(name) : null;
    }

    /** 1.12 metadata values that 1.7.10 does not have: polished stones and the wet sponge. */
    private static boolean newerVariant(String name, int meta) {
        return name.equals("minecraft:stone") && meta != 0 || name.equals("minecraft:sponge") && meta != 0;
    }

    /** A 1.12 palette entry: vanilla states by the 1.12 table, other blocks by name and SchematicaPlusMeta. */
    static BlockMapping legacyMapping(NBTTagCompound entry, BlockStateTranslator translator) {
        String state = LegacyBlockStates.state(entry);
        LegacyBlockStates.Entry known = LegacyBlockStates.entry(state);
        if (known != null) {
            Block block = registered(known.name);
            if (block != null && !newerVariant(known.name, known.meta())) return new BlockMapping(block, known.meta());
            return translator.translate(known.modern);
        }
        Block block = registered(entry.getString("Name"));
        if (block == null) return translator.translate(state);
        if (entry.hasKey(LitematicExport.META_KEY) || !entry.hasKey("Properties")) {
            return new BlockMapping(block, entry.getInteger(LitematicExport.META_KEY) & 0xffff);
        }
        BlockMapping translated = translator.translate(state);
        return translated.block == block ? translated : new BlockMapping(block, 0);
    }

    private void readRegion(LitematicRegions.Region rd, ISchematic schematic, BlockStateTranslator translator, boolean legacy) {
        int absSizeX = rd.width, absSizeY = rd.height, absSizeZ = rd.length;
        NBTTagList paletteList = rd.palette;
        int paletteSize = paletteList.tagCount();

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
            palette[i] = legacy ? legacyMapping(entry, translator) : translator.translate(fullState);
            paletteStateStrings[i] = fullState;
        }

        Map<Long, String> posToBlockState = new HashMap<>();

        for (int y = 0; y < absSizeY; y++) {
            for (int z = 0; z < absSizeZ; z++) {
                for (int x = 0; x < absSizeX; x++) {
                    int paletteIndex = rd.paletteIndex(x, y, z);
                    BlockMapping mapping = palette[paletteIndex];
                    if (mapping.block == Blocks.air) continue;
                    int localX = x, localY = y, localZ = z;

                    if (localX >= 0 && localX < schematic.getWidth()
                        && localY >= 0 && localY < schematic.getHeight()
                        && localZ >= 0 && localZ < schematic.getLength()) {
                        schematic.setBlock(localX, localY, localZ, mapping.block, mapping.metadata);

                        String stateStr = paletteStateStrings[paletteIndex];
                        if (stateStr != null && needsBlockStateForTE(stateStr)) {
                            long posKey = ((long) localX & 0xFFFFL) | (((long) localY & 0xFFFFL) << 16) | (((long) localZ & 0xFFFFL) << 32);
                            posToBlockState.put(posKey, stateStr);
                        }
                    }
                }
            }
        }

        TileEntityTranslator teTranslator = TileEntityTranslator.instance();
        java.util.Set<Long> existingTEPositions = new java.util.HashSet<>();
        {
            NBTTagList tileEntitiesList = rd.tileEntities();
            int teLoaded = 0;
            int teSkipped = 0;
            for (int i = 0; i < tileEntitiesList.tagCount(); i++) {
                try {
                    NBTTagCompound teTag = tileEntitiesList.getCompoundTagAt(i);
                    if (teTag.hasKey("x") && teTag.hasKey("y") && teTag.hasKey("z")) {
                        int teX = teTag.getInteger("x");
                        int teY = teTag.getInteger("y");
                        int teZ = teTag.getInteger("z");
                        teTag.setInteger("x", teX);
                        teTag.setInteger("y", teY);
                        teTag.setInteger("z", teZ);

                        long posKey = ((long) teX & 0xFFFFL) | (((long) teY & 0xFFFFL) << 16) | (((long) teZ & 0xFFFFL) << 32);
                        existingTEPositions.add(posKey);
                        String blockStateStr = posToBlockState.get(posKey);

                        String originalId = teTag.hasKey("id") ? teTag.getString("id") : "unknown";
                        if (legacy) {
                            teTag = LegacyNbt.tileFrom112(teTag);
                        } else if (!teTranslator.translate(teTag, blockStateStr)) {
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
                    Reference.logger.debug("Failed to load TileEntity from litematic region '{}': {}", rd.box.name, e.getMessage());
                    teSkipped++;
                }
            }
            if (teLoaded > 0 || teSkipped > 0) {
                Reference.logger.info("Region '{}': loaded {} TileEntities, skipped {}", rd.box.name, teLoaded, teSkipped);
            }
        }

        int synthCount = 0;
        for (Map.Entry<Long, String> entry : posToBlockState.entrySet()) {
            String stateStr = entry.getValue();
            if (!legacy && teTranslator.isPottedPlant(stateStr) && !existingTEPositions.contains(entry.getKey())) {
                long posKey = entry.getKey();
                int px = (int) (posKey & 0xFFFFL);
                int py = (int) ((posKey >> 16) & 0xFFFFL);
                int pz = (int) ((posKey >> 32) & 0xFFFFL);
                NBTTagCompound synthTE = teTranslator.createFlowerPotTE(px, py, pz, stateStr);
                TileEntity te = NBTHelper.readTileEntityFromCompound(synthTE);
                if (te != null) {
                    schematic.setTileEntity(px, py, pz, te);
                    synthCount++;
                }
            }
        }
        if (synthCount > 0) {
            Reference.logger.info("Region '{}': synthesized {} FlowerPot TileEntities for potted plants", rd.box.name, synthCount);
        }

        EntityTranslator entityTranslator = EntityTranslator.instance();
        {
            NBTTagList entitiesList = rd.entities();
            int entLoaded = 0;
            int entSkipped = 0;
            for (int i = 0; i < entitiesList.tagCount(); i++) {
                try {
                    NBTTagCompound entityTag = entitiesList.getCompoundTagAt(i);

                    String originalId = entityTag.hasKey("id") ? entityTag.getString("id") : "unknown";
                    if (legacy) {
                        entityTag = LegacyNbt.entityFrom112(entityTag);
                    } else if (!entityTranslator.translate(entityTag)) {
                        Reference.logger.debug("Entity '{}' could not be translated, skipping", originalId);
                        entSkipped++;
                        continue;
                    }

                    Entity entity = createEntity(entityTag);
                    if (entity != null) {
                        schematic.addEntity(entity);
                        entLoaded++;
                    } else {
                        entSkipped++;
                    }
                } catch (Exception e) {
                    Reference.logger.debug("Failed to load Entity from litematic region '{}': {}", rd.box.name, e.getMessage());
                    entSkipped++;
                }
            }
            if (entLoaded > 0 || entSkipped > 0) {
                Reference.logger.info("Region '{}': loaded {} Entities, skipped {}", rd.box.name, entLoaded, entSkipped);
            }
        }

        Reference.logger.info("Region '{}': {}x{}x{}, palette size {}, loaded successfully",
            rd.box.name, absSizeX, absSizeY, absSizeZ, paletteSize);
    }

    /** An entity from NBT with its previous position and rotation equal to the current ones, for still rendering. */
    static Entity createEntity(NBTTagCompound entityTag) {
        Entity entity = EntityList.createEntityFromNBT(entityTag, WorldDummy.instance());
        if (entity == null) return null;
        entity.prevRotationYaw = entity.rotationYaw;
        entity.prevRotationPitch = entity.rotationPitch;
        entity.prevPosX = entity.posX;
        entity.prevPosY = entity.posY;
        entity.prevPosZ = entity.posZ;
        entity.lastTickPosX = entity.posX;
        entity.lastTickPosY = entity.posY;
        entity.lastTickPosZ = entity.posZ;
        if (entity instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase) entity;
            living.renderYawOffset = entity.rotationYaw;
            living.prevRenderYawOffset = entity.rotationYaw;
            living.rotationYawHead = entity.rotationYaw;
            living.prevRotationYawHead = entity.rotationYaw;
        }
        return entity;
    }

    /**
     * Returns true if the block state string is for a block type that needs
     * its state info passed to the TileEntity translator (skulls, signs, etc.)
     */
    private boolean needsBlockStateForTE(String stateStr) {
        return stateStr.contains("skull") || stateStr.contains("head")
            || stateStr.contains("sign") || stateStr.contains("banner")
            || stateStr.contains("potted_");
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

    public static boolean isLitematicFormat(NBTTagCompound tagCompound) {
        return tagCompound.hasKey("Version")
            && tagCompound.hasKey("Regions")
            && tagCompound.hasKey("Metadata");
    }

}
