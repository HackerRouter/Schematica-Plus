package com.github.lunatrius.schematica;

import java.util.Map;

import com.github.lunatrius.schematica.proxy.CommonProxy;
import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.common.versioning.ArtifactVersion;
import cpw.mods.fml.common.versioning.DefaultArtifactVersion;
import cpw.mods.fml.relauncher.Side;

@Mod(
    modid = Reference.MODID,
    name = Reference.NAME,
    version = Reference.VERSION,
    dependencies = Reference.DEPENDENCIES,
    guiFactory = Reference.GUI_FACTORY)
public class Schematica {

    @Instance(Reference.MODID)
    public static Schematica instance;

    @SidedProxy(serverSide = Reference.PROXY_SERVER, clientSide = Reference.PROXY_CLIENT)
    public static CommonProxy proxy;

    private final ArtifactVersion minimumClientJoinVersion = new DefaultArtifactVersion("1.11.0");

    /**
     * Block any clients older than 1.11.0 to ensure the server-client settings are respected
     */
    @SuppressWarnings("unused")
    @NetworkCheckHandler
    public boolean checkModList(Map<String, String> versions, Side side) {
        if (side == Side.CLIENT && versions.containsKey(Reference.MODID)) {
            return minimumClientJoinVersion.compareTo(new DefaultArtifactVersion(versions.get(Reference.MODID))) <= 0;
        }
        return true;
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
    public void serverStopped(cpw.mods.fml.common.event.FMLServerStoppedEvent event) {
        com.github.lunatrius.schematica.handler.WorldEditQueue.INSTANCE.clear();
        com.github.lunatrius.schematica.handler.QueueTickHandler.INSTANCE.clear();
        com.github.lunatrius.schematica.handler.DownloadHandler.INSTANCE.transferMap.clear();
    }
}
