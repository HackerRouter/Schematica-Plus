// SPDX-License-Identifier: LGPL-3.0-only
// The recipe lookups of Litematica's raw material list (RecipeBookUtils), adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.gui.material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.common.registry.GameData;

/**
 * Crafting and smelting recipes by output item. Items are "modid:name" or "modid:name:damage" strings;
 * an ingredient slot lists its alternatives (an ore dictionary entry has several).
 */
public final class RecipeIndex {
    public enum Type { SHAPED, SHAPELESS, FURNACE, UNKNOWN }

    public static final class Recipe {
        public final int id;
        public final Type type;
        public final String output;
        public final int outputCount;
        public final List<List<String>> ingredients;

        Recipe(int id, Type type, String output, int outputCount, List<List<String>> ingredients) {
            this.id = id;
            this.type = type;
            this.output = output;
            this.outputCount = Math.max(1, outputCount);
            List<List<String>> slots = new ArrayList<>();
            for (List<String> slot : ingredients) if (!slot.isEmpty()) slots.add(Collections.unmodifiableList(new ArrayList<>(slot)));
            this.ingredients = Collections.unmodifiableList(slots);
        }

        public boolean crafting() { return type == Type.SHAPED || type == Type.SHAPELESS; }

        public String category() { return crafting() ? "crafting" : "furnace"; }
    }

    /** n items packed into one target, such as nine ingots into a block, when the block also unpacks back into them. */
    public static final class Packing {
        public final String target;
        public final int ratio;

        Packing(String target, int ratio) {
            this.target = target;
            this.ratio = ratio;
        }
    }

    private final Map<String, List<Recipe>> byOutput = new HashMap<>();
    private final List<Recipe> all = new ArrayList<>();
    private Map<String, Packing> packing;

    public void add(Type type, String output, int outputCount, List<List<String>> ingredients) {
        if (output == null) return;
        Recipe recipe = new Recipe(all.size(), type, output, outputCount, ingredients);
        if (recipe.ingredients.isEmpty()) return;
        all.add(recipe);
        byOutput.computeIfAbsent(output, ignored -> new ArrayList<>()).add(recipe);
        packing = null;
    }

    public List<Recipe> recipesFor(String item) {
        List<Recipe> recipes = byOutput.get(item);
        return recipes == null ? Collections.<Recipe>emptyList() : Collections.unmodifiableList(recipes);
    }

    /** The packed form of an item (ingots into blocks, nuggets into ingots), or null. */
    public Packing packing(String item) {
        if (packing == null) packing = findPacking();
        return packing.get(item);
    }

    /** Litematica's unpacked block items: kept as base materials instead of being taken apart further. */
    public boolean isBaseMaterial(String item) { return packing(item) != null; }

    private Map<String, Packing> findPacking() {
        Map<String, Packing> result = new HashMap<>();
        for (Recipe recipe : all) {
            if (!recipe.crafting() || recipe.outputCount != 1) continue;
            int slots = recipe.ingredients.size();
            if (slots != 4 && slots != 9) continue;
            String input = recipe.ingredients.get(0).get(0);
            boolean same = true;
            for (List<String> slot : recipe.ingredients) same &= slot.size() == 1 && slot.get(0).equals(input);
            if (!same || input.equals(recipe.output) || result.containsKey(input)) continue;
            for (Recipe back : recipesFor(input)) {
                if (back.crafting() && back.ingredients.size() == 1 && back.outputCount == slots
                    && back.ingredients.get(0).contains(recipe.output)) {
                    result.put(input, new Packing(recipe.output, slots));
                    break;
                }
            }
        }
        return result;
    }

    public static String id(ItemStack stack) {
        Object name = GameData.getItemRegistry().getNameForObject(stack.getItem());
        int damage = stack.getItemDamage() == OreDictionary.WILDCARD_VALUE ? 0 : stack.getItemDamage();
        return damage == 0 ? String.valueOf(name) : name + ":" + damage;
    }

    /** The recipes of the running game: the crafting manager's recipes in registration order, then smelting. */
    public static RecipeIndex fromGame() {
        RecipeIndex index = new RecipeIndex();
        for (Object object : CraftingManager.getInstance().getRecipeList()) {
            try {
                IRecipe recipe = (IRecipe) object;
                ItemStack output = recipe.getRecipeOutput();
                if (output == null || output.getItem() == null) continue;
                List<List<String>> slots = new ArrayList<>();
                Type type;
                if (recipe instanceof ShapedRecipes) {
                    type = Type.SHAPED;
                    for (ItemStack stack : ((ShapedRecipes) recipe).recipeItems) slots.add(slot(stack));
                } else if (recipe instanceof ShapelessRecipes) {
                    type = Type.SHAPELESS;
                    for (Object stack : ((ShapelessRecipes) recipe).recipeItems) slots.add(slot(stack));
                } else if (recipe instanceof ShapedOreRecipe) {
                    type = Type.SHAPED;
                    for (Object input : ((ShapedOreRecipe) recipe).getInput()) slots.add(slot(input));
                } else if (recipe instanceof ShapelessOreRecipe) {
                    type = Type.SHAPELESS;
                    for (Object input : ((ShapelessOreRecipe) recipe).getInput()) slots.add(slot(input));
                } else continue;
                index.add(type, id(output), output.stackSize, slots);
            } catch (RuntimeException error) {
                Reference.logger.debug("Skipped a recipe for the raw material list", error);
            }
        }
        for (Object object : FurnaceRecipes.smelting().getSmeltingList().entrySet()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) object;
            if (!(entry.getKey() instanceof ItemStack) || !(entry.getValue() instanceof ItemStack)) continue;
            ItemStack output = (ItemStack) entry.getValue();
            if (output.getItem() == null) continue;
            index.add(Type.FURNACE, id(output), output.stackSize, Collections.singletonList(slot(entry.getKey())));
        }
        return index;
    }

    private static List<String> slot(Object input) {
        List<String> alternatives = new ArrayList<>();
        if (input instanceof ItemStack) {
            if (((ItemStack) input).getItem() != null) alternatives.add(id((ItemStack) input));
        } else if (input instanceof List) {
            for (Object stack : (List<?>) input) {
                if (stack instanceof ItemStack && ((ItemStack) stack).getItem() != null) {
                    String id = id((ItemStack) stack);
                    if (!alternatives.contains(id)) alternatives.add(id);
                }
            }
        }
        return alternatives;
    }
}
