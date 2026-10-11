// Native facade item identity regressions, by HackerRouter, 2026.
package com.github.lunatrius.schematica.compat;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Test;
import static org.junit.Assert.*;

import appeng.items.parts.ItemFacade;
import com.github.lunatrius.schematica.client.gui.material.MaterialItemKey;

public class AE2FacadeMaterialTest {
    @Test public void clientAndCraftedFacadesShareMaterialIdentityWithoutChangingStacks() throws Exception {
        ItemFacade item = new ItemFacade();
        ItemStack client = new ItemStack(item, 1, 0);
        client.setTagCompound(new NBTTagCompound());
        client.getTagCompound().setIntArray("x", new int[] {1, 0});
        ItemStack crafted = item.createFacadeForItem(item.getTextureItem(client), false);
        MaterialItemKey key = new MaterialItemKey(client);
        assertEquals(new MaterialItemKey(crafted), key);
        assertEquals(new MaterialItemKey(crafted).hashCode(), key.hashCode());
        assertFalse(client.getTagCompound().hasKey("modid"));
        assertEquals("stone", key.stack().getTagCompound().getString("itemname"));
        AE2ItemsTest.Host wanted = new AE2ItemsTest.Host(), actual = new AE2ItemsTest.Host();
        wanted.facades.faces.put(ForgeDirection.WEST, new AE2ItemsTest.Facade("unused") {
            @Override public ItemStack getItemStack() { return crafted; }
        });
        actual.facades.faces.put(ForgeDirection.WEST, new AE2ItemsTest.Facade("unused") {
            @Override public ItemStack getItemStack() { return client; }
        });
        assertTrue(AE2Items.missing(wanted, actual, false).isEmpty());
        ItemStack glass = client.copy();
        glass.getTagCompound().setIntArray("x", new int[] {20, 0});
        assertNotEquals(key, new MaterialItemKey(glass));
        ItemStack named = client.copy();
        named.getTagCompound().setString("display", "custom label");
        assertNotEquals(key, new MaterialItemKey(named));
    }
}
