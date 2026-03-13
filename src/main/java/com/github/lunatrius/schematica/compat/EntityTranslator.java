
package com.github.lunatrius.schematica.compat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import com.github.lunatrius.schematica.reference.Reference;

/**
 * Translates modern (1.13+) Entity NBT data to 1.7.10 format.
 * Modern litematic files store entities with namespaced IDs like "minecraft:creeper",
 * but 1.7.10's EntityList expects short IDs like "Creeper".
 *
 * @author HackerRouter
 */
public final class EntityTranslator {

    private static final EntityTranslator INSTANCE = new EntityTranslator();
    private final Map<String, String> idMap = new HashMap<>();

    private EntityTranslator() {
        initIdMappings();
    }

    public static EntityTranslator instance() {
        return INSTANCE;
    }

    /**
     * Gets the legacy 1.7.10 entity ID for a modern namespaced ID.
     * @return legacy ID or null if unknown
     */
    public String getLegacyId(String modernId) {
        if (modernId == null) return null;
        if (!modernId.contains(":")) return modernId;

        String legacyId = idMap.get(modernId);
        if (legacyId != null) return legacyId;

        // Guess: strip namespace and CamelCase it
        String stripped = modernId;
        if (stripped.startsWith("minecraft:")) {
            stripped = stripped.substring(10);
        }
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
        return sb.toString();
    }

    /**
     * Translates a modern entity NBT compound to 1.7.10 format in-place.
     */
    public boolean translate(NBTTagCompound entityTag) {
        if (entityTag == null || !entityTag.hasKey("id")) {
            return false;
        }

        String modernId = entityTag.getString("id");
        if (!modernId.contains(":")) return true;

        String legacyId = getLegacyId(modernId);
        if (legacyId == null) {
            Reference.logger.warn("EntityTranslator: Unknown entity id '{}'", modernId);
            return false;
        }

        entityTag.setString("id", legacyId);
        convertEntityNBT(modernId, entityTag);
        return true;
    }

