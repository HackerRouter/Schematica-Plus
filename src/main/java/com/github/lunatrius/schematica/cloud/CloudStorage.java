package com.github.lunatrius.schematica.cloud;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.FileUtils;
import com.github.lunatrius.schematica.util.MessageException;
import com.github.lunatrius.schematica.world.schematic.SchematicFileSnapshot;
import com.github.lunatrius.schematica.world.schematic.SchematicFiles;
import com.github.lunatrius.schematica.world.schematic.SchematicFormat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public final class CloudStorage {
    static final String OWNERS = ".owners.json";
    private static final Gson GSON = new Gson();
    private static final Type OWNERS_TYPE = new TypeToken<TreeMap<String, Owner>>() {}.getType();
    private static final int INFO_CACHE = 8192;

    private final File root;
    private final Map<String, Described> described = new LinkedHashMap<String, Described>(64, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Described> eldest) { return size() > INFO_CACHE; }
    };

    public static final class Owner {
        public String uuid, name;

        Owner(UUID uuid, String name) { this.uuid = uuid.toString(); this.name = name; }
    }

    private static final class Described {
        final long size, modified;
        final CloudFile file;

        Described(long size, long modified, CloudFile file) { this.size = size; this.modified = modified; this.file = file; }
    }

    interface Validator {
        void check(SchematicFileSnapshot snapshot) throws IOException;
    }

    private final Validator validator;

    public CloudStorage(File root) {
        this(root, SchematicFormat::readFromSnapshot);
    }

    CloudStorage(File root, Validator validator) {
        this.root = root;
        this.validator = validator;
    }

    public File root() { return root; }

    public File base(String scope, UUID player) {
        if (CloudPath.SHARED.equals(scope)) return new File(root, CloudPath.SHARED);
        return CloudPath.PUBLIC.equals(scope) ? publicBase() : new File(new File(root, player.toString()), CloudPath.PRIVATE);
    }

    private File publicBase() { return new File(root, CloudPath.PUBLIC); }

    private File parent(CloudPath path, UUID player, String playerName) throws IOException {
        File base = base(path.scope, player);
        if (!base.isDirectory() && !base.mkdirs()) throw new IOException("Could not create " + base);
        CloudPath directory = CloudPath.root(path.scope);
        for (String part : path.parent().parts()) {
            directory = directory.child(part);
            File file = file(directory, player);
            if (file.isDirectory()) continue;
            if (file.exists() || !file.mkdir()) throw new MessageException("schematica.message.cloud.not_found", directory.toString());
            if (CloudPath.PUBLIC.equals(path.scope)) setOwner(directory, player, playerName);
        }
        return file(path.parent(), player);
    }

    public File file(CloudPath path, UUID player) {
        return path.resolve(base(path.scope, player));
    }

    public synchronized CloudCatalog catalog(UUID player, long quota, boolean upload, boolean publish, boolean manage) throws IOException {
        List<CloudPath> directories = new ArrayList<>();
        List<CloudFile> files = new ArrayList<>();
        Map<String, Owner> owners = owners();
        for (String scope : new String[] {CloudPath.PRIVATE, CloudPath.PUBLIC}) {
            scan(base(scope, player), CloudPath.root(scope), owners, directories, files);
        }
        return new CloudCatalog(directories, files, used(player), quota, upload, publish, manage);
    }

    private void scan(File directory, CloudPath path, Map<String, Owner> owners, List<CloudPath> directories, List<CloudFile> files) throws IOException {
        File[] children = directory.listFiles();
        if (children == null) return;
        Arrays.sort(children, Comparator.comparing(file -> file.getName().toLowerCase(Locale.ROOT)));
        for (File child : children) {
            if (directories.size() + files.size() >= CloudCatalog.MAX_ENTRIES) return;
            if (!CloudPath.validName(child.getName()) || Files.isSymbolicLink(child.toPath())) continue;
            CloudPath next = path.child(child.getName());
            if (child.isDirectory()) {
                if (next.parts().size() >= CloudPath.MAX_DEPTH) continue;
                directories.add(next);
                scan(child, next, owners, directories, files);
            } else if (child.isFile() && CloudPath.schematicName(child.getName())) {
                Owner owner = CloudPath.PUBLIC.equals(next.scope) ? owners.get(next.relative()) : null;
                files.add(describe(child, next).withOwner(owner == null ? "" : owner.name));
            }
        }
    }

    private CloudFile describe(File file, CloudPath path) throws IOException {
        String key = file.getAbsolutePath();
        long size = file.length(), modified = file.lastModified();
        Described cached = described.get(key);
        if (cached != null && cached.size == size && cached.modified == modified && cached.file.path.equals(path)) return cached.file;
        CloudFile result = new CloudFile(path, size, modified, hash(file), "");
        try {
            result.describe(SchematicFiles.info(file));
        } catch (IOException | RuntimeException e) {
            Reference.logger.debug("Could not read the details of {}", file, e);
        }
        if (result.name.isEmpty()) result.name = stem(file.getName());
        described.put(key, new Described(size, modified, result));
        return result;
    }

    static String stem(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    public static String hash(File file) throws IOException {
        try (InputStream input = Files.newInputStream(file.toPath())) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[65536];
            int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
            return hex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    public static String hash(byte[] data) {
        try {
            return hex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String hex(byte[] digest) {
        StringBuilder text = new StringBuilder(digest.length * 2);
        for (byte value : digest) text.append(String.format("%02x", value & 0xFF));
        return text.toString();
    }

    public synchronized long used(UUID player) {
        long used = size(base(CloudPath.PRIVATE, player));
        File publicRoot = publicBase();
        for (Map.Entry<String, Owner> entry : owners().entrySet()) {
            if (!player.toString().equals(entry.getValue().uuid)) continue;
            File file = new File(publicRoot, entry.getKey());
            if (file.isFile()) used += file.length();
        }
        return used;
    }

    private static long size(File directory) {
        File[] children = directory.listFiles();
        if (children == null) return 0;
        long total = 0;
        for (File child : children) {
            if (Files.isSymbolicLink(child.toPath())) continue;
            total += child.isDirectory() ? size(child) : child.length();
        }
        return total;
    }

    public synchronized File writeShared(CloudPath path, byte[] data) throws IOException {
        if (!CloudPath.SHARED.equals(path.scope) || path.parts().size() != 1 || !path.name().startsWith(hash(data) + ".")) {
            throw new MessageException("schematica.message.cloud.upload_failed", path.name());
        }
        File target = file(path, null);
        if (target.isFile()) return target;
        SchematicFileSnapshot snapshot = SchematicFileSnapshot.of(path.name(), data);
        try {
            validator.check(snapshot);
        } catch (IOException | RuntimeException e) {
            throw new MessageException("schematica.message.cloud.invalid_file", path.name());
        }
        File base = base(CloudPath.SHARED, null);
        if (!base.isDirectory() && !base.mkdirs()) throw new IOException("Could not create " + base);
        snapshot.write(target, false);
        return target;
    }

    public synchronized File write(CloudPath path, UUID player, String playerName, byte[] data, long quota) throws IOException {
        if (CloudPath.SHARED.equals(path.scope)) throw new MessageException("schematica.message.cloud.no_permission");
        if (path.isRoot() || !CloudPath.schematicName(path.name())) throw new MessageException("schematica.message.cloud.invalid_name", path.name());
        File target = file(path, player);
        if (target.exists()) throw new MessageException("schematica.message.cloud.exists", path.name());
        if (quota >= 0 && used(player) + data.length > quota) {
            throw new MessageException("schematica.message.cloud.quota", FileUtils.humanReadableByteCount(used(player)), FileUtils.humanReadableByteCount(quota));
        }
        SchematicFileSnapshot snapshot = SchematicFileSnapshot.of(path.name(), data);
        try {
            validator.check(snapshot);
        } catch (IOException | RuntimeException e) {
            throw new MessageException("schematica.message.cloud.invalid_file", path.name());
        }
        parent(path, player, playerName);
        snapshot.write(target, false);
        if (CloudPath.PUBLIC.equals(path.scope)) setOwner(path, player, playerName);
        return target;
    }

    public synchronized void createDirectory(CloudPath path, UUID player, String playerName) throws IOException {
        if (CloudPath.SHARED.equals(path.scope)) throw new MessageException("schematica.message.cloud.no_permission");
        if (path.isRoot()) throw new MessageException("schematica.message.cloud.invalid_name", path.name());
        File target = file(path, player);
        if (target.exists()) throw new MessageException("schematica.message.cloud.exists", path.name());
        parent(path, player, playerName);
        if (!target.mkdir()) throw new IOException("Could not create " + target);
        if (CloudPath.PUBLIC.equals(path.scope)) setOwner(path, player, playerName);
    }

    public synchronized boolean owns(CloudPath path, UUID player) {
        if (CloudPath.PRIVATE.equals(path.scope)) return true;
        Owner owner = owners().get(path.relative());
        return owner != null && player.toString().equals(owner.uuid);
    }

    public synchronized void delete(CloudPath path, UUID player, boolean manage) throws IOException {
        if (CloudPath.SHARED.equals(path.scope)) throw new MessageException("schematica.message.cloud.no_permission");
        File target = file(path, player);
        if (path.isRoot() || !target.exists()) throw new MessageException("schematica.message.cloud.not_found", path.toString());
        if (!manage && !owns(path, player)) throw new MessageException("schematica.message.cloud.no_permission");
        String[] children = target.list();
        if (target.isDirectory() && children != null && children.length > 0) throw new MessageException("schematica.message.cloud.not_empty", path.name());
        Files.delete(target.toPath());
        if (CloudPath.PUBLIC.equals(path.scope)) {
            Map<String, Owner> owners = owners();
            if (owners.remove(path.relative()) != null) saveOwners(owners);
        }
    }

    public synchronized CloudPath publish(CloudPath path, CloudPath directory, UUID player, String playerName) throws IOException {
        if (!CloudPath.PRIVATE.equals(path.scope) || !CloudPath.PUBLIC.equals(directory.scope)) throw new MessageException("schematica.message.cloud.no_permission");
        File source = file(path, player);
        if (!source.isFile()) throw new MessageException("schematica.message.cloud.not_found", path.toString());
        CloudPath target = directory.child(path.name());
        File destination = file(target, player);
        if (destination.exists()) throw new MessageException("schematica.message.cloud.exists", path.name());
        parent(target, player, playerName);
        Files.copy(source.toPath(), destination.toPath());
        setOwner(target, player, playerName);
        return target;
    }

    public synchronized byte[] read(CloudPath path, UUID player, long limit) throws IOException {
        File source = file(path, player);
        if (path.isRoot() || !source.isFile() || !CloudPath.schematicName(path.name())) throw new MessageException("schematica.message.cloud.not_found", path.toString());
        if (source.length() > limit) throw new MessageException("schematica.message.cloud.too_large", path.name(), FileUtils.humanReadableByteCount(limit));
        return Files.readAllBytes(source.toPath());
    }

    private Map<String, Owner> owners() {
        File file = new File(publicBase(), OWNERS);
        if (!file.isFile()) return new TreeMap<>();
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            Map<String, Owner> owners = GSON.fromJson(reader, OWNERS_TYPE);
            return owners == null ? new TreeMap<>() : owners;
        } catch (IOException | RuntimeException e) {
            Reference.logger.warn("Could not read the owners of the public schematics", e);
            return new TreeMap<>();
        }
    }

    private void setOwner(CloudPath path, UUID player, String name) throws IOException {
        Map<String, Owner> owners = owners();
        owners.put(path.relative(), new Owner(player, name));
        saveOwners(owners);
    }

    private void saveOwners(Map<String, Owner> owners) throws IOException {
        File directory = publicBase();
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Could not create " + directory);
        FileUtils.writeUtf8Atomically(new File(directory, OWNERS), GSON.toJson(owners, OWNERS_TYPE));
    }
}
