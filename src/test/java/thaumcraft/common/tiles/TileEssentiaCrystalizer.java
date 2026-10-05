package thaumcraft.common.tiles;

public class TileEssentiaCrystalizer extends net.minecraft.tileentity.TileEntity {
    public Color aspect;
    public float cr = 1.0F, cg = 1.0F, cb = 1.0F;

    public static class Color {
        private final int color;
        public Color(int color) { this.color = color; }
        public int getColor() { return color; }
    }
}
