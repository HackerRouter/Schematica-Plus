package openmods.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public class OpenBlock extends Block {
    public OpenBlock() { super(Material.rock); }

    public BlockRotationMode getRotationMode() { return BlockRotationMode.FOUR_DIRECTIONS; }
}
