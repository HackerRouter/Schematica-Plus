// StructureLib ExtendedFacing under schematic rotation and mirroring, by HackerRouter, 2026.
package com.github.lunatrius.schematica.compat;

import java.lang.reflect.Method;

import net.minecraftforge.common.util.ForgeDirection;

import com.github.lunatrius.schematica.util.SchematicTransform;

/**
 * An ExtendedFacing (front, rotation, flip) is a frame of three world directions; the transformed frame is turned
 * back into the ExtendedFacing with the same left, up and back directions. Mirrors give the opposite handedness,
 * which a flip holds. Candidates that the caller's limits reject (such as vertical flips of GT multiblocks) are
 * avoided when an equivalent one exists.
 */
final class StructureLibFacing {
    private static final String PACKAGE = "com.gtnewhorizon.structurelib.alignment.enumerable.";

    private StructureLibFacing() {}

    /** Returns {front ordinal, rotation index, flip index}, or null when StructureLib is missing. */
    static int[] transform(ForgeDirection front, int rotation, int flip, char op, boolean allowVerticalFlip, boolean allowFlip)
        throws ReflectiveOperationException {
        Class<?> facing, rotations, flips;
        try {
            facing = Class.forName(PACKAGE + "ExtendedFacing");
            rotations = Class.forName(PACKAGE + "Rotation");
            flips = Class.forName(PACKAGE + "Flip");
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
        Object current = facing.getMethod("of", ForgeDirection.class, rotations, flips).invoke(null, front,
            rotations.getMethod("byIndex", int.class).invoke(null, rotation), flips.getMethod("byIndex", int.class).invoke(null, flip));
        Method left = facing.getMethod("getRelativeLeftInWorld"), up = facing.getMethod("getRelativeUpInWorld"),
            back = facing.getMethod("getRelativeBackInWorld"), direction = facing.getMethod("getDirection"),
            rotationOf = facing.getMethod("getRotation"), flipOf = facing.getMethod("getFlip");
        ForgeDirection wantLeft = SchematicTransform.direction(op, (ForgeDirection) left.invoke(current));
        ForgeDirection wantUp = SchematicTransform.direction(op, (ForgeDirection) up.invoke(current));
        ForgeDirection wantBack = SchematicTransform.direction(op, (ForgeDirection) back.invoke(current));
        Object best = null;
        int bestScore = -1;
        for (Object candidate : (Object[]) facing.getField("VALUES").get(null)) {
            if (up.invoke(candidate) != wantUp || back.invoke(candidate) != wantBack) continue;
            Object candidateFlip = flipOf.invoke(candidate);
            boolean vertical = (Boolean) flips.getMethod("isVerticallyFliped").invoke(candidateFlip);
            boolean flipped = !(Boolean) flips.getMethod("isNotFlipped").invoke(candidateFlip);
            int score = (left.invoke(candidate) == wantLeft ? 4 : 0) + (allowVerticalFlip || !vertical ? 2 : 0) + (allowFlip || !flipped ? 1 : 0);
            if (score > bestScore) { best = candidate; bestScore = score; }
        }
        if (best == null) return null;
        return new int[] {((ForgeDirection) direction.invoke(best)).ordinal(),
            (Integer) rotations.getMethod("getIndex").invoke(rotationOf.invoke(best)),
            (Integer) flips.getMethod("getIndex").invoke(flipOf.invoke(best))};
    }

    /** The ExtendedFacing value for the triple, for in-memory fields. */
    static Object of(int[] triple) throws ReflectiveOperationException {
        Class<?> facing = Class.forName(PACKAGE + "ExtendedFacing"), rotations = Class.forName(PACKAGE + "Rotation"),
            flips = Class.forName(PACKAGE + "Flip");
        return facing.getMethod("of", ForgeDirection.class, rotations, flips).invoke(null, ForgeDirection.getOrientation(triple[0]),
            rotations.getMethod("byIndex", int.class).invoke(null, triple[1]), flips.getMethod("byIndex", int.class).invoke(null, triple[2]));
    }

    /** {front ordinal, rotation index, flip index} of an ExtendedFacing value. */
    static int[] triple(Object extendedFacing) throws ReflectiveOperationException {
        Object rotation = extendedFacing.getClass().getMethod("getRotation").invoke(extendedFacing);
        Object flip = extendedFacing.getClass().getMethod("getFlip").invoke(extendedFacing);
        return new int[] {((ForgeDirection) extendedFacing.getClass().getMethod("getDirection").invoke(extendedFacing)).ordinal(),
            (Integer) rotation.getClass().getMethod("getIndex").invoke(rotation), (Integer) flip.getClass().getMethod("getIndex").invoke(flip)};
    }
}
