package com.github.lunatrius.schematica.client.gui.cloud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeMap;

import com.github.lunatrius.schematica.cloud.CloudCatalog;
import com.github.lunatrius.schematica.cloud.CloudFile;
import com.github.lunatrius.schematica.cloud.CloudPath;

final class CloudNode {
    enum Type { DIRECTORY, SCHEMATIC }

    final Type type;
    final CloudPath path;
    final CloudFile file;
    final int count;

    private CloudNode(Type type, CloudPath path, CloudFile file, int count) {
        this.type = type; this.path = path; this.file = file; this.count = count;
    }

    String name() {
        return file != null ? file.path.name() : path.name();
    }

    static List<CloudNode> list(CloudCatalog catalog, CloudPath directory, String query) {
        List<CloudNode> nodes = new ArrayList<>();
        if (catalog == null) return nodes;
        String search = query.trim().toLowerCase(Locale.ROOT);
        if (!search.isEmpty()) {
            for (CloudFile file : catalog.files) {
                if (directory != null && !file.path.within(directory)) continue;
                if (file.path.name().toLowerCase(Locale.ROOT).contains(search) || file.name.toLowerCase(Locale.ROOT).contains(search)) {
                    nodes.add(new CloudNode(Type.SCHEMATIC, file.path, file, 0));
                }
            }
            nodes.sort(Comparator.comparing(node -> node.name().toLowerCase(Locale.ROOT)));
            return nodes;
        }
        if (directory == null) {
            for (String scope : new String[] {CloudPath.PRIVATE, CloudPath.PUBLIC}) {
                CloudPath root = CloudPath.root(scope);
                nodes.add(new CloudNode(Type.DIRECTORY, root, null, count(catalog, root)));
            }
            return nodes;
        }
        TreeMap<String, CloudNode> directories = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (CloudPath path : catalog.directories) {
            if (path.isRoot() || !Objects.equals(path.parent(), directory)) continue;
            directories.put(path.name(), new CloudNode(Type.DIRECTORY, path, null, count(catalog, path)));
        }
        nodes.addAll(directories.values());
        List<CloudNode> files = new ArrayList<>();
        for (CloudFile file : catalog.files) {
            if (file.path.parent().equals(directory)) files.add(new CloudNode(Type.SCHEMATIC, file.path, file, 0));
        }
        files.sort(Comparator.comparing(node -> node.name().toLowerCase(Locale.ROOT)));
        nodes.addAll(files);
        return nodes;
    }

    static int count(CloudCatalog catalog, CloudPath directory) {
        int count = 0;
        for (CloudFile file : catalog.files) if (file.path.within(directory)) count++;
        return count;
    }

    @Override public boolean equals(Object other) {
        return other instanceof CloudNode && type == ((CloudNode) other).type && Objects.equals(path, ((CloudNode) other).path);
    }

    @Override public int hashCode() { return Objects.hash(type, path); }
}
