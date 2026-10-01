package com.github.lunatrius.schematica.client.input;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

import com.github.lunatrius.schematica.client.input.Hotkey.Action;
import com.github.lunatrius.schematica.client.input.Hotkey.Context;

public final class HotkeyEngine {
    private final List<Hotkey> bindings;
    private final Set<Integer> pressed = new LinkedHashSet<>(), blocked = new HashSet<>();
    private Object context;
    private boolean ingame, focused, triggered;

    public HotkeyEngine(List<Hotkey> bindings) { this.bindings = bindings; }

    public void context(Object identity, boolean ingame, boolean focused) {
        if (context != identity || this.ingame != ingame || this.focused != focused) {
            reset(); context = identity; this.ingame = ingame; this.focused = focused;
        }
    }
    public void reset() {
        blocked.addAll(pressed); triggered = false;
        for (Hotkey binding : bindings) binding.held = false;
    }
    public Set<Integer> pressed() { return new LinkedHashSet<>(pressed); }
    public boolean held(Hotkey binding) {
        return valid(binding) && (binding.held || binding.keys().isEmpty() && binding.settings.allowEmpty);
    }
    private boolean valid(Hotkey binding) {
        return focused && (binding.settings.context == Context.ANY || (binding.settings.context == Context.INGAME) == ingame);
    }
    private boolean matches(Hotkey binding) {
        List<Integer> keys = binding.keys();
        if (keys.isEmpty() || !valid(binding) || !pressed.containsAll(keys)) return false;
        for (int key : keys) if (blocked.contains(key)) return false;
        if (!binding.settings.allowExtra && pressed.size() != keys.size()) return false;
        if (!binding.settings.ordered) return true;
        int index = 0;
        for (int code : pressed) {
            if (keys.get(index) == code) { if (++index == keys.size()) return true; }
            else if (index > 0 || pressed.size() == keys.size()) return false;
        }
        return false;
    }

    public boolean event(int code, boolean down, boolean repeat, BiPredicate<Hotkey, Action> callback) {
        if (code == 0) return false;
        boolean changed = down ? pressed.add(code) : pressed.remove(code);
        if (!down) blocked.remove(code);
        if (!changed || repeat) return false;
        List<Hotkey> changedBindings = new ArrayList<>();
        for (Hotkey binding : bindings) {
            boolean next = matches(binding);
            if (next != binding.held) { binding.held = next; changedBindings.add(binding); }
        }
        changedBindings.sort(Comparator.comparingInt((Hotkey binding) -> binding.keys().size()).reversed());
        boolean cancel = false;
        for (Hotkey binding : changedBindings) {
            Action action = binding.held ? Action.PRESS : Action.RELEASE;
            if (!valid(binding) || binding.settings.exclusive && triggered
                || binding.settings.action != Action.BOTH && binding.settings.action != action) continue;
            if (!cancel && callback.test(binding, action) && binding.settings.cancel) { cancel = true; triggered = true; }
        }
        if (pressed.isEmpty()) triggered = false;
        return cancel;
    }

    public void reconcile(java.util.function.IntPredicate physicallyDown) {
        boolean missing = pressed.removeIf(code -> !physicallyDown.test(code));
        blocked.removeIf(code -> !physicallyDown.test(code));
        if (missing) {
            for (Hotkey binding : bindings) binding.held = matches(binding);
            if (pressed.isEmpty()) triggered = false;
        }
    }
}
