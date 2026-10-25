package com.github.lunatrius.schematica.client.printer;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import com.github.lunatrius.schematica.client.world.Placements;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.PrinterActions;
import com.github.lunatrius.schematica.world.storage.Schematic;

public final class PrinterBatchSmoke {
    private final ConcurrentLinkedQueue<FutureTask<?>> tasks = new ConcurrentLinkedQueue<>();
    private final List<int[]> harvested = new ArrayList<>();
    private final Minecraft mc;
    private final int x, y, z;
    private EntityPlayerMP player;

    private PrinterBatchSmoke(Minecraft mc, int x, int y, int z) { this.mc = mc; this.x = x; this.y = y; this.z = z; }

    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        FutureTask<?> task = tasks.poll();
        if (task != null) task.run();
    }

    @SubscribeEvent public void harvest(BlockEvent.BreakEvent event) {
        if (event.x >= x && event.x < x + 4 && event.y >= y && event.y < y + 4 && event.z >= z && event.z < z + 4) {
            ItemStack held = player.getCurrentEquippedItem();
            harvested.add(new int[] {event.y, held == null ? 0 : Item.getIdFromItem(held.getItem())});
        }
    }

    private <T> T server(Callable<T> call) throws Exception {
        FutureTask<T> task = new FutureTask<>(call); tasks.add(task);
        return task.get(5, TimeUnit.SECONDS);
    }

    public static void run(Minecraft mc, int x, int y, int z) throws Exception {
        new PrinterBatchSmoke(mc, x, y, z).run();
    }

    private void run() throws Exception {
        check(PrinterReach.fastBreaking(), "integrated-server handshake enables fast breaking");
        Map<Field, Object> settings = new HashMap<>();
        for (Field field : ConfigurationHandler.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())
                && (field.getType().isPrimitive() || field.getType() == String.class)) settings.put(field, field.get(null));
        }
        ItemStack[] inventory = mc.thePlayer.inventory.mainInventory.clone();
        int slot = mc.thePlayer.inventory.currentItem;
        String name = mc.thePlayer.getCommandSenderName();
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
        try {
            server(() -> { player = MinecraftServer.getServer().getConfigurationManager().func_152612_a(name); return null; });
            ConfigurationHandler.destroyBlocks = true;
            ConfigurationHandler.destroyInstantly = true;
            ConfigurationHandler.printBreakExtraBlock = true;
            ConfigurationHandler.printBreakWrongBlock = true;
            ConfigurationHandler.printBreakWrongStateBlock = true;
            ConfigurationHandler.printAutoTool = true;
            ConfigurationHandler.printerWorkRange = 128;
            ConfigurationHandler.printerIterationTimeLimit = 0;
            ConfigurationHandler.breakBlocksPerTick = 64;
            ConfigurationHandler.printerLagCheck = false;
            ConfigurationHandler.printerPauseWhileMoving = false;
            ConfigurationHandler.printSelectionType = "render_layers";
            ConfigurationHandler.printerBuildOrder = "layers";
            ConfigurationHandler.printerYAxisReverse = false;
            mc.playerController.setGameType(WorldSettings.GameType.SURVIVAL);
            scenario(Blocks.stone, Blocks.air);
            scenario(Blocks.stone, Blocks.cobblestone);
            scenario(Blocks.sand, Blocks.air);
            scenario(Blocks.glass, Blocks.air);
            System.out.println("PRINTER_BATCH_SMOKE_PASSED: real singleplayer scan, packets and harvest for 64 extra stone, wrong stone, extra sand and extra glass targets");
        } finally {
            SchematicPrinter.INSTANCE.refresh();
            if (player != null) server(() -> { PrinterActions.INSTANCE.forget(player); player.theItemInWorldManager.setGameType(WorldSettings.GameType.CREATIVE); return null; });
            mc.playerController.setGameType(WorldSettings.GameType.CREATIVE);
            mc.thePlayer.inventory.mainInventory = inventory;
            mc.thePlayer.inventory.currentItem = slot;
            for (Map.Entry<Field, Object> setting : settings.entrySet()) setting.getKey().set(null, setting.getValue());
            MinecraftForge.EVENT_BUS.unregister(this);
            FMLCommonHandler.instance().bus().unregister(this);
        }
    }

    private void scenario(Block real, Block expected) throws Exception {
        server(() -> {
            player.theItemInWorldManager.setGameType(WorldSettings.GameType.SURVIVAL);
            Arrays.fill(player.inventory.mainInventory, null);
            player.inventory.currentItem = 0;
            player.inventory.mainInventory[1] = new ItemStack(Items.iron_pickaxe);
            player.inventory.mainInventory[2] = new ItemStack(Items.iron_shovel);
            fill(player.worldObj, real);
            harvested.clear();
            return null;
        });
        Arrays.fill(mc.thePlayer.inventory.mainInventory, null);
        mc.thePlayer.inventory.currentItem = 0;
        mc.thePlayer.inventory.mainInventory[1] = new ItemStack(Items.iron_pickaxe);
        mc.thePlayer.inventory.mainInventory[2] = new ItemStack(Items.iron_shovel);
        fill(mc.theWorld, real);
        Schematic source = new Schematic(null, 4, 4, 4);
        for (int sy = 0; sy < 4; sy++) for (int sz = 0; sz < 4; sz++) for (int sx = 0; sx < 4; sx++) source.setBlock(sx, sy, sz, expected, 0);
        SchematicWorld placement = new SchematicWorld(source);
        placement.name = "Printer Batch Smoke"; placement.position.set(x, y, z); placement.isRendering = true;
        Placements.LIBRARY.create(Placements.LIBRARY.addMemory(new File("printer-batch-smoke.schemplus"), null), (data, previous) -> placement);
        try {
            SchematicPrinter.INSTANCE.refresh();
            SchematicPrinter.INSTANCE.print();
            int remaining = 64;
            for (int wait = 0; wait < 40 && remaining != 0; wait++) remaining = server(() -> {
                int count = 0;
                for (int sy = 0; sy < 4; sy++) for (int sz = 0; sz < 4; sz++) for (int sx = 0; sx < 4; sx++) {
                    if (!player.worldObj.isAirBlock(x + sx, y + sy, z + sz)) count++;
                }
                return count;
            });
            int count = server(() -> harvested.size());
            System.out.println("PRINTER_BATCH_RESULT: real=" + Block.blockRegistry.getNameForObject(real)
                + ", expected=" + Block.blockRegistry.getNameForObject(expected) + ", remaining=" + remaining + ", harvested=" + count);
            check(remaining == 0 && count == 64, "all scanned targets reach native harvesting");
            server(() -> {
                int toolSlot = real == Blocks.sand ? 2 : 1;
                check(player.inventory.mainInventory[toolSlot].getItemDamage() == 64, "best tool supplies all 64 harvests");
                return null;
            });
        } finally { Placements.LIBRARY.remove(placement); }
    }

    private void fill(World world, Block block) {
        for (int sz = 0; sz < 4; sz++) for (int sx = 0; sx < 4; sx++) {
            world.setBlock(x + sx, y - 1, z + sz, Blocks.stone, 0, 2);
            for (int sy = 0; sy < 4; sy++) world.setBlock(x + sx, y + sy, z + sz, block, 0, 2);
        }
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
