// Native ProjectRed multipart placement with matching item and orientation, by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.printer;

import java.lang.reflect.Method;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.github.lunatrius.schematica.compat.MultipartItems;
import com.github.lunatrius.schematica.compat.MultipartItems.Part;
import com.github.lunatrius.schematica.reference.Reference;

final class MultipartPlacement {
    static final class Plan {
        final ItemStack stack;
        final int face;
        final PrinterLook look;
        final boolean sneak;
        final Vec3 hit;
        Plan(ItemStack stack, int face, PrinterLook look, boolean sneak, Vec3 hit) {
            this.stack = stack; this.face = face; this.look = look; this.sneak = sneak; this.hit = hit;
        }
    }

    private MultipartPlacement() {}

    static Plan plan(EntityPlayer player, World world, TileEntity template, int x, int y, int z) {
        float yaw = player.rotationYaw, pitch = player.rotationPitch;
        try {
            List<Part> expected = MultipartItems.parts(template), actual = MultipartItems.parts(world.getTileEntity(x, y, z));
            for (Part part : MultipartItems.missing(expected, actual)) {
                if (part.stack == null || part.stack.getItem() == null
                    || !part.stack.getItem().getClass().getName().startsWith("mrtjp.projectred.")) continue;
                int mounted = -1;
                for (int i = 0; i < actual.size(); i++) if (part.sameGateMount(actual.get(i))) { mounted = i; break; }
                if (mounted >= 0) {
                    Plan adjustment = screwdriver(player, world, x, y, z, part, actual.get(mounted), mounted);
                    if (adjustment != null) return adjustment;
                    continue;
                }
                Plan found = probe(player, world, part, x, y, z, yaw, pitch);
                if (found != null) return found;
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            Reference.logger.debug("Could not plan multipart placement", e);
        } finally {
            player.rotationYaw = yaw;
            player.rotationPitch = pitch;
        }
        return null;
    }

    private static Plan screwdriver(EntityPlayer player, World world, int x, int y, int z, Part wanted, Part actual,
        int partIndex) throws ReflectiveOperationException {
        ForgeDirection support = ForgeDirection.getOrientation((actual.state.getByte("orient") & 255) >> 2);
        Vec3 eye = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight() - player.getDefaultEyeHeight(), player.posZ);
        Vec3 end = Vec3.createVectorHelper(x + 0.5 + support.offsetX * 0.49, y + 0.5 + support.offsetY * 0.49,
            z + 0.5 + support.offsetZ * 0.49);
        MovingObjectPosition hit = world.rayTraceBlocks(eye, end);
        if (hit == null || hit.blockX != x || hit.blockY != y || hit.blockZ != z
            || eye.distanceTo(hit.hitVec) > Minecraft.getMinecraft().playerController.getBlockReachDistance()) return null;
        Object data = Class.forName("codechicken.lib.raytracer.ExtendedMOP")
            .getMethod("getData", MovingObjectPosition.class).invoke(null, hit);
        if (!Class.forName("scala.Tuple2").isInstance(data)
            || !Integer.valueOf(partIndex).equals(data.getClass().getMethod("_1").invoke(data))) return null;
        Class<?> screwdriver = Class.forName("mrtjp.projectred.api.IScrewdriver");
        for (ItemStack tool : player.inventory.mainInventory) {
            if (tool == null || !screwdriver.isInstance(tool.getItem())
                || !Boolean.TRUE.equals(screwdriver.getMethod("canUse", EntityPlayer.class, ItemStack.class).invoke(tool.getItem(), player, tool))) continue;
            boolean configure = wanted.state.getByte("orient") == actual.state.getByte("orient");
            PlacementAim aim = new PlacementAim(end.xCoord - eye.xCoord, end.yCoord - eye.yCoord, end.zCoord - eye.zCoord);
            return new Plan(tool, hit.sideHit, new PrinterLook(aim.yaw, aim.pitch), configure, hit.hitVec);
        }
        return null;
    }

    private static Plan probe(EntityPlayer player, World world, Part wanted, int x, int y, int z, float yaw, float pitch)
        throws ReflectiveOperationException {
        ClassLoader loader = MultipartPlacement.class.getClassLoader();
        Class<?> coord = Class.forName("codechicken.lib.vec.BlockCoord", true, loader);
        Class<?> vector = Class.forName("codechicken.lib.vec.Vector3", true, loader);
        Class<?> nativePart = Class.forName("codechicken.multipart.TMultiPart", true, loader);
        Method create = wanted.stack.getItem().getClass().getMethod("newPart", ItemStack.class, EntityPlayer.class,
            World.class, coord, int.class, vector);
        Method canPlace = Class.forName("codechicken.multipart.TileMultipart", true, loader)
            .getMethod("canPlacePart", World.class, coord, nativePart);
        PrinterLook[] turns = PrinterLook.candidates(yaw);
        int support = wanted.state.hasKey("orient") ? (wanted.state.getByte("orient") & 255) >> 2
            : wanted.state.hasKey("side") ? wanted.state.getByte("side") & 255 : -1;
        for (int face = 0; face < 6; face++) {
            if (support >= 0 && face != (support ^ 1)) continue;
            for (int i = -1; i < turns.length; i++) {
                PrinterLook look = i < 0 ? new PrinterLook(yaw, pitch) : turns[i];
                player.rotationYaw = look.yaw;
                player.rotationPitch = look.pitch;
                Object pos = coord.getConstructor(int.class, int.class, int.class).newInstance(x, y, z);
                Object hit = vector.getConstructor(double.class, double.class, double.class).newInstance(0.5, 0.5, 0.5);
                Object candidate = create.invoke(wanted.stack.getItem(), wanted.stack.copy(), player, world, pos, face, hit);
                if (candidate != null && wanted.placeableAs(MultipartItems.describe(candidate))
                    && (Boolean) canPlace.invoke(null, world, pos, candidate)) return new Plan(wanted.stack, face, look, true, Vec3.createVectorHelper(x + 0.5, y + 0.5, z + 0.5));
            }
        }
        return null;
    }
}