    /**
     * Applies entity-type-specific NBT conversions.
     * Handles modern (1.20.5+/1.21+) snake_case NBT tag renames and
     * structural changes to convert back to 1.7.10 format.
     */
    private void convertEntityNBT(String modernId, NBTTagCompound tag) {
        // Convert UUID from int array to Most/Least long pair
        if (tag.hasKey("UUID", Constants.NBT.TAG_INT_ARRAY)) {
            int[] uuid = tag.getIntArray("UUID");
            if (uuid.length == 4) {
                long most = ((long) uuid[0] << 32) | (uuid[1] & 0xFFFFFFFFL);
                long least = ((long) uuid[2] << 32) | (uuid[3] & 0xFFFFFFFFL);
                tag.setLong("UUIDMost", most);
                tag.setLong("UUIDLeast", least);
            }
            tag.removeTag("UUID");
        }

        // ---- Modern snake_case → 1.7.10 CamelCase tag renames ----
        // Entity base tags (from 1.20.5+ / 1.21+ NBT component changes)
        renameTag(tag, "fall_distance", "FallDistance");
        renameTag(tag, "no_gravity", "NoGravity");
        renameTag(tag, "portal_cooldown", "PortalCooldown");

        // LivingEntity tags
        renameTag(tag, "absorption_amount", "AbsorptionAmount");
        renameTag(tag, "active_effects", "ActiveEffects");
        renameTag(tag, "death_time", "DeathTime");
        renameTag(tag, "hurt_time", "HurtTime");

        // Mob tags
        renameTag(tag, "can_pick_up_loot", "CanPickUpLoot");
        renameTag(tag, "persistence_required", "PersistenceRequired");
        renameTag(tag, "left_handed", "LeftHanded");

        // Creeper-specific
        renameTag(tag, "explosion_radius", "ExplosionRadius");

        // Zombie-specific
        renameTag(tag, "is_baby", "IsBaby");
        renameTag(tag, "can_break_doors", "CanBreakDoors");
        renameTag(tag, "drowned_conversion_time", "DrownedConversionTime");
        renameTag(tag, "in_water_time", "InWaterTime");

        // Convert modern 'attributes' (lowercase, new structure) → 'Attributes' (1.7.10 format)
        convertAttributes(tag);

        // Convert passengers (modern) to Riding (1.7.10)
        if (tag.hasKey("Passengers", Constants.NBT.TAG_LIST)) {
            NBTTagList passengers = tag.getTagList("Passengers", Constants.NBT.TAG_COMPOUND);
            if (passengers.tagCount() > 0) {
                NBTTagCompound rider = passengers.getCompoundTagAt(0);
                translate(rider); // Recursively translate the rider
                tag.setTag("Riding", rider);
            }
            tag.removeTag("Passengers");
        }

        // Convert item entities
        if ("minecraft:item".equals(modernId)) {
            if (tag.hasKey("Item", Constants.NBT.TAG_COMPOUND)) {
                TileEntityTranslator.instance().convertItemPublic(tag.getCompoundTag("Item"));
            }
        }

        // Convert item frames
        if ("minecraft:item_frame".equals(modernId) || "minecraft:glow_item_frame".equals(modernId)) {
            if (tag.hasKey("Item", Constants.NBT.TAG_COMPOUND)) {
                TileEntityTranslator.instance().convertItemPublic(tag.getCompoundTag("Item"));
            }
        }

        // Convert armor stands
        if ("minecraft:armor_stand".equals(modernId)) {
            convertArmorStandItems(tag);
        }

        // Convert paintings: modern "variant" -> 1.7.10 "Motive"
        if ("minecraft:painting".equals(modernId)) {
            convertPainting(tag);
        }

        // Convert wither_skeleton: in 1.7.10 it's a Skeleton with SkeletonType=1
        if ("minecraft:wither_skeleton".equals(modernId)) {
            tag.setByte("SkeletonType", (byte) 1);
        }
        // Stray is also a Skeleton variant but 1.7.10 doesn't have it; map to SkeletonType=0
        if ("minecraft:stray".equals(modernId)) {
            tag.setByte("SkeletonType", (byte) 0);
        }

        // Convert elder_guardian: in 1.7.10 it's a Guardian with Elder=1
        if ("minecraft:elder_guardian".equals(modernId)) {
            tag.setByte("Elder", (byte) 1);
        }

        // Convert zombie_villager: in 1.7.10 it's a Zombie with IsVillager=1
        if ("minecraft:zombie_villager".equals(modernId)) {
            tag.setByte("IsVillager", (byte) 1);
        }

        // Convert villager profession (modern uses "VillagerData" compound)
        if ("minecraft:villager".equals(modernId) || "minecraft:wandering_trader".equals(modernId)) {
            convertVillager(tag);
        }

        // Convert minecart containers (chest_minecart, hopper_minecart)
        if ("minecraft:chest_minecart".equals(modernId) || "minecraft:hopper_minecart".equals(modernId)) {
            convertContainerItems(tag);
        }

        // Convert horse variants: donkey, mule, skeleton_horse, zombie_horse → EntityHorse with Type
        convertHorseVariant(modernId, tag);

        // Remove modern-only tags that don't exist in 1.7.10
        tag.removeTag("Brain");
        tag.removeTag("bukkit.updateLevel");
    }

    /**
     * Renames an NBT tag key if the old key exists and the new key doesn't.
     */
    private void renameTag(NBTTagCompound tag, String oldName, String newName) {
        if (oldName.equals(newName)) return;
        if (tag.hasKey(oldName) && !tag.hasKey(newName)) {
            NBTBase value = tag.getTag(oldName);
            tag.setTag(newName, value);
            tag.removeTag(oldName);
        }
    }

