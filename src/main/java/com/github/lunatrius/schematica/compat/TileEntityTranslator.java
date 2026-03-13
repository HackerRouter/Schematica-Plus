
package com.github.lunatrius.schematica.compat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import com.github.lunatrius.schematica.reference.Reference;

/**
 * Translates modern (1.13+) TileEntity/BlockEntity NBT data to 1.7.10 format.
 *
 * @author HackerRouter
 */
public final class TileEntityTranslator {

    private static final TileEntityTranslator INSTANCE = new TileEntityTranslator();

    /** Modern namespaced ID -> 1.7.10 short ID */
    private final Map<String, String> idMap = new HashMap<>();

    private TileEntityTranslator() {
        initIdMappings();
    }

    public static TileEntityTranslator instance() {
        return INSTANCE;
    }

    /**
     * Translates a modern BlockEntity NBT compound to 1.7.10 format in-place.
     */
    public boolean translate(NBTTagCompound teTag) {
        return translate(teTag, null);
    }

    /**
     * Translates a modern BlockEntity NBT compound to 1.7.10 format in-place.
     *
     * @param teTag the NBT compound to translate (modified in-place)
     * @param blockStateString the full block state string (e.g. "minecraft:player_head[rotation=8]"), may be null
     * @return true if translation succeeded
     */
    public boolean translate(NBTTagCompound teTag, String blockStateString) {
        if (teTag == null || !teTag.hasKey("id")) {
            return false;
        }

        String modernId = teTag.getString("id");

        // Already a 1.7.10 short ID (no colon)
        if (!modernId.contains(":")) {
            return true;
        }

        String legacyId = idMap.get(modernId);
        if (legacyId == null) {
            String stripped = modernId;
            if (stripped.startsWith("minecraft:")) {
                stripped = stripped.substring(10);
            }
            if (!stripped.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                boolean capitalizeNext = true;
                for (char c : stripped.toCharArray()) {
                    if (c == '_') {
                        capitalizeNext = true;
                    } else {
                        sb.append(capitalizeNext ? Character.toUpperCase(c) : c);
                        capitalizeNext = false;
                    }
                }
                legacyId = sb.toString();
            }
            if (legacyId == null) {
                Reference.logger.warn("TileEntityTranslator: Unknown TE id '{}', cannot translate", modernId);
                return false;
            }
            Reference.logger.debug("TileEntityTranslator: Guessed legacy id '{}' for modern id '{}'", legacyId, modernId);
        }

        teTag.setString("id", legacyId);
        convertNBTData(modernId, teTag, blockStateString);
        return true;
    }

    private void convertNBTData(String modernId, NBTTagCompound teTag, String blockStateString) {
        // Categorize by type for variant-rich blocks
        if (isSkullId(modernId)) {
            convertSkull(teTag, blockStateString);
            return;
        }
        if (isSignId(modernId)) {
            SignTextConverter.convertSign(teTag);
            return;
        }
        if (isBannerId(modernId)) {
            convertBanner(teTag);
            return;
        }

        switch (modernId) {
            case "minecraft:chest":
            case "minecraft:trapped_chest":
            case "minecraft:barrel":
                convertContainer(teTag);
                break;
            case "minecraft:furnace":
            case "minecraft:blast_furnace":
            case "minecraft:smoker":
                convertFurnace(teTag);
                break;
            case "minecraft:brewing_stand":
                convertBrewingStand(teTag);
                break;
            case "minecraft:hopper":
            case "minecraft:dispenser":
            case "minecraft:dropper":
                convertContainer(teTag);
                break;
            case "minecraft:command_block":
                convertCommandBlock(teTag);
                break;
            case "minecraft:comparator":
                break;
            case "minecraft:spawner":
                convertSpawner(teTag);
                break;
            case "minecraft:jukebox":
                convertJukebox(teTag);
                break;
            case "minecraft:lectern":
                convertLectern(teTag);
                break;
            case "minecraft:enchanting_table":
                break;
            case "minecraft:beacon":
                convertBeacon(teTag);
                break;
            case "minecraft:flower_pot":
                convertFlowerPot(teTag);
                break;
            case "minecraft:noteblock":
            case "minecraft:note_block":
                convertNoteBlock(teTag);
                break;
            default:
                convertItemsGeneric(teTag);
                break;
        }
    }

