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
            @Override public boolean loadSchematic(EntityPlayer player, File directory, String filename) { return false; }
            @Override public boolean isPlayerQuotaExceeded(EntityPlayer player) { return false; }
            @Override public File getPlayerSchematicDirectory(EntityPlayer player, boolean privateDirectory) { return data; }
        };
    }

    @After public void reset() { SchematicaPlus.proxy = previousProxy; }

    private static Configuration legacy(Boolean onlyAir) {
        Configuration config = new Configuration();
        if (onlyAir != null) config.get("tool", "pasteOnlyAir", false).set(onlyAir);
        return config;
    }

    @Test public void freshConfigUsesTheLitematicaDefault() {
        Configuration config = legacy(null);
        assertEquals(ReplaceBehavior.NONE, ConfigurationHandler.loadPasteReplaceBehavior(config));
        assertEquals("none", config.getCategory("tool").get("pasteReplaceBehavior").getString());
    }

    @Test public void legacyOnlyAirSettingsMigrateOnce() {
        Configuration onlyAir = legacy(true);
        assertEquals(ReplaceBehavior.NONE, ConfigurationHandler.loadPasteReplaceBehavior(onlyAir));
        assertFalse(onlyAir.getCategory("tool").containsKey("pasteOnlyAir"));
        Configuration replacing = legacy(false);
        assertEquals(ReplaceBehavior.WITH_NON_AIR, ConfigurationHandler.loadPasteReplaceBehavior(replacing));
        replacing.getCategory("tool").get("pasteReplaceBehavior").set("all");
        replacing.get("tool", "pasteOnlyAir", true);
        assertEquals(ReplaceBehavior.ALL, ConfigurationHandler.loadPasteReplaceBehavior(replacing));
        assertFalse(replacing.getCategory("tool").containsKey("pasteOnlyAir"));
    }
}
