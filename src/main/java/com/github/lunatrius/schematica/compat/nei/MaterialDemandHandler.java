// NEI planning recipes for schematic material demands, by HackerRouter, 2026.
package com.github.lunatrius.schematica.compat.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Gui;
import net.minecraft.item.ItemStack;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.Recipe;
import codechicken.nei.recipe.TemplateRecipeHandler;

/** An informational recipe: one saved plan requires the exact material counts, with no crafting overlay. */
public final class MaterialDemandHandler extends TemplateRecipeHandler {
    public MaterialDemandHandler() {}

    static Recipe recipe(List<ItemStack> stacks) {
        MaterialDemandHandler handler = new MaterialDemandHandler();
        handler.loadCraftingRecipes(MaterialDemand.marker(stacks));
        return Recipe.of(handler, 0);
    }

    @Override public String getRecipeName() { return UiTranslations.format("schematica.nei.demand"); }
    @Override public String getGuiTexture() { return ""; }
    @Override public void drawForeground(int recipe) {}

    @Override public void loadCraftingRecipes(ItemStack result) {
        List<ItemStack> stacks = MaterialDemand.read(result);
        if (!stacks.isEmpty()) arecipes.add(new DemandRecipe(result, stacks));
    }

    @Override public int getRecipeHeight(int recipe) {
        return Math.max(48, 12 + ((getIngredientStacks(recipe).size() - 1 + 7) / 8) * 18);
    }

    @Override public void drawBackground(int recipe) {
        for (PositionedStack stack : getIngredientStacks(recipe)) slot(stack);
        slot(getResultStack(recipe));
    }

    private static void slot(PositionedStack stack) {
        Gui.drawRect(stack.relx - 1, stack.rely - 1, stack.relx + 17, stack.rely + 17, 0xFF555555);
        Gui.drawRect(stack.relx, stack.rely, stack.relx + 16, stack.rely + 16, 0xFFAAAAAA);
    }

    private final class DemandRecipe extends CachedRecipe {
        private final List<PositionedStack> ingredients = new ArrayList<>();
        private final PositionedStack result;

        DemandRecipe(ItemStack marker, List<ItemStack> stacks) {
            ItemStack output = marker.copy();
            output.stackSize = 1;
            result = new PositionedStack(output, 148, 6);
            for (int i = 0; i < stacks.size(); i++) {
                ingredients.add(new PositionedStack(stacks.get(i), 4 + i % 8 * 18, 6 + i / 8 * 18));
            }
            // NEI's recipe identity ignores amounts and shapeless outputs. A zero-cost plan distinguishes snapshots.
            ItemStack identity = marker.copy();
            identity.stackSize = 0;
            ingredients.add(new PositionedStack(identity, 148, 26));
        }

        @Override public PositionedStack getResult() { return result; }
        @Override public List<PositionedStack> getIngredients() { return ingredients; }
    }
}
