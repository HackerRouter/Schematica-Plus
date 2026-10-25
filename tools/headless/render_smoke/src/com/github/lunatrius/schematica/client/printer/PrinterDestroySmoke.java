package com.github.lunatrius.schematica.client.printer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.world.storage.Schematic;

public final class PrinterDestroySmoke {
    public static void run(Minecraft mc, int x, int y, int z) throws Exception {
        boolean destroy = ConfigurationHandler.destroyBlocks, extra = ConfigurationHandler.printBreakExtraBlock;
        boolean wrong = ConfigurationHandler.printBreakWrongBlock, state = ConfigurationHandler.printBreakWrongStateBlock;
        boolean autoTool = ConfigurationHandler.printAutoTool;
        int placements = SchematicaPlus.proxy.printerMaxPlacementsPerTick, breaks = SchematicaPlus.proxy.printerMaxBreaksPerTick;
        WorldSettings.GameType mode = mc.playerController.isInCreativeMode() ? WorldSettings.GameType.CREATIVE : WorldSettings.GameType.SURVIVAL;
        int slot = mc.thePlayer.inventory.currentItem;
        ItemStack item = mc.thePlayer.inventory.mainInventory[slot];
        SchematicWorld schematic = new SchematicWorld(new Schematic(null, 1, 1, 1));
        schematic.position.set(x, y, z);
        SchematicPrinter printer = SchematicPrinter.INSTANCE;
        Method start = SchematicPrinter.class.getDeclaredMethod("startBreak", World.class, SchematicWorld.class, int.class, int.class, int.class);
        start.setAccessible(true);
        Field field = SchematicPrinter.class.getDeclaredField("miner"); field.setAccessible(true);
        BlockMiner miner = (BlockMiner) field.get(printer);
        try {
            ConfigurationHandler.printBreakExtraBlock = false;
            ConfigurationHandler.printBreakWrongBlock = false;
            ConfigurationHandler.printBreakWrongStateBlock = false;
            ConfigurationHandler.printAutoTool = false;
            mc.thePlayer.inventory.mainInventory[slot] = new ItemStack(Items.iron_pickaxe);
            mc.playerController.setGameType(WorldSettings.GameType.SURVIVAL);
            for (int[] limits : new int[][] {{0, 0}, {64, 0}, {64, 64}}) {
                SchematicaPlus.proxy.printerMaxPlacementsPerTick = limits[0];
                SchematicaPlus.proxy.printerMaxBreaksPerTick = limits[1];
                mc.theWorld.setBlock(x, y, z, Blocks.stone, 0, 0);
                ConfigurationHandler.destroyBlocks = false;
                check(!(Boolean) start.invoke(printer, mc.theWorld, schematic, x, y, z), "disabled destruction leaves the target alone");
                ConfigurationHandler.destroyBlocks = true;
                check((Boolean) start.invoke(printer, mc.theWorld, schematic, x, y, z), "survival destroyBlocks starts without individual break toggles");
                check(miner.active() == (limits[1] == 0), "normal survival mining retains progress unless fast breaking is supported");
                printer.stopMining();
                mc.theWorld.setBlock(x, y, z, Blocks.bedrock, 0, 0);
                check(!(Boolean) start.invoke(printer, mc.theWorld, schematic, x, y, z), "survival skips unbreakable blocks");
                mc.theWorld.setBlock(x, y, z, Blocks.water, 0, 0);
                check(!(Boolean) start.invoke(printer, mc.theWorld, schematic, x, y, z), "survival skips fluids");
            }
            mc.playerController.setGameType(WorldSettings.GameType.CREATIVE);
            mc.theWorld.setBlock(x, y, z, Blocks.bedrock, 0, 0);
            check((Boolean) start.invoke(printer, mc.theWorld, schematic, x, y, z), "creative assisted destruction retains bedrock handling");
            check(mc.thePlayer.getCurrentEquippedItem().getItemDamage() == 0, "creative client does not consume durability");
            SchematicaPlus.proxy.printerMaxPlacementsPerTick = 0;
            check(!(Boolean) start.invoke(printer, mc.theWorld, schematic, x, y, z), "unassisted creative retains its existing placement-pass path");
            System.out.println("PRINTER_DESTROY_SMOKE_PASSED: survival toggle, normal/assisted/fast mining, unbreakable/fluid exclusions, creative path retained");
        } finally {
            printer.stopMining();
            mc.theWorld.setBlockToAir(x, y, z);
            mc.playerController.setGameType(mode);
            mc.thePlayer.inventory.currentItem = slot;
            mc.thePlayer.inventory.mainInventory[slot] = item;
            ConfigurationHandler.destroyBlocks = destroy;
            ConfigurationHandler.printBreakExtraBlock = extra;
            ConfigurationHandler.printBreakWrongBlock = wrong;
            ConfigurationHandler.printBreakWrongStateBlock = state;
            ConfigurationHandler.printAutoTool = autoTool;
            SchematicaPlus.proxy.printerMaxPlacementsPerTick = placements;
            SchematicaPlus.proxy.printerMaxBreaksPerTick = breaks;
        }
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
