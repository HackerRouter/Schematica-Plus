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
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.client.world.SchematicLibrary;
import com.github.lunatrius.schematica.client.world.SchematicSourceData;
import com.github.lunatrius.schematica.handler.client.WorldHandler;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.util.Coordinates;

public final class SchematicGuiLoader {

    private SchematicGuiLoader() {}

    public static SchematicLibrary.Source<SchematicSourceData> load(Minecraft minecraft, File file, boolean createPlacement) throws IOException {
        if (minecraft.theWorld == null || minecraft.thePlayer == null || !SchematicaPlus.proxy.isLoadEnabled) {
            throw new IOException("Schematic loading is unavailable in this world");
        }
        SchematicLibrary.Source<SchematicSourceData> source = ClientProxy.loadSource(file);
        if (createPlacement) createPlacement(minecraft, source, true);
        WorldHandler.INSTANCE.saveSession();
        return source;
    }

    public static SchematicWorld createPlacement(Minecraft minecraft, SchematicLibrary.Source<SchematicSourceData> source,
        boolean restoreCoordinates) throws IOException {
        if (minecraft.theWorld == null || minecraft.thePlayer == null || !SchematicaPlus.proxy.isLoadEnabled) {
            throw new IOException("Schematic loading is unavailable in this world");
        }
        SchematicWorld previous = ClientProxy.schematic;
        SchematicWorld schematic = ClientProxy.createPlacement(source);
        try {
            Coordinates coord = restoreCoordinates ? ClientProxy.getCoordinates(worldServerName(minecraft), schematic.name) : null;
            if (!restoreCoordinates) {
                ClientProxy.moveSchematicToPlayer(schematic);
            } else if (coord == null) {
                moveToLookTarget(minecraft, schematic);
            } else {
                ForgeDirection[] axes = {ForgeDirection.EAST, ForgeDirection.UP, ForgeDirection.SOUTH};
                int[] rotations = {coord.rotX, coord.rotY, coord.rotZ};
                int[] flips = {coord.flipX, coord.flipY, coord.flipZ};
                for (int axis = 0; axis < axes.length; axis++) {
                    for (int i = 0; i < Math.floorMod(rotations[axis], 4); i++) schematic.rotate(axes[axis]);
                }
                for (int axis = 0; axis < axes.length; axis++) {
                    if (Math.floorMod(flips[axis], 2) != 0) schematic.flip(axes[axis]);
                }
                ClientProxy.moveSchematic(schematic, coord.posX, coord.posY, coord.posZ);
            }
            if (net.minecraft.client.gui.GuiScreen.isShiftKeyDown()) schematic.setPlacementSettings(schematic.placementSettings().enabled(false));
            ClientProxy.selectSchematic(schematic);
            SchematicPrinter.INSTANCE.refresh();
            WorldHandler.INSTANCE.saveSession();
            return schematic;
        } catch (RuntimeException e) {
            ClientProxy.removePlacement(schematic);
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
            schematic.moveOriginTo(target.blockX, target.blockY + 1, target.blockZ);
        } else {
            ClientProxy.moveSchematicToPlayer(schematic);
        }
    }
}
