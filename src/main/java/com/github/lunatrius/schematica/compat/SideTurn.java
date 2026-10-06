// gcewing's side + turn orientations (ArchitectureCraft shapes), turned with the schematic, by HackerRouter, 2026.
package com.github.lunatrius.schematica.compat;

import com.github.lunatrius.schematica.util.SchematicTransform;

/**
 * Greg's mod base orients a block by the rotation sideRotations[side] * turnRotations[turn] (Matrix3). The block's
 * local y and z axes are turned with the schematic and the pair that maps them there looked up; a mirror keeps both
 * axes where they land and so flips the local x axis.
 */
final class SideTurn {
    private static final int[][][][] MATRICES = new int[6][4][][];

    static {
        int[][][] sides = {identity(), rot(180, 1, 2), rot(90, 1, 2), mul(rot(-90, 1, 2), rot(180, 2, 0)),
            mul(rot(-90, 0, 1), rot(90, 2, 0)), mul(rot(90, 0, 1), rot(-90, 2, 0))};
        for (int side = 0; side < 6; side++) for (int turn = 0; turn < 4; turn++) MATRICES[side][turn] = mul(sides[side], rot(90 * turn, 2, 0));
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

    private static int[] turn(int[] v, char operation) {
        double[] p = SchematicTransform.point(operation, v[0], v[1], v[2], 0, 0, 0);
        return new int[] {(int) Math.round(p[0]), (int) Math.round(p[1]), (int) Math.round(p[2])};
    }
}
