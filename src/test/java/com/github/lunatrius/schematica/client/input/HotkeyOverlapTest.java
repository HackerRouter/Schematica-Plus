package com.github.lunatrius.schematica.client.input;

import org.junit.Test;
import org.lwjgl.input.Keyboard;

import static org.junit.Assert.*;

public class HotkeyOverlapTest {
    private static Hotkey key(String id, Hotkey.Context context, Hotkey.Action action, int... keys) {
        Hotkey.Settings settings = new Hotkey.Settings();
        settings.context = context;
        settings.action = action;
        return new Hotkey(id, settings, keys);
    }

    @Test public void longerChordContainsShorterOneStartingWithTheSameKey() {
        Hotkey menu = key("menu", Hotkey.Context.INGAME, Hotkey.Action.RELEASE, Keyboard.KEY_M);
        Hotkey settings = key("settings", Hotkey.Context.INGAME, Hotkey.Action.RELEASE, Keyboard.KEY_M, Keyboard.KEY_C);
        assertTrue(settings.overlaps(menu));
        assertFalse(menu.overlaps(settings));
    }

    @Test public void differentContextsOrActionsDoNotOverlap() {
        Hotkey game = key("game", Hotkey.Context.INGAME, Hotkey.Action.PRESS, Keyboard.KEY_M);
        Hotkey gui = key("gui", Hotkey.Context.GUI, Hotkey.Action.PRESS, Keyboard.KEY_M);
        Hotkey release = key("release", Hotkey.Context.INGAME, Hotkey.Action.RELEASE, Keyboard.KEY_M);
        Hotkey any = key("any", Hotkey.Context.ANY, Hotkey.Action.BOTH, Keyboard.KEY_M);
        assertFalse(game.overlaps(gui));
        assertFalse(game.overlaps(release));
        assertTrue(game.overlaps(any));
    }

    @Test public void orderSensitiveChordsNeedTheSameSequence() {
        Hotkey ab = key("ab", Hotkey.Context.INGAME, Hotkey.Action.PRESS, Keyboard.KEY_A, Keyboard.KEY_B);
        Hotkey ba = key("ba", Hotkey.Context.INGAME, Hotkey.Action.PRESS, Keyboard.KEY_B, Keyboard.KEY_A);
        assertFalse(ab.overlaps(ba));
        ba.settings.ordered = false;
        assertTrue(ab.overlaps(ba));
    }

    @Test public void emptyAndSelfNeverOverlap() {
        Hotkey empty = key("empty", Hotkey.Context.INGAME, Hotkey.Action.PRESS);
        Hotkey a = key("a", Hotkey.Context.INGAME, Hotkey.Action.PRESS, Keyboard.KEY_A);
        assertFalse(a.overlaps(empty));
        assertFalse(a.overlaps(a));
    }
}
