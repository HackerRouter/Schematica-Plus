// Minimal native facade item fixture, by HackerRouter, 2026.
package appeng.items.parts;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ItemFacade extends Item {
    private final Item texture = new Item();

    public ItemStack getTextureItem(ItemStack facade) {
        return new ItemStack(texture, 1, facade.getTagCompound().getIntArray("x")[0]);
    }

    public ItemStack createFacadeForItem(ItemStack material, boolean returnItem) {
        if (returnItem) return material;
        ItemStack result = new ItemStack(this, 1, 0);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setIntArray("x", new int[] {material.getItemDamage(), 0});
        tag.setString("modid", "minecraft");
        tag.setString("itemname", material.getItemDamage() == 1 ? "stone" : "glass");
        result.setTagCompound(tag);
        return result;
    }
}
