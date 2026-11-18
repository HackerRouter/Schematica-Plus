package com.github.lunatrius.schematica.handler;

import java.io.File;

import net.minecraft.entity.player.EntityPlayer;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.proxy.CommonProxy;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

public class SchematicDirectoryConfigTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private CommonProxy previousProxy;

    @Before public void proxy() {
        previousProxy = SchematicaPlus.proxy;
        File data = temporary.getRoot();
        if (SchematicaPlus.proxy == null) SchematicaPlus.proxy = new CommonProxy() {
            @Override public File getDataDirectory() { return data; }
            @Override public boolean isPlayerQuotaExceeded(EntityPlayer player) { return false; }
            @Override public File getPlayerSchematicDirectory(EntityPlayer player, boolean privateDirectory) { return data; }
        };
    }

    @After public void reset() { SchematicaPlus.proxy = previousProxy; }

    @Test public void siblingDirectoriesSharingThePrefixStayAbsolute() throws Exception {
        File games = temporary.newFolder("games");
        File data = new File(games, "GTNH"), sibling = new File(games, "GTNH-blueprints"), inside = new File(data, "schematics/sub");
        assertEquals(sibling.getCanonicalPath().replace('\\', '/'), ConfigurationHandler.configuredDirectory(sibling, data));
        assertEquals("schematics/sub", ConfigurationHandler.configuredDirectory(inside, data));
    }
}
