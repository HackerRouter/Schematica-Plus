package com.github.lunatrius.schematica.client.printer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidContainerRegistry.FluidContainerData;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fluids.IFluidContainerItem;
import com.github.lunatrius.schematica.client.printer.FluidContainerSupport.Use;

final class FluidPrinter {
    private FluidPrinter() {}

    static boolean isFluid(Block block) { return block instanceof BlockLiquid || block instanceof IFluidBlock; }

    static boolean sameFluid(Block first, Block second) {
        Fluid fluid = fluid(first);
        return fluid != null && fluid == fluid(second);
    }

    private static Fluid fluid(Block block) {
        if (block == Blocks.water || block == Blocks.flowing_water) return FluidRegistry.WATER;
        if (block == Blocks.lava || block == Blocks.flowing_lava) return FluidRegistry.LAVA;
        return block instanceof IFluidBlock ? ((IFluidBlock) block).getFluid() : null;
    }

    static FluidStack source(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block instanceof IFluidBlock) {
            IFluidBlock fluidBlock = (IFluidBlock) block;
            if (!fluidBlock.canDrain(world, x, y, z)) return null;
            FluidStack stack = fluidBlock.drain(world, x, y, z, false);
            return stack != null && stack.amount == FluidContainerRegistry.BUCKET_VOLUME ? stack : null;
        }
        Fluid fluid = fluid(block);
        return fluid != null && world.getBlockMetadata(x, y, z) == 0
            ? new FluidStack(fluid, FluidContainerRegistry.BUCKET_VOLUME) : null;
    }

    static boolean matches(World schematic, int x, int y, int z, World world, int wx, int wy, int wz) {
        Fluid expected = fluid(schematic.getBlock(x, y, z));
        if (expected == null || expected != fluid(world.getBlock(wx, wy, wz))) return false;
        FluidStack required = source(schematic, x, y, z);
        return required == null || required.isFluidEqual(source(world, wx, wy, wz));
    }

    private static boolean isContainer(ItemStack stack, FluidStack fluid) {
        if (stack == null || stack.stackSize <= 0 || FluidContainerSupport.use(stack.getItem()) == null) return false;
        ItemStack copy = stack.copy();
        copy.stackSize = 1;
        FluidStack contained = copy.getItem() instanceof IFluidContainerItem
            ? ((IFluidContainerItem) copy.getItem()).drain(copy, FluidContainerRegistry.BUCKET_VOLUME, false)
            : FluidContainerRegistry.getFluidForFilledItem(copy);
        return contained != null && contained.amount == FluidContainerRegistry.BUCKET_VOLUME
            && contained.isFluidEqual(fluid);
    }

    private static List<ItemStack> findContainers(EntityClientPlayerMP player, FluidStack fluid) {
        List<ItemStack> containers = new ArrayList<>();
        for (ItemStack stack : player.inventory.mainInventory) {
            if (isContainer(stack, fluid)) containers.add(stack.copy());
        }
        if (player.capabilities.isCreativeMode) {
            for (FluidContainerData container : FluidContainerRegistry.getRegisteredFluidContainerData()) {
                if (isContainer(container.filledContainer, fluid)) containers.add(container.filledContainer.copy());
            }
        }
        return containers;
    }

    static boolean place(Minecraft minecraft, FluidStack fluid, int x, int y, int z,
        Predicate<ItemStack> selectContainer) {
        if (fluid == null || !fluid.getFluid().canBePlacedInWorld()) return false;
        EntityClientPlayerMP player = minecraft.thePlayer;
        World world = minecraft.theWorld;
        if (fluid.getFluid() == FluidRegistry.WATER && world.provider.isHellWorld) return false;
        List<ItemStack> containers = findContainers(player, fluid);
        if (containers.isEmpty()) return false;
        double eyeY = player.posY + player.getEyeHeight() - player.getDefaultEyeHeight();
        Vec3 start = Vec3.createVectorHelper(player.posX, eyeY, player.posZ);
        double reach = Math.min(5.0, minecraft.playerController.getBlockReachDistance());
        float yaw = player.rotationYaw;
        float pitch = player.rotationPitch;
        boolean sentLook = false;
        try {
            for (ItemStack container : containers) {
                Use use = FluidContainerSupport.use(container.getItem());
                if (use == Use.FORESTRY_BUCKET && world.provider.isHellWorld
                    && fluid.getFluid() != FluidRegistry.LAVA) continue;
                for (ForgeDirection neighbor : ForgeDirection.VALID_DIRECTIONS) {
                    double hitX = x + 0.5 + neighbor.offsetX * 0.55;
                    double hitY = y + 0.5 + neighbor.offsetY * 0.55;
                    double hitZ = z + 0.5 + neighbor.offsetZ * 0.55;
                    PlacementAim aim = new PlacementAim(hitX - start.xCoord, hitY - start.yCoord, hitZ - start.zCoord);
                    player.rotationYaw = aim.yaw;
                    player.rotationPitch = aim.pitch;
                    Vec3 look = player.getLook(1.0f);
                    MovingObjectPosition hit = world.func_147447_a(start,
                        start.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach),
                        use.stopOnLiquids, !use.stopOnLiquids, false);
                    if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) continue;
                    ForgeDirection face = ForgeDirection.getOrientation(hit.sideHit);
                    if (hit.blockX + face.offsetX != x || hit.blockY + face.offsetY != y
                        || hit.blockZ + face.offsetZ != z) continue;
                    Block clicked = world.getBlock(hit.blockX, hit.blockY, hit.blockZ);
                    if (use.stopOnLiquids && isFluid(clicked)) continue;
                    if (use.onBlock && (!clicked.getMaterial().isSolid()
                        || clicked.isReplaceable(world, hit.blockX, hit.blockY, hit.blockZ)
                        || world.getTileEntity(hit.blockX, hit.blockY, hit.blockZ) != null)) continue;
                    if (!selectContainer.test(container)) break;
                    if (ForgeEventFactory.onPlayerInteract(player,
                        use.onBlock ? Action.RIGHT_CLICK_BLOCK : Action.RIGHT_CLICK_AIR,
                        use.onBlock ? hit.blockX : 0, use.onBlock ? hit.blockY : 0,
                        use.onBlock ? hit.blockZ : 0, use.onBlock ? hit.sideHit : -1, world).isCanceled()) return false;
                    player.sendQueue.addToSendQueue(new C05PacketPlayerLook(aim.yaw, aim.pitch, player.onGround));
                    sentLook = true;
                    if (use.onBlock) {
                        minecraft.playerController.onPlayerRightClick(player, minecraft.theWorld,
                            player.getCurrentEquippedItem(), hit.blockX, hit.blockY, hit.blockZ, hit.sideHit, hit.hitVec);
                    } else {
                        minecraft.playerController.sendUseItem(player, world, player.getCurrentEquippedItem());
                    }
                    player.swingItem();
                    return true;
                }
            }
            return false;
        } finally {
            player.rotationYaw = yaw;
            player.rotationPitch = pitch;
            if (sentLook) player.sendQueue.addToSendQueue(new C05PacketPlayerLook(yaw, pitch, player.onGround));
        }
    }
}
