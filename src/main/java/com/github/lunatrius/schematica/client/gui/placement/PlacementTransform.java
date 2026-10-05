package com.github.lunatrius.schematica.client.gui.placement;

import java.util.List;

import com.github.lunatrius.schematica.util.SchematicTransform;

public final class PlacementTransform {
    private PlacementTransform() {}

    public static com.github.lunatrius.schematica.api.SchematicOrigin transformOrigin(
        com.github.lunatrius.schematica.api.SchematicOrigin origin, int width, int height, int length, String operations) {
        int[] size = {width, height, length};
        for (int i = 0; i < operations.length(); i++) {
            char op = operation(operations.substring(i, i + 1));
            origin = origin.transform(op, size[0], size[1], size[2]);
            permuteSize(size, op);
        }
        return origin;
    }

    public static int[] anchorOffset(int width, int height, int length, List<String> operations) {
        int[] size = {width, height, length};
        for (int i = operations.size() - 1; i >= 0; i--) permuteSize(size, operation(operations.get(i)));
        double[] anchor = {0, 0, 0};
        for (String entry : operations) {
            char op = operation(entry);
            anchor = SchematicTransform.point(op, anchor[0], anchor[1], anchor[2], size[0] - 1, size[1] - 1, size[2] - 1);
            permuteSize(size, op);
        }
        return new int[] {(int) anchor[0], (int) anchor[1], (int) anchor[2]};
    }

    public static int[] transformedSize(int width, int height, int length, String operations) {
        int[] size = {width, height, length};
        for (int i = 0; i < operations.length(); i++) permuteSize(size, operation(operations.substring(i, i + 1)));
        return size;
    }

    public static int[] reloadedMinimum(int[] minimum, int[] previousSize, int[] nextSize, List<String> operations) {
        int[] previous = anchorOffset(previousSize[0], previousSize[1], previousSize[2], operations);
        int[] next = anchorOffset(nextSize[0], nextSize[1], nextSize[2], operations);
        return new int[] {Math.addExact(minimum[0], previous[0] - next[0]),
            Math.addExact(minimum[1], previous[1] - next[1]), Math.addExact(minimum[2], previous[2] - next[2])};
    }

    private static void permuteSize(int[] size, char op) {
        if (Character.isLowerCase(op)) return;
        int a = op == 'X' ? 1 : 0;
        int b = op == 'Z' ? 1 : 2;
        int value = size[a];
        size[a] = size[b];
        size[b] = value;
    }

    private static char operation(String value) {
        if (value == null || value.length() != 1 || "XYZxyz".indexOf(value.charAt(0)) < 0) {
            throw new IllegalArgumentException("Invalid placement transform");
        }
        return value.charAt(0);
    }

    public static Orientation orientation(List<String> operations) {
        double[][] basis = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
        Orientation state = new Orientation(0, 0);
        for (String entry : operations) {
            char op = operation(entry);
            for (int i = 0; i < basis.length; i++) {
                basis[i] = SchematicTransform.point(op, basis[i][0], basis[i][1], basis[i][2], 0, 0, 0);
            }
            if (state != null && op == 'Y') {
                state = new Orientation((state.rotation + 1) % 4, state.mirror);
            } else if (state != null && (op == 'x' || op == 'z')) {
                int mirror = op == 'z' ? 1 : 2;
                if (state.rotation % 2 != 0) mirror = 3 - mirror;
                state = state.mirror == 0 ? new Orientation(state.rotation, mirror)
                    : state.mirror == mirror ? new Orientation(state.rotation, 0)
                    : new Orientation((state.rotation + 2) % 4, 0);
            } else {
                state = match(basis);
            }
        }
        return state;
    }

    private static Orientation match(double[][] basis) {
        if (basis[1][0] != 0 || basis[1][1] != 1 || basis[1][2] != 0) return null;
        for (int mirror = 0; mirror < 3; mirror++) {
            for (int rotation = 0; rotation < 4; rotation++) {
                double[] x = {mirror == 2 ? -1 : 1, 0, 0};
                double[] z = {0, 0, mirror == 1 ? -1 : 1};
                for (int i = 0; i < rotation; i++) {
                    x = SchematicTransform.point('Y', x[0], x[1], x[2], 0, 0, 0);
                    z = SchematicTransform.point('Y', z[0], z[1], z[2], 0, 0, 0);
                }
                if (java.util.Arrays.equals(x, basis[0]) && java.util.Arrays.equals(z, basis[2])) {
                    return new Orientation(rotation, mirror);
                }
            }
        }
        return null;
    }

    public static final class Orientation {
        public final int rotation;
        public final int mirror;

        private Orientation(int rotation, int mirror) {
            this.rotation = rotation;
            this.mirror = mirror;
        }

        public String rotationName() {
            return new String[] {"NONE", "CW_90", "CW_180", "CCW_90"}[rotation];
        }

        public String mirrorName() {
            return new String[] {"NONE", "LEFT_RIGHT", "FRONT_BACK"}[mirror];
        }

        public String cycleMirror(boolean reverse) {
            int next = Math.floorMod(mirror + (reverse ? -1 : 1), 3);
            return mirrorAxis(mirror) + mirrorAxis(next);
        }

        private String mirrorAxis(int mirror) {
            if (mirror == 0) return "";
            boolean z = (mirror == 1) == (rotation % 2 == 0);
            return z ? "z" : "x";
        }
    }

    /**
     * Entropy5's Align to Map: the start coordinate that puts the far (east or south) edge of a placement of this size
     * on the nearest map border (maps cover -64 + 128k to 63 + 128k), so a 128 wide map art fills one map and a 129
     * long one keeps its extra shading row just north of it.
     */
    static int mapAlignedStart(int start, int size) {
        long end = (long) start + size;
        return (int) (Math.floorDiv(end + 128, 128L) * 128 - 64 - size);
    }
}
