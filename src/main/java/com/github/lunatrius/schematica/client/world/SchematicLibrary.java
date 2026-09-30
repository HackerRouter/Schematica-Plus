package com.github.lunatrius.schematica.client.world;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class SchematicLibrary<D, P> {

    public interface Reader<D> { D read(File file) throws IOException; }
    public interface Factory<D, P> { P create(D data, P previous) throws IOException; }

    private final Reader<D> reader;
    private final List<Source<D>> sources = new ArrayList<>();
    private final List<P> placements = new ArrayList<>();
    private final Map<P, Source<D>> owners = new IdentityHashMap<>();

    public SchematicLibrary(Reader<D> reader) { this.reader = reader; }

    public List<Source<D>> sources() { return Collections.unmodifiableList(sources); }
    public List<P> placements() { return Collections.unmodifiableList(placements); }
    public Source<D> sourceOf(P placement) { return owners.get(placement); }

    public Source<D> load(File file) throws IOException {
        File canonical = file.getCanonicalFile();
        for (Source<D> source : sources) if (source.file.equals(canonical)) return source;
        Source<D> source = new Source<>(canonical, reader.read(canonical));
        sources.add(source);
        return source;
    }

    public P create(Source<D> source, Factory<D, P> factory) throws IOException {
        require(source);
        P placement = factory.create(source.data, null);
        if (placement == null || owners.containsKey(placement)) throw new IllegalArgumentException("Placement must be new");
        placements.add(placement);
        owners.put(placement, source);
        return placement;
    }

    public Map<P, P> reload(Source<D> source, Factory<D, P> factory) throws IOException {
        require(source);
        D replacement = reader.read(source.file);
        Map<P, P> changes = new IdentityHashMap<>();
        Map<P, Boolean> staged = new IdentityHashMap<>();
        for (P placement : placements) {
            if (owners.get(placement) != source) continue;
            P next = factory.create(replacement, placement);
            if (next == null || owners.containsKey(next) || staged.put(next, true) != null) {
                throw new IllegalArgumentException("Reloaded placement must be new");
            }
            changes.put(placement, next);
        }
        for (int i = 0; i < placements.size(); i++) {
            P previous = placements.get(i);
            P next = changes.get(previous);
            if (next == null) continue;
            placements.set(i, next);
            owners.remove(previous);
            owners.put(next, source);
        }
        source.data = replacement;
        return changes;
    }

    public boolean remove(P placement) {
        if (owners.remove(placement) == null) return false;
        for (int i = 0; i < placements.size(); i++) {
            if (placements.get(i) == placement) { placements.remove(i); break; }
        }
        return true;
    }

    public List<P> unload(Source<D> source) {
        if (!sources.remove(source)) return Collections.emptyList();
        List<P> removed = new ArrayList<>();
        for (P placement : new ArrayList<>(placements)) {
            if (owners.get(placement) == source) {
                remove(placement);
                removed.add(placement);
            }
        }
        return removed;
    }

    public void renamed(File previous, File next) throws IOException {
        checkRename(previous, next);
        File canonical = next.getCanonicalFile();
        for (Source<D> source : sources) if (source.file.equals(previous)) source.file = canonical;
    }

    public void checkRename(File previous, File next) throws IOException {
        File canonical = next.getCanonicalFile();
        for (Source<D> source : sources) {
            if (source.file.equals(canonical) && !source.file.equals(previous)) {
                throw new java.nio.file.FileAlreadyExistsException("A source is already loaded under this name: " + canonical);
            }
        }
    }

    public void clear() {
        owners.clear();
        placements.clear();
        sources.clear();
    }

    private void require(Source<D> source) {
        if (!sources.contains(source)) throw new IllegalArgumentException("Source is no longer loaded");
    }

    public static final class Source<D> {
        private File file;
        private D data;

        private Source(File file, D data) { this.file = file; this.data = data; }

        public File file() { return file; }
        public D data() { return data; }
        public String name() { return file.getName().replaceAll("(?i)\\.(schematic|litematic|schemplus)$", ""); }
    }
}
