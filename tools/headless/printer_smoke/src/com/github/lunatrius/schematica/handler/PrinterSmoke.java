package com.github.lunatrius.schematica.handler;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.*;
import net.minecraft.init.*;
import net.minecraft.item.*;
import net.minecraft.network.*;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import com.github.lunatrius.schematica.network.message.*;

@Mod(modid="printer_smoke", name="Printer Smoke", version="1", dependencies="required-after:schematica_plus", acceptableRemoteVersions="*")
public class PrinterSmoke {
    private static final int BASE = 256, Y = 150;
    private WorldServer world;
    private EntityPlayerMP player;
    private RecordingNet network;
    private final Protection protection = new Protection();
    private final Item nbtTool = new ItemPickaxe(Item.ToolMaterial.IRON) {
        @Override public boolean onBlockDestroyed(ItemStack stack, World world, Block block, int x, int y, int z, EntityLivingBase entity) {
            if (stack.getTagCompound() == null) stack.setTagCompound(new NBTTagCompound());
            stack.getTagCompound().setInteger("uses", stack.getTagCompound().getInteger("uses") + 1);
            return super.onBlockDestroyed(stack, world, block, x, y, z, entity);
        }
    };

    @Mod.EventHandler public void prepare(FMLPreInitializationEvent event) {
        GameRegistry.registerItem(nbtTool, "nbt_pickaxe");
    }

    @Mod.EventHandler public void started(FMLServerStartedEvent event) {
        try {
            MinecraftServer server = MinecraftServer.getServer();
            world = server.worldServerForDimension(0);
            for (int x = BASE - 16; x <= BASE + 144; x += 16) world.getChunkFromBlockCoords(x, 0);
            player = FakePlayerFactory.getMinecraft(world);
            player.setPosition(BASE + .5, Y, .5);
            player.theItemInWorldManager.setGameType(WorldSettings.GameType.SURVIVAL);
            network = new RecordingNet(server, player);
            player.playerNetServerHandler = network;
            Field moved = NetHandlerPlayServer.class.getDeclaredField("hasMoved"); moved.setAccessible(true); moved.setBoolean(network, true);
            ConfigurationHandler.printerEnabled = true;
            ConfigurationHandler.printerMaxRange = 128;
            ConfigurationHandler.printerMaxPlacementsPerTick = 64;
            ConfigurationHandler.printerMaxBreaksPerTick = 64;
            MinecraftForge.EVENT_BUS.register(protection);
            breaks();
            nbtTools();
            protections();
            placements();
            slabs();
            limits();
            System.out.println("PRINTER_SMOKE_PASSED: native harvest, durability, drops, Forge cancellation, ordered placement, orientation, stale targets, quotas");
            Files.write(Paths.get("printer-smoke-result.txt"), "PASSED".getBytes(StandardCharsets.UTF_8));
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.out.println("PRINTER_SMOKE_FAILED");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(protection);
            if (player != null) PrinterActions.INSTANCE.forget(player);
            MinecraftServer.getServer().initiateShutdown();
        }
    }

    private void tick() { PrinterActions.INSTANCE.tick(new TickEvent.ServerTickEvent(TickEvent.Phase.END)); }
    private void check(boolean value, String text) { if (!value) throw new AssertionError(text); }
    private MessagePrinterAction action(int action, int x, ItemStack item) {
        MessagePrinterAction m = new MessagePrinterAction();
        m.action = action; m.x = BASE + x; m.y = Y; m.z = 0; m.side = 1; m.slot = 0;
        m.hitX = .5f; m.hitY = .5f; m.hitZ = .5f; m.expectedItem = item.copy(); m.expectedItem.stackSize = 1;
        m.snapshot(world, m.x, Y, 0);
        return m;
    }

