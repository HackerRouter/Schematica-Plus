package com.github.lunatrius.schematica.proxy;

import java.io.File;
import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Property;

import com.github.lunatrius.schematica.client.container.ContainerVerifier;
import com.github.lunatrius.schematica.client.gui.browser.SchematicPreview;
import com.github.lunatrius.schematica.client.gui.material.MaterialItemCache;
import com.github.lunatrius.schematica.client.gui.material.MaterialItemKey;
import com.github.lunatrius.schematica.client.gui.material.MaterialListModel;
import com.github.lunatrius.schematica.client.gui.material.MaterialLists;
import com.github.lunatrius.schematica.client.input.Hotkeys;
import com.github.lunatrius.schematica.client.printer.DestroyAssist;
import com.github.lunatrius.schematica.client.printer.SchematicPrinter;
import com.github.lunatrius.schematica.client.printer.SignTextPaste;
import com.github.lunatrius.schematica.client.projects.SchematicProjects;
import com.github.lunatrius.schematica.client.renderer.RendererSchematicGlobal;
import com.github.lunatrius.schematica.client.renderer.hud.BlockInfoHud;
import com.github.lunatrius.schematica.client.selection.AreaSelections;
import com.github.lunatrius.schematica.client.util.InfoUtils;
import com.github.lunatrius.schematica.client.verifier.VerificationManager;
import com.github.lunatrius.schematica.client.world.Placements;
import com.github.lunatrius.schematica.client.world.RenderLayerSettings;
import com.github.lunatrius.schematica.compat.ILOTRPresent;
import com.github.lunatrius.schematica.compat.NoLOTRProxy;
import com.github.lunatrius.schematica.compat.nei.NeiBridge;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.client.ChatEventHandler;
import com.github.lunatrius.schematica.handler.client.CommandEditQueue;
import com.github.lunatrius.schematica.handler.client.ModInfoHandler;
import com.github.lunatrius.schematica.handler.client.NbtRequestClient;
import com.github.lunatrius.schematica.handler.client.OverlayHandler;
import com.github.lunatrius.schematica.handler.client.RemoteEditClient;
import com.github.lunatrius.schematica.handler.client.RenderTickHandler;
import com.github.lunatrius.schematica.handler.client.TickHandler;
import com.github.lunatrius.schematica.handler.client.WorldHandler;
import com.github.lunatrius.schematica.network.message.MessageEditStatus;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.tool.WorldMoveController;
import com.github.lunatrius.schematica.util.PlayerMessage;
import com.github.lunatrius.schematica.world.chunk.ChunkNbt;
import com.github.lunatrius.schematica.world.chunk.SchematicContainer;

