package com.github.lunatrius.schematica.handler;

import java.io.File;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.config.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.proxy.CommonProxy;
import com.github.lunatrius.schematica.tool.ReplaceBehavior;

public class PasteReplaceBehaviorConfigTest {
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

    @Test public void freshConfigUsesTheLitematicaDefault() {
        Configuration config = new Configuration();
        assertEquals(ReplaceBehavior.NONE, ConfigurationHandler.loadPasteReplaceBehavior(config));
        assertEquals("none", config.getCategory("tool").get("pasteReplaceBehavior").getString());
    }
}
