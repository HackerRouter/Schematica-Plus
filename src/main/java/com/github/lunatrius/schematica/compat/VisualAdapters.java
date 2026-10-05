package com.github.lunatrius.schematica.compat;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.CopyOnWriteArrayList;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import com.github.lunatrius.schematica.api.ISchematicVisualAdapter;
import com.github.lunatrius.schematica.reference.Reference;

public final class VisualAdapters {
    private static final List<ISchematicVisualAdapter> ADAPTERS = new CopyOnWriteArrayList<>();
    private static String environmentProtocol;

    static {
        register(new StreamVisualAdapter("forestry:tiles", "forestry.core.network.IStreamable", "Forestry",
            "writeData", "readData", "forestry.core.network.DataOutputStreamForestry",
            "forestry.core.network.DataInputStreamForestry"));
        register(new StreamVisualAdapter("railcraft:tiles", "mods.railcraft.common.blocks.RailcraftTileEntity", "Railcraft",
            "writePacketData", "readPacketData", "java.io.DataOutputStream", "java.io.DataInputStream"));
        register(new StreamVisualAdapter("buildcraft:tiles", "buildcraft.core.lib.block.TileBuildCraft", "BuildCraft|Core",
            "writeData", "readData", null, null));
        register(new BuildCraftPipeAdapter());
        register(new AE2VisualAdapter());
        register(new GalacticraftVisualAdapter());
        register(new MultipartVisualAdapter());
        register(new GregTechVisualAdapter());
        register(new EnderIOConduitAdapter());
        register(new LogisticsPipesVisualAdapter());
        register(new CodeChickenVisualAdapter());
        register(new BinnieVisualAdapter());
        register(new MalisisVisualAdapter());
        register(new ThaumicExplorationVisualAdapter());
        register(new CarpentersVisualAdapter());
        register(new RailcraftTrackAdapter());
        register(new TileFacingAdapter());
        register(new NamedFieldsAdapter("stevesaddons:rf_node", "stevesaddons.tileentities.TileEntityRFNode", "inputSides", "outputSides"));
    }

    private VisualAdapters() {}

    public static String typeName(Object tile) {
        String cableBus = "appeng.tile.networking.TileCableBus";
        if (Reflect.is(tile, cableBus)) return cableBus;
        String multipart = "codechicken.multipart.TileMultipart";
        return Reflect.is(tile, multipart) ? multipart : tile.getClass().getName();
    }

    static synchronized String environmentProtocol() {
        if (environmentProtocol != null) return environmentProtocol;
        try {
            List<String> mods = new ArrayList<>();
            for (ModContainer mod : Loader.instance().getModList()) {
                if (!mod.getModId().equals(Reference.MODID)) mods.add(mod.getModId() + "=" + mod.getVersion());
            }
            Collections.sort(mods);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(String.join("\n", mods).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : digest) result.append(String.format("%02x", value & 255));
            return environmentProtocol = result.toString();
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    public static String packetProtocol(TileEntity tile) {
        if (Reflect.is(tile, "appeng.tile.AEBaseTile")) return StreamVisualAdapter.version("appliedenergistics2");
        if (Reflect.is(tile, "gregtech.api.metatileentity.CommonBaseMetaTileEntity")) return StreamVisualAdapter.version("gregtech");
        return "1";
    }

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

    public static void beforeRender(TileEntity tile, float partialTicks) {
        for (ISchematicVisualAdapter adapter : ADAPTERS) {
            try { adapter.beforeRender(tile, partialTicks); }
            catch (Exception | LinkageError e) { Reference.logger.debug("Could not animate {}", adapter.id(), e); }
        }
    }

    public static void transformPreview(TileEntity tile, char operation) {
        for (ISchematicVisualAdapter adapter : ADAPTERS) {
            if (!adapter.supports(tile)) continue;
            try { adapter.transformPreview(tile, operation); }
            catch (Exception | LinkageError e) {
                Reference.logger.warn("Could not transform {} for {}", adapter.id(), tile.getClass().getName(), e);
            }
        }
    }

    /** Whether an adapter turns this tile's saved data; such blocks are not also turned by Block.rotateBlock. */
    public static boolean transformsTile(TileEntity tile) {
        if (tile == null) return false;
        for (ISchematicVisualAdapter adapter : ADAPTERS) {
            try { if (adapter.supports(tile) && adapter.transformsNBT(tile)) return true; }
            catch (RuntimeException | LinkageError e) { Reference.logger.debug("Could not check {}", adapter.id(), e); }
        }
        return false;
    }

    public static NBTTagCompound transformNBT(TileEntity tile, java.util.function.Supplier<NBTTagCompound> source, char operation) {
        NBTTagCompound data = null;
        for (ISchematicVisualAdapter adapter : ADAPTERS) {
            if (!adapter.supports(tile) || !adapter.transformsNBT(tile)) continue;
            try {
                NBTTagCompound transformed = data == null ? source.get() : (NBTTagCompound) data.copy();
                adapter.transformNBT(tile, transformed, operation);
                data = transformed;
            } catch (Exception | LinkageError e) {
                Reference.logger.warn("Could not transform saved {} for {}", adapter.id(), tile.getClass().getName(), e);
            }
        }
        return data;
    }
}
