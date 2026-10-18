package com.github.lunatrius.schematica.proxy;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.reference.Reference;

public class ServerProxy extends CommonProxy {

    @Override
    public boolean dedicatedServer() {
        return true;
    }

    @Override
    public File cloudDirectory() {
        return ConfigurationHandler.schematicDirectory;
    }

    @Override
    public File getDataDirectory() {
        final File file = MinecraftServer.getServer()
            .getFile(".");
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            Reference.logger.info("Could not canonize path!", e);
        }
        return file;
    }

    @Override
    public boolean isPlayerQuotaExceeded(EntityPlayer player) {
        long spaceUsed = 0;

        // Space used by private directory
        File schematicDirectory = getPlayerSchematicDirectory(player, true);
        spaceUsed += getSpaceUsedByDirectory(schematicDirectory);

        // Space used by public directory
        schematicDirectory = getPlayerSchematicDirectory(player, false);
        spaceUsed += getSpaceUsedByDirectory(schematicDirectory);
        return ((spaceUsed / 1024) > ConfigurationHandler.playerQuotaKilobytes);
    }

    private long getSpaceUsedByDirectory(File directory) {
        long spaceUsed = 0;
        // If we don't have a player directory yet, then they haven't uploaded any files yet.
        if (directory == null || !directory.exists()) {
            return 0;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            files = new File[0];
        }
        for (File path : files) {
            spaceUsed += path.length();
        }
        return spaceUsed;
    }

    @Override
    public File getPlayerSchematicDirectory(EntityPlayer player, boolean privateDirectory) {
        final UUID playerId = player.getUniqueID();
        if (playerId == null) {
            Reference.logger.warn("Unable to identify player {}", player.toString());
            return null;
        }

        File playerDir = new File(ConfigurationHandler.schematicDirectory.getAbsolutePath(), playerId.toString());
        if (privateDirectory) {
            return new File(playerDir, "private");
        } else {
            return new File(playerDir, "public");
        }
    }
}
