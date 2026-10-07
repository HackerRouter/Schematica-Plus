package com.github.lunatrius.schematica.handler;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.network.PacketHandler;
import com.github.lunatrius.schematica.network.message.MessageDownloadBegin;
import com.github.lunatrius.schematica.network.message.MessageDownloadChunk;
import com.github.lunatrius.schematica.network.message.MessageDownloadEnd;
import com.github.lunatrius.schematica.network.transfer.SchematicTransfer;
import com.github.lunatrius.schematica.reference.Constants;
import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class DownloadHandler {

    public static final DownloadHandler INSTANCE = new DownloadHandler();

    public ISchematic schematic = null;
    private final java.util.BitSet receivedChunks = new java.util.BitSet();
    private static final int MAX_REGION_BYTES = 32 * 1024 * 1024;
    private byte[][] regionParts;
    private int regionReceived, regionBytes;
    private boolean regionFailed;

    public synchronized void beginDownload(ISchematic schematic) {
        this.schematic = schematic;
        receivedChunks.clear();
        regionParts = null;
        regionReceived = regionBytes = 0;
        regionFailed = false;
    }

    /** Client: one slice of the independent region contents, sent after the block chunks. */
    public synchronized void receivedRegions(com.github.lunatrius.schematica.network.message.MessageDownloadRegions message) {
        if (schematic == null || regionFailed) return;
        if (regionParts == null) {
            if ((long) message.count * com.github.lunatrius.schematica.network.message.MessageDownloadRegions.CHUNK_SIZE > MAX_REGION_BYTES + 30000L) {
                regionFailed = true;
                return;
            }
            regionParts = new byte[message.count][];
        }
        if (message.count != regionParts.length || regionParts[message.index] != null
            || (regionBytes += message.data.length) > MAX_REGION_BYTES) {
            regionFailed = true;
            return;
        }
        regionParts[message.index] = message.data;
        regionReceived++;
    }

    /** Client, at the end: the downloaded schematic, with independent regions when they were sent; null when incomplete. */
    public synchronized ISchematic completedSchematic() throws java.io.IOException {
        if (!isDownloadComplete() || regionFailed) return null;
        if (regionParts == null) return schematic;
        if (regionReceived != regionParts.length) return null;
        java.io.ByteArrayOutputStream payload = new java.io.ByteArrayOutputStream(regionBytes);
        for (byte[] part : regionParts) payload.write(part);
        return com.github.lunatrius.schematica.world.schematic.SchematicAlpha.withRegionPayload(schematic, payload.toByteArray());
    }

    public boolean validChunk(int x, int y, int z) {
        return schematic != null && x >= 0 && y >= 0 && z >= 0
            && x < schematic.getWidth() && y < schematic.getHeight() && z < schematic.getLength()
            && x % 16 == 0 && y % 16 == 0 && z % 16 == 0;
    }

    private int chunkIndex(int x, int y, int z) {
        int width = (schematic.getWidth() + 15) / 16, height = (schematic.getHeight() + 15) / 16;
        return x / 16 + width * (y / 16 + height * (z / 16));
    }

    public boolean hasReceivedChunk(int x, int y, int z) {
        return validChunk(x, y, z) && receivedChunks.get(chunkIndex(x, y, z));
    }

    public void receivedChunk(int x, int y, int z) {
        if (validChunk(x, y, z)) receivedChunks.set(chunkIndex(x, y, z));
    }

    public boolean isDownloadComplete() {
        return schematic != null && receivedChunks.cardinality() == ((schematic.getWidth() + 15) / 16)
            * ((schematic.getHeight() + 15) / 16) * ((schematic.getLength() + 15) / 16);
    }

    public final Map<EntityPlayerMP, SchematicTransfer> transferMap = new LinkedHashMap<>();

    private DownloadHandler() {}

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            return;
        }

        processQueue();
    }

    private void processQueue() {
        if (this.transferMap.isEmpty()) {
            return;
        }

        final EntityPlayerMP player = this.transferMap.keySet()
            .iterator()
            .next();
        final SchematicTransfer transfer = this.transferMap.remove(player);

        if (transfer == null || transfer.cancelled) {
            return;
        }

        if (!transfer.state.isWaiting()) {
            if (++transfer.timeout >= Constants.Network.TIMEOUT) {
                if (++transfer.retries >= Constants.Network.RETRIES) {
                    Reference.logger.warn("{}'s download was dropped!", player.getDisplayName());
                    return;
                }

                Reference.logger
                    .warn("{}'s download timed out, retrying (#{})", player.getDisplayName(), transfer.retries);

                if (transfer.state == SchematicTransfer.State.BEGIN) sendBegin(player, transfer);
                else sendChunk(player, transfer);
                transfer.timeout = 0;
            }
        } else if (transfer.state == SchematicTransfer.State.BEGIN_WAIT) {
            sendBegin(player, transfer);
        } else if (transfer.state == SchematicTransfer.State.CHUNK_WAIT) {
            sendChunk(player, transfer);
        } else if (transfer.state == SchematicTransfer.State.END_WAIT) {
            try {
                if (sendEnd(transfer, message -> PacketHandler.INSTANCE.sendTo(message, player))) {
                    this.transferMap.put(player, transfer);
                }
            } catch (java.io.IOException | RuntimeException e) {
                transfer.cancelled = true;
                Reference.logger.warn("Could not complete the download of {}", transfer.name, e);
                player.addChatMessage(new net.minecraft.util.ChatComponentTranslation(
                    com.github.lunatrius.schematica.reference.Names.Command.Download.Message.DOWNLOAD_FAILED, transfer.name));
            }
            return;
        }

        if (!transfer.cancelled) this.transferMap.put(player, transfer);
    }

    private void sendBegin(EntityPlayerMP player, SchematicTransfer transfer) {
        transfer.setState(SchematicTransfer.State.BEGIN);

        MessageDownloadBegin message = new MessageDownloadBegin(transfer.schematic);
        PacketHandler.INSTANCE.sendTo(message, player);
    }

    private void sendChunk(EntityPlayerMP player, SchematicTransfer transfer) {
        transfer.setState(SchematicTransfer.State.CHUNK);

        Reference.logger.trace("Sending chunk {},{},{}", transfer.baseX, transfer.baseY, transfer.baseZ);
        MessageDownloadChunk message = new MessageDownloadChunk(
            transfer.schematic,
            transfer.baseX,
            transfer.baseY,
            transfer.baseZ);
        PacketHandler.INSTANCE.sendTo(message, player);
    }

    /** Sends up to eight slices of the independent region payload; returns whether slices remain to be sent. */
    private static boolean sendRegions(SchematicTransfer transfer,
        java.util.function.Consumer<cpw.mods.fml.common.network.simpleimpl.IMessage> sender) throws java.io.IOException {
        if (!transfer.regionSupport) return false;
        if (transfer.regionPayload == null) {
            transfer.regionPayload = com.github.lunatrius.schematica.world.schematic.SchematicAlpha.regionPayload(transfer.schematic);
        }
        byte[] payload = transfer.regionPayload;
        if (payload == null) return false;
        int size = com.github.lunatrius.schematica.network.message.MessageDownloadRegions.CHUNK_SIZE;
        int count = (payload.length + size - 1) / size;
        for (int i = 0; i < 8 && transfer.regionSent < count; i++, transfer.regionSent++) {
            int from = transfer.regionSent * size;
            sender.accept(new com.github.lunatrius.schematica.network.message.MessageDownloadRegions(transfer.regionSent, count,
                java.util.Arrays.copyOfRange(payload, from, Math.min(payload.length, from + size))));
        }
        return transfer.regionSent < count;
    }

    static boolean sendEnd(SchematicTransfer transfer,
        java.util.function.Consumer<cpw.mods.fml.common.network.simpleimpl.IMessage> sender) throws java.io.IOException {
        if (sendRegions(transfer, sender)) return true;
        sender.accept(new MessageDownloadEnd(transfer.name));
        transfer.setState(SchematicTransfer.State.END);
        return false;
    }
}