    private void breaks() {
        int previousDrops = droppedStone();
        ItemStack tool = new ItemStack(Items.iron_pickaxe);
        player.inventory.mainInventory[0] = tool;
        for (int x = 1; x <= 64; x++) {
            world.setBlock(BASE + x, Y, 0, Blocks.stone);
            PrinterActions.INSTANCE.enqueue(player, action(MessagePrinterAction.BREAK, x, tool));
        }
        tick();
        for (int x = 1; x <= 64; x++) check(world.isAirBlock(BASE + x, Y, 0), "break " + x);
        check(tool.getItemDamage() == 64, "native tool wear " + tool.getItemDamage());
        check(droppedStone() - previousDrops == 64, "cobblestone drops " + (droppedStone() - previousDrops));
        tool.setItemDamage(tool.getMaxDamage() - 12);
        for (int x = 1; x <= 5; x++) {
            world.setBlock(BASE + x, Y, 0, Blocks.stone);
            MessagePrinterAction m = action(MessagePrinterAction.BREAK, x, tool); m.minimumDurability = 10;
            PrinterActions.INSTANCE.enqueue(player, m);
        }
        tick();
        check(tool.getItemDamage() == tool.getMaxDamage() - 10, "durability reserve");
        check(world.isAirBlock(BASE+2,Y,0) && world.getBlock(BASE+3,Y,0)==Blocks.stone, "reserve stops later requests");
    }

    private int droppedStone() {
        int drops = 0;
        for (Object object : world.getEntitiesWithinAABB(EntityItem.class, AxisAlignedBB.getBoundingBox(BASE, Y-2, -2, BASE+66, Y+3, 3))) {
            EntityItem entity = (EntityItem) object;
            if (entity.getEntityItem().getItem() == Item.getItemFromBlock(Blocks.cobblestone)) drops += entity.getEntityItem().stackSize;
        }
        return drops;
    }

    private void nbtTools() {
        ItemStack tool = new ItemStack(nbtTool);
        ItemStack other = new ItemStack(nbtTool);
        other.setTagCompound(new NBTTagCompound());
        other.getTagCompound().setString("owner", "other");
        player.inventory.mainInventory[0] = tool;
        player.inventory.mainInventory[1] = other;
        for (int x = 1; x <= 64; x++) {
            world.setBlock(BASE+x,Y,0,Blocks.stone);
            PrinterActions.INSTANCE.enqueue(player, action(MessagePrinterAction.BREAK,x,tool));
        }
        tick();
        for (int x = 1; x <= 64; x++) check(world.isAirBlock(BASE+x,Y,0), "NBT tool break " + x);
        check(tool.getItemDamage() == 64 && tool.getTagCompound().getInteger("uses") == 64, "NBT tool updates");
        check(other.getItemDamage() == 0 && !other.getTagCompound().hasKey("uses"), "different NBT tool remains unused");
    }

    private void protections() {
        ItemStack tool = new ItemStack(Items.iron_pickaxe); player.inventory.mainInventory[0] = tool;
        world.setBlock(BASE+1,Y,0,Blocks.stone);
        MessagePrinterAction m = action(MessagePrinterAction.BREAK,1,tool);
        protection.interact = true; check(!PrinterActions.harvest(player,m),"interaction cancellation");
        protection.interact = false; protection.breaking = true; check(!PrinterActions.harvest(player,m),"break cancellation");
        protection.breaking = false;
        check(tool.getItemDamage()==0 && world.getBlock(BASE+1,Y,0)==Blocks.stone,"cancelled block unchanged");
        world.setBlock(BASE+1,Y,0,Blocks.bedrock); m.snapshot(world,BASE+1,Y,0);
        check(!PrinterActions.harvest(player,m),"bedrock remains unbreakable");
    }

    private void placements() {
        ItemStack stairs = new ItemStack(Blocks.oak_stairs,64); player.inventory.mainInventory[0] = stairs;
        float yaw = player.rotationYaw;
        for (int x = 1; x <= 64; x++) {
            world.setBlockToAir(BASE+x,Y,0); world.setBlock(BASE+x,Y-1,0,Blocks.stone);
            MessagePrinterAction m = action(MessagePrinterAction.PLACE,x,stairs); m.yaw = x*40;
            m.intents.add(new MessagePlacementIntent(BASE+x,Y,0,"minecraft:oak_stairs",x%8));
            PrinterActions.INSTANCE.enqueue(player,m);
        }
        player.inventory.mainInventory[20] = stairs;
        player.inventory.mainInventory[0] = new ItemStack(Blocks.dirt,64);
        tick();
        for (int x = 1; x <= 64; x++) {
            check(world.getBlock(BASE+x,Y,0)==Blocks.oak_stairs,"stairs " + x);
            check(world.getBlockMetadata(BASE+x,Y,0)==x%8,"orientation " + x);
        }
        check(player.inventory.mainInventory[20]==null,"inventory consumption");
        check(player.rotationYaw==yaw,"restore player orientation");
        check(network.placed.size()==64,"placement count");
        for (int i=0;i<64;i++) check(network.placed.get(i)==BASE+i+1,"placement order");
        ItemStack stone = new ItemStack(Blocks.stone,64); player.inventory.mainInventory[0] = stone;
        world.setBlockToAir(BASE+1,Y,0);
        MessagePrinterAction stale=action(MessagePrinterAction.PLACE,1,stone);
        PrinterActions.INSTANCE.enqueue(player,stale);world.setBlock(BASE+1,Y,0,Blocks.glass);tick();
        check(network.placed.size()==64,"stale request rejected");
    }

