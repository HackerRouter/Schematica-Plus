// SPDX-License-Identifier: LGPL-3.0-only
// Block entity, entity and item NBT between 1.7.10 and 1.12 (vanilla data fixer steps up to 1.12), by HackerRouter, 2026.
package com.github.lunatrius.schematica.world.schematic;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants.NBT;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import com.github.lunatrius.schematica.util.SchematicLimits;

import cpw.mods.fml.common.registry.GameData;

/**
 * 1.12 files carry data version 1343, so readers apply no fixes from before 1.12: block entity and entity ids,
 * string item ids, sign JSON text, spawner data and equipment lists are converted here as the vanilla fixers would.
 */
public final class LegacyNbt {
    private static final Map<String, String> TILES = new HashMap<>(), TILES_BACK = new HashMap<>();
    private static final Map<String, String> ENTITIES = new HashMap<>(), ENTITIES_BACK = new HashMap<>();
    private static final String[] HORSES = {"horse", "donkey", "mule", "zombie_horse", "skeleton_horse"};
    private static final int MAX_RIDING = 16;
    /** The 1.9 ItemPotionFix table: potion damage & 127 to the potion name (null is water). */
    private static final String[] POTIONS = {
        "water", "regeneration", "swiftness", "fire_resistance", "poison", "healing", "night_vision", null,
        "weakness", "strength", "slowness", "leaping", "harming", "water_breathing", "invisibility", null,
        "awkward", "regeneration", "swiftness", "fire_resistance", "poison", "healing", "night_vision", null,
        "weakness", "strength", "slowness", "leaping", "harming", "water_breathing", "invisibility", null,
        "thick", "strong_regeneration", "strong_swiftness", "fire_resistance", "strong_poison", "strong_healing", "night_vision", null,
        "weakness", "strong_strength", "slowness", "strong_leaping", "strong_harming", "water_breathing", "invisibility", null,
        null, "strong_regeneration", "strong_swiftness", "fire_resistance", "strong_poison", "strong_healing", "night_vision", null,
        "weakness", "strong_strength", "slowness", "strong_leaping", "strong_harming", "water_breathing", "invisibility", null,
        "mundane", "long_regeneration", "long_swiftness", "long_fire_resistance", "long_poison", "healing", "long_night_vision", null,
        "long_weakness", "long_strength", "long_slowness", "long_leaping", "harming", "long_water_breathing", "long_invisibility", null,
        "awkward", "long_regeneration", "long_swiftness", "long_fire_resistance", "long_poison", "healing", "long_night_vision", null,
        "long_weakness", "long_strength", "long_slowness", "long_leaping", "harming", "long_water_breathing", "long_invisibility", null,
        "thick", "regeneration", "swiftness", "long_fire_resistance", "poison", "strong_healing", "long_night_vision", null,
        "long_weakness", "strength", "long_slowness", "leaping", "strong_harming", "long_water_breathing", "long_invisibility", null,
        null, "regeneration", "swiftness", "long_fire_resistance", "poison", "strong_healing", "long_night_vision", null,
        "long_weakness", "strength", "long_slowness", "leaping", "strong_harming", "long_water_breathing", "long_invisibility", null,
    };