    private boolean isSkullId(String id) {
        return id.contains("skull") || id.contains("head");
    }

    private boolean isSignId(String id) {
        return id.contains("sign");
    }

    private boolean isBannerId(String id) {
        return id.contains("banner");
    }

    // ==================== Skull ====================

    private void convertSkull(NBTTagCompound teTag, String blockStateString) {
        // Determine SkullType from block name
        // 0=skeleton, 1=wither_skeleton, 2=zombie, 3=player, 4=creeper, 5=dragon
        if (!teTag.hasKey("SkullType")) {
            byte skullType = 0;
            if (blockStateString != null) {
                String blockName = blockStateString;
                int bracket = blockName.indexOf('[');
                if (bracket >= 0) blockName = blockName.substring(0, bracket);
                if (blockName.contains("player")) skullType = 3;
                else if (blockName.contains("zombie")) skullType = 2;
                else if (blockName.contains("creeper")) skullType = 4;
                else if (blockName.contains("dragon")) skullType = 5;
                else if (blockName.contains("piglin")) skullType = 3;
                else if (blockName.contains("wither")) skullType = 1;
                else if (blockName.contains("skeleton")) skullType = 0;
            }
            teTag.setByte("SkullType", skullType);
        }

        // Extract rotation from block state properties (floor skulls: rotation=0..15)
        if (!teTag.hasKey("Rot")) {
            byte rot = 0;
            if (blockStateString != null) {
                int bracketStart = blockStateString.indexOf('[');
                if (bracketStart >= 0) {
                    String propsStr = blockStateString.substring(bracketStart + 1, blockStateString.length() - 1);
                    for (String prop : propsStr.split(",")) {
                        String[] kv = prop.split("=", 2);
                        if (kv.length == 2 && "rotation".equals(kv[0].trim())) {
                            try {
                                rot = Byte.parseByte(kv[1].trim());
                            } catch (NumberFormatException e) {
                                // ignore
                            }
                            break;
                        }
                    }
                }
            }
            teTag.setByte("Rot", rot);
        }

        // Convert modern "profile" tag to "Owner" for 1.7.10
        if (teTag.hasKey("profile", Constants.NBT.TAG_COMPOUND)) {
            NBTTagCompound profile = teTag.getCompoundTag("profile");
            NBTTagCompound owner = new NBTTagCompound();

            if (profile.hasKey("name")) {
                owner.setString("Name", profile.getString("name"));
                teTag.setString("ExtraType", profile.getString("name"));
            }

            if (profile.hasKey("id", Constants.NBT.TAG_INT_ARRAY)) {
                int[] uuidInts = profile.getIntArray("id");
                if (uuidInts.length == 4) {
                    owner.setString("Id", intArrayToUUID(uuidInts));
                }
            } else if (profile.hasKey("id", Constants.NBT.TAG_STRING)) {
                owner.setString("Id", profile.getString("id"));
            }

            // Convert properties — modern format is a TAG_LIST of compounds {name, value, signature}
            if (profile.hasKey("properties", Constants.NBT.TAG_LIST)) {
                NBTTagList propsList = profile.getTagList("properties", Constants.NBT.TAG_COMPOUND);
                NBTTagCompound ownerProps = new NBTTagCompound();
                Map<String, NBTTagList> grouped = new HashMap<>();
                for (int i = 0; i < propsList.tagCount(); i++) {
                    NBTTagCompound prop = propsList.getCompoundTagAt(i);
                    String propName = prop.getString("name");
                    NBTTagCompound legacyProp = new NBTTagCompound();
                    if (prop.hasKey("value")) legacyProp.setString("Value", prop.getString("value"));
                    if (prop.hasKey("signature")) legacyProp.setString("Signature", prop.getString("signature"));
                    if (!grouped.containsKey(propName)) {
                        grouped.put(propName, new NBTTagList());
                    }
                    grouped.get(propName).appendTag(legacyProp);
                }
                for (Map.Entry<String, NBTTagList> entry : grouped.entrySet()) {
                    ownerProps.setTag(entry.getKey(), entry.getValue());
                }
                owner.setTag("Properties", ownerProps);
            } else if (profile.hasKey("properties", Constants.NBT.TAG_COMPOUND)) {
                NBTTagCompound props = profile.getCompoundTag("properties");
                NBTTagCompound ownerProps = new NBTTagCompound();
                Set<String> keys = props.func_150296_c();
                for (String key : keys) {
                    if (props.hasKey(key, Constants.NBT.TAG_LIST)) {
                        ownerProps.setTag(key, props.getTagList(key, Constants.NBT.TAG_COMPOUND));
                    }
                }
                owner.setTag("Properties", ownerProps);
            }

            teTag.setTag("Owner", owner);
            teTag.removeTag("profile");

            // If we have a profile, it's a player head
            teTag.setByte("SkullType", (byte) 3);
        }

        // Handle SkullOwner
        if (teTag.hasKey("SkullOwner", Constants.NBT.TAG_COMPOUND)) {
            NBTTagCompound skullOwner = teTag.getCompoundTag("SkullOwner");
            if (skullOwner.hasKey("Name")) {
                teTag.setString("ExtraType", skullOwner.getString("Name"));
            }
            teTag.setTag("Owner", skullOwner);
            teTag.removeTag("SkullOwner");
        } else if (teTag.hasKey("SkullOwner", Constants.NBT.TAG_STRING)) {
            teTag.setString("ExtraType", teTag.getString("SkullOwner"));
            teTag.removeTag("SkullOwner");
        }

        // Remove modern-only tags
        teTag.removeTag("note_block_sound");
        teTag.removeTag("custom_name");
    }

