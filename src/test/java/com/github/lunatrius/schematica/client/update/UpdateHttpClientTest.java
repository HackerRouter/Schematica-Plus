package com.github.lunatrius.schematica.client.update;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class UpdateHttpClientTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private HttpServer server;
    private byte[] validJarBytes;
    private String validSha256;

    @Before public void startServer() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("mcmod.info"));
            zip.write("{\"modid\":\"schematica_plus\"}".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        validJarBytes = out.toByteArray();
        validSha256 = sha256(validJarBytes);

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "/download");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/download", exchange -> {
            exchange.sendResponseHeaders(200, validJarBytes.length);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write(validJarBytes);
            }
        });
        server.createContext("/corrupt", exchange -> {
            byte[] bad = "corrupt-data".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bad.length);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write(bad);
            }
        });
        server.createContext("/etag", exchange -> {
            String etag = "\"v1\"";
            if (etag.equals(exchange.getRequestHeaders().getFirst("If-None-Match"))) {
                exchange.getResponseHeaders().add("ETag", etag);
                exchange.sendResponseHeaders(304, -1);
                exchange.close();
                return;
            }
            byte[] payload = "{\"releases\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("ETag", etag);
            exchange.sendResponseHeaders(200, payload.length);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write(payload);
            }
        });
        server.createContext("/ratelimit", exchange -> {
            long reset = System.currentTimeMillis() / 1000L + 600L;
            exchange.getResponseHeaders().add("X-RateLimit-Remaining", "0");
            exchange.getResponseHeaders().add("X-RateLimit-Reset", Long.toString(reset));
            exchange.sendResponseHeaders(403, -1);
            exchange.close();
        });
        server.createContext("/missing", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
    }

    @After public void stopServer() {
        if (server != null) server.stop(0);
    }

    private String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    private static String sha256(byte[] data) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
        StringBuilder builder = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            builder.append(Character.forDigit((value >> 4) & 0xF, 16));
            builder.append(Character.forDigit(value & 0xF, 16));
        }
        return builder.toString();
    }

    @Test public void followsRedirectAndVerifiesJarAndSha256() throws Exception {
        File destination = new File(temporary.getRoot(), "schematica_plus-1.0.0-beta.6.jar");
        AtomicLong lastProgress = new AtomicLong();
        AtomicBoolean sawVerify = new AtomicBoolean();
        UpdateHttpClient.downloadVerifiedJar(url("/redirect"), destination, validJarBytes.length, validSha256,
            new AtomicBoolean(false), (done, total, verifying) -> {
                lastProgress.set(done);
                if (verifying) sawVerify.set(true);
            });
        assertTrue(destination.isFile());
        assertArrayEquals(validJarBytes, Files.readAllBytes(destination.toPath()));
        assertEquals(validJarBytes.length, lastProgress.get());
        assertTrue(sawVerify.get());
    }

    @Test public void rejectsHashMismatchAndCleansPartFile() {
        File destination = new File(temporary.getRoot(), "bad-hash.jar");
        try {
            UpdateHttpClient.downloadVerifiedJar(url("/download"), destination, validJarBytes.length,
                "0000000000000000000000000000000000000000000000000000000000000000", new AtomicBoolean(false), null);
            fail("Expected SHA-256 mismatch error");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("SHA-256"));
        }
        assertFalse(destination.exists());
        assertFalse(new File(temporary.getRoot(), "bad-hash.jar.part").exists());
    }

    @Test public void rejectsCorruptArchiveAndCleansPartFile() {
        File destination = new File(temporary.getRoot(), "corrupt.jar");
        try {
            UpdateHttpClient.downloadVerifiedJar(url("/corrupt"), destination, -1L, null, new AtomicBoolean(false), null);
            fail("Expected invalid archive error");
        } catch (IOException expected) {
            assertFalse(destination.exists());
            assertFalse(new File(temporary.getRoot(), "corrupt.jar.part").exists());
        }
    }

    @Test public void cancelsCleanlyAndDeletesPartFile() throws Exception {
        File destination = new File(temporary.getRoot(), "cancelled.jar");
        AtomicBoolean cancelled = new AtomicBoolean(true);
        try {
            UpdateHttpClient.downloadVerifiedJar(url("/download"), destination, validJarBytes.length, validSha256, cancelled, null);
            fail("Expected cancellation");
        } catch (CancellationException expected) {
            assertFalse(destination.exists());
            assertFalse(new File(temporary.getRoot(), "cancelled.jar.part").exists());
        }
    }

    @Test public void reusesCachedBodyOnEtagNotModifiedAndFallsBackWhenManifestMissing() throws Exception {
        UpdateHttpClient.CachedResponse first = UpdateHttpClient.fetchTextCached(url("/etag"), "application/json", null, null);
        assertFalse(first.notModified);
        assertEquals("\"v1\"", first.etag);
        assertEquals("{\"releases\":[]}", first.body);

        UpdateHttpClient.CachedResponse second = UpdateHttpClient.fetchTextCached(url("/etag"), "application/json", first.etag, first.body);
        assertTrue(second.notModified);
        assertEquals("{\"releases\":[]}", second.body);

        UpdateManager.ManifestWithFallbackTransport transport =
            new UpdateManager.ManifestWithFallbackTransport(url("/missing"), url("/etag"));
        assertEquals("{\"releases\":[]}", transport.fetchReleasesJson());
        assertEquals("{\"releases\":[]}", transport.fetchReleasesJson());
    }

    @Test public void formatsGitHubRateLimitErrorMessage() {
        try {
            UpdateHttpClient.fetchText(url("/ratelimit"));
            fail("Expected rate limit IOException");
        } catch (IOException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("rate limit exceeded"));
            assertTrue(expected.getMessage(), expected.getMessage().contains("minute"));
        }
    }
}
