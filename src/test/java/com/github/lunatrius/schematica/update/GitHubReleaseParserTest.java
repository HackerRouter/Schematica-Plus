package com.github.lunatrius.schematica.update;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class GitHubReleaseParserTest {

    private static final String SHA_BETA5 = "ec81bef46d2452b88889446acbe3dc1d8f6ca812a1b8e135b621edd71805dee4";
    private static final String SHA_BETA6 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private static final String SAMPLE_RELEASES = "["
        + "{"
        + "  \"tag_name\": \"1.0.0-beta.6\","
        + "  \"name\": \"1.0.0-beta.6\","
        + "  \"body\": \"Beta 6 changelog\","
        + "  \"html_url\": \"https://example.invalid/releases/beta6\","
        + "  \"draft\": false,"
        + "  \"prerelease\": true,"
        + "  \"published_at\": \"2026-11-20T00:00:00Z\","
        + "  \"assets\": ["
        + "    {\"name\": \"schematica_plus-1.0.0-beta.6-sources.jar\", \"size\": 100, \"browser_download_url\": \"https://example.invalid/src.jar\"},"
        + "    {\"name\": \"schematica_plus-1.0.0-beta.6.jar\", \"size\": 2660000, \"digest\": \"sha256:" + SHA_BETA6 + "\", \"browser_download_url\": \"https://example.invalid/beta6.jar\"}"
        + "  ]"
        + "},"
        + "{"
        + "  \"tag_name\": \"1.0.0-beta.5\","
        + "  \"name\": \"1.0.0-beta.5\","
        + "  \"body\": \"Beta 5 changelog\","
        + "  \"html_url\": \"https://example.invalid/releases/beta5\","
        + "  \"draft\": false,"
        + "  \"prerelease\": false,"
        + "  \"published_at\": \"2026-11-18T05:14:04Z\","
        + "  \"assets\": ["
        + "    {\"name\": \"schematica_plus-1.0.0-beta.5.jar\", \"size\": 2653089, \"digest\": \"sha256:" + SHA_BETA5 + "\", \"browser_download_url\": \"https://example.invalid/beta5.jar\"}"
        + "  ]"
        + "},"
        + "{"
        + "  \"tag_name\": \"2.0.0-alpha.5\","
        + "  \"name\": \"2.0.0-alpha.5\","
        + "  \"body\": \"Legacy Schematica\","
        + "  \"html_url\": \"https://example.invalid/releases/legacy\","
        + "  \"draft\": false,"
        + "  \"prerelease\": true,"
        + "  \"published_at\": \"2026-03-29T20:02:41Z\","
        + "  \"assets\": ["
        + "    {\"name\": \"Schematica-2.0.0-alpha.5-3003-0402.jar\", \"size\": 387359, \"browser_download_url\": \"https://example.invalid/legacy.jar\"}"
        + "  ]"
        + "},"
        + "{"
        + "  \"tag_name\": \"9.9.9\","
        + "  \"name\": \"Draft\","
        + "  \"draft\": true,"
        + "  \"prerelease\": false,"
        + "  \"assets\": ["
        + "    {\"name\": \"schematica_plus-9.9.9.jar\", \"size\": 1000, \"browser_download_url\": \"https://example.invalid/draft.jar\"}"
        + "  ]"
        + "}"
        + "]";

    @Test public void ignoresLegacySchematicaReleasesAndDrafts() {
        UpdateRelease release = GitHubReleaseParser.findLatestUpdate(SAMPLE_RELEASES, "1.0.0-beta.4", "auto");
        assertNotNull(release);
        assertEquals("1.0.0-beta.6", release.tag);
        assertEquals("schematica_plus-1.0.0-beta.6.jar", release.assetName);
        assertEquals(SHA_BETA6, release.sha256);
        assertEquals(2660000L, release.assetSize);
    }

    @Test public void respectsUpdateChannelSelection() {
        UpdateRelease stable = GitHubReleaseParser.findLatestUpdate(SAMPLE_RELEASES, "1.0.0-beta.4", "stable");
        assertNotNull(stable);
        assertEquals("1.0.0-beta.5", stable.tag);
        assertEquals(SHA_BETA5, stable.sha256);

        assertNull(GitHubReleaseParser.findLatestUpdate(SAMPLE_RELEASES, "1.0.0", "auto"));

        String withNextPre = "[{\"tag_name\":\"1.1.0-beta.1\",\"draft\":false,\"prerelease\":true,"
            + "\"assets\":[{\"name\":\"schematica_plus-1.1.0-beta.1.jar\",\"size\":10,\"browser_download_url\":\"https://example.invalid/11b1.jar\"}]}]";
        assertNull(GitHubReleaseParser.findLatestUpdate(withNextPre, "1.0.0", "auto"));
        UpdateRelease optedIn = GitHubReleaseParser.findLatestUpdate(withNextPre, "1.0.0", "prerelease");
        assertNotNull(optedIn);
        assertEquals("1.1.0-beta.1", optedIn.tag);
    }

    @Test public void handlesMasterDevBuildAndUnparseableVersions() {
        UpdateRelease fromMaster = GitHubReleaseParser.findLatestUpdate(SAMPLE_RELEASES, "1.0.0-beta.5-master.342+8875ebf3d2", "stable");
        assertNull(fromMaster);

        UpdateRelease nextBeta = GitHubReleaseParser.findLatestUpdate(SAMPLE_RELEASES, "1.0.0-beta.5-master.342+8875ebf3d2", "auto");
        assertNotNull(nextBeta);
        assertEquals("1.0.0-beta.6", nextBeta.tag);

        assertNull(GitHubReleaseParser.findLatestUpdate(SAMPLE_RELEASES, "db05cb5", "auto"));
    }

    @Test public void filtersAuxiliaryJarArtifacts() {
        assertEquals(false, GitHubReleaseParser.isMainModAssetName("schematica_plus-1.0.0-sources.jar"));
        assertEquals(false, GitHubReleaseParser.isMainModAssetName("schematica_plus-1.0.0-dev.jar"));
        assertEquals(false, GitHubReleaseParser.isMainModAssetName("schematica_plus-1.0.0-api.jar"));
        assertEquals(false, GitHubReleaseParser.isMainModAssetName("schematica_plus-1.0.0-javadoc.jar"));
        assertEquals(false, GitHubReleaseParser.isMainModAssetName("Schematica-2.0.0-alpha.5.jar"));
        assertEquals(true, GitHubReleaseParser.isMainModAssetName("schematica_plus-1.0.0-beta.5.jar"));
    }

    @Test public void parsesStaticUpdatesManifestObject() {
        String manifest = "{\"schemaVersion\":1,\"generatedAt\":\"2026-11-19T00:00:00Z\",\"releases\":" + SAMPLE_RELEASES + "}";
        UpdateRelease release = GitHubReleaseParser.findLatestUpdate(manifest, "1.0.0-beta.4", "stable");
        assertNotNull(release);
        assertEquals("1.0.0-beta.5", release.tag);
        assertEquals(SHA_BETA5, release.sha256);
    }
}
