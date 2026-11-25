package com.github.lunatrius.schematica.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ServerModVersionTest {

    @Test public void tracksHandshakeCapabilitiesAndDisconnectLifecycle() {
        ServerModVersion state = new ServerModVersion();
        assertFalse(state.isConnectedToRemoteServer());
        assertNull(state.serverVersion());

        state.recordHandshake("1.0.0-beta.4");
        state.onClientConnect(false);
        state.clear();
        assertTrue(state.applyPending());
        assertTrue(state.isConnectedToRemoteServer());
        assertEquals("1.0.0-beta.4", state.serverVersion());
        assertTrue(state.isMismatch("1.0.0-beta.5"));
        assertTrue(state.isServerProtocolCompatible("1.0.0-beta.5"));
        assertFalse(state.isClientOlderThanServer("1.0.0-beta.5"));
        assertFalse(state.shouldSendClientHello("1.0.0-beta.5"));

        state.onCapabilitiesReceived("1.0.0-beta.6");
        assertEquals("1.0.0-beta.6", state.serverVersion());
        assertFalse(state.isServerProtocolCompatible("1.0.0-beta.5"));
        assertTrue(state.isClientOlderThanServer("1.0.0-beta.5"));
        assertFalse(state.shouldSendClientHello("1.0.0-beta.5"));
        assertTrue(state.shouldSendClientHello("1.0.0-beta.6"));
        assertTrue(state.shouldSendClientHello("1.0.0"));

        state.onClientDisconnect();
        state.clear();
        assertFalse(state.applyPending());
        assertFalse(state.isConnectedToRemoteServer());
        assertNull(state.serverVersion());
        assertFalse(state.isMismatch("1.0.0-beta.5"));
    }

    @Test public void distinguishesSingleplayerAndVanillaRemoteServers() {
        ServerModVersion state = new ServerModVersion();

        state.recordHandshake("1.0.0-beta.5");
        state.onClientConnect(true);
        state.clear();
        assertTrue(state.applyPending());
        assertFalse(state.isConnectedToRemoteServer());
        assertNull(state.serverVersion());
        assertTrue(state.isServerProtocolCompatible("1.0.0-beta.5"));
        assertTrue(state.shouldSendClientHello("1.0.0-beta.5"));

        state.recordHandshake(null);
        state.onClientConnect(false);
        state.clear();
        assertTrue(state.applyPending());
        assertTrue(state.isConnectedToRemoteServer());
        assertNull(state.serverVersion());
        assertFalse(state.isMismatch("1.0.0-beta.5"));
        assertFalse(state.isServerProtocolCompatible("1.0.0-beta.5"));
        assertFalse(state.shouldSendClientHello("1.0.0-beta.5"));
    }

    @Test public void comparesSemanticAndRawVersionsForEquality() {
        assertTrue(ServerModVersion.versionsMatch("1.0.0-beta.5", "1.0.0-beta.5"));
        assertTrue(ServerModVersion.versionsMatch("v1.0.0-beta.5+abc", "1.0.0-beta.5+def"));
        assertFalse(ServerModVersion.versionsMatch("1.0.0-beta.5", "1.0.0-beta.4"));
        assertFalse(ServerModVersion.versionsMatch("1.0.0-beta.5-master.10+abc", "1.0.0-beta.5"));
        assertTrue(ServerModVersion.versionsMatch("db05cb5", "db05cb5"));
        assertFalse(ServerModVersion.versionsMatch("db05cb5", "1.0.0-beta.5"));
    }
}
