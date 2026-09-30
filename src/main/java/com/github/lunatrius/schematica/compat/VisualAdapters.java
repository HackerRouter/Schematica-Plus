package com.github.lunatrius.schematica.compat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import com.github.lunatrius.schematica.api.ISchematicVisualAdapter;
import com.github.lunatrius.schematica.reference.Reference;

public final class VisualAdapters {
    private static final List<ISchematicVisualAdapter> ADAPTERS = new CopyOnWriteArrayList<>();

    static {
        register(new StreamVisualAdapter("forestry:tiles", "forestry.core.network.IStreamable", "Forestry",
            "writeData", "readData", "forestry.core.network.DataOutputStreamForestry",
            "forestry.core.network.DataInputStreamForestry"));
        register(new StreamVisualAdapter("railcraft:tiles", "mods.railcraft.common.blocks.RailcraftTileEntity", "Railcraft",
            "writePacketData", "readPacketData", "java.io.DataOutputStream", "java.io.DataInputStream"));
        register(new StreamVisualAdapter("buildcraft:tiles", "buildcraft.core.lib.block.TileBuildCraft", "BuildCraft|Core",
            "writeData", "readData", null, null));
        register(new BuildCraftPipeAdapter());
    }

    private VisualAdapters() {}

    public static synchronized void register(ISchematicVisualAdapter adapter) {
        if (adapter == null || !adapter.id().matches("[a-z0-9_.:-]{1,128}")) {
            throw new IllegalArgumentException("Invalid visual adapter ID");
        }
        for (ISchematicVisualAdapter existing : ADAPTERS) {
            if (existing.id().equals(adapter.id())) throw new IllegalArgumentException("Duplicate visual adapter ID");
        }
        ADAPTERS.add(adapter);
    }

    public static boolean replacesDescriptionPacket(TileEntity tile) {
        for (ISchematicVisualAdapter adapter : ADAPTERS) {
            if (adapter.supports(tile) && adapter.replacesDescriptionPacket(tile)) return true;
        }
        return false;
    }

    public static NBTTagCompound capture(TileEntity tile) {
        NBTTagCompound result = new NBTTagCompound();
        for (ISchematicVisualAdapter adapter : ADAPTERS) {
            if (!adapter.supports(tile)) continue;
            try {
                NBTTagCompound data = adapter.capture(tile);
                if (data == null) continue;
                NBTTagCompound entry = new NBTTagCompound();
                entry.setString("Protocol", adapter.protocol());
                entry.setTag("Data", data);
                result.setTag(adapter.id(), entry);
            } catch (Exception | LinkageError e) {
                Reference.logger.warn("Could not capture {} for {}", adapter.id(), tile.getClass().getName(), e);
            }
        }
        return result;
    }

    public static void restore(TileEntity tile, NBTTagCompound data) {
        for (ISchematicVisualAdapter adapter : ADAPTERS) {
            if (!data.hasKey(adapter.id(), 10) || !adapter.supports(tile)) continue;
            NBTTagCompound entry = data.getCompoundTag(adapter.id());
            if (!entry.getString("Protocol").equals(adapter.protocol())) {
                Reference.logger.warn("Skipping incompatible visual protocol {} for {}", adapter.id(), tile.getClass().getName());
                continue;
            }
            try {
                adapter.restore(tile, (NBTTagCompound) entry.getCompoundTag("Data").copy());
            } catch (Exception | LinkageError e) {
                Reference.logger.warn("Could not restore {} for {}", adapter.id(), tile.getClass().getName(), e);
            }
        }
    }
}
