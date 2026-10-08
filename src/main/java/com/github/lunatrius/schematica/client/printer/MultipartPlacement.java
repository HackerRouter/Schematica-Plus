// Native ProjectRed multipart placement with matching item and orientation, by HackerRouter, 2026.
package com.github.lunatrius.schematica.client.printer;

import java.lang.reflect.Method;
import java.util.List;

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
        Plan(ItemStack stack, int face, PrinterLook look) { this.stack = stack; this.face = face; this.look = look; }
    }

    private MultipartPlacement() {}

    static Plan plan(EntityPlayer player, World world, TileEntity template, int x, int y, int z) {
        float yaw = player.rotationYaw, pitch = player.rotationPitch;
        try {
            List<Part> expected = MultipartItems.parts(template), actual = MultipartItems.parts(world.getTileEntity(x, y, z));
            for (Part part : MultipartItems.missing(expected, actual)) {
                if (part.stack == null || part.stack.getItem() == null
                    || !part.stack.getItem().getClass().getName().startsWith("mrtjp.projectred.")) continue;
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
                if (candidate != null && wanted.matches(MultipartItems.describe(candidate))
                    && (Boolean) canPlace.invoke(null, world, pos, candidate)) return new Plan(wanted.stack, face, look);
            }
        }
        return null;
    }
}