    /**
     * Converts modern 'attributes' (1.21+ format) to 1.7.10 'Attributes' format.
     *
     * Modern format:
     *   attributes: [{id: "minecraft:movement_speed", base: 0.25, modifiers: [{id: "minecraft:random_spawn_bonus", amount: 0.03, operation: add_multiplied_base}]}]
     *
     * 1.7.10 format:
     *   Attributes: [{Name: "generic.movementSpeed", Base: 0.25, Modifiers: [{Name: "Random spawn bonus", Amount: 0.03, Operation: 2, UUIDMost: ..., UUIDLeast: ...}]}]
     */
    private void convertAttributes(NBTTagCompound tag) {
        // Handle both lowercase 'attributes' (1.21+) and already-correct 'Attributes'
        NBTTagList modernAttrs = null;
        boolean wasLowercase = false;

        if (tag.hasKey("attributes", Constants.NBT.TAG_LIST)) {
            modernAttrs = tag.getTagList("attributes", Constants.NBT.TAG_COMPOUND);
            wasLowercase = true;
        } else if (tag.hasKey("Attributes", Constants.NBT.TAG_LIST)) {
            // Check if it's modern format (has 'id' key instead of 'Name')
            NBTTagList existing = tag.getTagList("Attributes", Constants.NBT.TAG_COMPOUND);
            if (existing.tagCount() > 0) {
                NBTTagCompound first = existing.getCompoundTagAt(0);
                if (first.hasKey("id") && !first.hasKey("Name")) {
                    modernAttrs = existing;
                }
            }
        }

        if (modernAttrs == null || modernAttrs.tagCount() == 0) return;

        NBTTagList legacyAttrs = new NBTTagList();
        for (int i = 0; i < modernAttrs.tagCount(); i++) {
            NBTTagCompound modernAttr = modernAttrs.getCompoundTagAt(i);
            NBTTagCompound legacyAttr = convertSingleAttribute(modernAttr);
            if (legacyAttr != null) {
                legacyAttrs.appendTag(legacyAttr);
            }
        }

        if (wasLowercase) {
            tag.removeTag("attributes");
        }
        tag.setTag("Attributes", legacyAttrs);
    }

    private NBTTagCompound convertSingleAttribute(NBTTagCompound modernAttr) {
        String modernId = modernAttr.getString("id");
        String legacyName = getAttributeLegacyName(modernId);
        if (legacyName == null) return null;

        NBTTagCompound legacy = new NBTTagCompound();
        legacy.setString("Name", legacyName);
        legacy.setDouble("Base", modernAttr.getDouble("base"));

        // Convert modifiers
        if (modernAttr.hasKey("modifiers", Constants.NBT.TAG_LIST)) {
            NBTTagList modernMods = modernAttr.getTagList("modifiers", Constants.NBT.TAG_COMPOUND);
            NBTTagList legacyMods = new NBTTagList();
            for (int i = 0; i < modernMods.tagCount(); i++) {
                NBTTagCompound modernMod = modernMods.getCompoundTagAt(i);
                NBTTagCompound legacyMod = convertSingleModifier(modernMod);
                if (legacyMod != null) {
                    legacyMods.appendTag(legacyMod);
                }
            }
            if (legacyMods.tagCount() > 0) {
                legacy.setTag("Modifiers", legacyMods);
            }
        }

        return legacy;
    }

    private NBTTagCompound convertSingleModifier(NBTTagCompound modernMod) {
        NBTTagCompound legacy = new NBTTagCompound();

        String modId = modernMod.getString("id");
        legacy.setString("Name", modId); // Use the id as the name

        legacy.setDouble("Amount", modernMod.getDouble("amount"));

        // Convert operation string to int
        String opStr = modernMod.getString("operation");
        int op = 0;
        if ("add_value".equals(opStr)) op = 0;
        else if ("add_multiplied_base".equals(opStr)) op = 1;
        else if ("add_multiplied_total".equals(opStr)) op = 2;
        legacy.setInteger("Operation", op);

        // Generate a UUID from the modifier id string for 1.7.10 compatibility
        UUID uuid = UUID.nameUUIDFromBytes(modId.getBytes());
        legacy.setLong("UUIDMost", uuid.getMostSignificantBits());
        legacy.setLong("UUIDLeast", uuid.getLeastSignificantBits());

        return legacy;
    }