    // ==================== Container ====================

    private void convertContainer(NBTTagCompound teTag) {
        convertItemsGeneric(teTag);
        teTag.removeTag("LootTable");
        teTag.removeTag("LootTableSeed");
    }

    // ==================== Furnace ====================

    private void convertFurnace(NBTTagCompound teTag) {
        convertItemsGeneric(teTag);
        if (teTag.hasKey("lit_time")) {
            teTag.setShort("BurnTime", teTag.getShort("lit_time"));
            teTag.removeTag("lit_time");
        }
        if (teTag.hasKey("cooking_time")) {
            teTag.setShort("CookTime", teTag.getShort("cooking_time"));
            teTag.removeTag("cooking_time");
        }
        teTag.removeTag("lit_total_time");
        teTag.removeTag("cooking_total_time");
        teTag.removeTag("RecipesUsed");
    }

    // ==================== Brewing Stand ====================

    private void convertBrewingStand(NBTTagCompound teTag) {
        convertItemsGeneric(teTag);
        if (teTag.hasKey("fuel")) {
            teTag.setByte("Fuel", teTag.getByte("fuel"));
            teTag.removeTag("fuel");
        }
    }

    // ==================== Command Block ====================

    private void convertCommandBlock(NBTTagCompound teTag) {
        teTag.removeTag("auto");
        teTag.removeTag("powered");
        teTag.removeTag("conditionMet");
        teTag.removeTag("UpdateLastExecution");
        teTag.removeTag("LastExecution");
    }

    // ==================== Banner ====================

    private void convertBanner(NBTTagCompound teTag) {
        if (teTag.hasKey("patterns", Constants.NBT.TAG_LIST)) {
            NBTTagList modernPatterns = teTag.getTagList("patterns", Constants.NBT.TAG_COMPOUND);
            NBTTagList legacyPatterns = new NBTTagList();
            for (int i = 0; i < modernPatterns.tagCount(); i++) {
                NBTTagCompound modernPattern = modernPatterns.getCompoundTagAt(i);
                NBTTagCompound legacyPattern = new NBTTagCompound();
                if (modernPattern.hasKey("pattern")) {
                    String pattern = modernPattern.getString("pattern");
                    if (pattern.contains(":")) {
                        pattern = pattern.substring(pattern.indexOf(':') + 1);
                    }
                    legacyPattern.setString("Pattern", pattern);
                }
                if (modernPattern.hasKey("color", Constants.NBT.TAG_STRING)) {
                    // Modern 1.20.5+ stores color as a string like "minecraft:red"
                    String colorStr = modernPattern.getString("color");
                    legacyPattern.setInteger("Color", bannerColorStringToInt(colorStr));
                } else if (modernPattern.hasKey("color")) {
                    // Older modern format stores color as an integer
                    legacyPattern.setInteger("Color", modernPattern.getInteger("color"));
                }
                legacyPatterns.appendTag(legacyPattern);
            }
            teTag.setTag("Patterns", legacyPatterns);
            teTag.removeTag("patterns");
        }
        if (!teTag.hasKey("Base")) {
            teTag.setInteger("Base", 0);
        }
    }

