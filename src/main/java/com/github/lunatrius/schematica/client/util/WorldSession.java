package com.github.lunatrius.schematica.client.util;

import java.util.Objects;
import java.util.function.Consumer;

public final class WorldSession<T> {
    private final Consumer<String> save;
    private final Runnable clear;
    private final Consumer<String> restore;
    private T world;
    private String key;

    public WorldSession(Consumer<String> save, Runnable clear, Consumer<String> restore) {
        this.save = save;
        this.clear = clear;
        this.restore = restore;
    }

    public void update(T nextWorld, String nextKey) {
        if (world == nextWorld && Objects.equals(key, nextKey)) return;
        save();
        clear.run();
        world = nextWorld;
        key = nextKey;
        if (world != null && key != null) restore.accept(key);
    }

    public void unload(T oldWorld) {
        if (world == oldWorld && world != null) update(null, null);
    }

    public void save() {
        if (world != null && key != null) save.accept(key);
    }
}
