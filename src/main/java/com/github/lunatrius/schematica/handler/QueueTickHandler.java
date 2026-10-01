package com.github.lunatrius.schematica.handler;

import java.util.ArrayDeque;
import java.util.Queue;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;

import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.task.TaskRegistry;
import com.github.lunatrius.schematica.world.chunk.SchematicContainer;
import com.github.lunatrius.schematica.world.schematic.SchematicFormat;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class QueueTickHandler {
    public static final QueueTickHandler INSTANCE = new QueueTickHandler();
    private final Queue<Save> queue = new ArrayDeque<>();
    private volatile boolean clientPending, serverPending;

    private static final class Save {
        final SchematicContainer container;
        final TaskRegistry.Task task;

        Save(SchematicContainer container) {
            this.container = container;
            task = TaskRegistry.INSTANCE.start(container.player.getUniqueID(), container.world.provider.dimensionId,
                TaskRegistry.Kind.SAVE, container.world.isRemote ? TaskRegistry.Backend.CLIENT : TaskRegistry.Backend.SERVER,
                container.file.getName());
        }
    }

    private QueueTickHandler() {}

    public synchronized void clear() {
        for (Save save : queue) save.task.finish();
        queue.clear();
        clientPending = serverPending = false;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !clientPending) return;
        Minecraft mc = Minecraft.getMinecraft();
        processQueue(true, mc.theWorld, mc.thePlayer);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && serverPending) processQueue(false, null, null);
    }

    private EntityPlayer owner(Save save, World clientWorld, EntityPlayer clientPlayer) {
        World world = save.container.world;
        if (world.isRemote) {
            return world == clientWorld && clientPlayer != null && save.task.owner.equals(clientPlayer.getUniqueID())
                ? clientPlayer : null;
        }
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || net.minecraftforge.common.DimensionManager.getWorld(save.task.dimension) != world) return null;
        for (EntityPlayerMP player : server.getConfigurationManager().playerEntityList) {
            if (save.task.owner.equals(player.getUniqueID()) && player.worldObj == world) return player;
        }
        return null;
    }

    private synchronized void processQueue(boolean client, World clientWorld, EntityPlayer clientPlayer) {
        Save save = null;
        for (int remaining = queue.size(); remaining > 0; remaining--) {
            Save candidate = queue.poll();
            if (candidate.container.world.isRemote == client) { save = candidate; break; }
            queue.offer(candidate);
        }
        if (save == null) return;
        SchematicContainer container = save.container;
        EntityPlayer player = null;
        boolean pending = false;
        try {
            player = owner(save, clientWorld, clientPlayer);
            if (player == null || save.task.progress().cancelling) return;
            if (container.hasNext()) {
                if (container.isFirst()) player.addChatMessage(new ChatComponentTranslation(
                    Names.Command.Save.Message.SAVE_STARTED, container.chunkCount, container.file.getName()));
                container.next();
            }
            save.task.update(TaskRegistry.Stage.CAPTURE, container.processedChunks, container.chunkCount, 0, 0);
            if (container.hasNext()) {
                pending = true;
                return;
            }
            if (!save.task.beginWrite()) return;
            for (TileEntity entity : container.schematic.getTileEntities()) {
                if (!entity.hasWorldObj()) entity.setWorldObj(container.world);
            }
            if (container.memory != null) {
                com.github.lunatrius.schematica.world.schematic.SchematicFileSnapshot snapshot =
                    com.github.lunatrius.schematica.world.schematic.SchematicFileSnapshot.capture(container.schematic);
                java.util.function.Consumer<com.github.lunatrius.schematica.world.schematic.SchematicFileSnapshot> memory = container.memory;
                Minecraft.getMinecraft().func_152344_a(() -> memory.accept(snapshot));
                return;
            }
            java.io.File saved = SchematicFormat.saveToFile(container.file, container.schematic, container.world,
                container.includeNBT, container.includeEntities);
            boolean renamed = saved != null && !saved.getName().equals(container.file.getName());
            String message = saved != null ? (renamed ? Names.Command.Save.Message.SAVE_EXTENDED
                : Names.Command.Save.Message.SAVE_SUCCESSFUL) : Names.Command.Save.Message.SAVE_FAILED;
            player.addChatMessage(new ChatComponentTranslation(message,
                saved != null ? saved.getName() : container.file.getName()));
        } catch (Exception e) {
            Reference.logger.error("Schematic save task failed", e);
            if (player != null) player.addChatMessage(new ChatComponentTranslation(
                Names.Command.Save.Message.SAVE_FAILED, container.file.getName()));
        } finally {
            if (save.task.progress().cancelling) {
                pending = false;
                if (player != null) player.addChatMessage(new ChatComponentTranslation(
                    "litematica.message.error.schematic_save_interrupted"));
            }
            if (pending) queue.offer(save);
            else save.task.finish();
            updatePending();
        }
    }

    private void updatePending() {
        clientPending = queue.stream().anyMatch(save -> save.container.world.isRemote);
        serverPending = queue.stream().anyMatch(save -> !save.container.world.isRemote);
    }

    public synchronized boolean canQueue(EntityPlayer player) {
        return queue.size() < 4 && queue.stream().noneMatch(task -> task.task.owner.equals(player.getUniqueID()));
    }

    public synchronized void queueSchematic(SchematicContainer container) {
        if (!canQueue(container.player)) throw new IllegalStateException("A save is already pending or the save queue is full");
        queue.offer(new Save(container));
        updatePending();
    }
}
