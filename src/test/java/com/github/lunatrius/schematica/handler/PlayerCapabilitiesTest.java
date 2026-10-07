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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

    private MessageCapabilities advertised(boolean enabled) throws Exception {
        boolean previous = ConfigurationHandler.remoteEditsEnabled;
        ByteBuf wire = Unpooled.buffer();
        try {
            ConfigurationHandler.remoteEditsEnabled = enabled;
            Method factory = PlayerHandler.class.getDeclaredMethod("capabilities");
            factory.setAccessible(true);
            ((MessageCapabilities) factory.invoke(null)).toBytes(wire);
            MessageCapabilities received = new MessageCapabilities();
            received.fromBytes(wire);
            assertEquals(0, wire.readableBytes());
            return received;
        } finally {
            wire.release();
            ConfigurationHandler.remoteEditsEnabled = previous;
        }
    }

    @Test public void disabledEditsStillAdvertiseProtocolSoRequestsReachServerRejection() throws Exception {
        assertTrue(advertised(false).supportsRemoteEdit);
    }

    @Test public void enabledEditsAdvertiseProtocol() throws Exception {
        assertTrue(advertised(true).supportsRemoteEdit);
    }

    @Test public void legacyServersWithoutTheProtocolKeepCommandFallback() {
        for (boolean worldMoveField : new boolean[] {false, true}) {
            ByteBuf wire = Unpooled.buffer();
            try {
                wire.writeBoolean(true);
                wire.writeBoolean(true);
                wire.writeBoolean(true);
                if (worldMoveField) wire.writeBoolean(true);
                MessageCapabilities received = new MessageCapabilities();
                received.fromBytes(wire);
                assertTrue(received.isPrinterEnabled);
                assertTrue(received.isSaveEnabled);
                assertTrue(received.isLoadEnabled);
                assertEquals(worldMoveField, received.supportsWorldMove);
                assertFalse(received.supportsRemoteEdit);
                assertFalse(received.supportsAccuratePlacement);
                assertEquals(0, wire.readableBytes());
            } finally {
                wire.release();
            }
        }
    }
}