    static {
        tile("Furnace", "furnace"); tile("Chest", "chest"); tile("EnderChest", "ender_chest");
        tile("RecordPlayer", "jukebox"); tile("Trap", "dispenser"); tile("Dropper", "dropper");
        tile("Sign", "sign"); tile("MobSpawner", "mob_spawner"); tile("Music", "noteblock");
        tile("Piston", "piston"); tile("Cauldron", "brewing_stand"); tile("EnchantTable", "enchanting_table");
        tile("Airportal", "end_portal"); tile("Control", "command_block"); tile("Beacon", "beacon");
        tile("Skull", "skull"); tile("DLDetector", "daylight_detector"); tile("Hopper", "hopper");
        tile("Comparator", "comparator"); tile("FlowerPot", "flower_pot");
        String[][] entities = {
            {"Item", "item"}, {"XPOrb", "xp_orb"}, {"LeashKnot", "leash_knot"}, {"Painting", "painting"},
            {"Arrow", "arrow"}, {"Snowball", "snowball"}, {"Fireball", "fireball"}, {"SmallFireball", "small_fireball"},
            {"ThrownEnderpearl", "ender_pearl"}, {"EyeOfEnderSignal", "eye_of_ender_signal"}, {"ThrownPotion", "potion"},
            {"ThrownExpBottle", "xp_bottle"}, {"ItemFrame", "item_frame"}, {"WitherSkull", "wither_skull"},
            {"PrimedTnt", "tnt"}, {"FallingSand", "falling_block"}, {"FireworksRocketEntity", "fireworks_rocket"},
            {"Boat", "boat"}, {"MinecartRideable", "minecart"}, {"MinecartChest", "chest_minecart"},
            {"MinecartFurnace", "furnace_minecart"}, {"MinecartTNT", "tnt_minecart"}, {"MinecartHopper", "hopper_minecart"},
            {"MinecartSpawner", "spawner_minecart"}, {"MinecartCommandBlock", "commandblock_minecart"},
            {"Creeper", "creeper"}, {"Skeleton", "skeleton"}, {"Spider", "spider"}, {"Giant", "giant"},
            {"Zombie", "zombie"}, {"Slime", "slime"}, {"Ghast", "ghast"}, {"PigZombie", "zombie_pigman"},
            {"Enderman", "enderman"}, {"CaveSpider", "cave_spider"}, {"Silverfish", "silverfish"}, {"Blaze", "blaze"},
            {"LavaSlime", "magma_cube"}, {"EnderDragon", "ender_dragon"}, {"WitherBoss", "wither"}, {"Bat", "bat"},
            {"Witch", "witch"}, {"Pig", "pig"}, {"Sheep", "sheep"}, {"Cow", "cow"}, {"Chicken", "chicken"},
            {"Squid", "squid"}, {"Wolf", "wolf"}, {"MushroomCow", "mooshroom"}, {"SnowMan", "snowman"},
            {"Ozelot", "ocelot"}, {"VillagerGolem", "villager_golem"}, {"EntityHorse", "horse"},
            {"Villager", "villager"}, {"EnderCrystal", "ender_crystal"}};
        for (String[] pair : entities) {
            ENTITIES.put(pair[0], "minecraft:" + pair[1]);
            ENTITIES_BACK.put("minecraft:" + pair[1], pair[0]);
        }
        for (String horse : HORSES) ENTITIES_BACK.put("minecraft:" + horse, "EntityHorse");
        ENTITIES_BACK.put("minecraft:wither_skeleton", "Skeleton");
        ENTITIES_BACK.put("minecraft:zombie_villager", "Zombie");
    }

    private LegacyNbt() {}

    private static void tile(String legacy, String modern) {
        TILES.put(legacy, "minecraft:" + modern);
        TILES_BACK.put("minecraft:" + modern, legacy);
    }

    /** A 1.7.10 block entity tag as 1.12 stores it. */
    public static NBTTagCompound tileTo112(NBTTagCompound source) {
        NBTTagCompound tag = (NBTTagCompound) source.copy();
        String id = tag.getString("id");
        if (TILES.containsKey(id)) tag.setString("id", TILES.get(id));
        if ("Sign".equals(id)) {
            for (int i = 1; i <= 4; i++) tag.setString("Text" + i, json(tag.getString("Text" + i)));
        } else if ("MobSpawner".equals(id)) {
            spawnerTo112(tag);
        } else if ("FlowerPot".equals(id) && tag.hasKey("Item", NBT.TAG_ANY_NUMERIC)) {
            Item item = Item.getItemById(tag.getInteger("Item"));
            String name = item == null ? null : GameData.getItemRegistry().getNameForObject(item);
            tag.setString("Item", name == null ? "minecraft:air" : name);
        }
        itemsTo112(tag, 0);
        return tag;
    }

    /** A 1.12 block entity tag as 1.7.10 reads it. */
    public static NBTTagCompound tileFrom112(NBTTagCompound source) {
        NBTTagCompound tag = (NBTTagCompound) source.copy();
        String id = tag.getString("id");
        String legacy = TILES_BACK.get(id.indexOf(':') < 0 ? "minecraft:" + id.toLowerCase(java.util.Locale.ROOT) : id);
        if (legacy != null) tag.setString("id", legacy);
        if ("Sign".equals(legacy)) {
            for (int i = 1; i <= 4; i++) tag.setString("Text" + i, plain(tag.getString("Text" + i)));
        } else if ("MobSpawner".equals(legacy)) {
            spawnerFrom112(tag);
        } else if ("FlowerPot".equals(legacy) && tag.hasKey("Item", NBT.TAG_STRING)) {
            Item item = (Item) GameData.getItemRegistry().getObject(tag.getString("Item"));
            tag.setInteger("Item", item == null ? 0 : Item.getIdFromItem(item));
        }
        itemsFrom112(tag, 0);
        return tag;
    }

