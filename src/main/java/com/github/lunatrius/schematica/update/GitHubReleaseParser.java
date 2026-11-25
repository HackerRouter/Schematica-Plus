package com.github.lunatrius.schematica.update;

import java.util.Locale;

import com.github.lunatrius.schematica.util.ModVersion;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class GitHubReleaseParser {
    public static final String MANIFEST_URL = "https://raw.githubusercontent.com/HackerRouter/Schematica-Plus/update-manifest/updates.json";
    public static final String RELEASES_API_URL = "https://api.github.com/repos/HackerRouter/Schematica-Plus/releases?per_page=20";
    private static final String ASSET_PREFIX = "schematica_plus-";

    private GitHubReleaseParser() {}

    public static UpdateRelease findLatestUpdate(String json, String currentVersionRaw, String channel) {
        ModVersion current = ModVersion.parse(currentVersionRaw);
        if (current == null) return null;
        UpdateRelease latest = findLatestEligibleRelease(json, current, channel);
        if (latest == null) return null;
        return latest.version.compareTo(current) > 0 ? latest : null;
    }

    public static UpdateRelease findLatestEligibleRelease(String json, ModVersion current, String channel) {
        if (json == null || json.trim().isEmpty()) return null;
        JsonElement root = new JsonParser().parse(json);
        JsonArray releases = extractReleasesArray(root);
        if (releases == null) return null;
        boolean includePrereleases = shouldIncludePrereleases(current, channel);
        UpdateRelease best = null;
        for (JsonElement element : releases) {
            if (!element.isJsonObject()) continue;
            UpdateRelease candidate = parseRelease(element.getAsJsonObject(), includePrereleases);
            if (candidate == null) continue;
            if (best == null || candidate.version.compareTo(best.version) > 0) best = candidate;
        }
        return best;
    }

    private static JsonArray extractReleasesArray(JsonElement root) {
        if (root == null) return null;
        if (root.isJsonArray()) return root.getAsJsonArray();
        if (root.isJsonObject()) {
            JsonElement releases = root.getAsJsonObject().get("releases");
            if (releases != null && releases.isJsonArray()) return releases.getAsJsonArray();
        }
        return null;
    }

    static boolean shouldIncludePrereleases(ModVersion current, String channel) {
        if ("prerelease".equalsIgnoreCase(channel)) return true;
        if ("stable".equalsIgnoreCase(channel)) return false;
        return current == null || current.isPrerelease();
    }

    private static UpdateRelease parseRelease(JsonObject obj, boolean includePrereleases) {
        if (boolField(obj, "draft")) return null;
        boolean prerelease = boolField(obj, "prerelease");
        if (prerelease && !includePrereleases) return null;
        String tag = stringField(obj, "tag_name");
        ModVersion version = ModVersion.parse(tag);
        if (version == null) return null;
        JsonElement assetsElement = obj.get("assets");
        if (assetsElement == null || !assetsElement.isJsonArray()) return null;
        JsonObject asset = selectMainJarAsset(assetsElement.getAsJsonArray());
        if (asset == null) return null;
        String assetName = stringField(asset, "name");
        String downloadUrl = stringField(asset, "browser_download_url");
        if (assetName == null || downloadUrl == null || downloadUrl.isEmpty()) return null;
        long assetSize = longField(asset, "size");
        String sha256 = parseSha256Digest(stringField(asset, "digest"));
        return new UpdateRelease(
            tag,
            version,
            stringField(obj, "name"),
            stringField(obj, "body"),
            stringField(obj, "html_url"),
            assetName,
            downloadUrl,
            assetSize,
            sha256,
            prerelease,
            stringField(obj, "published_at"));
    }

    static JsonObject selectMainJarAsset(JsonArray assets) {
        for (JsonElement element : assets) {
            if (!element.isJsonObject()) continue;
            JsonObject asset = element.getAsJsonObject();
            String name = stringField(asset, "name");
            if (!isMainModAssetName(name)) continue;
            String url = stringField(asset, "browser_download_url");
            if (url != null && !url.isEmpty()) return asset;
        }
        return null;
    }

    static boolean isMainModAssetName(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.ROOT);
        if (!lower.startsWith(ASSET_PREFIX) || !lower.endsWith(".jar")) return false;
        return !lower.endsWith("-sources.jar")
            && !lower.endsWith("-dev.jar")
            && !lower.endsWith("-api.jar")
            && !lower.endsWith("-javadoc.jar");
    }

    static String parseSha256Digest(String digest) {
        if (digest == null) return null;
        String trimmed = digest.trim().toLowerCase(Locale.ROOT);
        if (!trimmed.startsWith("sha256:")) return null;
        String hex = trimmed.substring("sha256:".length());
        if (hex.length() != 64) return null;
        for (int i = 0; i < hex.length(); i++) {
            char ch = hex.charAt(i);
            boolean valid = (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f');
            if (!valid) return null;
        }
        return hex;
    }

    private static String stringField(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }

    private static boolean boolField(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        return element != null && element.isJsonPrimitive() && element.getAsBoolean();
    }

    private static long longField(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        if (element == null || !element.isJsonPrimitive()) return -1L;
        try {
            return element.getAsLong();
        } catch (NumberFormatException error) {
            return -1L;
        }
    }
}
