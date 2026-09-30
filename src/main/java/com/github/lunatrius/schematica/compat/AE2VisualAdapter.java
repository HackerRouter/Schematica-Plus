package com.github.lunatrius.schematica.compat;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import com.github.lunatrius.schematica.api.ISchematicVisualAdapter;

final class AE2VisualAdapter implements ISchematicVisualAdapter {
    private static final Map<String, String[]> FIELDS = new LinkedHashMap<>();
    private static final String MONITOR = "appeng.tile.crafting.TileCraftingMonitorTile";

    static {
        fields("parts.networking.PartCable", "connections", "channelsOnSide", "powered");
        fields("parts.PartBasicState", "clientFlags");
        fields("parts.reporting.AbstractPartReporting", "clientFlags", "spin", "opacity");
        fields("parts.reporting.PartThroughputMonitor", "timeMode", "itemNumsChange");
        fields("parts.p2p.PartP2PLight", "lastValue");
        fields("tile.storage.TileChest", "state", "type", "storageType", "paintedColor");
        fields("tile.storage.TileDrive", "state", "type", "storageTypes", "paintedColor");
        fields("tile.networking.TileController", "paintedColor", "controllerAnimation");
        fields("tile.networking.TileWireless", "clientFlags");
        fields("tile.networking.TileWirelessBase", "color");
        fields("tile.misc.TileInterface", "clientFlags", "paintedColor");
        fields("tile.misc.TileQuartzGrowthAccelerator", "hasPower");
        fields("tile.misc.TileSecurity", "isActive", "paintedColor");
        fields("tile.misc.TileStorageReshuffle", "wasRunning");
        fields("tile.misc.TileSuperMEReplenisher", "isPowered", "status");
        fields("tile.spatial.TileSpatialPylon", "displayBits");
        fields("tile.spatial.TileSpatialLinkChamber", "cachedStorageDim");
        fields("tile.spatial.TileSpatialNetworkRelay", "hasConnection");
        fields("tile.crafting.TileMolecularAssembler", "isPowered");
        fields("tile.grindstone.TileCrank", "rotation", "visibleRotation");
        FIELDS.put("com.glodblock.github.common.parts.base.FCPart", new String[] {"clientFlags", "spin"});
        for (String name : new String[] {"TileFluidBuffer", "TileLevelMaintainer", "TileSuperStockReplenisher"}) {
            FIELDS.put("com.glodblock.github.common.tile." + name, new String[] {"isPowered"});
        }
    }

    private static void fields(String name, String... fields) { FIELDS.put("appeng." + name, fields); }

    @Override public String id() { return "ae2:client_state"; }

    @Override public boolean supports(TileEntity tile) {
        return Reflect.is(tile, "appeng.tile.AEBaseTile") || Reflect.is(tile, "appeng.api.parts.IPartHost");
    }

    @Override public boolean replacesDescriptionPacket(TileEntity tile) { return tile.hasWorldObj() && tile.getWorldObj().isRemote; }

    @Override public NBTTagCompound capture(TileEntity tile) throws Exception {
        return replacesDescriptionPacket(tile) ? captureObject(tile) : null;
    }

    static NBTTagCompound captureObject(Object target) throws Exception {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Class", VisualAdapters.typeName(target));
        transferFields(target, tag, false);
        if (Reflect.is(target, "appeng.api.parts.IPartHost")) {
            for (ForgeDirection side : ForgeDirection.values()) {
                Object part = Reflect.call(target, "getPart", new Class<?>[] {ForgeDirection.class}, side);
                if (part != null) tag.setTag(side.name(), captureObject(part));
            }
        }
        if (Reflect.is(target, MONITOR)) {
            tag.setString("MonitorVersion", StreamVisualAdapter.version("appliedenergistics2"));
            tag.setByteArray("Monitor", VisualStreams.writeBuffer(target, "writeToStream_TileCraftingMonitorTile"));
        }
        return tag;
    }

    @Override public void restore(TileEntity tile, NBTTagCompound tag) throws Exception {
        restoreObject(tile, tag);
        Class<?> cableBus = Reflect.type(tile.getClass(), "appeng.tile.networking.TileCableBus");
        if (cableBus != null) {
            java.lang.reflect.Method update = cableBus.getDeclaredMethod("updateTileSetting");
            update.setAccessible(true);
            update.invoke(tile);
        }
    }

    static void restoreObject(Object target, NBTTagCompound tag) throws Exception {
        if (!tag.getString("Class").equals(VisualAdapters.typeName(target))) return;
        transferFields(target, tag, true);
        if (Reflect.is(target, "appeng.api.parts.IPartHost")) {
            for (ForgeDirection side : ForgeDirection.values()) {
                Object part = Reflect.call(target, "getPart", new Class<?>[] {ForgeDirection.class}, side);
                if (part != null && tag.hasKey(side.name(), 10)) restoreObject(part, tag.getCompoundTag(side.name()));
            }
        }
        if (Reflect.is(target, MONITOR) && tag.hasKey("Monitor", 7)
            && tag.getString("MonitorVersion").equals(StreamVisualAdapter.version("appliedenergistics2"))) {
            VisualStreams.readBuffer(target, "readFromStream_TileCraftingMonitorTile", tag.getByteArray("Monitor"));
        }
    }

    private static void transferFields(Object target, NBTTagCompound tag, boolean restore) throws ReflectiveOperationException {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            String[] names = FIELDS.get(type.getName());
            if (names == null) continue;
            for (String name : names) {
                try {
                    Field field = type.getDeclaredField(name);
                    if (restore) VisualFields.restore(target, field, tag);
                    else VisualFields.capture(target, field, tag);
                } catch (NoSuchFieldException ignored) {}
            }
        }
    }
}
