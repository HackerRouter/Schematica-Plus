// Modern vanilla item import, by HackerRouter, 2026.
package com.github.lunatrius.schematica.world.schematic;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants.NBT;

import com.github.lunatrius.schematica.compat.ModernText;

/** Converts flattened item names and 1.20.5+ components to native item stacks. */
public final class ModernItemNbt {
    private static final Map<String, Integer> ENCHANTMENTS = new HashMap<>();
    private static final Map<String, Integer> EFFECTS = new HashMap<>();
    private static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};

    static {
        names(ENCHANTMENTS, 0, "protection", "fire_protection", "feather_falling", "blast_protection", "projectile_protection",
            "respiration", "aqua_affinity", "thorns");
        names(ENCHANTMENTS, 16, "sharpness", "smite", "bane_of_arthropods", "knockback", "fire_aspect", "looting");
        names(ENCHANTMENTS, 32, "efficiency", "silk_touch", "unbreaking");
        names(ENCHANTMENTS, 35, "fortune");
        names(ENCHANTMENTS, 48, "power", "punch", "flame", "infinity");
        names(ENCHANTMENTS, 61, "luck_of_the_sea", "lure");
        names(EFFECTS, 1, "speed", "slowness", "haste", "mining_fatigue", "strength", "instant_health", "instant_damage",
            "jump_boost", "nausea", "regeneration", "resistance", "fire_resistance", "water_breathing", "invisibility",
            "blindness", "night_vision", "hunger", "weakness", "poison", "wither", "health_boost", "absorption", "saturation");
    }

    private ModernItemNbt() {}

    private static void names(Map<String, Integer> map, int first, String... names) {
        for (int i = 0; i < names.length; i++) map.put("minecraft:" + names[i], first + i);
    }

    public static void convert(NBTTagCompound stack, int dataVersion) {
        convert(stack, dataVersion, ItemIdMaps.CURRENT);
    }

    static void convert(NBTTagCompound stack, int dataVersion, ItemIdMaps.Ids ids) {
        if (stack == null || !stack.hasKey("id", NBT.TAG_STRING)) return;
        String original = stack.getString("id");
        String name = original.contains(":") ? original : "minecraft:" + original;
        int count = stack.hasKey("count", NBT.TAG_ANY_NUMERIC) ? stack.getInteger("count")
            : stack.hasKey("Count", NBT.TAG_ANY_NUMERIC) ? stack.getInteger("Count") : 1;
        if (count < 1 || count > 127) { discard(stack, original + " (count=" + count + ")"); return; }
        stack.setString("id", name);
        stack.setByte("Count", (byte) count);
        stack.removeTag("count");
        NBTTagCompound tag = stack.getCompoundTag("tag");
        if (tag.hasKey("Damage", NBT.TAG_ANY_NUMERIC)) {
            stack.setShort("Damage", (short) Math.max(0, Math.min(32767, tag.getInteger("Damage"))));
            tag.removeTag("Damage");
        }
        normalizeDisplay(tag, dataVersion);
        if (name.equals("minecraft:written_book") && tag.hasKey("pages", NBT.TAG_LIST)) {
            tag.setTag("pages", textList(tag.getTagList("pages", NBT.TAG_STRING), dataVersion, false));
        }
        if (tag.hasKey("Enchantments", NBT.TAG_LIST)) {
            tag.setTag("ench", enchantmentList(tag.getTagList("Enchantments", NBT.TAG_COMPOUND)));
            tag.removeTag("Enchantments");
        }
        if (tag.hasKey("StoredEnchantments", NBT.TAG_LIST)) {
            tag.setTag("StoredEnchantments", enchantmentList(tag.getTagList("StoredEnchantments", NBT.TAG_COMPOUND)));
        }
        if (tag.hasKey("custom_potion_effects", NBT.TAG_LIST)) {
            tag.setTag("CustomPotionEffects", effects(tag.getTagList("custom_potion_effects", NBT.TAG_COMPOUND)));
            tag.removeTag("custom_potion_effects");
        }
        components(stack, tag, dataVersion);
        stack.removeTag("components");
        if (!tag.hasNoTags()) stack.setTag("tag", tag);
        mapName(stack, name);
        String potion = tag.getString("Potion");
        if (!potion.isEmpty() && LegacyNbt.potionDamage(potion, false) == 0 && !potion.equals("minecraft:water") && !potion.equals("water")) {
            ImportReport.omittedData("potion: " + potion);
        }
        LegacyNbt.itemDataFrom112(stack);
        String mapped = stack.getString("id");
        if (mapped.equals("minecraft:spawn_egg") && stack.getShort("Damage") <= 0) { discard(stack, original); return; }
        int id = ids.item(mapped);
        if (id <= 0) { discard(stack, original); return; }
        stack.setShort("id", (short) id);
        if (!stack.hasKey("Damage")) stack.setShort("Damage", (short) 0);
    }

    private static void discard(NBTTagCompound stack, String detail) {
        stack.setShort("id", (short) 0);
        stack.setByte("Count", (byte) 0);
        stack.removeTag("count");
        ItemIdMaps.Result report = ImportReport.items();
        report.removed++;
        report.unknownItems.add(detail);
    }

    public static void list(NBTTagCompound parent, String key, int dataVersion) {
        if (!parent.hasKey(key, NBT.TAG_LIST)) return;
        NBTTagList input = parent.getTagList(key, NBT.TAG_COMPOUND), output = new NBTTagList();
        for (int i = 0; i < input.tagCount(); i++) {
            NBTTagCompound item = input.getCompoundTagAt(i);
            convert(item, dataVersion);
            if (item.getInteger("Count") > 0) output.appendTag(item);
        }
        parent.setTag(key, output);
    }

    private static void mapName(NBTTagCompound stack, String name) {
        if (!name.startsWith("minecraft:")) return;
        String path = name.substring(10), target = null;
        int damage = 0;
        for (int i = 0; i < COLORS.length; i++) if (path.equals(COLORS[i] + "_dye")) { target = "dye"; damage = 15 - i; }
        switch (path) {
            case "ink_sac": target = "dye"; break;
            case "lapis_lazuli": target = "dye"; damage = 4; break;
            case "cocoa_beans": target = "dye"; damage = 3; break;
            case "bone_meal": target = "dye"; damage = 15; break;
            case "charcoal": target = "coal"; damage = 1; break;
            case "cod": target = "fish"; break;
            case "salmon": target = "fish"; damage = 1; break;
            case "tropical_fish": target = "fish"; damage = 2; break;
            case "pufferfish": target = "fish"; damage = 3; break;
            case "cooked_cod": target = "cooked_fished"; break;
            case "cooked_salmon": target = "cooked_fished"; damage = 1; break;
            case "enchanted_golden_apple": target = "golden_apple"; damage = 1; break;
            case "skeleton_skull": target = "skull"; break;
            case "wither_skeleton_skull": target = "skull"; damage = 1; break;
            case "zombie_head": target = "skull"; damage = 2; break;
            case "player_head": target = "skull"; damage = 3; break;
            case "creeper_head": target = "skull"; damage = 4; break;
            case "oak_sign": target = "sign"; break;
            case "oak_door": target = "wooden_door"; break;
            case "oak_boat": target = "boat"; break;
            case "sugar_cane": target = "reeds"; break;
            case "nether_brick": target = "netherbrick"; break;
            case "melon_slice": target = "melon"; break;
            case "glistering_melon_slice": target = "speckled_melon"; break;
            case "firework_rocket": target = "fireworks"; break;
            case "firework_star": target = "firework_charge"; break;
            case "repeater": target = "repeater"; break;
            case "comparator": target = "comparator"; break;
            case "redstone_torch": target = "redstone_torch"; break;
            default: break;
        }
        if (path.startsWith("music_disc_")) target = "record_" + path.substring(11);
        if (target != null) {
            stack.setString("id", "minecraft:" + target);
            stack.setShort("Damage", (short) damage);
        } else if (path.endsWith("_spawn_egg")) {
            stack.setString("id", "minecraft:spawn_egg");
            NBTTagCompound tag = stack.getCompoundTag("tag"), entity = tag.getCompoundTag("EntityTag");
            entity.setString("id", "minecraft:" + path.substring(0, path.length() - 10));
            tag.setTag("EntityTag", entity); stack.setTag("tag", tag);
        } else {
            LegacyBlockStates.Entry block = LegacyBlockStates.modernItem(name);
            if (block == null && path.endsWith("_wood")) {
                block = LegacyBlockStates.modernItem(name.substring(0, name.length() - 5) + "_bark");
            }
            if (block != null && !block.name.equals(name)) {
                String item = block.name;
                int meta = block.meta();
                if (item.equals("minecraft:double_stone_slab")) { item = "minecraft:stone_slab"; meta &= 7; }
                if (item.equals("minecraft:double_wooden_slab")) { item = "minecraft:wooden_slab"; meta &= 7; }
                if (item.equals("minecraft:anvil")) meta >>= 2;
                stack.setString("id", item);
                stack.setShort("Damage", (short) meta);
            }
        }
    }

    private static void normalizeDisplay(NBTTagCompound tag, int version) {
        NBTTagCompound display = tag.getCompoundTag("display");
        if (display.hasKey("Name")) display.setString("Name", ModernText.plain(display.getTag("Name"), version));
        if (display.hasKey("Lore", NBT.TAG_LIST)) display.setTag("Lore", textList(display.getTagList("Lore", NBT.TAG_STRING), version, false));
    }

    private static void components(NBTTagCompound stack, NBTTagCompound tag, int version) {
        NBTTagCompound components = stack.getCompoundTag("components");
        NBTTagCompound custom = components.getCompoundTag("minecraft:custom_data");
        for (Object key : custom.func_150296_c()) tag.setTag((String) key, custom.getTag((String) key).copy());
        NBTTagCompound display = tag.getCompoundTag("display");
        for (Object rawKey : components.func_150296_c()) {
            String key = (String) rawKey;
            switch (key) {
                case "minecraft:custom_data": break;
                case "minecraft:damage": stack.setShort("Damage", (short) Math.max(0, Math.min(32767, components.getInteger(key)))); break;
                case "minecraft:custom_name": display.setString("Name", ModernText.plain(components.getTag(key), version)); break;
                case "minecraft:item_name":
                    if (!components.hasKey("minecraft:custom_name")) display.setString("Name", ModernText.plain(components.getTag(key), version));
                    break;
                case "minecraft:lore":
                    if (components.hasKey(key, NBT.TAG_LIST)) display.setTag("Lore", textList((NBTTagList) components.getTag(key), version, false));
                    else ImportReport.omittedData(key + " (invalid list)");
                    break;
                case "minecraft:dyed_color":
                    display.setInteger("color", components.hasKey(key, NBT.TAG_COMPOUND)
                        ? components.getCompoundTag(key).getInteger("rgb") : components.getInteger(key));
                    break;
                case "minecraft:repair_cost": tag.setInteger("RepairCost", components.getInteger(key)); break;
                case "minecraft:unbreakable": tag.setBoolean("Unbreakable", true); break;
                case "minecraft:enchantments": tag.setTag("ench", enchantments(components.getCompoundTag(key))); break;
                case "minecraft:stored_enchantments": tag.setTag("StoredEnchantments", enchantments(components.getCompoundTag(key))); break;
                case "minecraft:potion_contents":
                    NBTTagCompound potion = components.getCompoundTag(key);
                    if (potion.hasKey("potion")) tag.setString("Potion", potion.getString("potion"));
                    if (potion.hasKey("custom_effects")) tag.setTag("CustomPotionEffects", effects(potion.getTagList("custom_effects", NBT.TAG_COMPOUND)));
                    break;
                case "minecraft:written_book_content": case "minecraft:writable_book_content":
                    NBTTagCompound book = components.getCompoundTag(key);
                    boolean written = key.equals("minecraft:written_book_content");
                    if (written) {
                        NBTBase title = book.getTag("title");
                        if (title instanceof NBTTagCompound) title = ((NBTTagCompound) title).getTag("raw");
                        tag.setString("title", ModernText.plain(title, ModernText.NBT_COMPONENT_VERSION));
                        tag.setString("author", book.getString("author"));
                    }
                    NBTTagList pages = book.hasKey("pages", NBT.TAG_LIST) ? (NBTTagList) book.getTag("pages") : new NBTTagList();
                    tag.setTag("pages", textList(pages, written ? version : ModernText.NBT_COMPONENT_VERSION, true));
                    break;
                default: ImportReport.omittedData(key); break;
            }
        }
        if (!display.hasNoTags()) tag.setTag("display", display);
    }

    private static NBTTagList textList(NBTTagList input, int version, boolean filtered) {
        NBTTagList result = new NBTTagList();
        for (int i = 0; i < input.tagCount(); i++) {
            NBTBase value = ModernText.element(input, i);
            if (filtered && value instanceof NBTTagCompound && ((NBTTagCompound) value).hasKey("raw")) value = ((NBTTagCompound) value).getTag("raw");
            result.appendTag(new NBTTagString(ModernText.plain(value, version)));
        }
        return result;
    }

    private static NBTTagList enchantments(NBTTagCompound value) {
        NBTTagCompound levels = value.hasKey("levels", NBT.TAG_COMPOUND) ? value.getCompoundTag("levels") : value;
        NBTTagList result = new NBTTagList();
        for (Object key : levels.func_150296_c()) {
            if (!levels.hasKey((String) key, NBT.TAG_ANY_NUMERIC)) continue;
            enchantment(result, (String) key, levels.getInteger((String) key));
        }
        return result;
    }

    private static NBTTagList enchantmentList(NBTTagList input) {
        NBTTagList result = new NBTTagList();
        for (int i = 0; i < input.tagCount(); i++) {
            NBTTagCompound entry = input.getCompoundTagAt(i);
            if (entry.hasKey("id", NBT.TAG_STRING)) enchantment(result, entry.getString("id"), entry.getInteger("lvl"));
            else result.appendTag(entry.copy());
        }
        return result;
    }

    private static void enchantment(NBTTagList output, String name, int level) {
        Integer id = ENCHANTMENTS.get(name.contains(":") ? name : "minecraft:" + name);
        if (id == null) { ImportReport.omittedData("enchantment: " + name); return; }
        NBTTagCompound entry = new NBTTagCompound();
        entry.setShort("id", id.shortValue()); entry.setShort("lvl", (short) Math.max(1, Math.min(32767, level)));
        output.appendTag(entry);
    }

    public static NBTTagList effects(NBTTagList input) {
        NBTTagList result = new NBTTagList();
        for (int i = 0; i < input.tagCount(); i++) {
            NBTTagCompound effect = input.getCompoundTagAt(i);
            String name = effect.getString("id");
            Integer id = EFFECTS.get(name.contains(":") ? name : "minecraft:" + name);
            if (id == null) { ImportReport.omittedData("effect: " + name); continue; }
            NBTTagCompound legacy = new NBTTagCompound();
            legacy.setByte("Id", id.byteValue()); legacy.setByte("Amplifier", (byte) Math.max(0, Math.min(127, effect.getInteger("amplifier"))));
            legacy.setInteger("Duration", Math.max(0, effect.getInteger("duration")));
            legacy.setBoolean("Ambient", effect.getBoolean("ambient"));
            result.appendTag(legacy);
        }
        return result;
    }
}
