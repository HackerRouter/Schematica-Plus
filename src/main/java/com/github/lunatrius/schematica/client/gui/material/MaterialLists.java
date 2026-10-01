package com.github.lunatrius.schematica.client.gui.material;

/** DataManager's material list: the last viewed list, for the Material List hotkey and the info HUD. */
public final class MaterialLists {
    private static MaterialList current;
    private static long ticks;

    private MaterialLists() {}

    public static MaterialList current() { return current; }

    public static void setCurrent(MaterialList list) {
        if (current != null && current != list) current.setHud(false);
        current = list;
    }

    public static void clear() { setCurrent(null); }

    static long tickCount() { return ticks; }

    /** Client tick start: the screen and the end of the tick then step the same list only once. */
    public static void startTick() { ticks++; }

    public static void tick() {
        if (current != null) current.tick();
    }
}
