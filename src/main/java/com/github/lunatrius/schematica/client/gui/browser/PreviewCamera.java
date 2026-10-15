package com.github.lunatrius.schematica.client.gui.browser;

/** View-space navigation, independent of the game camera and the schematic data. */
final class PreviewCamera {
    static final float TAN_HALF_FOV = (float) Math.tan(Math.toRadians(25));
    final float extent;
    float yaw = 45, pitch = 30, zoom = 1, x, y, z;
    private float aspect = 1;
    boolean rotating = true;

    PreviewCamera(float extent) { this.extent = Math.max(1, extent); }

    void reset() {
        yaw = 45; pitch = 30; zoom = 1;
        x = y = z = 0;
    }

    void viewport(int width, int height) { aspect = (float) Math.max(1, width) / Math.max(1, height); }

    float distance() {
        float tangent = TAN_HALF_FOV * Math.min(1, aspect);
        return extent * 0.525f * (float) Math.sqrt(1 + tangent * tangent) / (tangent * zoom);
    }

    void turn(float dx, float dy) {
        rotating = false;
        yaw = (yaw + dx * 0.5f) % 360;
        pitch = Math.max(-90, Math.min(90, pitch + dy * 0.5f));
    }

    void zoom(int wheel) {
        if (wheel == 0) return;
        zoom = Math.max(0.125f, Math.min(32, zoom * (wheel > 0 ? 1.25f : 0.8f)));
    }

    void pan(float dx, float dy, int height) {
        rotating = false;
        float scale = 2 * Math.max(extent * 0.02f, Math.abs(distance() - z)) * TAN_HALF_FOV / Math.max(1, height);
        x += dx * scale;
        y -= dy * scale;
        clampPosition();
    }

    void advance(float seconds, int right, int up, int forward) {
        float dt = Math.max(0, Math.min(0.1f, seconds));
        float length = (float) Math.sqrt(right * right + up * up + forward * forward);
        if (length > 0) {
            rotating = false;
            float speed = extent * 0.5f * dt / length;
            x -= right * speed;
            y -= up * speed;
            z += forward * speed;
            clampPosition();
        } else if (rotating) yaw = (yaw + dt * 20) % 360;
    }

    private void clampPosition() {
        float limit = extent * 100;
        x = Math.max(-limit, Math.min(limit, x));
        y = Math.max(-limit, Math.min(limit, y));
        z = Math.max(-limit, Math.min(limit, z));
    }
}