    /** Maps modern namespaced attribute IDs to 1.7.10 attribute names */
    private static final Map<String, String> ATTRIBUTE_ID_MAP = new HashMap<>();
    static {
        ATTRIBUTE_ID_MAP.put("minecraft:generic.max_health", "generic.maxHealth");
        ATTRIBUTE_ID_MAP.put("minecraft:max_health", "generic.maxHealth");
        ATTRIBUTE_ID_MAP.put("minecraft:generic.follow_range", "generic.followRange");
        ATTRIBUTE_ID_MAP.put("minecraft:follow_range", "generic.followRange");
        ATTRIBUTE_ID_MAP.put("minecraft:generic.knockback_resistance", "generic.knockbackResistance");
        ATTRIBUTE_ID_MAP.put("minecraft:knockback_resistance", "generic.knockbackResistance");
        ATTRIBUTE_ID_MAP.put("minecraft:generic.movement_speed", "generic.movementSpeed");
        ATTRIBUTE_ID_MAP.put("minecraft:movement_speed", "generic.movementSpeed");
        ATTRIBUTE_ID_MAP.put("minecraft:generic.attack_damage", "generic.attackDamage");
        ATTRIBUTE_ID_MAP.put("minecraft:attack_damage", "generic.attackDamage");
        ATTRIBUTE_ID_MAP.put("minecraft:zombie.spawn_reinforcements", "zombie.spawnReinforcements");
        ATTRIBUTE_ID_MAP.put("minecraft:spawn_reinforcements", "zombie.spawnReinforcements");
        ATTRIBUTE_ID_MAP.put("minecraft:horse.jump_strength", "horse.jumpStrength");
        ATTRIBUTE_ID_MAP.put("minecraft:jump_strength", "horse.jumpStrength");
    }

    private String getAttributeLegacyName(String modernId) {
        if (modernId == null || modernId.isEmpty()) return null;
        String legacy = ATTRIBUTE_ID_MAP.get(modernId);
        if (legacy != null) return legacy;
        // Fallback: strip namespace and convert snake_case to camelCase
        String stripped = modernId.startsWith("minecraft:") ? modernId.substring(10) : modernId;
        // Try "generic." prefix
        if (stripped.contains(".")) return stripped;
        return "generic." + snakeToCamel(stripped);
    }

    private String snakeToCamel(String snake) {
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = false;
        for (char c : snake.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else {
                sb.append(capitalizeNext ? Character.toUpperCase(c) : c);
                capitalizeNext = false;
            }
        }
        return sb.toString();
    }

    private void convertArmorStandItems(NBTTagCompound tag) {
        TileEntityTranslator teTranslator = TileEntityTranslator.instance();
        if (tag.hasKey("ArmorItems", Constants.NBT.TAG_LIST)) {
            NBTTagList armorItems = tag.getTagList("ArmorItems", Constants.NBT.TAG_COMPOUND);
            // 1.7.10 uses Equipment[0-4]: held, feet, legs, chest, head
            NBTTagList equipment = new NBTTagList();
            // Held item (HandItems[0] in modern)
            if (tag.hasKey("HandItems", Constants.NBT.TAG_LIST)) {
                NBTTagList handItems = tag.getTagList("HandItems", Constants.NBT.TAG_COMPOUND);
                if (handItems.tagCount() > 0) {
                    NBTTagCompound held = handItems.getCompoundTagAt(0);
                    teTranslator.convertItemPublic(held);
                    equipment.appendTag(held);
                } else {
                    equipment.appendTag(new NBTTagCompound());
                }
            } else {
                equipment.appendTag(new NBTTagCompound());
            }
            // Armor: feet(0), legs(1), chest(2), head(3)
            for (int i = 0; i < 4 && i < armorItems.tagCount(); i++) {
                NBTTagCompound armor = armorItems.getCompoundTagAt(i);
                teTranslator.convertItemPublic(armor);
                equipment.appendTag(armor);
            }
            tag.setTag("Equipment", equipment);
            tag.removeTag("ArmorItems");
            tag.removeTag("HandItems");
        }
    }

