// NEI (GTNH NotEnoughItems 2.8.44 and later) bookmark group and recipe button integration, by HackerRouter, 2026.
package com.github.lunatrius.schematica.compat.nei;

import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.reference.Reference;

import codechicken.nei.BookmarkPanel;
import codechicken.nei.ItemPanels;
import codechicken.nei.bookmark.BookmarkGrid;
import codechicken.nei.bookmark.BookmarkGroup;
import codechicken.nei.bookmark.BookmarkItem;
import codechicken.nei.recipe.GuiRecipeButton;
import codechicken.nei.recipe.Recipe;
import codechicken.nei.recipe.RecipeHandlerRef;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Sends a material list to NEI as a bookmark group in crafting chain mode (NEI then adds up the ingredients of the
 * recipes put into it, and in GTNH 2.9 shows its crafting tree) and adds an "S" button to every recipe on NEI's
 * recipe pages that puts that recipe into the last sent group, instead of bookmarking it and dragging it over.
 */
final class NeiIntegration {
    static final NeiIntegration INSTANCE = new NeiIntegration();
    private static int group = -1;

    private NeiIntegration() {}

    private static BookmarkGrid grid() { return (BookmarkGrid) ItemPanels.bookmarkPanel.getGrid(); }

    static int sendGroup(List<ItemStack> stacks) {
        BookmarkGrid grid = grid();
        int id = grid.addGroup(new BookmarkGroup(BookmarkPanel.BookmarkViewMode.DEFAULT, true));
        int count = 0;
        for (ItemStack stack : stacks) {
            if (stack == null || stack.getItem() == null || stack.stackSize <= 0) continue;
            grid.addItem(BookmarkItem.of(id, stack.copy()), false);
            count++;
        }
        group = id;
        ItemPanels.bookmarkPanel.save();
        return count;
    }

    private static boolean groupExists() {
        try {
            return group >= 0 && grid().getGroup(group) != null;
        } catch (RuntimeException error) {
            return false;
        }
    }

    static void addRecipe(RecipeHandlerRef ref) {
        Minecraft mc = Minecraft.getMinecraft();
        String message;
        if (!groupExists()) {
            message = UiTranslations.format("schematica.nei.no_group");
        } else {
            Recipe recipe = Recipe.of(ref);
            boolean added = recipe != null && ItemPanels.bookmarkPanel.addRecipe(recipe, 1, group);
            if (added) ItemPanels.bookmarkPanel.save();
            ItemStack result = recipe == null ? null : recipe.getResult();
            String name = result == null ? "?" : result.getDisplayName();
            message = UiTranslations.format(added ? "schematica.nei.recipe_added" : "schematica.nei.recipe_present", name);
        }
        if (mc.ingameGUI != null) mc.ingameGUI.func_110326_a(message, false);
    }

    /** One more button above NEI's own buttons of each recipe, while a sent group exists. */
    @SubscribeEvent
    public void onRecipeButtons(GuiRecipeButton.UpdateRecipeButtonsEvent.Post event) {
        try {
            if (!groupExists() || event.buttonList.isEmpty()) return;
            GuiRecipeButton first = event.buttonList.get(0);
            int top = first.yPosition;
            for (GuiRecipeButton button : event.buttonList) top = Math.min(top, button.yPosition);
            event.buttonList.add(new GroupButton(first.handlerRef, first.xPosition, top - GuiRecipeButton.BUTTON_HEIGHT - 1));
        } catch (LinkageError | RuntimeException error) {
            Reference.logger.debug("Could not add the Schematica group button to an NEI recipe", error);
        }
    }

    private static final class GroupButton extends GuiRecipeButton {
        GroupButton(RecipeHandlerRef ref, int x, int y) { super(ref, x, y, 0x5C4E + ref.recipeIndex, "S"); }

        @Override public List<String> handleTooltip(List<String> tip) {
            tip.add(UiTranslations.format("schematica.nei.add_recipe"));
            return tip;
        }

        @Override public Map<String, String> handleHotkeys(int mouseX, int mouseY, Map<String, String> hotkeys) { return hotkeys; }

        @Override public void mouseReleased(int mouseX, int mouseY) { addRecipe(this.handlerRef); }

        @Override public void lastKeyTyped(char keyChar, int keyID) {}

        @Override public void drawItemOverlay() {}
    }
}
