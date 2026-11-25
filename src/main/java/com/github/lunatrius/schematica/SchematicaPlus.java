package com.github.lunatrius.schematica;

import java.util.Map;

import net.minecraft.server.MinecraftServer;

import com.github.lunatrius.schematica.client.gui.SchematicaPlusConflictException;
import com.github.lunatrius.schematica.handler.DownloadHandler;
import com.github.lunatrius.schematica.handler.PlayerHandler;
import com.github.lunatrius.schematica.handler.QueueTickHandler;
import com.github.lunatrius.schematica.handler.RemoteEdits;
import com.github.lunatrius.schematica.handler.ServerClients;
import com.github.lunatrius.schematica.handler.SharedPlacements;
import com.github.lunatrius.schematica.handler.WorldEditQueue;
import com.github.lunatrius.schematica.proxy.CommonProxy;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.ModVersion;
import com.github.lunatrius.schematica.util.ServerModVersion;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderException;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Mod(
    modid = Reference.MODID,
    name = Reference.NAME,
    version = Reference.VERSION,
    dependencies = Reference.DEPENDENCIES,
    guiFactory = Reference.GUI_FACTORY)
public class SchematicaPlus {

    @Instance(Reference.MODID)
    public static SchematicaPlus instance;

    @SidedProxy(serverSide = Reference.PROXY_SERVER, clientSide = Reference.PROXY_CLIENT)
    public static CommonProxy proxy;

    public SchematicaPlus() {
        String conflict = findLegacyMod(Loader.instance().getIndexedModList().keySet());
        if (conflict != null) {
            String message = "Schematica Plus (schematica_plus) cannot load alongside " + conflict
                + ". Remove the original/GTNH Schematica jar, or remove Schematica Plus.";
            if (FMLCommonHandler.instance().getSide().isClient()) {
                showConflict(message, conflict);
            }
            throw new LoaderException(message);
        }
    }

    @SideOnly(Side.CLIENT)
    private static void showConflict(String message, String conflict) {
        throw new SchematicaPlusConflictException(message, conflict);
    }

    static String findLegacyMod(Iterable<String> modIds) {
        for (String modId : modIds) {
            if ("schematica".equalsIgnoreCase(modId)) return modId;
        }
        return null;
    }

    @SuppressWarnings("unused")
    @NetworkCheckHandler
    public boolean checkModList(Map<String, String> versions, Side side) {
        if (side == Side.SERVER && isClientHandshake()) {
            ServerModVersion.INSTANCE.recordHandshake(versions.get(Reference.MODID));
        }
        return acceptsRemoteMods(Reference.VERSION, versions, side);
    }

    private static boolean isClientHandshake() {
        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            if (frame.getClassName().contains(".FMLHandshakeClientState")) return true;
        }
        return false;
    }

    static boolean acceptsRemoteMods(String localVersion, Map<String, String> versions, Side remoteSide) {
        return true;
    }

    public static boolean isClientCompatibleWithServer(String serverVersion, String clientVersion) {
        if (serverVersion == null || clientVersion == null) return false;
        String server = serverVersion.trim();
        String client = clientVersion.trim();
        if (server.isEmpty() || client.isEmpty()) return false;
        if (server.equals(client)) return true;
        ModVersion parsedServer = ModVersion.parse(server);
        ModVersion parsedClient = ModVersion.parse(client);
        if (parsedServer == null || parsedClient == null) return false;
        return parsedClient.compareTo(parsedServer) >= 0;
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        WorldEditQueue.INSTANCE.rollbackBeforeShutdown(MinecraftServer.getServer());
    }

    @EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        RemoteEdits.INSTANCE.clear();
        WorldEditQueue.INSTANCE.clear();
        QueueTickHandler.INSTANCE.clear();
        DownloadHandler.INSTANCE.stopAll();
        SharedPlacements.INSTANCE.clear();
        ServerClients.INSTANCE.clear();
        PlayerHandler.INSTANCE.clear();
    }
}
