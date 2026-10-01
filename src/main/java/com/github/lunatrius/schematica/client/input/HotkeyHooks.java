package com.github.lunatrius.schematica.client.input;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.IntPredicate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import com.github.lunatrius.schematica.reference.Reference;

public final class HotkeyHooks {
    public interface Capture {
        boolean capturingHotkey();
        boolean captureHotkey(int code, boolean down);
    }

    /** Receives vanilla attack/use presses that no hotkey consumed; returning true cancels them. */
    public interface Click {
        boolean click(boolean attack);
    }

    private static HotkeyEngine engine;
    private static BiPredicate<Hotkey, Hotkey.Action> callback;
    private static IntPredicate scroll;
    private static Click click;
    private static final Set<Integer> consumed = new HashSet<>();

    private HotkeyHooks() {}

    public static void initialize(List<Hotkey> bindings, BiPredicate<Hotkey, Hotkey.Action> action, IntPredicate wheel) {
        engine = new HotkeyEngine(bindings); callback = action; scroll = wheel;
    }
    public static void click(Click handler) { click = handler; }
    public static boolean held(Hotkey key) { return engine != null && key != null && engine.held(key); }
    public static void reset() { if (engine != null) engine.reset(); }

    private static void context() {
        Minecraft mc = Minecraft.getMinecraft();
        engine.context(mc.currentScreen == null ? mc.theWorld : mc.currentScreen,
            mc.currentScreen == null && mc.theWorld != null,
            Display.isActive() && (mc.currentScreen != null || mc.inGameHasFocus));
    }

    public static void tick() {
        if (engine == null) return;
        context();
        engine.reconcile(HotkeyHooks::physicallyDown);
        consumed.removeIf(code -> {
            if (physicallyDown(code)) return false;
            KeyBinding.setKeyBindState(code, false);
            return true;
        });
    }

    private static boolean physicallyDown(int code) {
        return code < 0 ? Mouse.isCreated() && code + 100 < Mouse.getButtonCount() && Mouse.isButtonDown(code + 100)
            : Keyboard.isCreated() && Keyboard.isKeyDown(code);
    }

    private static boolean event(int code, boolean down, boolean repeat) {
        if (engine == null || code == 0) return false;
        context();
        Minecraft mc = Minecraft.getMinecraft();
        Capture capture = mc.currentScreen instanceof Capture ? (Capture) mc.currentScreen : null;
        boolean capturing = capture != null && capture.capturingHotkey();
        boolean cancel = engine.event(code, down, repeat, (key, action) -> {
            if (capturing) return false;
            try { return callback.test(key, action); }
            catch (RuntimeException error) {
                Reference.logger.error("Hotkey failed: " + key.id, error);
                return true;
            }
        });
        if (!cancel && !capturing && down && !repeat) cancel = vanillaClick(mc, code);
        if (capturing) {
            cancel = repeat || capture.captureHotkey(code, down);
        }
        if (down && consumed.contains(code)) cancel = true;
        if (down && cancel) consumed.add(code);
        if (!down && consumed.remove(code)) cancel = true;
        if (cancel) KeyBinding.setKeyBindState(code, false);
        return cancel;
    }

    private static boolean vanillaClick(Minecraft mc, int code) {
        if (click == null || mc.currentScreen != null || mc.theWorld == null || mc.thePlayer == null || !mc.inGameHasFocus) return false;
        boolean attack = code == mc.gameSettings.keyBindAttack.getKeyCode();
        if (!attack && code != mc.gameSettings.keyBindUseItem.getKeyCode()) return false;
        try { return click.click(attack); }
        catch (RuntimeException error) {
            Reference.logger.error("Schematic edit failed", error);
            return false;
        }
    }

    public static boolean nextKeyboard() {
        while (Keyboard.next()) {
            if (!event(Keyboard.getEventKey(), Keyboard.getEventKeyState(), Keyboard.isRepeatEvent())) return true;
        }
        return false;
    }

    public static boolean nextMouse() {
        while (Mouse.next()) {
            if (Mouse.getEventButton() >= 0 && event(Mouse.getEventButton() - 100, Mouse.getEventButtonState(), false)) continue;
            if (engine != null && Mouse.getEventDWheel() != 0) {
                context();
                if (Minecraft.getMinecraft().currentScreen == null && Display.isActive()) {
                    try { if (scroll.test(Mouse.getEventDWheel())) continue; }
                    catch (RuntimeException error) { Reference.logger.error("Tool scroll failed", error); continue; }
                }
            }
            return true;
        }
        return false;
    }
}
