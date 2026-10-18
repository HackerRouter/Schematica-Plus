package com.github.lunatrius.schematica.cloud;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.github.lunatrius.schematica.util.FileUtils;

public final class CloudPath {
    public static final String PRIVATE = "private", PUBLIC = "public";
    public static final List<String> EXTENSIONS = Collections.unmodifiableList(Arrays.asList(".litematic", ".schematic", ".schemplus", ".schem", ".nbt"));
    static final int MAX_DEPTH = 16, MAX_NAME = 128;

    public final String scope;
    private final List<String> parts;

    private CloudPath(String scope, List<String> parts) {
        this.scope = scope;
        this.parts = Collections.unmodifiableList(parts);
    }

    public static CloudPath root(String scope) {
        if (!PRIVATE.equals(scope) && !PUBLIC.equals(scope)) throw new IllegalArgumentException("Unknown scope");
        return new CloudPath(scope, new ArrayList<>());
    }

    public static CloudPath parse(String path) {
        if (path == null) throw new IllegalArgumentException("Missing path");
        String[] split = path.replace('\\', '/').split("/", -1);
        CloudPath result = root(split[0]);
        for (int i = 1; i < split.length; i++) result = result.child(split[i]);
        return result;
    }

    public static boolean validName(String name) {
        if (name == null || name.isEmpty() || name.length() > MAX_NAME || name.startsWith(".") || name.endsWith(".") || name.endsWith(" ")
            || !name.trim().equals(name) || FileUtils.isReservedName(name)) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < 32 || "/\\:*?\"<>|".indexOf(c) >= 0) return false;
        }
        return true;
    }

    public static boolean schematicName(String name) {
        if (!validName(name)) return false;
        String lower = name.toLowerCase(Locale.ROOT);
        for (String extension : EXTENSIONS) if (lower.endsWith(extension) && lower.length() > extension.length()) return true;
        return false;
    }

    public CloudPath child(String name) {
        if (!validName(name) || parts.size() >= MAX_DEPTH) throw new IllegalArgumentException("Invalid name");
        List<String> next = new ArrayList<>(parts);
        next.add(name);
        return new CloudPath(scope, next);
    }

    public CloudPath parent() {
        return parts.isEmpty() ? this : new CloudPath(scope, new ArrayList<>(parts.subList(0, parts.size() - 1)));
    }

    public boolean isRoot() { return parts.isEmpty(); }

    public String name() { return parts.isEmpty() ? scope : parts.get(parts.size() - 1); }

    public List<String> parts() { return parts; }

    public String relative() { return String.join("/", parts); }

    public boolean within(CloudPath other) {
        return scope.equals(other.scope) && parts.size() >= other.parts.size() && parts.subList(0, other.parts.size()).equals(other.parts);
    }

    public File resolve(File base) {
        File file = base;
        for (String part : parts) file = new File(file, part);
        return file;
    }

    @Override public String toString() { return parts.isEmpty() ? scope : scope + "/" + relative(); }

    @Override public boolean equals(Object other) {
        return other instanceof CloudPath && scope.equals(((CloudPath) other).scope) && parts.equals(((CloudPath) other).parts);
    }

    @Override public int hashCode() { return Objects.hash(scope, parts); }
}
