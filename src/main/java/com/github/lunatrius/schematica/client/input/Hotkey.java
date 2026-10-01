package com.github.lunatrius.schematica.client.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Hotkey {
    public enum Context { INGAME, GUI, ANY }
    public enum Action { PRESS, RELEASE, BOTH }

    public static final class Settings {
        public Context context = Context.INGAME;
        public Action action = Action.PRESS;
        public boolean allowExtra, ordered = true, exclusive, cancel = true, allowEmpty;

        public Settings copy() {
            Settings copy = new Settings();
            copy.context = context; copy.action = action; copy.allowExtra = allowExtra; copy.ordered = ordered;
            copy.exclusive = exclusive; copy.cancel = cancel; copy.allowEmpty = allowEmpty;
            return copy;
        }
        @Override public boolean equals(Object other) {
            if (!(other instanceof Settings)) return false;
            Settings s = (Settings) other;
            return context == s.context && action == s.action && allowExtra == s.allowExtra && ordered == s.ordered
                && exclusive == s.exclusive && cancel == s.cancel && allowEmpty == s.allowEmpty;
        }
        @Override public int hashCode() { return Objects.hash(context, action, allowExtra, ordered, exclusive, cancel, allowEmpty); }
    }

    public final String id;
    private final List<Integer> defaults;
    private final Settings defaultSettings;
    private List<Integer> keys;
    public Settings settings;
    boolean held;

    public Hotkey(String id, Settings settings, int... keys) {
        this.id = id; this.defaultSettings = settings.copy(); this.settings = settings.copy();
        List<Integer> values = new ArrayList<>();
        for (int key : keys) values.add(key);
        setKeys(values); defaults = this.keys;
    }
    public List<Integer> keys() { return keys; }
    public void setKeys(List<Integer> values) {
        if (values.size() > 16) throw new IllegalArgumentException("Too many keys");
        List<Integer> next = new ArrayList<>();
        for (Integer key : values) {
            if (key == null || key == 0 || key < -100 || key > 255 || next.contains(key)) throw new IllegalArgumentException("Invalid key combination");
            next.add(key);
        }
        keys = Collections.unmodifiableList(next); held = false;
    }
    public boolean modified() { return !keys.equals(defaults) || !settings.equals(defaultSettings); }
    public boolean settingsModified() { return !settings.equals(defaultSettings); }
    public void reset() { keys = defaults; resetSettings(); }
    public void resetSettings() { settings = defaultSettings.copy(); held = false; }
    public String translationKey() { return id.equals("uiDemo") ? "schematica.key.uiDemo" : "litematica.config.hotkeys.name." + id; }
}
