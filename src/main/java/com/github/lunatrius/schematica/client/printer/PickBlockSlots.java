// SPDX-License-Identifier: LGPL-3.0-only
// Litematica InventoryUtils pick block slots (pickBlockableSlots, pickBlockAvoid*), adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.printer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;

import com.github.lunatrius.schematica.handler.ConfigurationHandler;

/** Moves a picked item into the hand: its hotbar slot, else one of the pick-blockable hotbar slots. */
public final class PickBlockSlots {
    private static List<Integer> slots = Collections.emptyList();
    private static String parsedFrom;
    private static int next;

    private PickBlockSlots() {}

    /** setPickBlockableSlots: the 1-based hotbar slots of the config, without duplicates. */
    static List<Integer> parse(String config) {
        List<Integer> parsed = new ArrayList<>();
        for (String part : config.split(",")) {
            try {
                int slot = Integer.parseInt(part.trim()) - 1;
                if (slot >= 0 && slot < 9 && !parsed.contains(slot)) parsed.add(slot);
            } catch (NumberFormatException ignored) {}
        }
        return Collections.unmodifiableList(parsed);
    }

    public static List<Integer> slots() {
        String config = ConfigurationHandler.pickBlockableSlots;
        if (!config.equals(parsedFrom)) {
            slots = parse(config);
            parsedFrom = config;
            next = 0;
        }
        return slots;
    }

    static boolean isTool(ItemStack stack) {
        return stack.getItem() instanceof ItemTool || stack.getItem() instanceof ItemHoe || stack.getItem() instanceof ItemSword;
    }

    static boolean canPickToSlot(InventoryPlayer inventory, int slot) {
        if (!slots().contains(slot)) return false;
        ItemStack stack = inventory.getStackInSlot(slot);
        if (stack == null || stack.getItem() == null) return true;
        return (!ConfigurationHandler.pickBlockAvoidDamageable || !stack.isItemStackDamageable())
            && (!ConfigurationHandler.pickBlockAvoidTools || !isTool(stack));
    }

    static int emptySlot(InventoryPlayer inventory) {
        for (int slot : slots()) if (inventory.getStackInSlot(slot) == null) return slot;
        return -1;
    }

    /** getPickBlockTargetSlot: the selected slot when allowed, else the next allowed one in turn. */
    static int targetSlot(InventoryPlayer inventory) {
        if (slots().isEmpty()) return -1;
        if (canPickToSlot(inventory, inventory.currentItem)) return inventory.currentItem;
        for (int i = 0; i < slots.size(); i++) {
            if (next >= slots.size()) next = 0;
            int slot = slots.get(next++);
            if (canPickToSlot(inventory, slot)) return slot;
        }
        return -1;
    }

    static int find(InventoryPlayer inventory, ItemStack stack, boolean matchNbt) {
        for (int i = 0; i < inventory.mainInventory.length; i++) {
            ItemStack slot = inventory.mainInventory[i];
            if (slot != null && slot.isItemEqual(stack) && (!matchNbt || ItemStack.areItemStackTagsEqual(slot, stack))) return i;
        }
        return -1;
    }

    /**
     * The slot of a container that shows the inventory index, or -1. Offhand mods such as Backhand keep the offhand
     * past the vanilla 36 indices, in a container slot of its own, so the index is not always the slot number.
     */
    static int containerSlot(Container container, IInventory inventory, int index) {
        for (int i = 0; i < container.inventorySlots.size(); i++) {
            Slot slot = (Slot) container.inventorySlots.get(i);
            if (slot.inventory == inventory && slot.getSlotIndex() == index) return i;
        }
        return -1;
    }

    /** Swaps the stack at an inventory index with a hotbar slot through the player's own container. */
    public static boolean swapToHotbar(Minecraft mc, EntityPlayer player, int index, int hotbar) {
        int slot = containerSlot(player.inventoryContainer, player.inventory, index);
        if (slot < 0) return false;
        mc.playerController.windowClick(player.inventoryContainer.windowId, slot, hotbar, 2, player);
        return true;
    }

    /** setPickedItemToHand; returns whether the item is in the hand now. */
    public static boolean pickToHand(Minecraft mc, ItemStack stack, boolean matchNbt) {
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null || stack == null || stack.getItem() == null) return false;
        InventoryPlayer inventory = player.inventory;
        boolean creative = mc.playerController.isInCreativeMode();
        int source = find(inventory, stack, matchNbt);
        if (source >= 0 && source < 9) {
            inventory.currentItem = source;
            return true;
        }
        if (source < 0 && !creative) return false;
        if (slots().isEmpty()) {
            EasyPlace.warn("litematica.message.warn.pickblock.no_valid_slots_configured");
            return false;
        }
        int hotbar = emptySlot(inventory);
        if (hotbar == -1) hotbar = targetSlot(inventory);
        if (hotbar == -1) {
            EasyPlace.warn("litematica.message.warn.pickblock.no_suitable_slot_found");
            return false;
        }
        if (!creative && !swapToHotbar(mc, player, source, hotbar)) return false;
        inventory.currentItem = hotbar;
        if (creative) {
            ItemStack copy = stack.copy();
            copy.stackSize = Math.max(1, copy.stackSize);
            inventory.setInventorySlotContents(hotbar, copy);
            mc.playerController.sendSlotPacket(copy, 36 + hotbar);
        }
        return true;
    }

}
