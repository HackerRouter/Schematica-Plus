package com.github.lunatrius.schematica.client.update;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipFile;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import com.github.lunatrius.schematica.reference.Reference;

public final class UpdateHttpClient {

    public interface ProgressListener {
        void onProgress(long downloadedBytes, long totalBytes, boolean verifying);
    }

    public static final class CachedResponse {
        public final String body;
        public final String etag;
        public final boolean notModified;

        public CachedResponse(String body, String etag, boolean notModified) {
            this.body = body;
            this.etag = etag;
            this.notModified = notModified;
        }
    }

    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int API_READ_TIMEOUT_MS = 20000;
    private static final int DOWNLOAD_READ_TIMEOUT_MS = 30000;
    private static final int MAX_REDIRECTS = 5;
    private static final int MAX_TEXT_BYTES = 2 * 1024 * 1024;
    private static final SSLSocketFactory TLS_SOCKET_FACTORY = createTlsSocketFactory();

    private UpdateHttpClient() {}

    public static String fetchText(String url) throws IOException {
        return fetchTextCached(url, "application/vnd.github+json", null, null).body;
    }

    public static CachedResponse fetchTextCached(String url, String accept, String cachedEtag, String cachedBody) throws IOException {
        HttpURLConnection connection = openFollowingRedirects(url, accept, cachedEtag, API_READ_TIMEOUT_MS);
        try {
            int code = connection.getResponseCode();
            String responseEtag = connection.getHeaderField("ETag");
            if (code == HttpURLConnection.HTTP_NOT_MODIFIED && cachedBody != null) {
                return new CachedResponse(cachedBody, responseEtag != null ? responseEtag : cachedEtag, true);
            }
            if (code < 200 || code >= 300) {
                throw new IOException(describeHttpError(code, connection));
            }
            try (InputStream stream = connection.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int total = 0;
                int read;
                while ((read = stream.read(buffer)) >= 0) {
                    if (read == 0) continue;
                    total += read;
                    if (total > MAX_TEXT_BYTES) {
                        throw new IOException("Update metadata response exceeded size limit");
                    }
                    out.write(buffer, 0, read);
                }
                return new CachedResponse(new String(out.toByteArray(), StandardCharsets.UTF_8), responseEtag, false);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String describeHttpError(int code, HttpURLConnection connection) {
        if (code == 403 || code == 429) {
            String remaining = connection.getHeaderField("X-RateLimit-Remaining");
            String resetHeader = connection.getHeaderField("X-RateLimit-Reset");
            if ("0".equals(remaining) || code == 429) {
                long waitMinutes = parseResetWaitMinutes(resetHeader);
                if (waitMinutes > 0L) {
                    return "GitHub API rate limit exceeded; retry in about " + waitMinutes + " minute(s)";
                }
                return "GitHub API rate limit exceeded; please try again later";
            }
        }
        return "HTTP " + code + " while fetching update metadata";
    }

    static long parseResetWaitMinutes(String resetEpochSeconds) {
        if (resetEpochSeconds == null || resetEpochSeconds.trim().isEmpty()) return -1L;
        try {
            long resetSec = Long.parseLong(resetEpochSeconds.trim());
            long nowSec = System.currentTimeMillis() / 1000L;
            long diff = resetSec - nowSec;
            if (diff <= 0L) return 1L;
            return (diff + 59L) / 60L;
        } catch (NumberFormatException error) {
            return -1L;
        }
    }

    public static void downloadVerifiedJar(String url, File destination, long expectedSize, String expectedSha256,
        AtomicBoolean cancelled, ProgressListener listener) throws IOException {
        File parent = destination.getAbsoluteFile().getParentFile();
        if (parent != null) Files.createDirectories(parent.toPath());
        File partFile = new File(parent, destination.getName() + ".part");
        HttpURLConnection connection = openFollowingRedirects(url, "application/octet-stream", null, DOWNLOAD_READ_TIMEOUT_MS);
        try {
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IOException("HTTP " + code + " while downloading update");
            }
            long contentLength = connection.getContentLengthLong();
            long totalExpected = contentLength > 0 ? contentLength : expectedSize;
            MessageDigest sha256 = createSha256();
            long downloaded = 0L;
            if (listener != null) listener.onProgress(0L, totalExpected, false);
            try (InputStream in = connection.getInputStream(); FileOutputStream out = new FileOutputStream(partFile)) {
                byte[] buffer = new byte[16384];
                int read;
                while ((read = in.read(buffer)) >= 0) {
                    if (cancelled != null && cancelled.get()) {
                        throw new CancellationException("Update download cancelled");
                    }
                    if (read == 0) continue;
                    out.write(buffer, 0, read);
                    sha256.update(buffer, 0, read);
                    downloaded += read;
                    if (listener != null) listener.onProgress(downloaded, totalExpected, false);
                }
            }
            if (cancelled != null && cancelled.get()) {
                throw new CancellationException("Update download cancelled");
            }
            if (listener != null) listener.onProgress(downloaded, totalExpected, true);
            if (downloaded <= 0L) {
                throw new IOException("Downloaded update file is empty");
            }
            if (expectedSize > 0L && downloaded != expectedSize) {
                throw new IOException("Downloaded file size mismatch: expected " + expectedSize + " bytes, got " + downloaded);
            }
            if (expectedSha256 != null && !expectedSha256.isEmpty()) {
                String actualHash = toHex(sha256.digest());
                if (!expectedSha256.equalsIgnoreCase(actualHash)) {
                    throw new IOException("Downloaded file SHA-256 mismatch");
                }
            }
            verifyZipArchive(partFile);
            Files.move(partFile.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException error) {
            try {
                Files.deleteIfExists(partFile.toPath());
            } catch (IOException ignored) {
            }
            throw error;
        } finally {
            connection.disconnect();
        }
    }

    private static HttpURLConnection openFollowingRedirects(String initialUrl, String accept, String ifNoneMatch,
        int readTimeoutMs) throws IOException {
        URL current = new URL(initialUrl);
        for (int hops = 0; hops <= MAX_REDIRECTS; hops++) {
            String protocol = current.getProtocol().toLowerCase(Locale.ROOT);
            if (!"https".equals(protocol) && !"http".equals(protocol)) {
                throw new IOException("Unsupported URL protocol: " + protocol);
            }
            HttpURLConnection connection = (HttpURLConnection) current.openConnection();
            if (connection instanceof HttpsURLConnection && TLS_SOCKET_FACTORY != null) {
                ((HttpsURLConnection) connection).setSSLSocketFactory(TLS_SOCKET_FACTORY);
            }
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(readTimeoutMs);
            connection.setRequestProperty("User-Agent", "Schematica-Plus/" + Reference.VERSION);
            if (accept != null) connection.setRequestProperty("Accept", accept);
            if (ifNoneMatch != null && !ifNoneMatch.trim().isEmpty()) {
                connection.setRequestProperty("If-None-Match", ifNoneMatch);
            }
            int code = connection.getResponseCode();
            if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                String location = connection.getHeaderField("Location");
                connection.disconnect();
                if (location == null || location.trim().isEmpty()) {
                    throw new IOException("Redirect response missing Location header");
                }
                current = new URL(current, location);
                continue;
            }
            return connection;
        }
        throw new IOException("Too many redirects while connecting to update server");
    }

    private static void verifyZipArchive(File file) throws IOException {
        try (ZipFile zip = new ZipFile(file)) {
            if (zip.size() <= 0) {
                throw new IOException("Downloaded archive contains no entries");
            }
        }
    }

    private static MessageDigest createSha256() throws IOException {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException error) {
            throw new IOException("SHA-256 algorithm is unavailable", error);
        }
    }

    private static String toHex(byte[] digest) {
        StringBuilder builder = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            builder.append(Character.forDigit((value >> 4) & 0xF, 16));
            builder.append(Character.forDigit(value & 0xF, 16));
        }
        return builder.toString();
    }

    private static SSLSocketFactory createTlsSocketFactory() {
        try {
            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(null, null, null);
            return context.getSocketFactory();
        } catch (GeneralSecurityException error) {
            return null;
        }
    }
}
