package com.github.lunatrius.schematica.handler;

import java.util.UUID;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ServerClientsTest {

    @Test public void marksOnlyEqualOrNewerClientsAsCompatible() {
        ServerClients clients = new ServerClients();
        UUID newer = UUID.randomUUID();
        UUID equal = UUID.randomUUID();
        UUID older = UUID.randomUUID();

        assertTrue(clients.register(newer, "NewerPlayer", "1.0.0-beta.5", "1.0.0"));
        assertTrue(clients.isCompatible(newer));
        assertEquals("1.0.0", clients.version(newer));

        assertTrue(clients.register(equal, "EqualPlayer", "1.0.0-beta.5", "1.0.0-beta.5"));
        assertTrue(clients.isCompatible(equal));

        assertFalse(clients.register(older, "OlderPlayer", "1.0.0-beta.5", "1.0.0-beta.4"));
        assertFalse(clients.isCompatible(older));
        assertNull(clients.version(older));

        assertFalse(clients.register(newer, "NewerPlayer", "1.0.0", "1.0.0-beta.5"));
        assertFalse(clients.isCompatible(newer));

        clients.clear();
        assertFalse(clients.isCompatible(equal));
    }
}