    private void limits() {
        network.placed.clear();
        ConfigurationHandler.printerMaxPlacementsPerTick=1;ConfigurationHandler.printerMaxBreaksPerTick=1;
        ItemStack stone=new ItemStack(Blocks.stone,64),tool=new ItemStack(Items.iron_pickaxe);
        player.inventory.mainInventory[0]=stone;player.inventory.mainInventory[1]=tool;
        for(int x:new int[]{1,32,2,33}) {
            if(x<10)world.setBlockToAir(BASE+x,Y,0);else world.setBlock(BASE+x,Y,0,Blocks.stone);
            PrinterActions.INSTANCE.enqueue(player,action(x<10?MessagePrinterAction.PLACE:MessagePrinterAction.BREAK,x,x<10?stone:tool));
        }
        tick();check(network.placed.equals(Collections.singletonList(BASE+1)),"first placement quota");
        check(world.isAirBlock(BASE+32,Y,0)&&world.getBlock(BASE+33,Y,0)==Blocks.stone,"first break quota");
        tick();check(network.placed.equals(Arrays.asList(BASE+1,BASE+2)),"queued order");check(world.isAirBlock(BASE+33,Y,0),"second break");
        ConfigurationHandler.printerMaxBreaksPerTick=0;
        world.setBlock(BASE+32,Y,0,Blocks.stone);PrinterActions.INSTANCE.enqueue(player,action(MessagePrinterAction.BREAK,32,tool));tick();
        check(world.getBlock(BASE+32,Y,0)==Blocks.stone,"disabled fast break");
    }

    private void slabs() {
        ItemStack slabs = new ItemStack(Blocks.stone_slab, 2);
        player.inventory.mainInventory[0] = slabs;
        world.setBlockToAir(BASE+1,Y,0);
        world.setBlock(BASE+1,Y-1,0,Blocks.stone);
        MessagePrinterAction request = action(MessagePrinterAction.PLACE,1,slabs);
        request.completeSlab = true;
        PrinterActions.INSTANCE.enqueue(player,request);
        tick();
        check(world.getBlock(BASE+1,Y,0) == Blocks.double_stone_slab, "double slab completion");
        check(player.inventory.mainInventory[0] == null, "double slab consumes two items");
        slabs = new ItemStack(Blocks.stone_slab, 1);
        player.inventory.mainInventory[0] = slabs;
        world.setBlockToAir(BASE+1,Y,0);
        request = action(MessagePrinterAction.PLACE,1,slabs);
        request.completeSlab = true;
        PrinterActions.INSTANCE.enqueue(player,request);
        tick();
        check(world.getBlock(BASE+1,Y,0) == Blocks.stone_slab, "missing second slab is not created");
    }

    public static class Protection {
        boolean interact,breaking;
        @SubscribeEvent public void interact(PlayerInteractEvent e) { if(interact)e.setCanceled(true); }
        @SubscribeEvent public void breaking(BlockEvent.BreakEvent e) { if(breaking)e.setCanceled(true); }
    }
    private static class RecordingNet extends NetHandlerPlayServer {
        final List<Integer> placed=new ArrayList<>();
        RecordingNet(MinecraftServer server,EntityPlayerMP player) { super(server,new NetworkManager(false){@Override public boolean isChannelOpen(){return true;}},player); }
        @Override public void sendPacket(Packet packet) {}
        @Override public void processPlayerBlockPlacement(C08PacketPlayerBlockPlacement packet) {placed.add(packet.func_149576_c());super.processPlayerBlockPlacement(packet);}
    }
}
