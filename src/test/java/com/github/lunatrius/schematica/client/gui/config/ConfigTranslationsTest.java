package com.github.lunatrius.schematica.client.gui.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.StringTranslate;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

import net.minecraft.entity.player.EntityPlayer;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.proxy.CommonProxy;

public class ConfigTranslationsTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private java.lang.reflect.Field minecraftHome;
    private Object previousHome;
    private Configuration previous;
    private CommonProxy previousProxy;

    @Before public void initializeForgeDirectory() throws Exception {
        minecraftHome = cpw.mods.fml.relauncher.FMLInjectionData.class.getDeclaredField("minecraftHome");
        minecraftHome.setAccessible(true);
        previousHome = minecraftHome.get(null);
        minecraftHome.set(null, temporary.getRoot());
        previousProxy = SchematicaPlus.proxy;
        File data = temporary.getRoot();
        if (SchematicaPlus.proxy == null) SchematicaPlus.proxy = new CommonProxy() {
            @Override public File getDataDirectory() { return data; }
            @Override public boolean loadSchematic(EntityPlayer player, File directory, String filename) { return false; }
            @Override public boolean isPlayerQuotaExceeded(EntityPlayer player) { return false; }
            @Override public File getPlayerSchematicDirectory(EntityPlayer player, boolean privateDirectory) { return data; }
        };
        previous = ConfigurationHandler.configuration;
    }

    @After public void reset() throws Exception {
        ConfigurationHandler.configuration = previous;
        SchematicaPlus.proxy = previousProxy;
        minecraftHome.set(null, previousHome);
    }

    private Map<String, String> catalog(String language) throws IOException {
        Map<String, String> result = new HashMap<>();
        for (String domain : new String[] {"schematica_plus_litematica", "schematica"}) {
            try (InputStream stream = getClass().getResourceAsStream("/assets/" + domain + "/lang/" + language + ".lang")) {
                if (stream != null) result.putAll(StringTranslate.parseLangFile(stream));
            }
        }
        return result;
    }

    @Test public void everyConfigLabelAndCommentIsTranslated() throws Exception {
        ConfigurationHandler.configuration = new Configuration(new File(temporary.getRoot(), "schematica_plus.cfg"));
        ConfigurationHandler.loadConfiguration();
        for (String language : new String[] {"en_US", "zh_CN"}) {
            Map<String, String> translations = catalog(language);
            for (String name : ConfigurationHandler.configuration.getCategoryNames()) {
                ConfigCategory category = ConfigurationHandler.configuration.getCategory(name);
                for (Property property : category.values()) {
                    if (!property.showInGui()) continue;
                    String key = property.getLanguageKey();
                    assertTrue(language + ": " + key, translations.containsKey(ConfigTranslations.label(key)));
                    assertTrue(language + ": " + key, translations.containsKey(ConfigTranslations.comment(key)));
                }
            }
        }
    }
}