    /**
     * Converts modern painting NBT to 1.7.10 format.
     * Modern: variant="minecraft:kebab" (or just "kebab")
     * 1.7.10: Motive="Kebab"
     */
    private void convertPainting(NBTTagCompound tag) {
        // Modern 1.20+ uses "variant" field
        if (tag.hasKey("variant", Constants.NBT.TAG_STRING)) {
            String variant = tag.getString("variant");
            String motive = convertPaintingVariant(variant);
            tag.setString("Motive", motive);
            tag.removeTag("variant");
        }
        // Ensure Motive exists
        if (!tag.hasKey("Motive")) {
            tag.setString("Motive", "Kebab");
        }

        // Convert modern "facing" (byte 0-3) to 1.7.10 "Direction" (byte 0-3)
        // In modern: 0=south, 1=west, 2=north, 3=east (same as 1.7.10 Direction)
        if (tag.hasKey("facing", Constants.NBT.TAG_BYTE) && !tag.hasKey("Direction")) {
            tag.setByte("Direction", tag.getByte("facing"));
        }

        // Convert TileX/TileY/TileZ if not present (modern uses block_pos or Pos)
        if (!tag.hasKey("TileX") && tag.hasKey("block_pos", Constants.NBT.TAG_COMPOUND)) {
            NBTTagCompound blockPos = tag.getCompoundTag("block_pos");
            tag.setInteger("TileX", blockPos.getInteger("X"));
            tag.setInteger("TileY", blockPos.getInteger("Y"));
            tag.setInteger("TileZ", blockPos.getInteger("Z"));
            tag.removeTag("block_pos");
        }
    }

    private static final Map<String, String> PAINTING_VARIANT_MAP = new HashMap<>();
    static {
        PAINTING_VARIANT_MAP.put("minecraft:kebab", "Kebab");
        PAINTING_VARIANT_MAP.put("minecraft:aztec", "Aztec");
        PAINTING_VARIANT_MAP.put("minecraft:alban", "Alban");
        PAINTING_VARIANT_MAP.put("minecraft:aztec2", "Aztec2");
        PAINTING_VARIANT_MAP.put("minecraft:bomb", "Bomb");
        PAINTING_VARIANT_MAP.put("minecraft:plant", "Plant");
        PAINTING_VARIANT_MAP.put("minecraft:wasteland", "Wasteland");
        PAINTING_VARIANT_MAP.put("minecraft:pool", "Pool");
        PAINTING_VARIANT_MAP.put("minecraft:courbet", "Courbet");
        PAINTING_VARIANT_MAP.put("minecraft:sea", "Sea");
        PAINTING_VARIANT_MAP.put("minecraft:sunset", "Sunset");
        PAINTING_VARIANT_MAP.put("minecraft:creebet", "Creebet");
        PAINTING_VARIANT_MAP.put("minecraft:wanderer", "Wanderer");
        PAINTING_VARIANT_MAP.put("minecraft:graham", "Graham");
        PAINTING_VARIANT_MAP.put("minecraft:match", "Match");
        PAINTING_VARIANT_MAP.put("minecraft:bust", "Bust");
        PAINTING_VARIANT_MAP.put("minecraft:stage", "Stage");
        PAINTING_VARIANT_MAP.put("minecraft:void", "Void");
        PAINTING_VARIANT_MAP.put("minecraft:skull_and_roses", "SkullAndRoses");
        PAINTING_VARIANT_MAP.put("minecraft:wither", "Wither");
        PAINTING_VARIANT_MAP.put("minecraft:fighters", "Fighters");
        PAINTING_VARIANT_MAP.put("minecraft:pointer", "Pointer");
        PAINTING_VARIANT_MAP.put("minecraft:pigscene", "Pigscene");
        PAINTING_VARIANT_MAP.put("minecraft:burning_skull", "BurningSkull");
        PAINTING_VARIANT_MAP.put("minecraft:skeleton", "Skeleton");
        PAINTING_VARIANT_MAP.put("minecraft:donkey_kong", "DonkeyKong");
    }