    /** A 1.7.10 entity tag as 1.12 stores it; a rider becomes a passenger of its mount, which becomes the stored entity. */
    public static NBTTagCompound entityTo112(NBTTagCompound source) {
        NBTTagCompound root = null, rider = null;
        NBTTagCompound current = source;
        for (int depth = 0; current != null && depth < MAX_RIDING; depth++) {
            NBTTagCompound mount = current.hasKey("Riding", NBT.TAG_COMPOUND) ? current.getCompoundTag("Riding") : null;
            NBTTagCompound converted = singleEntityTo112(current);
            converted.removeTag("Riding");
            if (rider != null) {
                NBTTagList passengers = new NBTTagList();
                passengers.appendTag(rider);
                converted.setTag("Passengers", passengers);
            }
            rider = converted;
            root = converted;
            current = mount;
        }
        return root;
    }

    private static NBTTagCompound singleEntityTo112(NBTTagCompound source) {
        NBTTagCompound tag = (NBTTagCompound) source.copy();
        entityIdTo112(tag);
        if (tag.hasKey("Equipment", NBT.TAG_LIST)) {
            NBTTagList equipment = tag.getTagList("Equipment", NBT.TAG_COMPOUND);
            NBTTagList hand = new NBTTagList(), armor = new NBTTagList();
            for (int i = 0; i < 5; i++) {
                NBTTagCompound item = i < equipment.tagCount() ? equipment.getCompoundTagAt(i) : new NBTTagCompound();
                (i == 0 ? hand : armor).appendTag(item);
            }
            hand.appendTag(new NBTTagCompound());
            tag.setTag("HandItems", hand);
            tag.setTag("ArmorItems", armor);
            tag.removeTag("Equipment");
        }
        if (tag.hasKey("DropChances", NBT.TAG_LIST)) {
            NBTTagList chances = tag.getTagList("DropChances", NBT.TAG_FLOAT);
            NBTTagList hand = new NBTTagList(), armor = new NBTTagList();
            for (int i = 0; i < 5; i++) (i == 0 ? hand : armor).appendTag(new NBTTagFloat(i < chances.tagCount() ? chances.func_150308_e(i) : 0.085F));
            hand.appendTag(new NBTTagFloat(0.085F));
            tag.setTag("HandDropChances", hand);
            tag.setTag("ArmorDropChances", armor);
            tag.removeTag("DropChances");
        }
        itemsTo112(tag, 0);
        return tag;
    }

    /**
     * A 1.12 entity tag as 1.7.10 reads it; the first passenger chain becomes riders holding their mounts, at the mount's
     * position (1.7.10 has one rider per entity, further passengers are dropped).
     */
    public static NBTTagCompound entityFrom112(NBTTagCompound source) {
        NBTTagCompound mount = null;
        NBTTagCompound current = source;
        for (int depth = 0; current != null && depth < MAX_RIDING; depth++) {
            NBTTagList passengers = current.getTagList("Passengers", NBT.TAG_COMPOUND);
            NBTTagCompound converted = singleEntityFrom112(current);
            converted.removeTag("Passengers");
            if (mount != null) {
                if (mount.hasKey("Pos", NBT.TAG_LIST)) converted.setTag("Pos", mount.getTag("Pos").copy());
                converted.setTag("Riding", mount);
            }
            mount = converted;
            current = passengers.tagCount() > 0 ? passengers.getCompoundTagAt(0) : null;
        }
        return mount;
    }

