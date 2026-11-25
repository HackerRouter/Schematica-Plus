package com.github.lunatrius.schematica.update;

import java.util.Locale;

import com.github.lunatrius.schematica.util.ModVersion;

public final class UpdateRelease {
    public final String tag;
    public final ModVersion version;
    public final String name;
    public final String body;
    public final String htmlUrl;
    public final String assetName;
    public final String downloadUrl;
    public final long assetSize;
    public final String sha256;
    public final boolean prerelease;
    public final String publishedAt;

    public UpdateRelease(String tag, ModVersion version, String name, String body, String htmlUrl,
        String assetName, String downloadUrl, long assetSize, String sha256, boolean prerelease, String publishedAt) {
        this.tag = tag;
        this.version = version;
        this.name = name == null ? "" : name;
        this.body = body == null ? "" : body;
        this.htmlUrl = htmlUrl == null ? "" : htmlUrl;
        this.assetName = assetName;
        this.downloadUrl = downloadUrl;
        this.assetSize = assetSize;
        this.sha256 = sha256;
        this.prerelease = prerelease;
        this.publishedAt = publishedAt == null ? "" : publishedAt;
    }

    public static String formatBytes(long bytes) {
        if (bytes < 0) return "?";
        if (bytes < 1024L) return bytes + " B";
        if (bytes < 1024L * 1024L) return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.ROOT, "%.2f MB", bytes / (1024.0 * 1024.0));
    }
}
