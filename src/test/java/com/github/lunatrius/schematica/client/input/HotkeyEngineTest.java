package com.github.lunatrius.schematica.client.input;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class HotkeyEngineTest {
    private final List<String> calls = new ArrayList<>();
    private boolean call(Hotkey key, Hotkey.Action action) { calls.add(key.id + ":" + action); return true; }
    private HotkeyEngine engine(Hotkey... keys) {
        HotkeyEngine engine = new HotkeyEngine(Arrays.asList(keys)); engine.context(this, true, true); return engine;
    }
    @Test public void mainMenuReleaseDoesNotFireAfterAChord() {
        Hotkey.Settings release = new Hotkey.Settings(); release.action = Hotkey.Action.RELEASE; release.exclusive = true;
        Hotkey menu = new Hotkey("menu", release, 50), config = new Hotkey("config", new Hotkey.Settings(), 50, 46);
        HotkeyEngine engine = engine(menu, config);
        assertFalse(engine.event(50, true, false, this::call));
        assertTrue(engine.event(46, true, false, this::call));
        engine.event(46, false, false, this::call); engine.event(50, false, false, this::call);
        assertEquals(Collections.singletonList("config:PRESS"), calls);
        engine.event(50, true, false, this::call); engine.event(50, false, false, this::call);
        assertEquals(Arrays.asList("config:PRESS", "menu:RELEASE"), calls);
    }
    @Test public void chordsDistinguishOrderExtraKeysAndMouseButtons() {
        Hotkey key = new Hotkey("tool", new Hotkey.Settings(), 29, -100); HotkeyEngine engine = engine(key);
        engine.event(-100, true, false, this::call); engine.event(29, true, false, this::call); assertTrue(calls.isEmpty());
        engine.event(-100, false, false, this::call); engine.event(-100, true, false, this::call); assertEquals(1, calls.size());
        assertTrue(engine.held(key));
        engine.event(42, true, false, this::call); assertFalse(engine.held(key));
        key.settings.allowExtra = true; key.settings.ordered = false;
        engine.event(42, false, false, this::call); engine.event(42, true, false, this::call); assertTrue(engine.held(key));
    }
    @Test public void releaseBothAndRepeatHaveDistinctSemantics() {
        Hotkey.Settings settings = new Hotkey.Settings(); settings.action = Hotkey.Action.BOTH;
        Hotkey key = new Hotkey("action", settings, 30); HotkeyEngine engine = engine(key);
        engine.event(30, true, false, this::call); engine.event(30, true, true, this::call); engine.event(30, true, false, this::call);
        engine.event(30, false, false, this::call); engine.event(30, false, false, this::call);
        assertEquals(Arrays.asList("action:PRESS", "action:RELEASE"), calls);
    }
    @Test public void guiTransitionsAndLostFocusNeverSynthesizeReleaseActions() {
        Hotkey.Settings settings = new Hotkey.Settings(); settings.action = Hotkey.Action.BOTH;
        Hotkey key = new Hotkey("action", settings, 30); HotkeyEngine engine = engine(key);
        engine.event(30, true, false, this::call); engine.context(new Object(), false, true);
        assertFalse(engine.held(key)); engine.event(30, false, false, this::call);
        assertEquals(Collections.singletonList("action:PRESS"), calls);
        engine.context(this, true, true); engine.event(30, true, false, this::call); engine.context(this, true, false);
        engine.reconcile(code -> false); engine.context(this, true, true);
        assertFalse(engine.held(key)); assertEquals(2, calls.size());
    }
    @Test public void keysHeldAcrossScreensStayBlockedUntilReleased() {
        Hotkey key = new Hotkey("action", new Hotkey.Settings(), 29, 30); HotkeyEngine engine = engine(key);
        engine.event(29, true, false, this::call); engine.context(new Object(), false, true); engine.context(this, true, true);
        engine.event(30, true, false, this::call); assertTrue(calls.isEmpty());
        engine.event(30, false, false, this::call); engine.event(29, false, false, this::call);
        engine.event(29, true, false, this::call); engine.event(30, true, false, this::call); assertEquals(1, calls.size());
    }
    @Test public void modifiersAndAllowEmptyRespectContextWithoutTriggeringEmptyActions() {
        Hotkey.Settings settings = new Hotkey.Settings(); settings.allowEmpty = true;
        Hotkey key = new Hotkey("modifier", settings); HotkeyEngine engine = engine(key);
        assertTrue(engine.held(key)); engine.event(30, true, false, this::call); assertTrue(calls.isEmpty());
        engine.context(new Object(), false, true); assertFalse(engine.held(key));
        key.settings.context = Hotkey.Context.ANY; assertTrue(engine.held(key));
        engine.context(this, true, false); assertFalse(engine.held(key));
    }
    @Test public void unsuccessfulCallbacksNeverConsumeVanillaInput() {
        Hotkey key = new Hotkey("action", new Hotkey.Settings(), 30); HotkeyEngine engine = engine(key);
        assertFalse(engine.event(30, true, false, (binding, action) -> false));
        assertTrue(engine.held(key)); engine.event(30, false, false, this::call);
        key.settings.cancel = false; assertFalse(engine.event(30, true, false, this::call));
        assertEquals(Collections.singletonList("action:PRESS"), calls);
    }
    @Test public void guiOnlyAndAnyContextsAndMissingReleaseRecovery() {
        Hotkey.Settings settings = new Hotkey.Settings(); settings.context = Hotkey.Context.GUI;
        Hotkey key = new Hotkey("action", settings, 30); HotkeyEngine engine = engine(key);
        engine.event(30, true, false, this::call); assertTrue(calls.isEmpty());
        engine.event(30, false, false, this::call); engine.context(new Object(), false, true);
        engine.event(30, true, false, this::call); assertEquals(1, calls.size());
        engine.reconcile(code -> false); assertFalse(engine.held(key)); assertEquals(1, calls.size());
        key.settings.context = Hotkey.Context.ANY; engine.context(this, true, true);
        engine.event(30, true, false, this::call); assertEquals(2, calls.size());
    }
}