    private static NBTTagCompound singleEntityFrom112(NBTTagCompound source) {
        NBTTagCompound tag = (NBTTagCompound) source.copy();
        entityIdFrom112(tag);
        if (tag.hasKey("HandItems", NBT.TAG_LIST) || tag.hasKey("ArmorItems", NBT.TAG_LIST)) {
            NBTTagList hand = tag.getTagList("HandItems", NBT.TAG_COMPOUND), armor = tag.getTagList("ArmorItems", NBT.TAG_COMPOUND);
            NBTTagList equipment = new NBTTagList();
            equipment.appendTag(hand.tagCount() > 0 ? hand.getCompoundTagAt(0) : new NBTTagCompound());
            for (int i = 0; i < 4; i++) equipment.appendTag(i < armor.tagCount() ? armor.getCompoundTagAt(i) : new NBTTagCompound());
            tag.setTag("Equipment", equipment);
            tag.removeTag("HandItems");
            tag.removeTag("ArmorItems");
        }
        if (tag.hasKey("HandDropChances", NBT.TAG_LIST) || tag.hasKey("ArmorDropChances", NBT.TAG_LIST)) {
            NBTTagList hand = tag.getTagList("HandDropChances", NBT.TAG_FLOAT), armor = tag.getTagList("ArmorDropChances", NBT.TAG_FLOAT);
            NBTTagList chances = new NBTTagList();
            chances.appendTag(new NBTTagFloat(hand.tagCount() > 0 ? hand.func_150308_e(0) : 0.085F));
            for (int i = 0; i < 4; i++) chances.appendTag(new NBTTagFloat(i < armor.tagCount() ? armor.func_150308_e(i) : 0.085F));
            tag.setTag("DropChances", chances);
            tag.removeTag("HandDropChances");
            tag.removeTag("ArmorDropChances");
        }
        itemsFrom112(tag, 0);
        return tag;
    }

    private static void entityIdTo112(NBTTagCompound tag) {
        String id = tag.getString("id");
        if ("Skeleton".equals(id) && tag.getByte("SkeletonType") == 1) tag.setString("id", "minecraft:wither_skeleton");
        else if ("Zombie".equals(id) && tag.getBoolean("IsVillager")) {
            tag.setString("id", "minecraft:zombie_villager");
            if (tag.hasKey("VillagerProfession")) tag.setInteger("Profession", tag.getInteger("VillagerProfession"));
        } else if ("EntityHorse".equals(id)) {
            int type = tag.getInteger("Type");
            tag.setString("id", "minecraft:" + HORSES[type >= 0 && type < HORSES.length ? type : 0]);
        } else if (ENTITIES.containsKey(id)) tag.setString("id", ENTITIES.get(id));
        tag.removeTag("SkeletonType");
        tag.removeTag("IsVillager");
    }

    private static void entityIdFrom112(NBTTagCompound tag) {
        String id = tag.getString("id");
        if (id.indexOf(':') < 0) id = "minecraft:" + id;
        String legacy = ENTITIES_BACK.get(id);
        if (legacy == null) return;
        tag.setString("id", legacy);
        if (id.equals("minecraft:wither_skeleton")) tag.setByte("SkeletonType", (byte) 1);
        else if (id.equals("minecraft:zombie_villager")) {
            tag.setBoolean("IsVillager", true);
            if (tag.hasKey("Profession")) tag.setInteger("VillagerProfession", tag.getInteger("Profession"));
        } else if (legacy.equals("EntityHorse")) {
            for (int i = 0; i < HORSES.length; i++) if (id.equals("minecraft:" + HORSES[i])) tag.setInteger("Type", i);
        }
    }

    private static void spawnerTo112(NBTTagCompound tag) {
        if (tag.hasKey("EntityId", NBT.TAG_STRING)) {
            NBTTagCompound data = tag.getCompoundTag("SpawnData");
            data.setString("id", tag.getString("EntityId"));
            entityIdTo112(data);
            tag.setTag("SpawnData", data);
            tag.removeTag("EntityId");
        }
        NBTTagList potentials = tag.getTagList("SpawnPotentials", NBT.TAG_COMPOUND), converted = new NBTTagList();
        for (int i = 0; i < potentials.tagCount(); i++) {
            NBTTagCompound entry = potentials.getCompoundTagAt(i), result = new NBTTagCompound();
            NBTTagCompound entity = entry.hasKey("Properties", NBT.TAG_COMPOUND) ? entry.getCompoundTag("Properties") : new NBTTagCompound();
            entity.setString("id", entry.getString("Type"));
            entityIdTo112(entity);
            result.setTag("Entity", entity);
            result.setInteger("Weight", entry.getInteger("Weight"));
            converted.appendTag(result);
        }
        if (potentials.tagCount() > 0) tag.setTag("SpawnPotentials", converted);
    }

