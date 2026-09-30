package com.github.lunatrius.schematica.client.world;

import java.util.List;

import net.minecraftforge.common.util.ForgeDirection;

import com.github.lunatrius.schematica.client.gui.placement.PlacementTransform;

public final class PlacementState {

    private PlacementState() {}

    public static void applyTransforms(SchematicWorld world, List<String> operations) {
        for (String op : operations) {
            if (op == null || op.length() != 1 || "XYZxyz".indexOf(op.charAt(0)) < 0) {
                throw new IllegalArgumentException("Invalid saved transformation");
            }
            char axis = Character.toUpperCase(op.charAt(0));
            ForgeDirection direction = axis == 'X' ? ForgeDirection.EAST : axis == 'Y' ? ForgeDirection.UP : ForgeDirection.SOUTH;
            int count = world.transformOperations.size();
            if (Character.isLowerCase(op.charAt(0))) world.flip(direction);
            else world.rotate(direction);
            if (world.transformOperations.size() != count + 1) throw new IllegalArgumentException("Source does not support this transform");
        }
    }

    public static void copy(SchematicWorld previous, SchematicWorld next) {
        applyTransforms(next, previous.transformOperations);
        int[] minimum = PlacementTransform.reloadedMinimum(new int[] {previous.position.x, previous.position.y, previous.position.z},
            new int[] {previous.getWidth(), previous.getHeight(), previous.getLength()},
            new int[] {next.getWidth(), next.getHeight(), next.getLength()}, previous.transformOperations);
        next.position.set(minimum[0], minimum[1], minimum[2]);
        next.name = previous.name;
        next.isRendering = previous.isRendering;
        next.isRenderingEntities = previous.isRenderingEntities;
        next.isPastingBlockNBT = previous.isPastingBlockNBT;
        next.isRenderingLayer = previous.isRenderingLayer;
        next.renderingLayer = Math.max(0, Math.min(previous.renderingLayer, next.getHeight() - 1));
    }
}
