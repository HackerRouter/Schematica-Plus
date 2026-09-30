package com.github.lunatrius.schematica.nbt;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.compat.TileNBTCompat;
import com.github.lunatrius.schematica.compat.VisualAdapters;

public final class TileEntitySnapshots {
    private static final Map<TileEntity, TileEntitySnapshot> SNAPSHOTS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final ThreadLocal<Boolean> RESTORING = new ThreadLocal<>();

    private TileEntitySnapshots() {}

    static void capture(TileEntity tile, NBTTagCompound tag) {
        if (SNAPSHOTS.containsKey(tile)) return;
        NBTTagCompound visual = captureVisual(tile);
        if (visual != null) tag.setTag(TileUpdateData.KEY, visual);
    }

    private static NBTTagCompound captureVisual(TileEntity tile) {
        NBTTagCompound adapters = VisualAdapters.capture(tile);
        NBTTagCompound packet = null;
        try {
            if (!VisualAdapters.replacesDescriptionPacket(tile)
                && tile.getClass().getMethod("onDataPacket", NetworkManager.class, S35PacketUpdateTileEntity.class)
                    .getDeclaringClass() != TileEntity.class) {
                packet = TileUpdateData.capture(VisualAdapters.typeName(tile), tile.getDescriptionPacket());
                if (packet != null) packet.setString("Protocol", VisualAdapters.packetProtocol(tile));
            }
        } catch (Exception | LinkageError e) {
            Reference.logger.debug("Could not capture visual state for {}", tile.getClass().getName(), e);
        }
        return TileUpdateData.combine(VisualAdapters.typeName(tile), packet, adapters);
    }

    static void attach(TileEntity tile, NBTTagCompound tag) {
        if (tile != null) SNAPSHOTS.put(tile, new TileEntitySnapshot(tag));
    }

    static NBTTagCompound write(TileEntity tile) {
        TileEntitySnapshot snapshot = SNAPSHOTS.get(tile);
        if (snapshot == null) return writeCurrent(tile);
        NBTTagCompound current = snapshot.isInitialized() ? writeCurrent(tile) : null;
        return snapshot.write(current, tile.xCoord, tile.yCoord, tile.zCoord);
    }

    private static NBTTagCompound writeCurrent(TileEntity tile) {
        NBTTagCompound tag = TileNBTCompat.write(tile);
        ClientVisualState.capture(tile, tag);
        return tag;
    }

    public static boolean isRestoring() { return Boolean.TRUE.equals(RESTORING.get()); }

    public static void removeVisualData(NBTTagCompound tag) { tag.removeTag(TileUpdateData.KEY); }

    public static void replacePreview(TileEntity previous, TileEntity replacement) {
        if (!isRestoring() || previous == null || replacement == null
            || !VisualAdapters.typeName(previous).equals(VisualAdapters.typeName(replacement))) return;
        TileEntitySnapshot snapshot = SNAPSHOTS.get(previous);
        if (snapshot != null) SNAPSHOTS.put(replacement, snapshot);
    }

    @SideOnly(Side.CLIENT)
    public static void restorePreview(TileEntity tile) {
        TileEntitySnapshot snapshot = SNAPSHOTS.get(tile);
        if (snapshot == null || snapshot.isInitialized() || isRestoring() || !tile.hasWorldObj() || !tile.getWorldObj().isRemote) return;
        S35PacketUpdateTileEntity packet = TileUpdateData.packet(snapshot.visual(), VisualAdapters.typeName(tile),
            VisualAdapters.packetProtocol(tile), tile.xCoord, tile.yCoord, tile.zCoord);
        Boolean previous = RESTORING.get();
        RESTORING.set(true);
        int x = tile.xCoord, y = tile.yCoord, z = tile.zCoord;
        try {
            if (packet != null) tile.onDataPacket(null, packet);
            TileEntity current = tile.getWorldObj().getTileEntity(x, y, z);
            if (current != null && SNAPSHOTS.get(current) == snapshot) tile = current;
            VisualAdapters.restore(tile, TileUpdateData.adapters(snapshot.visual(), VisualAdapters.typeName(tile)));
        } catch (Exception | LinkageError e) {
            Reference.logger.warn("Could not restore visual state for {}", tile.getClass().getName(), e);
            NBTTagCompound original = snapshot.write(null, x, y, z);
            removeVisualData(original);
            tile.readFromNBT(original);
        } finally {
            tile.xCoord = x;
            tile.yCoord = y;
            tile.zCoord = z;
            if (previous == null) RESTORING.remove();
            else RESTORING.set(previous);
        }
        TileEntity current = tile.getWorldObj().getTileEntity(x, y, z);
        snapshot.initialize(writeCurrent(current != null && SNAPSHOTS.get(current) == snapshot ? current : tile));
    }

    @SideOnly(Side.CLIENT)
    public static void refreshPreview(TileEntity tile) {
        TileEntitySnapshot snapshot = SNAPSHOTS.get(tile);
        if (snapshot == null || snapshot.visual() == null) return;
        snapshot.visual(captureVisual(tile));
    }
}
