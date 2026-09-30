package com.github.lunatrius.schematica.client.gui.load;

import static com.github.lunatrius.schematica.client.util.WorldServerName.worldServerName;

import java.io.File;
import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.printer.SchematicPrinter;
import com.github.lunatrius.schematica.client.renderer.RendererSchematicGlobal;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.client.WorldHandler;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.util.Coordinates;

final class SchematicGuiLoader {

    private SchematicGuiLoader() {}

    static SchematicWorld load(Minecraft minecraft, File file) throws IOException {
        SchematicWorld previous = ClientProxy.schematic;
        if (!SchematicaPlus.proxy.loadSchematic(null, file.getParentFile(), file.getName())) {
            throw new IOException("Unable to read schematic: " + file);
        }
        SchematicWorld schematic = ClientProxy.schematic;
        try {
            Coordinates coord = ClientProxy.getCoordinates(worldServerName(minecraft), schematic.name);
            if (coord == null) {
                moveToLookTarget(minecraft, schematic);
            } else {
                ClientProxy.moveSchematic(schematic, coord.posX, coord.posY, coord.posZ);
                ForgeDirection[] axes = {ForgeDirection.EAST, ForgeDirection.UP, ForgeDirection.SOUTH};
                int[] rotations = {coord.rotX, coord.rotY, coord.rotZ};
                int[] flips = {coord.flipX, coord.flipY, coord.flipZ};
                for (int axis = 0; axis < axes.length; axis++) {
                    for (int i = 0; i < Math.floorMod(rotations[axis], 4); i++) schematic.rotate(axes[axis]);
                }
                for (int axis = 0; axis < axes.length; axis++) {
                    if (Math.floorMod(flips[axis], 2) != 0) schematic.flip(axes[axis]);
                }
            }
            RendererSchematicGlobal.INSTANCE.createRendererSchematicChunks(schematic);
            SchematicPrinter.INSTANCE.refresh();
            WorldHandler.INSTANCE.saveSession();
            return schematic;
        } catch (RuntimeException e) {
            SchematicaPlus.proxy.unloadSchematic();
            if (previous != null && ClientProxy.loadedSchematics.contains(previous)) ClientProxy.selectSchematic(previous);
            WorldHandler.INSTANCE.saveSession();
            throw e;
        }
    }

    private static void moveToLookTarget(Minecraft minecraft, SchematicWorld schematic) {
        EntityPlayer player = minecraft.thePlayer;
        Vec3 eye = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3 look = player.getLookVec();
        Vec3 end = eye.addVector(look.xCoord * 256, look.yCoord * 256, look.zCoord * 256);
        MovingObjectPosition target = player.worldObj.rayTraceBlocks(eye, end);
        if (target != null && target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            schematic.position.set(target.blockX, target.blockY + 1, target.blockZ);
        } else {
            ClientProxy.moveSchematicToPlayer(schematic);
        }
    }
}
