package openmods.geometry;

public enum Orientation {
    XP_YP(HalfAxis.POS_X, HalfAxis.POS_Y), ZN_YP(HalfAxis.NEG_Z, HalfAxis.POS_Y), XN_YP(HalfAxis.NEG_X, HalfAxis.POS_Y),
    ZP_YP(HalfAxis.POS_Z, HalfAxis.POS_Y);

    public final HalfAxis x, y;

    Orientation(HalfAxis x, HalfAxis y) { this.x = x; this.y = y; }

    public static Orientation lookupXY(HalfAxis x, HalfAxis y) {
        for (Orientation o : values()) if (o.x == x && o.y == y) return o;
        return null;
    }
}
