package com.github.lunatrius.schematica.client.gui.material;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class RawMaterialExportTest {
    private static List<List<String>> slots(String... items) {
        List<List<String>> slots = new java.util.ArrayList<>();
        for (String item : items) slots.add(Arrays.asList(item.split("\\|")));
        return slots;
    }

    private static RecipeIndex index() {
        RecipeIndex index = new RecipeIndex();
        index.add(RecipeIndex.Type.SHAPELESS, "minecraft:planks", 4, slots("minecraft:log"));
        index.add(RecipeIndex.Type.SHAPED, "minecraft:stick", 4, slots("minecraft:planks", "minecraft:planks"));
        index.add(RecipeIndex.Type.SHAPED, "minecraft:ladder", 3, slots("minecraft:stick", "minecraft:stick", "minecraft:stick",
            "minecraft:stick", "minecraft:stick", "minecraft:stick", "minecraft:stick"));
        index.add(RecipeIndex.Type.SHAPED, "minecraft:iron_block", 1, slots("minecraft:iron_ingot", "minecraft:iron_ingot", "minecraft:iron_ingot",
            "minecraft:iron_ingot", "minecraft:iron_ingot", "minecraft:iron_ingot", "minecraft:iron_ingot", "minecraft:iron_ingot", "minecraft:iron_ingot"));
        index.add(RecipeIndex.Type.SHAPELESS, "minecraft:iron_ingot", 9, slots("minecraft:iron_block"));
        index.add(RecipeIndex.Type.SHAPED, "minecraft:iron_bars", 16, slots("minecraft:iron_ingot", "minecraft:iron_ingot", "minecraft:iron_ingot",
            "minecraft:iron_ingot", "minecraft:iron_ingot", "minecraft:iron_ingot"));
        index.add(RecipeIndex.Type.FURNACE, "minecraft:iron_ingot", 1, slots("minecraft:iron_ore"));
        index.add(RecipeIndex.Type.FURNACE, "minecraft:glass", 1, slots("minecraft:sand"));
        index.add(RecipeIndex.Type.SHAPELESS, "minecraft:wool", 1, slots("minecraft:dye:15", "minecraft:wool"));
        index.add(RecipeIndex.Type.SHAPED, "minecraft:wool", 1, slots("minecraft:string", "minecraft:string", "minecraft:string", "minecraft:string"));
        index.add(RecipeIndex.Type.SHAPED, "mod:a", 1, slots("mod:b"));
        index.add(RecipeIndex.Type.SHAPED, "mod:b", 1, slots("mod:c"));
        index.add(RecipeIndex.Type.SHAPED, "mod:c", 1, slots("mod:a|mod:x"));
        return index;
    }

    @Test public void takesMaterialsApartThroughCraftingThenSmeltingRoundingUpCrafts() {
        RecipeIndex index = index();
        RawMaterialExport.Node ladder = RawMaterialExport.resolve(index, "minecraft:ladder", 4);
        assertEquals(RecipeIndex.Type.SHAPED, ladder.recipe.type);
        RawMaterialExport.Node sticks = ladder.requirements.get(0);
        assertEquals("minecraft:stick", sticks.item);
        assertEquals(14, sticks.count); // two crafts of seven sticks
        RawMaterialExport.Node planks = sticks.requirements.get(0);
        assertEquals(8, planks.count); // four crafts of two planks
        assertEquals("minecraft:log", planks.requirements.get(0).item);
        assertEquals(2, planks.requirements.get(0).count);
        assertTrue(planks.requirements.get(0).leaf());
        RawMaterialExport.Node glass = RawMaterialExport.resolve(index, "minecraft:glass", 5);
        assertEquals(RecipeIndex.Type.FURNACE, glass.recipe.type);
        assertEquals(5, glass.requirements.get(0).count);
    }

    @Test public void keepsBaseMaterialsSkipsRecoloringAndStopsLoops() {
        RecipeIndex index = index();
        assertTrue(index.isBaseMaterial("minecraft:iron_ingot"));
        assertFalse(index.isBaseMaterial("minecraft:planks"));
        RawMaterialExport.Node bars = RawMaterialExport.resolve(index, "minecraft:iron_bars", 20);
        assertTrue(bars.requirements.get(0).leaf());
        assertEquals(12, bars.requirements.get(0).count);
        RawMaterialExport.Node wool = RawMaterialExport.resolve(index, "minecraft:wool", 2);
        assertEquals("minecraft:string", wool.requirements.get(0).item);
        assertEquals(8, wool.requirements.get(0).count);
        RawMaterialExport.Node a = RawMaterialExport.resolve(index, "mod:a", 1);
        assertTrue(a.requirements.get(0).requirements.get(0).leaf());
        assertEquals("mod:c", a.requirements.get(0).requirements.get(0).item);
    }

    @Test public void writesStepsOnTheFirstRawMaterialAndPacksTheSimplifiedList() {
        RecipeIndex index = index();
        Map<String, Integer> materials = new LinkedHashMap<>();
        materials.put("minecraft:iron_bars", 160);
        materials.put("minecraft:iron_block", 1);
        materials.put("minecraft:ladder", 3);
        RawMaterialExport.Result result = RawMaterialExport.export(index, materials);
        assertEquals(3, result.details.size());
        assertEquals(2, result.flat.get(0).steps.size());
        assertEquals("minecraft:iron_bars", result.flat.get(0).steps.get(1).item);
        assertEquals("GATHER", result.flat.get(0).steps.get(0).category);
        RawMaterialExport.Raw ingots = null, blocks = null;
        for (RawMaterialExport.Raw raw : result.combined) {
            if (raw.item.equals("minecraft:iron_ingot")) ingots = raw;
            if (raw.item.equals("minecraft:iron_block")) blocks = raw;
        }
        // 60 + 9 ingots = 7 blocks and 6 ingots
        assertNotNull(blocks);
        assertEquals(7, blocks.total);
        assertEquals(6, ingots.total);
        assertEquals(Long.valueOf(160), ingots.results.get("minecraft:iron_bars"));
        JsonArray details = RawMaterialExport.detailsJson(result).getAsJsonArray();
        JsonObject bars = details.get(0).getAsJsonObject();
        assertEquals("minecraft:iron_bars", bars.get("Item").getAsString());
        JsonObject crafting = bars.getAsJsonObject("CraftingMaterials");
        assertEquals("ONE", crafting.get("Type").getAsString());
        JsonObject recipe = crafting.getAsJsonArray("Recipes").get(0).getAsJsonObject();
        assertEquals("crafting", recipe.get("Category").getAsString());
        assertEquals("EMPTY", recipe.getAsJsonArray("Requirements").get(0).getAsJsonObject()
            .getAsJsonObject("RemainingMaterials").get("Type").getAsString());
        JsonObject simplified = RawMaterialExport.rawJson(result.combined).getAsJsonArray().get(0).getAsJsonObject();
        assertTrue(simplified.has("RawItem") && simplified.has("TotalEstimate") && simplified.getAsJsonArray("Steps").size() == 0);
    }

    @Test public void packsNuggetsIntoIngotsBeforeBlocks() {
        RecipeIndex index = new RecipeIndex();
        index.add(RecipeIndex.Type.SHAPED, "mod:ingot", 1, slots("mod:nugget", "mod:nugget", "mod:nugget", "mod:nugget", "mod:nugget",
            "mod:nugget", "mod:nugget", "mod:nugget", "mod:nugget"));
        index.add(RecipeIndex.Type.SHAPELESS, "mod:nugget", 9, slots("mod:ingot"));
        index.add(RecipeIndex.Type.SHAPED, "mod:block", 1, slots("mod:ingot", "mod:ingot", "mod:ingot", "mod:ingot", "mod:ingot",
            "mod:ingot", "mod:ingot", "mod:ingot", "mod:ingot"));
        index.add(RecipeIndex.Type.SHAPELESS, "mod:ingot", 9, slots("mod:block"));
        RawMaterialExport.Result result = RawMaterialExport.export(index, Collections.singletonMap("mod:nugget", 9 * 9 * 2 + 9 + 4));
        assertEquals(3, result.combined.size());
        assertEquals("mod:block", result.combined.get(0).item);
        assertEquals(2, result.combined.get(0).total);
        assertEquals(1, result.combined.get(1).total);
        assertEquals(4, result.combined.get(2).total);
    }
}
