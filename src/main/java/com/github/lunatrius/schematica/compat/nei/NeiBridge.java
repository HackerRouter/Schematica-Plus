package com.github.lunatrius.schematica.compat.nei;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.common.Loader;

/** The only way into the NEI code: NeiIntegration is loaded only when GTNH NEI with bookmark groups is present. */
public final class NeiBridge {
    private static Boolean available;

    private NeiBridge() {}

    public static boolean available() {
        if (available == null) {
            boolean present = Loader.isModLoaded("NotEnoughItems");
            try {
                present = present && Class.forName("codechicken.nei.bookmark.BookmarkGrid", false, NeiBridge.class.getClassLoader()) != null
                    && Class.forName("codechicken.nei.recipe.GuiRecipeButton$UpdateRecipeButtonsEvent$Post", false, NeiBridge.class.getClassLoader()) != null;
            } catch (ClassNotFoundException | LinkageError e) {
                present = false;
            }
            available = present;
        }
        return available;
    }

    public static void register() {
        if (!available()) return;
        try {
            MinecraftForge.EVENT_BUS.register(NeiIntegration.INSTANCE);
        } catch (LinkageError | RuntimeException error) {
            available = false;
            Reference.logger.warn("This NEI version does not offer bookmark groups to Schematica Plus", error);
        }
    }

    /** A new NEI bookmark group in crafting chain mode with these stacks (stack size = amount); the number of items or -1. */
    public static int sendGroup(List<ItemStack> stacks) {
        if (!available()) return -1;
        try {
            return NeiIntegration.sendGroup(stacks);
        } catch (LinkageError | RuntimeException error) {
            Reference.logger.warn("Could not create the NEI bookmark group", error);
            return -1;
        }
    }
}
