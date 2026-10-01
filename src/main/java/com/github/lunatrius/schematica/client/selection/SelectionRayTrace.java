package com.github.lunatrius.schematica.client.selection;

import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Area;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Box;
import com.github.lunatrius.schematica.client.selection.AreaSelectionLibrary.Corner;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;

public final class SelectionRayTrace {
    private SelectionRayTrace() {}

    public static Hit trace(Area area, double x, double y, double z, double dx, double dy, double dz, double distance) {
        Hit corner = null, body = null;
        if (area == null) return null;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length == 0 || !Double.isFinite(length) || distance < 0) return null;
        double[] start = {x, y, z}, direction = {dx / length, dy / length, dz / length};
        for (Box box : area.boxes()) {
            Vector3i a = box.first(), b = box.second();
            for (Corner point : new Corner[] {Corner.FIRST, Corner.SECOND}) {
                Vector3i p = point == Corner.FIRST ? a : b;
                double hit = intersect(start, direction, p, p, distance);
                if (hit != Double.POSITIVE_INFINITY && (corner == null || hit < corner.distance)) corner = new Hit(box, point, hit);
            }
            double hit = intersect(start, direction, a, b, distance);
            if (hit != Double.POSITIVE_INFINITY && (body == null || hit < body.distance)) body = new Hit(box, Corner.NONE, hit);
        }
        Vector3i origin = area.manualOrigin();
        if (origin != null) {
            double hit = intersect(start, direction, origin, origin, distance);
            if (hit != Double.POSITIVE_INFINITY && (corner == null || hit <= corner.distance)) return new Hit(null, Corner.NONE, hit);
        }
        return corner != null ? corner : body;
    }

    private static double intersect(double[] start, double[] direction, Vector3i a, Vector3i b, double maxDistance) {
        int[] first = {a.x, a.y, a.z}, second = {b.x, b.y, b.z};
        double near = 0, far = maxDistance;
        for (int axis = 0; axis < 3; axis++) {
            double min = Math.min(first[axis], second[axis]), max = Math.max(first[axis], second[axis]) + 1.0;
            if (Math.abs(direction[axis]) < 1e-12) {
                if (start[axis] < min || start[axis] > max) return Double.POSITIVE_INFINITY;
            } else {
                double t1 = (min - start[axis]) / direction[axis], t2 = (max - start[axis]) / direction[axis];
                near = Math.max(near, Math.min(t1, t2));
                far = Math.min(far, Math.max(t1, t2));
                if (near > far) return Double.POSITIVE_INFINITY;
            }
        }
        return near;
    }

    public static double boxDistance(double x, double y, double z, double dx, double dy, double dz, Vector3i a, Vector3i b, double distance) {
        return intersect(new double[] {x, y, z}, new double[] {dx, dy, dz}, a, b, distance);
    }

    public static final class Hit {
        public final Box box;
        public final Corner corner;
        public final double distance;
        private Hit(Box box, Corner corner, double distance) { this.box = box; this.corner = corner; this.distance = distance; }
    }
}