    /**
     * Converts a modern banner color string (e.g. "minecraft:red") to a 1.7.10 dye color int.
     * In 1.7.10, banner colors use dye damage values (not wool damage values).
     */
    private int bannerColorStringToInt(String colorStr) {
        if (colorStr == null) return 0;
        if (colorStr.startsWith("minecraft:")) {
            colorStr = colorStr.substring(10);
        }
        switch (colorStr) {
            case "white": return 0;
            case "orange": return 1;
            case "magenta": return 2;
            case "light_blue": return 3;
            case "yellow": return 4;
            case "lime": return 5;
            case "pink": return 6;
            case "gray": return 7;
            case "light_gray": return 8;
            case "cyan": return 9;
            case "purple": return 10;
            case "blue": return 11;
            case "brown": return 12;
            case "green": return 13;
            case "red": return 14;
            case "black": return 15;
            default: return 0;
        }
    }

    // ==================== Spawner ====================

    private void convertSpawner(NBTTagCompound teTag) {
        if (teTag.hasKey("SpawnData", Constants.NBT.TAG_COMPOUND)) {
            NBTTagCompound spawnData = teTag.getCompoundTag("SpawnData");
            if (spawnData.hasKey("entity", Constants.NBT.TAG_COMPOUND)) {
                NBTTagCompound entity = spawnData.getCompoundTag("entity");
                if (entity.hasKey("id")) {
                    String legacyEntityId = EntityTranslator.instance().getLegacyId(entity.getString("id"));
                    if (legacyEntityId != null) teTag.setString("EntityId", legacyEntityId);
                }
            } else if (spawnData.hasKey("id")) {
                String legacyEntityId = EntityTranslator.instance().getLegacyId(spawnData.getString("id"));
                if (legacyEntityId != null) teTag.setString("EntityId", legacyEntityId);
            }
        }
        if (!teTag.hasKey("SpawnCount")) teTag.setShort("SpawnCount", (short) 4);
        if (!teTag.hasKey("SpawnRange")) teTag.setShort("SpawnRange", (short) 4);
        if (!teTag.hasKey("Delay")) teTag.setShort("Delay", (short) 20);
        if (!teTag.hasKey("MinSpawnDelay")) teTag.setShort("MinSpawnDelay", (short) 200);
        if (!teTag.hasKey("MaxSpawnDelay")) teTag.setShort("MaxSpawnDelay", (short) 800);
        if (!teTag.hasKey("MaxNearbyEntities")) teTag.setShort("MaxNearbyEntities", (short) 6);
        if (!teTag.hasKey("RequiredPlayerRange")) teTag.setShort("RequiredPlayerRange", (short) 16);
    }

    // ==================== Jukebox ====================

    private void convertJukebox(NBTTagCompound teTag) {
        if (teTag.hasKey("RecordItem", Constants.NBT.TAG_COMPOUND)) {
            convertItem(teTag.getCompoundTag("RecordItem"));
        }
    }

    // ==================== Lectern ====================

    private void convertLectern(NBTTagCompound teTag) {
        if (teTag.hasKey("Book", Constants.NBT.TAG_COMPOUND)) {
            convertItem(teTag.getCompoundTag("Book"));
        }
    }

    // ==================== Beacon ====================

    private void convertBeacon(NBTTagCompound teTag) {
        if (teTag.hasKey("primary_effect", Constants.NBT.TAG_STRING)) {
            teTag.setInteger("Primary", getEffectId(teTag.getString("primary_effect")));
            teTag.removeTag("primary_effect");
        }
        if (teTag.hasKey("secondary_effect", Constants.NBT.TAG_STRING)) {
            teTag.setInteger("Secondary", getEffectId(teTag.getString("secondary_effect")));
            teTag.removeTag("secondary_effect");
        }
    }

    // ==================== Flower Pot ====================

    private void convertFlowerPot(NBTTagCompound teTag) {
        if (!teTag.hasKey("Item")) {
            teTag.setString("Item", "");
            teTag.setInteger("Data", 0);
        }
    }

    // ==================== Note Block ====================

    private void convertNoteBlock(NBTTagCompound teTag) {
        // note tag is the same in both versions
    }

    // ==================== Items ====================