    private String convertPaintingVariant(String variant) {
        if (variant == null) return "Kebab";
        // Try direct lookup (with namespace)
        String mapped = PAINTING_VARIANT_MAP.get(variant);
        if (mapped != null) return mapped;
        // Try with namespace prefix if not present
        if (!variant.contains(":")) {
            mapped = PAINTING_VARIANT_MAP.get("minecraft:" + variant);
            if (mapped != null) return mapped;
        } else {
            // Strip namespace for fallback
            variant = variant.substring(variant.indexOf(':') + 1);
        }
        // CamelCase the snake_case name as fallback
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : variant.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else {
                sb.append(capitalizeNext ? Character.toUpperCase(c) : c);
                capitalizeNext = false;
            }
        }
        return sb.toString();
    }

    /**
     * Converts modern villager data to 1.7.10 format.
     * Modern: VillagerData {profession: "minecraft:farmer", level: 2, type: "minecraft:plains"}
     * 1.7.10: Profession (int), Career (int), CareerLevel (int)
     */
    private void convertVillager(NBTTagCompound tag) {
        if (tag.hasKey("VillagerData", Constants.NBT.TAG_COMPOUND)) {
            NBTTagCompound data = tag.getCompoundTag("VillagerData");
            String profession = data.getString("profession");
            if (profession.startsWith("minecraft:")) {
                profession = profession.substring(10);
            }
            int profId = getVillagerProfessionId(profession);
            tag.setInteger("Profession", profId);
            // Career and CareerLevel default to 0 if not set
            if (!tag.hasKey("Career")) {
                tag.setInteger("Career", 0);
            }
            if (!tag.hasKey("CareerLevel")) {
                tag.setInteger("CareerLevel", 1);
            }
            tag.removeTag("VillagerData");
        }
        // Convert Offers (trade data) - rename modern lowercase keys
        if (tag.hasKey("Offers", Constants.NBT.TAG_COMPOUND)) {
            // Offers structure is similar between versions, keep as-is
        }
    }

    private int getVillagerProfessionId(String profession) {
        switch (profession) {
            case "farmer": return 0;
            case "fisherman": return 0;
            case "shepherd": return 0;
            case "fletcher": return 0;
            case "librarian": return 1;
            case "cartographer": return 1;
            case "cleric": return 2;
            case "armorer": return 3;
            case "weapon_smith": return 3;
            case "tool_smith": return 3;
            case "butcher": return 4;
            case "leatherworker": return 4;
            case "nitwit": return 5;
            default: return 0;
        }
    }

    /**
     * Converts container items in entities (minecarts, etc.)
     */
    private void convertContainerItems(NBTTagCompound tag) {
        TileEntityTranslator teTranslator = TileEntityTranslator.instance();
        if (tag.hasKey("Items", Constants.NBT.TAG_LIST)) {
            NBTTagList items = tag.getTagList("Items", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < items.tagCount(); i++) {
                teTranslator.convertItemPublic(items.getCompoundTagAt(i));
            }
        }
    }

    /**
     * Converts modern horse variants to 1.7.10 EntityHorse Type field.
     * 1.7.10: Type 0=horse, 1=donkey, 2=mule, 3=zombie, 4=skeleton
     */
    private void convertHorseVariant(String modernId, NBTTagCompound tag) {
        switch (modernId) {
            case "minecraft:horse":
                tag.setInteger("Type", 0);
                break;
            case "minecraft:donkey":
                tag.setInteger("Type", 1);
                break;
            case "minecraft:mule":
                tag.setInteger("Type", 2);
                break;
            case "minecraft:zombie_horse":
                tag.setInteger("Type", 3);
                break;
            case "minecraft:skeleton_horse":
                tag.setInteger("Type", 4);
                break;
            // llama, camel etc. mapped to horse but no exact Type
            case "minecraft:llama":
            case "minecraft:trader_llama":
            case "minecraft:camel":
                tag.setInteger("Type", 1); // closest: donkey
                break;
            default:
                break;
        }
    }

    private void initIdMappings() {
        // Passive mobs
        idMap.put("minecraft:pig", "Pig");
        idMap.put("minecraft:sheep", "Sheep");
        idMap.put("minecraft:cow", "Cow");
        idMap.put("minecraft:chicken", "Chicken");
        idMap.put("minecraft:squid", "Squid");
        idMap.put("minecraft:wolf", "Wolf");
        idMap.put("minecraft:mooshroom", "MushroomCow");
        idMap.put("minecraft:snow_golem", "SnowMan");
        idMap.put("minecraft:ocelot", "Ozelot");
        idMap.put("minecraft:iron_golem", "VillagerGolem");
        idMap.put("minecraft:horse", "EntityHorse");
        idMap.put("minecraft:villager", "Villager");
        idMap.put("minecraft:bat", "Bat");
        idMap.put("minecraft:rabbit", "Rabbit");

        // Hostile mobs
        idMap.put("minecraft:zombie", "Zombie");
        idMap.put("minecraft:skeleton", "Skeleton");
        idMap.put("minecraft:creeper", "Creeper");
        idMap.put("minecraft:spider", "Spider");
        idMap.put("minecraft:cave_spider", "CaveSpider");
        idMap.put("minecraft:enderman", "Enderman");
        idMap.put("minecraft:slime", "Slime");
        idMap.put("minecraft:ghast", "Ghast");
        idMap.put("minecraft:zombie_pigman", "PigZombie");
        idMap.put("minecraft:zombified_piglin", "PigZombie");
        idMap.put("minecraft:blaze", "Blaze");
        idMap.put("minecraft:magma_cube", "LavaSlime");
        idMap.put("minecraft:wither_skeleton", "Skeleton");
        idMap.put("minecraft:witch", "Witch");
        idMap.put("minecraft:silverfish", "Silverfish");
        idMap.put("minecraft:guardian", "Guardian");
        idMap.put("minecraft:elder_guardian", "Guardian");
        idMap.put("minecraft:wither", "WitherBoss");
        idMap.put("minecraft:ender_dragon", "EnderDragon");
        idMap.put("minecraft:endermite", "Endermite");

        // Projectiles & misc
        idMap.put("minecraft:arrow", "Arrow");
        idMap.put("minecraft:spectral_arrow", "Arrow");
        idMap.put("minecraft:snowball", "Snowball");
        idMap.put("minecraft:fireball", "Fireball");
        idMap.put("minecraft:small_fireball", "SmallFireball");
        idMap.put("minecraft:ender_pearl", "ThrownEnderpearl");
        idMap.put("minecraft:eye_of_ender", "EyeOfEnderSignal");
        idMap.put("minecraft:potion", "ThrownPotion");
        idMap.put("minecraft:experience_bottle", "ThrownExpBottle");
        idMap.put("minecraft:tnt", "PrimedTnt");
        idMap.put("minecraft:falling_block", "FallingSand");
        idMap.put("minecraft:item", "Item");
        idMap.put("minecraft:experience_orb", "XPOrb");
        idMap.put("minecraft:painting", "Painting");
        idMap.put("minecraft:item_frame", "ItemFrame");
        idMap.put("minecraft:glow_item_frame", "ItemFrame");
        idMap.put("minecraft:armor_stand", "ArmorStand");
        idMap.put("minecraft:minecart", "MinecartRideable");
        idMap.put("minecraft:chest_minecart", "MinecartChest");
        idMap.put("minecraft:furnace_minecart", "MinecartFurnace");
        idMap.put("minecraft:tnt_minecart", "MinecartTNT");
        idMap.put("minecraft:hopper_minecart", "MinecartHopper");
        idMap.put("minecraft:spawner_minecart", "MinecartSpawner");
        idMap.put("minecraft:command_block_minecart", "MinecartCommandBlock");
        idMap.put("minecraft:boat", "Boat");
        idMap.put("minecraft:oak_boat", "Boat");
        idMap.put("minecraft:lightning_bolt", "LightningBolt");
        idMap.put("minecraft:firework_rocket", "FireworksRocketEntity");
        idMap.put("minecraft:fishing_bobber", "FishingHook");
        idMap.put("minecraft:leash_knot", "LeashKnot");
        idMap.put("minecraft:ender_crystal", "EnderCrystal");
        idMap.put("minecraft:end_crystal", "EnderCrystal");

        // Modern-only entities mapped to closest 1.7.10 equivalent
        idMap.put("minecraft:stray", "Skeleton");
        idMap.put("minecraft:husk", "Zombie");
        idMap.put("minecraft:drowned", "Zombie");
        idMap.put("minecraft:phantom", "Bat");
        idMap.put("minecraft:pillager", "Skeleton");
        idMap.put("minecraft:vindicator", "Zombie");
        idMap.put("minecraft:evoker", "Witch");
        idMap.put("minecraft:ravager", "VillagerGolem");
        idMap.put("minecraft:shulker", "Endermite");
        idMap.put("minecraft:vex", "Bat");
        idMap.put("minecraft:piglin", "PigZombie");
        idMap.put("minecraft:piglin_brute", "PigZombie");
        idMap.put("minecraft:hoglin", "Pig");
        idMap.put("minecraft:zoglin", "PigZombie");
        idMap.put("minecraft:strider", "Pig");
        idMap.put("minecraft:warden", "VillagerGolem");
        idMap.put("minecraft:allay", "Bat");
        idMap.put("minecraft:frog", "Chicken");
        idMap.put("minecraft:tadpole", "Squid");
        idMap.put("minecraft:camel", "EntityHorse");
        idMap.put("minecraft:sniffer", "Cow");
        idMap.put("minecraft:breeze", "Blaze");
        idMap.put("minecraft:fox", "Wolf");
        idMap.put("minecraft:bee", "Bat");
        idMap.put("minecraft:cat", "Ozelot");
        idMap.put("minecraft:panda", "Cow");
        idMap.put("minecraft:parrot", "Bat");
        idMap.put("minecraft:dolphin", "Squid");
        idMap.put("minecraft:cod", "Squid");
        idMap.put("minecraft:salmon", "Squid");
        idMap.put("minecraft:tropical_fish", "Squid");
        idMap.put("minecraft:pufferfish", "Squid");
        idMap.put("minecraft:turtle", "Squid");
        idMap.put("minecraft:axolotl", "Squid");
        idMap.put("minecraft:glow_squid", "Squid");
        idMap.put("minecraft:goat", "Sheep");
        idMap.put("minecraft:donkey", "EntityHorse");
        idMap.put("minecraft:mule", "EntityHorse");
        idMap.put("minecraft:skeleton_horse", "EntityHorse");
        idMap.put("minecraft:zombie_horse", "EntityHorse");
        idMap.put("minecraft:llama", "EntityHorse");
        idMap.put("minecraft:trader_llama", "EntityHorse");
        idMap.put("minecraft:wandering_trader", "Villager");
        idMap.put("minecraft:zombie_villager", "Zombie");
    }
}
