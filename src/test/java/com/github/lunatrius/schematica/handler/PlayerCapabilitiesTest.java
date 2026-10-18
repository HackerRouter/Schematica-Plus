// Server capability policy checks, by HackerRouter, 2026.
package com.github.lunatrius.schematica.handler;

import java.io.File;
import java.lang.reflect.Method;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.network.message.MessageCapabilities;
import com.github.lunatrius.schematica.proxy.CommonProxy;
import com.github.lunatrius.schematica.proxy.ServerProxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

public class PlayerCapabilitiesTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private CommonProxy previousProxy;

    @Before public void proxy() {
        previousProxy = SchematicaPlus.proxy;
        if (SchematicaPlus.proxy == null) SchematicaPlus.proxy = new ServerProxy() {
            @Override public File getDataDirectory() { return temporary.getRoot(); }
        };
    }

    @After public void reset() { SchematicaPlus.proxy = previousProxy; }

    private MessageCapabilities advertised() throws Exception {
        ByteBuf wire = Unpooled.buffer();
        try {
            Method factory = PlayerHandler.class.getDeclaredMethod("capabilities");
            factory.setAccessible(true);
            ((MessageCapabilities) factory.invoke(null)).toBytes(wire);
            MessageCapabilities received = new MessageCapabilities();
            received.fromBytes(wire);
            assertEquals(0, wire.readableBytes());
            return received;
        } finally {
            wire.release();
        }
    }

    @Test public void printerLimitTravelsWithCapabilities() throws Exception {
        double previous = ConfigurationHandler.printerMaxRange;
        boolean enabled = ConfigurationHandler.printerEnabled;
        try {
            ConfigurationHandler.printerEnabled = true;
            ConfigurationHandler.printerMaxRange = 24;
            assertEquals(24, advertised().printerMaxRange, 0);
            ConfigurationHandler.printerEnabled = false;
            assertEquals(0, advertised().printerMaxRange, 0);
        } finally {
            ConfigurationHandler.printerMaxRange = previous;
            ConfigurationHandler.printerEnabled = enabled;
        }
    }
}