import cpw.mods.fml.client.config.GuiConfigEntries;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    private static final Minecraft MINECRAFT = Minecraft.getMinecraft();
    public static ILOTRPresent lotrProxy = null;

    @Override public void worldMoveFinished(long id, boolean success) {
        WorldMoveController.finished(id, success);
    }
    @Override public void remoteEditStatus(MessageEditStatus status) {
        MINECRAFT.func_152344_a(() -> RemoteEditClient.INSTANCE.status(status));
    }
    @Override public void runSaveCallback(Runnable callback) {
        MINECRAFT.func_152344_a(callback);
    }
    @Override public void copyChunkToSchematic(SchematicContainer container, int chunkX, int chunkZ) {
        super.copyChunkToSchematic(container, chunkX, chunkZ);
        if (!container.world.isRemote || container.source != null || !NbtRequestClient.INSTANCE.available()) return;
        container.pendingNbt++;
        NbtRequestClient.INSTANCE.request(chunkX, chunkZ, container.minY, container.maxY, data -> {
            container.pendingNbt--;
            if (data == null) container.nbtUnavailable = true;
            else ChunkNbt.merge(container, chunkX, chunkZ, data);
        });
    }
    @Override public void sendMessage(EntityPlayer player, PlayerMessage message) {
        if (player == null || message == null) return;
        EntityPlayer local = MINECRAFT.thePlayer;
        boolean host = MINECRAFT.getIntegratedServer() != null && local != null && local.getUniqueID().equals(player.getUniqueID());
        if (player.worldObj.isRemote || host) MINECRAFT.func_152344_a(() -> InfoUtils.showGuiOrInGameMessage(message));
        else super.sendMessage(player, message);
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);

        final Property[] sliders = { ConfigurationHandler.propAlpha, ConfigurationHandler.propBlockDelta,
            ConfigurationHandler.propPlaceDelay, ConfigurationHandler.propTimeout };
        for (Property prop : sliders) {
            prop.setConfigEntryClass(GuiConfigEntries.NumberSliderEntry.class);
        }

        Hotkeys.initialize(event.getModConfigurationDirectory().toPath());
    }

    @Override
    public void init(FMLInitializationEvent event) {
        MaterialListModel.cachePriority = key -> key instanceof MaterialItemKey
            ? MaterialItemCache.priority((MaterialItemKey) key) : Integer.MAX_VALUE;
        super.init(event);
        FMLCommonHandler.instance().bus().register(
            CommandEditQueue.INSTANCE);
        MinecraftForge.EVENT_BUS.register(CommandEditQueue.INSTANCE);
        FMLCommonHandler.instance().bus().register(
            RemoteEditClient.INSTANCE);
        FMLCommonHandler.instance().bus().register(NbtRequestClient.INSTANCE);
        FMLCommonHandler.instance().bus().register(
            SchematicPreview.INSTANCE);

        FMLCommonHandler.instance()
            .bus()
            .register(TickHandler.INSTANCE);
        FMLCommonHandler.instance()
            .bus()
            .register(RenderTickHandler.INSTANCE);
        FMLCommonHandler.instance()
            .bus()
            .register(ConfigurationHandler.INSTANCE);

        MinecraftForge.EVENT_BUS.register(RendererSchematicGlobal.INSTANCE);
        MinecraftForge.EVENT_BUS.register(ChatEventHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(new OverlayHandler());
        MinecraftForge.EVENT_BUS.register(ContainerVerifier.INSTANCE);
        NeiBridge.register();
        MinecraftForge.EVENT_BUS.register(new ModInfoHandler());
        MinecraftForge.EVENT_BUS.register(WorldHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(SignTextPaste.INSTANCE);
        MinecraftForge.EVENT_BUS.register(DestroyAssist.INSTANCE);
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
        try {
            if (Loader.isModLoaded("lotr")) {
                Reference.logger.info("Lotr mod detected, creating proxy");
                lotrProxy = Class.forName(Reference.LOTR_PROXY)
                    .asSubclass(ILOTRPresent.class)
                    .getDeclaredConstructor()
                    .newInstance();
            } else {
                lotrProxy = new NoLOTRProxy();
            }
        } catch (Exception e) {
            Reference.logger.warn("Failed to create lotr proxy in the normal way");
            lotrProxy = new NoLOTRProxy();
        }
    }

    @Override
    public File getDataDirectory() {
        final File file = MINECRAFT.mcDataDir;
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            Reference.logger.debug("Could not canonize path!", e);
        }
        return file;
    }

    @Override
    public void resetSettings() {
        super.resetSettings();

        ChatEventHandler.INSTANCE.chatLines = 0;

        // A server without Schematica Plus does not restrict the printer; the switch is off after every connection change.
        SchematicPrinter.INSTANCE.setPrinting(false);
        SchematicPrinter.INSTANCE.setEnabled(true);

        WorldHandler.INSTANCE.closeSession();
        NbtRequestClient.INSTANCE.clear();
        clearWorldState();
    }

    public static void clearWorldState() {
        NeiBridge.clear();
        MaterialLists.clear();
        VerificationManager.INSTANCE.clear();
        BlockInfoHud.INSTANCE.clear();
        AreaSelections.clear();
        SchematicProjects.clear();
        RenderLayerSettings.RANGE.load(null);
        Placements.clear();
    }

    @Override
    public boolean isPlayerQuotaExceeded(EntityPlayer player) {
        return false;
    }

    @Override
    public File getPlayerSchematicDirectory(EntityPlayer player, boolean privateDirectory) {
        return ConfigurationHandler.schematicDirectory;
    }
}
