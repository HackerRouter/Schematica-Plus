// Blocks built with a tool instead of an item, after the behavior of Buildprint's assist tools, by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.printer;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemFlintAndSteel;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.github.lunatrius.schematica.handler.ConfigurationHandler;

/**
 * printUseTools: farmland is tilled from the dirt or grass already there with a hoe, and a nether portal is lit with
 * flint and steel on the obsidian below its lowest blocks (a portal has no item). Farmland on nothing gets its dirt
 * placed first as usual.
 */
public final class ToolUse {
    private ToolUse() {}

    /** The click on the world block that builds the schematic block with a tool: {x, y, z, side}; null when none applies. */
    public static int[] click(World world, Block block, Block real, int x, int y, int z) {
        if (!ConfigurationHandler.printUseTools) return null;
        if (block == Blocks.farmland && tillable(world, real, x, y, z)) return new int[] {x, y, z, 1};
        if (block == Blocks.portal && real.isAir(world, x, y, z) && world.getBlock(x, y - 1, z) == Blocks.obsidian) return new int[] {x, y - 1, z, 1};
        return null;
    }

    /** Dirt or grass with room above, which a hoe turns into farmland (ItemHoe.onItemUse). */
    public static boolean tillable(World world, Block real, int x, int y, int z) {
        return (real == Blocks.grass || real == Blocks.dirt && world.getBlockMetadata(x, y, z) == 0) && world.getBlock(x, y + 1, z).isAir(world, x, y + 1, z);
    }

    /** Whether a mismatch is one that a tool fixes, so that the world block is not mined. */
    public static boolean fixable(World world, Block block, Block real, int x, int y, int z) {
        return ConfigurationHandler.printUseTools && block == Blocks.farmland && tillable(world, real, x, y, z);
    }

    static boolean accepts(Block block, ItemStack stack) {
        if (stack == null) return false;
        return block == Blocks.farmland ? stack.getItem() instanceof ItemHoe : stack.getItem() instanceof ItemFlintAndSteel;
    }

    /** The item recorded as missing when no tool is carried. */
    static ItemStack missing(Block block) { return new ItemStack(block == Blocks.farmland ? Items.iron_hoe : Items.flint_and_steel); }

    /** Holds the tool and uses it; false when there is none or the click failed. */
    static boolean use(SchematicPrinter printer, World world, EntityPlayer player, Block block, int[] click) {
        if (!printer.swapToMatching(player.inventory, stack -> accepts(block, stack))) {
            PrinterMissingMaterials.record(missing(block));
            return false;
        }
        ItemStack held = player.getCurrentEquippedItem();
        if (!accepts(block, held)) return false;
        return printer.placeBlock(world, player, held, click[0], click[1], click[2], click[3],
            Vec3.createVectorHelper(click[0] + 0.5, click[1] + 1, click[2] + 0.5));
    }
}
