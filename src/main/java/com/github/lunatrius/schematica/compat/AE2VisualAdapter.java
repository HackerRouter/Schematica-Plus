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

    private static final ForgeDirection[] SPIN = {ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.NORTH, ForgeDirection.EAST};

    @Override public boolean transformsNBT(TileEntity tile) { return Reflect.is(tile, "appeng.tile.AEBaseTile"); }

    /**
     * Rotation and mirroring: a device's forward and up directions; a cable bus's parts ("def:N", "extra:N") and
     * facades ("facade:N") move to the turned side; terminals and panels on a floor or ceiling turn their spin.
     */
    @Override public void transformNBT(TileEntity tile, NBTTagCompound data, char operation) {
        transformTag(data, operation);
    }

    static void transformTag(NBTTagCompound data, char operation) {
        for (String key : new String[] {"orientation_forward", "orientation_up"}) {
            if (!data.hasKey(key, 8)) continue;
            try {
                ForgeDirection side = ForgeDirection.valueOf(data.getString(key));
                data.setString(key, com.github.lunatrius.schematica.util.SchematicTransform.direction(operation, side).name());
            } catch (IllegalArgumentException ignored) {}
        }
        Map<String, net.minecraft.nbt.NBTBase> moved = new LinkedHashMap<>();
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            ForgeDirection turned = com.github.lunatrius.schematica.util.SchematicTransform.direction(operation, side);
            for (String prefix : new String[] {"def:", "extra:", "facade:"}) {
                String key = prefix + side.ordinal();
                if (!data.hasKey(key)) continue;
                net.minecraft.nbt.NBTBase value = data.getTag(key);
                data.removeTag(key);
                if (prefix.equals("extra:") && value instanceof NBTTagCompound) spin((NBTTagCompound) value, side, turned, operation);
                moved.put(prefix + turned.ordinal(), value);
            }
        }
        for (Map.Entry<String, net.minecraft.nbt.NBTBase> entry : moved.entrySet()) data.setTag(entry.getKey(), entry.getValue());
    }

    /** AbstractPartReporting.spin on a floor or ceiling is a horizontal direction in the yaw encoding (0 S, 1 W, 2 N, 3 E). */
    private static void spin(NBTTagCompound extra, ForgeDirection side, ForgeDirection turned, char operation) {
        if (!extra.hasKey("spin", 1) || side.offsetY == 0) return;
        int spin = extra.getByte("spin") & 3;
        if (turned.offsetY == 0) { extra.setByte("spin", (byte) 0); return; }
        ForgeDirection direction = com.github.lunatrius.schematica.util.SchematicTransform.direction(operation, SPIN[spin]);
        for (int i = 0; i < 4; i++) if (SPIN[i] == direction) extra.setByte("spin", (byte) i);
    }

    /** The preview: orientation fields directly; a cable bus reads the transformed NBT, then its cable turns its client connections. */
    @Override public void transformPreview(TileEntity tile, char operation) throws Exception {
        if (Reflect.is(tile, "appeng.api.parts.IPartHost")) {
            NBTTagCompound data = new NBTTagCompound();
            tile.writeToNBT(data);
            transformTag(data, operation);
            tile.readFromNBT(data);
            Object cable = Reflect.call(tile, "getPart", new Class<?>[] {ForgeDirection.class}, ForgeDirection.UNKNOWN);
            if (Reflect.is(cable, "appeng.parts.networking.PartCable")) {
                Field connections = Reflect.field(cable.getClass(), "connections");
                @SuppressWarnings("unchecked") java.util.Set<ForgeDirection> sides = (java.util.Set<ForgeDirection>) connections.get(cable);
                java.util.EnumSet<ForgeDirection> turned = java.util.EnumSet.noneOf(ForgeDirection.class);
                for (ForgeDirection side : sides) turned.add(com.github.lunatrius.schematica.util.SchematicTransform.direction(operation, side));
                sides.clear();
                sides.addAll(turned);
                Object channels = Reflect.get(cable, "channelsOnSide");
                if (channels != null && channels.getClass().isArray() && java.lang.reflect.Array.getLength(channels) == 6) {
                    com.github.lunatrius.schematica.util.SchematicTransform.sides(operation, channels);
                }
            }
            return;
        }
        if (!(Boolean) Reflect.call(tile, "canBeRotated")) return;
        Field forward = Reflect.field(tile.getClass(), "forward"), up = Reflect.field(tile.getClass(), "up");
        for (Field field : new Field[] {forward, up}) {
            ForgeDirection side = (ForgeDirection) field.get(tile);
            if (side != null && side != ForgeDirection.UNKNOWN) field.set(tile, com.github.lunatrius.schematica.util.SchematicTransform.direction(operation, side));
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
