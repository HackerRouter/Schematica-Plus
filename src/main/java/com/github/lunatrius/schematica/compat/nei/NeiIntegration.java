// NEI (GTNH NotEnoughItems 2.8.44 and later) bookmark group and recipe button integration, by HackerRouter, 2026.
package com.github.lunatrius.schematica.compat.nei;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

import com.github.lunatrius.schematica.client.gui.framework.UiTranslations;
import com.github.lunatrius.schematica.reference.Reference;

import codechicken.nei.BookmarkPanel;
import codechicken.nei.ItemPanels;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.bookmark.BookmarkGrid;
import codechicken.nei.bookmark.BookmarkGroup;
import codechicken.nei.api.API;
import codechicken.nei.recipe.GuiRecipeButton;
import codechicken.nei.recipe.Recipe;
import codechicken.nei.recipe.RecipeHandlerRef;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Sends a material list to NEI as a bookmark group in crafting chain mode (NEI then adds up the ingredients of the
 * recipes put into it, and in GTNH 2.9 shows its crafting tree) and adds an "S" button to every recipe on NEI's
 * recipe pages that puts that recipe into the last sent group, instead of bookmarking it and dragging it over.
 */
public final class NeiIntegration {
    static final NeiIntegration INSTANCE = new NeiIntegration();
    private static int group = -1;
    private static BookmarkGrid sentGrid;
    private static BookmarkGroup sentGroup;

    private NeiIntegration() {}

    private static BookmarkGrid grid() {
        ItemPanels.bookmarkPanel.update();
        return (BookmarkGrid) ItemPanels.bookmarkPanel.getGrid();
    }

    static void register() {
        API.registerRecipeHandler(new MaterialDemandHandler());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    static void clear() {
        group = -1;
        sentGrid = null;
        sentGroup = null;
    }

    static int sendGroup(List<ItemStack> stacks) {
        List<ItemStack> demands = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack != null && stack.getItem() != null && stack.stackSize > 0) demands.add(stack.copy());
        }
        if (demands.isEmpty()) return 0;
        if (!bookmarksLoaded()) return -2;
        BookmarkGrid grid = grid();
        BookmarkGroup target = new BookmarkGroup(BookmarkPanel.BookmarkViewMode.DEFAULT, true);
        int id = grid.addGroup(target);
        try {
            grid.addRecipe(MaterialDemandHandler.recipe(demands), 1, id);
            ItemPanels.bookmarkPanel.save();
        } catch (RuntimeException | LinkageError error) {
            grid.removeGroup(id);
            throw error;
        }
        group = id;
        sentGrid = grid;
        sentGroup = target;
        return demands.size();
    }

    private static boolean groupExists() {
        return group >= 0 && grid() == sentGrid && sentGrid.getGroup(group) == sentGroup;
    }

    private static boolean bookmarksLoaded() {
        try {
            Field storageField = BookmarkPanel.class.getDeclaredField("storage");
            storageField.setAccessible(true);
            Object storage = storageField.get(ItemPanels.bookmarkPanel);
            Field fileField = storage.getClass().getDeclaredField("bookmarkFile");
            fileField.setAccessible(true);
            File loaded = (File) fileField.get(storage);
            String world = NEIClientConfig.getBooleanSetting("inventory.bookmarks.worldSpecific")
                ? NEIClientConfig.getWorldPath() : "global";
            File expected = new File(Minecraft.getMinecraft().mcDataDir, "saves/NEI/" + world + "/bookmarks.ini");
            // NEI assigns this file only after its background loader has installed the bookmark grids.
            return loaded != null && loaded.getCanonicalFile().equals(expected.getCanonicalFile());
        } catch (ReflectiveOperationException | IOException error) {
            throw new IllegalStateException("Could not determine whether NEI bookmarks finished loading", error);
        }
    }

    static void addRecipe(RecipeHandlerRef ref) {
        Minecraft mc = Minecraft.getMinecraft();
        String message;
        try {
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
        } catch (RuntimeException | LinkageError error) {
            Reference.logger.warn("Could not add a recipe to the NEI material group", error);
            message = UiTranslations.format("schematica.nei.send_failed");
        }
        if (mc.ingameGUI != null) mc.ingameGUI.func_110326_a(message, false);
    }

    /** One more button above NEI's own buttons of each recipe, while a sent group exists. */
    @SubscribeEvent
    public void onRecipeButtons(GuiRecipeButton.UpdateRecipeButtonsEvent.Post event) {
        try {
            if (!groupExists() || event.buttonList.isEmpty() || event.buttonList.get(0).handlerRef.handler instanceof MaterialDemandHandler) return;
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