    /**
     * Public item conversion method for use by EntityTranslator.
     * Converts a modern item NBT compound to 1.7.10 format in-place.
     */
    public void convertItemPublic(NBTTagCompound itemTag) {
        convertItem(itemTag);
    }

    private void convertItemsGeneric(NBTTagCompound teTag) {
        if (teTag.hasKey("Items", Constants.NBT.TAG_LIST)) {
            NBTTagList items = teTag.getTagList("Items", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < items.tagCount(); i++) {
                convertItem(items.getCompoundTagAt(i));
            }
        }
    }

    private void convertItem(NBTTagCompound itemTag) {
        if (itemTag == null) return;

        if (itemTag.hasKey("id", Constants.NBT.TAG_STRING)) {
            String itemId = itemTag.getString("id");
            net.minecraft.item.Item item = (net.minecraft.item.Item) net.minecraft.item.Item.itemRegistry.getObject(itemId);
            if (item == null) {
                String legacyName = VanillaBlockMappings.getBlockName_1_7_10(itemId);
                item = (net.minecraft.item.Item) net.minecraft.item.Item.itemRegistry.getObject(legacyName);
            }
            if (item != null) {
                itemTag.setShort("id", (short) net.minecraft.item.Item.getIdFromItem(item));
            } else {
                net.minecraft.block.Block block = net.minecraft.block.Block.getBlockFromName(itemId);
                if (block == null) {
                    block = net.minecraft.block.Block.getBlockFromName(VanillaBlockMappings.getBlockName_1_7_10(itemId));
                }
                if (block != null) {
                    itemTag.setShort("id", (short) net.minecraft.item.Item.getIdFromItem(net.minecraft.item.Item.getItemFromBlock(block)));
                } else {
                    itemTag.setShort("id", (short) 1);
                    Reference.logger.debug("TileEntityTranslator: Unknown item '{}', using stone fallback", itemId);
                }
            }
        }

        if (itemTag.hasKey("count") && !itemTag.hasKey("Count")) {
            itemTag.setByte("Count", itemTag.getByte("count"));
            itemTag.removeTag("count");
        }
        if (!itemTag.hasKey("Count")) itemTag.setByte("Count", (byte) 1);
        if (!itemTag.hasKey("Damage")) itemTag.setShort("Damage", (short) 0);
        itemTag.removeTag("components");
    }

    // ==================== Helpers ====================

    private String intArrayToUUID(int[] ints) {
        if (ints.length != 4) return "";
        long msb = ((long) ints[0] << 32) | (ints[1] & 0xFFFFFFFFL);
        long lsb = ((long) ints[2] << 32) | (ints[3] & 0xFFFFFFFFL);
        return new java.util.UUID(msb, lsb).toString();
    }

    private int getEffectId(String modernEffect) {
        if (modernEffect == null) return 0;
        switch (modernEffect) {
            case "minecraft:speed": return 1;
            case "minecraft:slowness": return 2;
            case "minecraft:haste": return 3;
            case "minecraft:mining_fatigue": return 4;
            case "minecraft:strength": return 5;
            case "minecraft:instant_health": return 6;
            case "minecraft:instant_damage": return 7;
            case "minecraft:jump_boost": return 8;
            case "minecraft:nausea": return 9;
            case "minecraft:regeneration": return 10;
            case "minecraft:resistance": return 11;
            case "minecraft:fire_resistance": return 12;
            case "minecraft:water_breathing": return 13;
            case "minecraft:invisibility": return 14;
            case "minecraft:blindness": return 15;
            case "minecraft:night_vision": return 16;
            case "minecraft:hunger": return 17;
            case "minecraft:weakness": return 18;
            case "minecraft:poison": return 19;
            case "minecraft:wither": return 20;
            case "minecraft:health_boost": return 21;
            case "minecraft:absorption": return 22;
            case "minecraft:saturation": return 23;
            default: return 0;
        }
    }

    // ==================== ID Mappings ====================