    private static void spawnerFrom112(NBTTagCompound tag) {
        if (tag.hasKey("SpawnData", NBT.TAG_COMPOUND)) {
            NBTTagCompound data = tag.getCompoundTag("SpawnData");
            entityIdFrom112(data);
            tag.setString("EntityId", data.getString("id"));
            data.removeTag("id");
        }
        NBTTagList potentials = tag.getTagList("SpawnPotentials", NBT.TAG_COMPOUND), converted = new NBTTagList();
        for (int i = 0; i < potentials.tagCount(); i++) {
            NBTTagCompound entry = potentials.getCompoundTagAt(i), result = new NBTTagCompound();
            NBTTagCompound entity = entry.getCompoundTag("Entity");
            entityIdFrom112(entity);
            result.setString("Type", entity.getString("id"));
            entity.removeTag("id");
            result.setTag("Properties", entity);
            result.setInteger("Weight", entry.getInteger("Weight"));
            converted.appendTag(result);
        }
        if (potentials.tagCount() > 0) tag.setTag("SpawnPotentials", converted);
    }

    private static boolean itemStack(NBTTagCompound tag) {
        return tag.hasKey("Count", NBT.TAG_BYTE) && tag.hasKey("id");
    }

    /** Numeric item ids in every nested stack become registry names. */
    private static void itemsTo112(NBTTagCompound tag, int depth) {
        if (depth > SchematicLimits.MAX_NBT_DEPTH) throw new IllegalArgumentException("NBT nesting is too deep");
        if (itemStack(tag) && tag.hasKey("id", NBT.TAG_SHORT)) {
            Item item = Item.getItemById(tag.getShort("id"));
            String name = item == null ? null : GameData.getItemRegistry().getNameForObject(item);
            if (name != null) tag.setString("id", name);
        }
        if (itemStack(tag) && tag.hasKey("id", NBT.TAG_STRING)) itemDataTo112(tag);
        for (Object key : tag.func_150296_c()) visit(tag.getTag((String) key), depth, true);
    }

    /** Registry-name item ids become this game's numeric ids; names unknown here stay and load as nothing. */
    private static void itemsFrom112(NBTTagCompound tag, int depth) {
        if (depth > SchematicLimits.MAX_NBT_DEPTH) throw new IllegalArgumentException("NBT nesting is too deep");
        if (itemStack(tag) && tag.hasKey("id", NBT.TAG_STRING)) itemDataFrom112(tag);
        if (itemStack(tag) && tag.hasKey("id", NBT.TAG_STRING)) {
            Item item = (Item) GameData.getItemRegistry().getObject(tag.getString("id"));
            if (item != null && GameData.getItemRegistry().containsKey(tag.getString("id"))) {
                tag.setShort("id", (short) Item.getIdFromItem(item));
            }
        }
        for (Object key : tag.func_150296_c()) visit(tag.getTag((String) key), depth, false);
    }

    /** ItemPotionFix and ItemSpawnEggFix: the damage of potions and spawn eggs moves into their tag. */
    static void itemDataTo112(NBTTagCompound stack) {
        String id = stack.getString("id");
        int damage = stack.getShort("Damage");
        if ("minecraft:potion".equals(id)) {
            NBTTagCompound tag = stack.getCompoundTag("tag");
            if (!tag.hasKey("Potion", NBT.TAG_STRING)) {
                String potion = POTIONS[damage & 127];
                tag.setString("Potion", "minecraft:" + (potion == null ? "water" : potion));
            }
            stack.setTag("tag", tag);
            if ((damage & 16384) != 0) stack.setString("id", "minecraft:splash_potion");
            stack.setShort("Damage", (short) 0);
        } else if ("minecraft:spawn_egg".equals(id)) {
            String legacy = net.minecraft.entity.EntityList.getStringFromID(damage);
            String modern = legacy == null ? null : ENTITIES.get(legacy);
            if (modern == null && legacy != null) modern = legacy;
            NBTTagCompound tag = stack.getCompoundTag("tag");
            NBTTagCompound entity = tag.getCompoundTag("EntityTag");
            if (modern != null && !entity.hasKey("id", NBT.TAG_STRING)) {
                entity.setString("id", modern);
                tag.setTag("EntityTag", entity);
                stack.setTag("tag", tag);
            }
            stack.setShort("Damage", (short) 0);
        }
    }

