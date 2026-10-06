package openmods.block;

public class OpenBlock extends net.minecraft.block.Block {
    public OpenBlock() { super(net.minecraft.block.material.Material.rock); }

    public BlockRotationMode getRotationMode() { return BlockRotationMode.FOUR_DIRECTIONS; }
}