    private void initIdMappings() {
        idMap.put("minecraft:chest", "Chest");
        idMap.put("minecraft:trapped_chest", "Chest");
        idMap.put("minecraft:ender_chest", "EnderChest");
        idMap.put("minecraft:furnace", "Furnace");
        idMap.put("minecraft:blast_furnace", "Furnace");
        idMap.put("minecraft:smoker", "Furnace");
        idMap.put("minecraft:sign", "Sign");
        idMap.put("minecraft:hanging_sign", "Sign");
        idMap.put("minecraft:mob_spawner", "MobSpawner");
        idMap.put("minecraft:spawner", "MobSpawner");
        idMap.put("minecraft:noteblock", "Music");
        idMap.put("minecraft:note_block", "Music");
        idMap.put("minecraft:piston", "Piston");
        idMap.put("minecraft:brewing_stand", "Cauldron");
        idMap.put("minecraft:enchanting_table", "EnchantTable");
        idMap.put("minecraft:enchantment_table", "EnchantTable");
        idMap.put("minecraft:end_portal", "Airportal");
        idMap.put("minecraft:beacon", "Beacon");
        idMap.put("minecraft:skull", "Skull");
        idMap.put("minecraft:daylight_detector", "DLDetector");
        idMap.put("minecraft:hopper", "Hopper");
        idMap.put("minecraft:comparator", "Comparator");
        idMap.put("minecraft:flower_pot", "FlowerPot");
        idMap.put("minecraft:banner", "Banner");
        idMap.put("minecraft:jukebox", "RecordPlayer");
        idMap.put("minecraft:dispenser", "Trap");
        idMap.put("minecraft:dropper", "Dropper");
        idMap.put("minecraft:command_block", "Control");
        idMap.put("minecraft:end_gateway", "EndGateway");
        idMap.put("minecraft:structure_block", "Structure");

        // All sign variants -> Sign
        String[] woodTypes = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "cherry", "bamboo", "pale_oak", "mangrove", "warped", "crimson"};
        for (String wood : woodTypes) {
            idMap.put("minecraft:" + wood + "_sign", "Sign");
            idMap.put("minecraft:" + wood + "_wall_sign", "Sign");
            idMap.put("minecraft:" + wood + "_hanging_sign", "Sign");
            idMap.put("minecraft:" + wood + "_wall_hanging_sign", "Sign");
        }

        // All skull/head variants -> Skull
        idMap.put("minecraft:skeleton_skull", "Skull");
        idMap.put("minecraft:wither_skeleton_skull", "Skull");
        idMap.put("minecraft:zombie_head", "Skull");
        idMap.put("minecraft:player_head", "Skull");
        idMap.put("minecraft:creeper_head", "Skull");
        idMap.put("minecraft:dragon_head", "Skull");
        idMap.put("minecraft:piglin_head", "Skull");
        idMap.put("minecraft:skeleton_wall_skull", "Skull");
        idMap.put("minecraft:wither_skeleton_wall_skull", "Skull");
        idMap.put("minecraft:zombie_wall_head", "Skull");
        idMap.put("minecraft:player_wall_head", "Skull");
        idMap.put("minecraft:creeper_wall_head", "Skull");
        idMap.put("minecraft:dragon_wall_head", "Skull");
        idMap.put("minecraft:piglin_wall_head", "Skull");

        // Modern-only TEs mapped to closest 1.7.10 equivalent
        idMap.put("minecraft:barrel", "Chest");
        idMap.put("minecraft:shulker_box", "Chest");
        idMap.put("minecraft:lectern", "Sign");
        idMap.put("minecraft:bell", "Beacon");
        idMap.put("minecraft:campfire", "Furnace");
        idMap.put("minecraft:soul_campfire", "Furnace");
        idMap.put("minecraft:beehive", "Chest");
        idMap.put("minecraft:bee_nest", "Chest");
        idMap.put("minecraft:conduit", "Beacon");
        idMap.put("minecraft:sculk_sensor", "Comparator");
        idMap.put("minecraft:sculk_catalyst", "Beacon");
        idMap.put("minecraft:sculk_shrieker", "Beacon");
        idMap.put("minecraft:chiseled_bookshelf", "Chest");
        idMap.put("minecraft:decorated_pot", "FlowerPot");
        idMap.put("minecraft:suspicious_sand", "Chest");
        idMap.put("minecraft:suspicious_gravel", "Chest");
        idMap.put("minecraft:crafter", "Dropper");

        // Colored variants
        String[] colors = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
            "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
        for (String color : colors) {
            idMap.put("minecraft:" + color + "_shulker_box", "Chest");
            idMap.put("minecraft:" + color + "_bed", "Bed");
            idMap.put("minecraft:" + color + "_banner", "Banner");
            idMap.put("minecraft:" + color + "_wall_banner", "Banner");
        }
    }
}