    /** The potion damage of 1.7.10: drinkable potions carry 8192, splash potions 16384. */
    static int potionDamage(String potion, boolean splash) {
        String name = potion.startsWith("minecraft:") ? potion.substring(10) : potion;
        int index = 0;
        for (int i = 0; i < POTIONS.length; i++) if (name.equals(POTIONS[i])) { index = i; break; }
        if (index == 0 || "awkward".equals(name) || "thick".equals(name) || "mundane".equals(name)) return splash ? index | 16384 : index;
        return index | (splash ? 16384 : 8192);
    }

    static void itemDataFrom112(NBTTagCompound stack) {
        String id = stack.getString("id");
        if (id.indexOf(':') < 0) id = "minecraft:" + id;
        boolean splash = "minecraft:splash_potion".equals(id) || "minecraft:lingering_potion".equals(id);
        if (splash || "minecraft:potion".equals(id)) {
            NBTTagCompound tag = stack.getCompoundTag("tag");
            stack.setShort("Damage", (short) potionDamage(tag.hasKey("Potion", NBT.TAG_STRING) ? tag.getString("Potion") : "water", splash));
            stack.setString("id", "minecraft:potion");
            tag.removeTag("Potion");
            if (tag.hasNoTags()) stack.removeTag("tag"); else stack.setTag("tag", tag);
        } else if ("minecraft:spawn_egg".equals(id) && stack.hasKey("tag", NBT.TAG_COMPOUND)) {
            NBTTagCompound tag = stack.getCompoundTag("tag");
            NBTTagCompound entity = tag.getCompoundTag("EntityTag");
            String modern = entity.getString("id");
            if (modern.indexOf(':') < 0 && !modern.isEmpty()) modern = "minecraft:" + modern;
            String legacy = ENTITIES_BACK.getOrDefault(modern, modern);
            int numeric = entityNumericId(legacy);
            if (numeric > 0) {
                stack.setShort("Damage", (short) numeric);
                entity.removeTag("id");
                if (entity.hasNoTags()) tag.removeTag("EntityTag");
                if (tag.hasNoTags()) stack.removeTag("tag");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static int entityNumericId(String legacy) {
        Class<?> type = net.minecraft.entity.EntityList.stringToClassMapping.get(legacy);
        if (type == null) return -1;
        for (Map.Entry<Integer, Class<? extends net.minecraft.entity.Entity>> entry : net.minecraft.entity.EntityList.IDtoClassMapping.entrySet()) {
            if (entry.getValue() == type) return entry.getKey();
        }
        return -1;
    }

    private static void visit(NBTBase child, int depth, boolean to112) {
        if (child instanceof NBTTagCompound) {
            if (to112) itemsTo112((NBTTagCompound) child, depth + 1);
            else itemsFrom112((NBTTagCompound) child, depth + 1);
        } else if (child instanceof NBTTagList && ((NBTTagList) child).func_150303_d() == NBT.TAG_COMPOUND) {
            NBTTagList list = (NBTTagList) child;
            for (int i = 0; i < list.tagCount(); i++) {
                if (to112) itemsTo112(list.getCompoundTagAt(i), depth + 1);
                else itemsFrom112(list.getCompoundTagAt(i), depth + 1);
            }
        }
    }

    /** Sign line as a JSON text component, as the 1.8 sign fixer stores it. */
    static String json(String text) {
        JsonObject object = new JsonObject();
        object.addProperty("text", text);
        return object.toString();
    }

    /** The plain text of a JSON sign line; text that is not JSON stays as it is. */
    static String plain(String text) {
        try {
            StringBuilder builder = new StringBuilder();
            flatten(new JsonParser().parse(text), builder);
            return builder.toString();
        } catch (RuntimeException e) {
            return text;
        }
    }

    private static void flatten(JsonElement element, StringBuilder builder) {
        if (element == null || element.isJsonNull()) return;
        if (element.isJsonPrimitive()) {
            builder.append(((JsonPrimitive) element).getAsString());
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) flatten(child, builder);
        } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("text")) builder.append(object.get("text").getAsString());
            if (object.has("extra")) flatten(object.get("extra"), builder);
        }
    }
}
