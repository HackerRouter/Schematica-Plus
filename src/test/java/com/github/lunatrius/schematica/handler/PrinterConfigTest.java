package com.github.lunatrius.schematica.handler;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import org.junit.Test;
import static org.junit.Assert.*;

public class PrinterConfigTest {
    @Test public void oldDecimalRangesBecomeIntegerSlidersWithoutLosingTheSetting() {
        Configuration config = new Configuration();
        config.get("printer", "printerWorkRange", 23.6);
        ConfigReader reader = new ConfigReader(config, "printer", ConfigReader.LEGACY);
        assertEquals(24, reader.integer("printerWorkRange", 0, 0, 128));
        Property property = config.getCategory("printer").get("printerWorkRange");
        assertEquals(Property.Type.INTEGER, property.getType());
        assertEquals("24", property.getString());
        assertEquals("0", property.getDefault());
        assertEquals("0", property.getMinValue());
        assertEquals("128", property.getMaxValue());
        assertEquals(24, reader.integer("printerWorkRange", 0, 0, 128));
    }

    @Test public void obsoleteProtocolVersionsMigrateToAutomaticAssistance() {
        for (String old : new String[] {"v2", "v3"}) {
            Configuration config = new Configuration();
            config.get("tool", "easyPlaceProtocolVersion", old);
            ConfigReader reader = new ConfigReader(config, "tool", ConfigReader.GENERIC);
            assertEquals("auto", reader.choice("easyPlaceProtocolVersion", "auto", "auto", "slabs_only", "none"));
            assertEquals("auto", config.getCategory("tool").get("easyPlaceProtocolVersion").getString());
        }
    }

    @Test public void decimalRangeMigrationClampsCorruptValues() {
        Configuration config = new Configuration();
        ConfigReader reader = new ConfigReader(config, "server", ConfigReader.LEGACY);
        config.get("server", "printerMaxRange", 999.0);
        assertEquals(128, reader.integer("printerMaxRange", 128, 0, 128));
        config.getCategory("server").remove("printerMaxRange");
        config.get("server", "printerMaxRange", Double.NaN);
        assertEquals(128, reader.integer("printerMaxRange", 128, 0, 128));
    }
}
