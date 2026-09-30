package com.github.lunatrius.schematica.client.renderer;

final class SchematicFrustum {
    private final double[] clip = new double[16];
    private final double[][] planes = new double[6][4];

    void update(float[] projection, float[] modelView) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                double value = 0;
                for (int k = 0; k < 4; k++) {
                    value += (double) projection[k * 4 + row] * modelView[column * 4 + k];
                }
                clip[column * 4 + row] = value;
            }
        }
        for (int axis = 0; axis < 3; axis++) {
            for (int column = 0; column < 4; column++) {
                planes[axis * 2][column] = clip[column * 4 + 3] + clip[column * 4 + axis];
                planes[axis * 2 + 1][column] = clip[column * 4 + 3] - clip[column * 4 + axis];
            }
        }
    }

    boolean isVisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        for (double[] plane : planes) {
            double x = plane[0] >= 0 ? maxX : minX;
            double y = plane[1] >= 0 ? maxY : minY;
            double z = plane[2] >= 0 ? maxZ : minZ;
            if (plane[0] * x + plane[1] * y + plane[2] * z + plane[3] < -1.0e-6) {
                return false;
            }
        }
        return true;
    }
}
