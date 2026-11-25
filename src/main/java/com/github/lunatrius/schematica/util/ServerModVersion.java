package com.github.lunatrius.schematica.util;

import com.github.lunatrius.schematica.SchematicaPlus;

public final class ServerModVersion {

    public static final ServerModVersion INSTANCE = new ServerModVersion();
    private static final ModVersion LEGACY_NO_HELLO_CUTOFF = ModVersion.parse("1.0.0-beta.5");

    private String pendingHandshakeVersion;
    private boolean hasPendingHandshake;
    private Boolean pendingRemoteConnection;
    private boolean pendingLocalConnection;
    private String pendingConnectionVersion;
    private boolean connectedRemote;
    private boolean connectedLocal;
    private String serverVersion;

    public ServerModVersion() {}

    public synchronized void recordHandshake(String version) {
        pendingHandshakeVersion = normalize(version);
        hasPendingHandshake = true;
    }

    public synchronized void onClientConnect(boolean local) {
        pendingRemoteConnection = !local;
        pendingLocalConnection = local;
        pendingConnectionVersion = !local && hasPendingHandshake ? pendingHandshakeVersion : null;
        hasPendingHandshake = false;
        pendingHandshakeVersion = null;
    }

    public synchronized void onClientDisconnect() {
        pendingRemoteConnection = null;
        pendingLocalConnection = false;
        pendingConnectionVersion = null;
        hasPendingHandshake = false;
        pendingHandshakeVersion = null;
    }

    public synchronized void clear() {
        connectedRemote = false;
        connectedLocal = false;
        serverVersion = null;
    }

    public synchronized boolean applyPending() {
        if (pendingRemoteConnection == null) return false;
        connectedRemote = pendingRemoteConnection;
        connectedLocal = pendingLocalConnection;
        serverVersion = pendingConnectionVersion;
        pendingRemoteConnection = null;
        pendingLocalConnection = false;
        pendingConnectionVersion = null;
        return true;
    }

    public synchronized void onCapabilitiesReceived(String version) {
        if (!connectedRemote) return;
        String normalized = normalize(version);
        if (normalized != null) serverVersion = normalized;
    }

    public synchronized boolean isConnectedToRemoteServer() {
        return connectedRemote;
    }

    public synchronized String serverVersion() {
        return serverVersion;
    }

    public synchronized boolean isMismatch(String clientVersion) {
        return connectedRemote && serverVersion != null && !versionsMatch(clientVersion, serverVersion);
    }

    public synchronized boolean isServerProtocolCompatible(String clientVersion) {
        if (!connectedRemote) return true;
        if (serverVersion == null) return false;
        return SchematicaPlus.isClientCompatibleWithServer(serverVersion, clientVersion);
    }

    public synchronized boolean isClientOlderThanServer(String clientVersion) {
        return connectedRemote && serverVersion != null && !SchematicaPlus.isClientCompatibleWithServer(serverVersion, clientVersion);
    }

    public synchronized boolean shouldSendClientHello(String clientVersion) {
        if (connectedLocal) return true;
        if (!connectedRemote || serverVersion == null) return false;
        if (!SchematicaPlus.isClientCompatibleWithServer(serverVersion, clientVersion)) return false;
        ModVersion parsedServer = ModVersion.parse(serverVersion);
        if (parsedServer == null) return versionsMatch(clientVersion, serverVersion);
        return LEGACY_NO_HELLO_CUTOFF == null || parsedServer.compareTo(LEGACY_NO_HELLO_CUTOFF) > 0;
    }

    public static boolean versionsMatch(String clientVersion, String serverVersion) {
        String left = normalize(clientVersion);
        String right = normalize(serverVersion);
        if (left == null || right == null) return false;
        if (left.equals(right)) return true;
        ModVersion parsedLeft = ModVersion.parse(left);
        ModVersion parsedRight = ModVersion.parse(right);
        return parsedLeft != null && parsedRight != null && parsedLeft.compareTo(parsedRight) == 0;
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
