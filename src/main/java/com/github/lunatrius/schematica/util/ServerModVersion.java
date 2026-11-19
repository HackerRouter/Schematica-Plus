package com.github.lunatrius.schematica.util;

public final class ServerModVersion {

    public static final ServerModVersion INSTANCE = new ServerModVersion();

    private String pendingHandshakeVersion;
    private boolean hasPendingHandshake;
    private Boolean pendingRemoteConnection;
    private String pendingConnectionVersion;
    private boolean connectedRemote;
    private String serverVersion;

    public ServerModVersion() {}

    public synchronized void recordHandshake(String version) {
        pendingHandshakeVersion = normalize(version);
        hasPendingHandshake = true;
    }

    public synchronized void onClientConnect(boolean local) {
        pendingRemoteConnection = !local;
        pendingConnectionVersion = !local && hasPendingHandshake ? pendingHandshakeVersion : null;
        hasPendingHandshake = false;
        pendingHandshakeVersion = null;
    }

    public synchronized void onClientDisconnect() {
        pendingRemoteConnection = null;
        pendingConnectionVersion = null;
        hasPendingHandshake = false;
        pendingHandshakeVersion = null;
    }

    public synchronized void clear() {
        connectedRemote = false;
        serverVersion = null;
    }

    public synchronized void applyPending() {
        if (pendingRemoteConnection == null) return;
        connectedRemote = pendingRemoteConnection;
        serverVersion = pendingConnectionVersion;
        pendingRemoteConnection = null;
        pendingConnectionVersion = null;
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
