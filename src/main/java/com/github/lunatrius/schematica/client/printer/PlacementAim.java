package com.github.lunatrius.schematica.client.printer;

final class PlacementAim {
    final float yaw;
    final float pitch;

    PlacementAim(double dx, double dy, double dz) {
        this.yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);
        this.pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
    }
}
