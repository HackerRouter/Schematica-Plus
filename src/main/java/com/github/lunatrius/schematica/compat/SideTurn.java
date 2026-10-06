// gcewing's side + turn orientations (ArchitectureCraft shapes), turned with the schematic, by HackerRouter, 2026.
package com.github.lunatrius.schematica.compat;

import com.github.lunatrius.schematica.util.SchematicTransform;

/**
 * Greg's mod base orients a block by the rotation sideRotations[side] * turnRotations[turn] (Matrix3). The block's
 * local y and z axes are turned with the schematic and the pair that maps them there looked up; a mirror keeps both
 * axes where they land and so flips the local x axis. ArchitectureCraft shapes that are not their own mirror image
 * across local x are then swapped for their other hand or turned in place (MIRRORS, matched from the shape models).
 */
final class SideTurn {
    private static final int[][][][] MATRICES = new int[6][4][][];

    static {
        int[][][] sides = {identity(), rot(180, 1, 2), rot(90, 1, 2), mul(rot(-90, 1, 2), rot(180, 2, 0)),
            mul(rot(-90, 0, 1), rot(90, 2, 0)), mul(rot(90, 0, 1), rot(-90, 2, 0))};
        for (int side = 0; side < 6; side++) for (int turn = 0; turn < 4; turn++) MATRICES[side][turn] = mul(sides[side], rot(90 * turn, 2, 0));
    }

    /** Shape id -> {mirrored shape id, local turn applied after the mirror}. */
    private static final java.util.Map<Integer, Object[]> MIRRORS = new java.util.HashMap<>();

    static {
        // corners: a quarter turn about local y; Ionic and Corinthian capitals a half turn; plain balustrades about z
        for (int shape : new int[] {1, 2, 8, 9, 12, 13, 14, 18, 19, 25, 28, 31, 36, 37, 38, 74, 76, 81, 82, 88, 92, 93, 115}) {
            MIRRORS.put(shape, new Object[] {shape, rot(-90, 2, 0)});
        }
        for (int shape : new int[] {22, 23}) MIRRORS.put(shape, new Object[] {shape, rot(180, 2, 0)});
        for (int shape : new int[] {77, 78}) MIRRORS.put(shape, new Object[] {shape, rot(180, 0, 1)});
        // left and right handed gable overhangs and cornices
        for (int[] pair : new int[][] {{40, 41}, {42, 43}, {50, 51}, {52, 53}}) {
            MIRRORS.put(pair[0], new Object[] {pair[1], identity()});
            MIRRORS.put(pair[1], new Object[] {pair[0], identity()});
        }
    }

    private SideTurn() {}

    private static int[][] identity() { return new int[][] {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}}; }

    private static int[][] rot(int degrees, int i, int j) {
        int c = (int) Math.round(Math.cos(Math.toRadians(degrees))), s = (int) Math.round(Math.sin(Math.toRadians(degrees)));
        int[][] r = identity();
        r[i][i] = c; r[i][j] = -s; r[j][i] = s; r[j][j] = c;
        return r;
    }

    private static int[][] mul(int[][] a, int[][] b) {
        int[][] r = new int[3][3];
        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) r[i][j] = a[i][0] * b[0][j] + a[i][1] * b[1][j] + a[i][2] * b[2][j];
        return r;
    }

    private static int[] column(int[][] m, int j) { return new int[] {m[0][j], m[1][j], m[2][j]}; }

    /** The (side, turn) after the operation, or null for values out of range. */
    static int[] apply(int side, int turn, char operation) {
        if (side < 0 || side > 5 || turn < 0 || turn > 3) return null;
        int[][] m = MATRICES[side][turn];
        int[] y = turn(column(m, 1), operation), z = turn(column(m, 2), operation);
        for (int s = 0; s < 6; s++) for (int t = 0; t < 4; t++) {
            if (java.util.Arrays.equals(column(MATRICES[s][t], 1), y) && java.util.Arrays.equals(column(MATRICES[s][t], 2), z)) return new int[] {s, t};
        }
        return null;
    }

    /** The (side, turn, shape) after the operation; a mirror swaps or turns shapes listed in MIRRORS. */
    static int[] apply(int side, int turn, int shape, char operation) {
        int[] turned = apply(side, turn, operation);
        if (turned == null) return null;
        Object[] mirror = Character.isLowerCase(operation) ? MIRRORS.get(shape) : null;
        if (mirror == null) return new int[] {turned[0], turned[1], shape};
        int[][] m = mul(MATRICES[turned[0]][turned[1]], (int[][]) mirror[1]);
        for (int s = 0; s < 6; s++) for (int t = 0; t < 4; t++) {
            if (java.util.Arrays.deepEquals(MATRICES[s][t], m)) return new int[] {s, t, (Integer) mirror[0]};
        }
        return new int[] {turned[0], turned[1], shape};
    }

    private static int[] turn(int[] v, char operation) {
        double[] p = SchematicTransform.point(operation, v[0], v[1], v[2], 0, 0, 0);
        return new int[] {(int) Math.round(p[0]), (int) Math.round(p[1]), (int) Math.round(p[2])};
    }
}
