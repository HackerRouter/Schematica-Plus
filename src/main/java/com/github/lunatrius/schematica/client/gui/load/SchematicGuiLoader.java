package com.github.lunatrius.schematica.client.gui.load;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.printer.SchematicPrinter;
import com.github.lunatrius.schematica.client.world.Placements;
import com.github.lunatrius.schematica.client.world.SchematicLibrary;
import com.github.lunatrius.schematica.client.world.SchematicSourceData;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.client.WorldHandler;


public final class SchematicGuiLoader {

    private SchematicGuiLoader() {}

    public static SchematicLibrary.Source<SchematicSourceData> load(Minecraft minecraft, File file, boolean createPlacement) throws IOException {
        if (minecraft.theWorld == null || minecraft.thePlayer == null || !SchematicaPlus.proxy.isLoadEnabled) {
            throw new IOException("Schematic loading is unavailable in this world");
        }
        SchematicLibrary.Source<SchematicSourceData> source = Placements.loadSource(file);
        if (createPlacement) createPlacement(minecraft, source, true);
        WorldHandler.INSTANCE.saveSession();
        return source;
    }

    /** load() with the file read on a worker thread; done or failed runs on the client thread afterwards. */
    public static boolean loadAsync(Minecraft minecraft, File file, boolean createPlacement,
        Consumer<SchematicLibrary.Source<SchematicSourceData>> done, Consumer<Exception> failed) throws IOException {
        if (minecraft.theWorld == null || minecraft.thePlayer == null || !SchematicaPlus.proxy.isLoadEnabled) {
            throw new IOException("Schematic loading is unavailable in this world");
        }
        return Placements.loadSourceAsync(file, source -> {
            try {
                if (createPlacement) createPlacement(minecraft, source, true);
                WorldHandler.INSTANCE.saveSession();
                done.accept(source);
            } catch (IOException | RuntimeException e) {
                failed.accept(e);
            }
        }, failed);
    }

    public static SchematicWorld createPlacement(Minecraft minecraft, SchematicLibrary.Source<SchematicSourceData> source,
        boolean atLookTarget) throws IOException {
        if (minecraft.theWorld == null || minecraft.thePlayer == null || !SchematicaPlus.proxy.isLoadEnabled) {
            throw new IOException("Schematic loading is unavailable in this world");
        }
        SchematicWorld previous = Placements.selected;
        SchematicWorld schematic = Placements.create(source);
        try {
            if (atLookTarget) {
                moveToLookTarget(minecraft, schematic);
            } else {
                Placements.moveToPlayer(schematic);
            }
            if (GuiScreen.isShiftKeyDown()) schematic.setPlacementSettings(schematic.placementSettings().enabled(false));
            Placements.select(schematic);
            SchematicPrinter.INSTANCE.refresh();
            WorldHandler.INSTANCE.saveSession();
            return schematic;
        } catch (RuntimeException e) {
            Placements.remove(schematic);
            if (previous != null && Placements.loaded.contains(previous)) Placements.select(previous);
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
            Placements.moveToPlayer(schematic);
        }
    }
}
