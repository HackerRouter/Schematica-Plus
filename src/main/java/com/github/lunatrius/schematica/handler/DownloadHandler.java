package com.github.lunatrius.schematica.handler;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.network.PacketHandler;
import com.github.lunatrius.schematica.network.message.MessageDownloadBegin;
import com.github.lunatrius.schematica.network.message.MessageDownloadChunk;
import com.github.lunatrius.schematica.network.message.MessageDownloadEnd;
import com.github.lunatrius.schematica.network.message.MessageDownloadRegions;
import com.github.lunatrius.schematica.network.transfer.SchematicTransfer;
import com.github.lunatrius.schematica.network.transfer.SliceAssembler;
import com.github.lunatrius.schematica.reference.Constants;
import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.world.schematic.LitematicaNBTReader;
import com.github.lunatrius.schematica.world.schematic.SchematicAlpha;
import com.github.lunatrius.schematica.world.schematic.SchematicFileSnapshot;
import com.github.lunatrius.schematica.world.schematic.SchematicFormat;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;

public class DownloadHandler {

    public static final DownloadHandler INSTANCE = new DownloadHandler();

    public ISchematic schematic = null;
    private final BitSet receivedChunks = new BitSet();
    private static final int MAX_REGION_BYTES = 32 * 1024 * 1024;
    private SliceAssembler regions;
    private boolean regionFailed;

    public synchronized void beginDownload(ISchematic schematic) {
        this.schematic = schematic;
        receivedChunks.clear();
        regions = null;
        regionFailed = false;
    }

    /** Client: one slice of the independent region contents, sent after the block chunks. */
    public synchronized void receivedRegions(MessageDownloadRegions message) {
        if (schematic == null || regionFailed) return;
        if (regions == null) regions = SliceAssembler.create(message.count, MessageDownloadRegions.CHUNK_SIZE, MAX_REGION_BYTES);
        if (regions == null || !regions.add(message.index, message.count, message.data)) regionFailed = true;
    }

    /** Client, at the end: the downloaded schematic, with independent regions when they were sent; null when incomplete. */
    public synchronized ISchematic completedSchematic() throws IOException {
        if (!isDownloadComplete() || regionFailed) return null;
        if (regions == null) return schematic;
        if (!regions.complete()) return null;
        return SchematicAlpha.withRegionPayload(schematic, regions.join());
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

    private final Map<EntityPlayerMP, SchematicTransfer> transferMap = new LinkedHashMap<>();
    private final Map<EntityPlayerMP, Read> reading = new LinkedHashMap<>();
    private final ExecutorService reader = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Schematica Plus download reader");
        thread.setDaemon(true);
        return thread;
    });

    private static final class Loaded {
        final SchematicFileSnapshot snapshot;
        final LitematicaNBTReader.Parsed parsed;

        Loaded(SchematicFileSnapshot snapshot) throws IOException {
            this.snapshot = snapshot; this.parsed = snapshot.parse();
        }
    }

    private static final class Read {
        final String name;
        final Future<Loaded> loaded;

        Read(String name, Future<Loaded> loaded) { this.name = name; this.loaded = loaded; }
    }

    public synchronized void start(EntityPlayerMP player, SchematicTransfer transfer) { transferMap.put(player, transfer); }

    public synchronized void read(EntityPlayerMP player, File file) {
        reading.put(player, new Read(file.getName(), reader.submit(() -> new Loaded(SchematicFileSnapshot.read(file)))));
    }

    public synchronized void stop(EntityPlayerMP player) {
        transferMap.remove(player);
        reading.remove(player);
    }

    public synchronized void stopAll() {
        transferMap.clear();
        reading.clear();
    }

    private void finishReads() {
        Iterator<Map.Entry<EntityPlayerMP, Read>> entries = reading.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<EntityPlayerMP, Read> entry = entries.next();
            Read read = entry.getValue();
            if (!read.loaded.isDone()) continue;
            entries.remove();
            ISchematic schematic = null;
            try {
                Loaded loaded = read.loaded.get();
                schematic = SchematicFormat.readFromSnapshot(loaded.snapshot, loaded.parsed);
            } catch (Exception e) {
                Reference.logger.warn("Could not read the download {}", read.name, e);
            }
            if (schematic != null) transferMap.put(entry.getKey(), new SchematicTransfer(schematic, read.name));
            else entry.getKey().addChatMessage(new ChatComponentTranslation(
                Names.Command.Download.Message.DOWNLOAD_FAILED, read.name));
        }
    }

    public synchronized void acceptBegin(EntityPlayerMP player) {
        SchematicTransfer transfer = transferMap.get(player);
        if (transfer != null && transfer.state == SchematicTransfer.State.BEGIN) transfer.setState(SchematicTransfer.State.CHUNK_WAIT);
    }

    public synchronized void acceptChunk(EntityPlayerMP player, int x, int y, int z) {
        SchematicTransfer transfer = transferMap.get(player);
        if (transfer != null) transfer.confirmChunk(x, y, z);
    }

    private DownloadHandler() {}

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            return;
        }

        processQueue();
    }

    private synchronized void processQueue() {
        if (!reading.isEmpty()) finishReads();
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
            } catch (IOException | RuntimeException e) {
                transfer.cancelled = true;
                Reference.logger.warn("Could not complete the download of {}", transfer.name, e);
                player.addChatMessage(new ChatComponentTranslation(
                    Names.Command.Download.Message.DOWNLOAD_FAILED, transfer.name));
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
        Consumer<IMessage> sender) throws IOException {
        if (transfer.regionPayload == null) {
            transfer.regionPayload = SchematicAlpha.regionPayload(transfer.schematic);
        }
        byte[] payload = transfer.regionPayload;
        if (payload == null) return false;
        int size = MessageDownloadRegions.CHUNK_SIZE;
        int count = (payload.length + size - 1) / size;
        for (int i = 0; i < 8 && transfer.regionSent < count; i++, transfer.regionSent++) {
            int from = transfer.regionSent * size;
            sender.accept(new MessageDownloadRegions(transfer.regionSent, count,
                Arrays.copyOfRange(payload, from, Math.min(payload.length, from + size))));
        }
        return transfer.regionSent < count;
    }

    static boolean sendEnd(SchematicTransfer transfer,
        Consumer<IMessage> sender) throws IOException {
        if (sendRegions(transfer, sender)) return true;
        sender.accept(new MessageDownloadEnd(transfer.name));
        transfer.setState(SchematicTransfer.State.END);
        return false;
    }
}
