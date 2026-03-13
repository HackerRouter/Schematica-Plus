package com.github.lunatrius.schematica.proxy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.lunatrius.core.util.vector.Vector3d;
import com.github.lunatrius.core.util.vector.Vector3i;
import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.client.printer.SchematicPrinter;
import com.github.lunatrius.schematica.client.renderer.RendererSchematicGlobal;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.compat.ILOTRPresent;
import com.github.lunatrius.schematica.compat.NoLOTRProxy;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.client.ChatEventHandler;
import com.github.lunatrius.schematica.handler.client.InputHandler;
import com.github.lunatrius.schematica.handler.client.OverlayHandler;
import com.github.lunatrius.schematica.handler.client.RenderTickHandler;
import com.github.lunatrius.schematica.handler.client.TickHandler;
import com.github.lunatrius.schematica.handler.client.ToolItemHandler;
import com.github.lunatrius.schematica.handler.client.WorldHandler;
import com.github.lunatrius.schematica.reference.Constants;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.Coordinates;
import com.github.lunatrius.schematica.world.schematic.SchematicFormat;
import com.github.lunatrius.schematica.command.CommandSchematicaSetBlock;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import cpw.mods.fml.client.config.GuiConfigEntries;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    public static final Vector3d playerPosition = new Vector3d();
    public static final Vector3i pointA = new Vector3i();
    public static final Vector3i pointB = new Vector3i();
    public static final Vector3i pointMin = new Vector3i();
    public static final Vector3i pointMax = new Vector3i();
    private static final Minecraft MINECRAFT = Minecraft.getMinecraft();
    public static boolean isRenderingGuide = false;
    public static boolean isPendingReset = false;
    /** Set to true after resetSettings clears schematics; WorldHandler.onLoad will restore them. */
    public static boolean isPendingRestore = false;
    public static ForgeDirection orientation = ForgeDirection.UNKNOWN;
    public static int rotationRender = 0;
    /** The currently active/selected schematic (for tools, printer, control GUI). */
    public static SchematicWorld schematic = null;
    /** All loaded schematics. The active schematic is always in this list. */
    public static final List<SchematicWorld> loadedSchematics = new ArrayList<>();
    public static MovingObjectPosition movingObjectPosition = null;
    public static ILOTRPresent lotrProxy = null;
    /** Tracks the last known world/server name for reliable save on disconnect. */
    public static String lastWorldServerName = null;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting()
        .create();
    private static final Type schematicDataType = new TypeToken<Map<String, Map<String, SchematicData>>>() {}.getType();
    private static final Type loadedSchematicsDataType = new TypeToken<Map<String, List<LoadedSchematicEntry>>>() {}.getType();
    private static final Type areaSelectionDataType = new TypeToken<Map<String, AreaSelectionData>>() {}.getType();

    public static void setPlayerData(EntityPlayer player, float partialTicks) {
        playerPosition.x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        playerPosition.y = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        playerPosition.z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        orientation = getOrientation(player);

        rotationRender = MathHelper.floor_double(player.rotationYaw / 90) & 3;
    }

    private static ForgeDirection getOrientation(EntityPlayer player) {
        if (player.rotationPitch > 45) {
            return ForgeDirection.DOWN;
        } else if (player.rotationPitch < -45) {
            return ForgeDirection.UP;
        } else {
            switch (MathHelper.floor_double(player.rotationYaw / 90.0 + 0.5) & 3) {
                case 0:
                    return ForgeDirection.SOUTH;
                case 1:
                    return ForgeDirection.WEST;
                case 2:
                    return ForgeDirection.NORTH;
                case 3:
                    return ForgeDirection.EAST;
            }
        }

        return ForgeDirection.UNKNOWN;
    }

    public static void updatePoints() {
        pointMin.x = Math.min(pointA.x, pointB.x);
        pointMin.y = Math.min(pointA.y, pointB.y);
        pointMin.z = Math.min(pointA.z, pointB.z);

        pointMax.x = Math.max(pointA.x, pointB.x);
        pointMax.y = Math.max(pointA.y, pointB.y);
        pointMax.z = Math.max(pointA.z, pointB.z);
    }

    public static void movePointToPlayer(Vector3i point) {
        point.x = (int) Math.floor(playerPosition.x);
        point.y = (int) Math.floor(playerPosition.y - 1);
        point.z = (int) Math.floor(playerPosition.z);

        switch (rotationRender) {
            case 0:
                point.x -= 1;
                point.z += 1;
                break;
            case 1:
                point.x -= 1;
                point.z -= 1;
                break;
            case 2:
                point.x += 1;
                point.z -= 1;
                break;
            case 3:
                point.x += 1;
                point.z += 1;
                break;
        }
    }

    public static void moveSchematicToPlayer(SchematicWorld schematic) {
        if (schematic != null) {
            Vector3i position = schematic.position;
            position.x = (int) Math.floor(playerPosition.x);
            position.y = (int) Math.floor(playerPosition.y) - 1;
            position.z = (int) Math.floor(playerPosition.z);

            switch (rotationRender) {
                case 0:
                    position.x -= schematic.getWidth();
                    position.z += 1;
                    break;
                case 1:
                    position.x -= schematic.getWidth();
                    position.z -= schematic.getLength();
                    break;
                case 2:
                    position.x += 1;
                    position.z -= schematic.getLength();
                    break;
                case 3:
                    position.x += 1;
                    position.z += 1;
                    break;
            }
        }
    }

    public static void moveSchematic(SchematicWorld schematic, Integer x, Integer y, Integer z) {
        if (schematic != null) {
            Vector3i position = schematic.position;
            position.x = x;
            position.y = y;
            position.z = z;
        }
    }

    /** Persistence data for area selection (pointA, pointB, isRenderingGuide). */
    private static class AreaSelectionData {
        public int ax, ay, az;
        public int bx, by, bz;
        public boolean renderingGuide;
        AreaSelectionData() {}
    }

    private static class SchematicData {

        public int X;
        public int Y;
        public int Z;
        // default value is zero, ensuring backwards compatibility for updates that don't store the rotation
        public int RotationX;
        public int Rotation;
        public int RotationZ;
        public int FlipX;
        public int FlipY;
        public int FlipZ;

        SchematicData() {}
    }

    /** Persistence entry for saving/restoring loaded schematics across sessions. */
    private static class LoadedSchematicEntry {
        public String filename;
        public String directory;
        public int X, Y, Z;
        public int RotationX, RotationY, RotationZ;
        public int FlipX, FlipY, FlipZ;
        public boolean isActive;

        LoadedSchematicEntry() {}
    }

    private static Map<String, Map<String, SchematicData>> openCoordinatesFile()
        throws ClassCastException, IOException {
        File coordinatesFile = new File(ConfigurationHandler.schematicDirectory, Constants.Files.Coordinates + ".json");
        Map<String, Map<String, SchematicData>> coordinates = new HashMap<>();
        if (coordinatesFile.exists() && coordinatesFile.canRead() && coordinatesFile.canWrite()) {
            try (Reader reader = Files.newBufferedReader(
                new File(ConfigurationHandler.schematicDirectory, Constants.Files.Coordinates + ".json").toPath(),
                StandardCharsets.UTF_8)) {
                coordinates = gson.fromJson(reader, schematicDataType);
            } catch (Exception e1) {
                // as I forgot to specify utf-8 before older Coordinates.json files will be in the default charset
                try (Reader reader = Files.newBufferedReader(
                    new File(ConfigurationHandler.schematicDirectory, Constants.Files.Coordinates + ".json").toPath(),
                    Charset.defaultCharset())) {
                    coordinates = gson.fromJson(reader, schematicDataType);
                } catch (Exception e2) {
                    // failed to read file in utf-8, trying with default charset
                    throw new ClassCastException("Failed to convert json file to Map<String,SchematicData>");
                }
            }

        } else if (!coordinatesFile.exists()) {
            if (saveCoordinatesFile(coordinates)) {
                Reference.logger.info("Created new coordinates file");
            } else throw new IOException("Failed to create coordinates file");
        } else {
            throw new IOException("No read/write permission for coordinates file");
        }
        return coordinates;
    }

    private static boolean saveCoordinatesFile(Map<String, Map<String, SchematicData>> map) {
        File coordinatesFile = new File(ConfigurationHandler.schematicDirectory, Constants.Files.Coordinates + ".json");
        try (OutputStreamWriter writer = new OutputStreamWriter(
            new FileOutputStream(coordinatesFile.getAbsoluteFile()),
            StandardCharsets.UTF_8)) {
            gson.toJson(map, schematicDataType, writer);
            writer.flush();
            Reference.logger.info("Successfully written to coordinates file");
            return true;
        } catch (IOException e) {
            Reference.logger.info("Failed to write to coordinates file");
            return false;
        }
    }

    public static boolean addCoordinatesAndRotation(String worldServerName, String schematicName, Integer X, Integer Y,
        Integer Z, Integer rotationX, Integer rotationY, Integer rotationZ, Integer flipX, Integer flipY,
        Integer flipZ) {
        try {
            Map<String, Map<String, SchematicData>> coordinates = openCoordinatesFile();
            SchematicData schematicData = new SchematicData();
            schematicData.X = X;
            schematicData.Y = Y;
            schematicData.Z = Z;
            schematicData.RotationX = rotationX; // This value is left as "Rotation" to provide backwards compat
            schematicData.Rotation = rotationY;
            schematicData.RotationZ = rotationZ;
            schematicData.FlipX = flipX;
            schematicData.FlipY = flipY;
            schematicData.FlipZ = flipZ;

            if (coordinates.containsKey(worldServerName)) {
                coordinates.get(worldServerName)
                    .put(schematicName, schematicData);
            } else {
                coordinates.put(worldServerName, new HashMap<>() {

                    {
                        put(schematicName, schematicData);
                    }
                });
            }
            saveCoordinatesFile(coordinates);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * gets the coordinates if present
     *
     * @return {@link Coordinates} containing rotation, flip and position information, or null if not found.
     */
    public static Coordinates getCoordinates(String worldServerName, String schematicName) {
        try {
            Map<String, Map<String, SchematicData>> coordinates = openCoordinatesFile();
            if (coordinates.containsKey(worldServerName)) {
                Map<String, SchematicData> schematicMap = coordinates.get(worldServerName);
                if (schematicMap.containsKey(schematicName)) {
                    SchematicData schematicData = schematicMap.get(schematicName);
                    return new Coordinates(
                        schematicData.RotationX,
                        schematicData.Rotation,
                        schematicData.RotationZ,
                        schematicData.FlipX,
                        schematicData.FlipY,
                        schematicData.FlipZ,
                        schematicData.X,
                        schematicData.Y,
                        schematicData.Z);
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);

        final Property[] sliders = { ConfigurationHandler.propAlpha, ConfigurationHandler.propBlockDelta,
            ConfigurationHandler.propPlaceDelay, ConfigurationHandler.propTimeout };
        for (Property prop : sliders) {
            prop.setConfigEntryClass(GuiConfigEntries.NumberSliderEntry.class);
        }

        for (KeyBinding keyBinding : InputHandler.KEY_BINDINGS) {
            ClientRegistry.registerKeyBinding(keyBinding);
        }
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        // Register client-side commands
        net.minecraftforge.client.ClientCommandHandler.instance.registerCommand(new CommandSchematicaSetBlock());

        FMLCommonHandler.instance()
            .bus()
            .register(InputHandler.INSTANCE);
        FMLCommonHandler.instance()
            .bus()
            .register(TickHandler.INSTANCE);
        FMLCommonHandler.instance()
            .bus()
            .register(RenderTickHandler.INSTANCE);
        FMLCommonHandler.instance()
            .bus()
            .register(ConfigurationHandler.INSTANCE);

        MinecraftForge.EVENT_BUS.register(RendererSchematicGlobal.INSTANCE);
        MinecraftForge.EVENT_BUS.register(ChatEventHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(new OverlayHandler());
        MinecraftForge.EVENT_BUS.register(new WorldHandler());
        MinecraftForge.EVENT_BUS.register(ToolItemHandler.INSTANCE);
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
        try {
            if (Loader.isModLoaded("lotr")) {
                Reference.logger.info("Lotr mod detected, creating proxy");
                lotrProxy = Class.forName(Reference.LOTR_PROXY)
                    .asSubclass(ILOTRPresent.class)
                    .getDeclaredConstructor()
                    .newInstance();
            } else {
                lotrProxy = new NoLOTRProxy();
            }
        } catch (Exception e) {
            Reference.logger.warn("Failed to create lotr proxy in the normal way");
            lotrProxy = new NoLOTRProxy();
        }
    }

    @Override
    public File getDataDirectory() {
        final File file = MINECRAFT.mcDataDir;
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            Reference.logger.debug("Could not canonize path!", e);
        }
        return file;
    }

    @Override
    public void resetSettings() {
        super.resetSettings();

        ChatEventHandler.INSTANCE.chatLines = 0;

        // Printer defaults to off — it's not balanced for normal gameplay.
        // Players should explicitly enable it when needed.
        SchematicPrinter.INSTANCE.setEnabled(false);

        // Save schematics and area selection before clearing so they can be restored on next world load.
        // Use lastWorldServerName since the server may already be gone at this point.
        if (lastWorldServerName != null && !lastWorldServerName.isEmpty()) {
            if (!loadedSchematics.isEmpty()) {
                saveLoadedSchematics(lastWorldServerName);
                Reference.logger.info("Saved schematics during reset for '{}'", lastWorldServerName);
            }
            saveAreaSelection(lastWorldServerName);
        }
        unloadAllSchematics();
        isPendingRestore = true;

        playerPosition.set(0, 0, 0);
        orientation = ForgeDirection.UNKNOWN;
        rotationRender = 0;

        // Clear area selection — will be restored from persistence on next world load
        pointA.set(0, 0, 0);
        pointB.set(0, 0, 0);
        pointMin.set(0, 0, 0);
        pointMax.set(0, 0, 0);
        isRenderingGuide = false;
    }

    @Override
    public void unloadSchematic() {
        if (schematic != null) {
            loadedSchematics.remove(schematic);
        }
        schematic = null;
        RendererSchematicGlobal.INSTANCE.destroyRendererSchematicChunks();
        SchematicPrinter.INSTANCE.setSchematic(null);
        // If there are still loaded schematics, select the first one
        if (!loadedSchematics.isEmpty()) {
            selectSchematic(loadedSchematics.get(0));
        }
    }

    /** Unloads all schematics. */
    public static void unloadAllSchematics() {
        schematic = null;
        loadedSchematics.clear();
        RendererSchematicGlobal.INSTANCE.destroyRendererSchematicChunks();
        SchematicPrinter.INSTANCE.setSchematic(null);
    }

    @Override
    public boolean loadSchematic(EntityPlayer player, File directory, String filename) {
        ISchematic schematicData = SchematicFormat.readFromFile(directory, filename);
        if (schematicData == null) {
            return false;
        }

        SchematicWorld world = new SchematicWorld(schematicData, filename);
        world.sourceDirectory = directory;
        world.sourceFilename = filename;

        Reference.logger
            .debug("Loaded {} [w:{},h:{},l:{}]", filename, world.getWidth(), world.getHeight(), world.getLength());

        // Allow multiple instances of the same schematic — assign unique display name
        String baseName = world.name;
        int instanceNum = 1;
        for (SchematicWorld existing : loadedSchematics) {
            if (existing.name.equals(baseName) || existing.name.matches("\\Q" + baseName + "\\E #\\d+")) {
                instanceNum++;
            }
        }
        if (instanceNum > 1) {
            world.name = baseName + " #" + instanceNum;
        }
        loadedSchematics.add(world);

        // Set as active
        selectSchematic(world);
        world.isRendering = true;

        return true;
    }

    /** Selects a schematic as the active one for tools/printer/control. */
    public static void selectSchematic(SchematicWorld world) {
        ClientProxy.schematic = world;
        if (world != null) {
            RendererSchematicGlobal.INSTANCE.createRendererSchematicChunks(world);
            SchematicPrinter.INSTANCE.setSchematic(world);
        }
    }

    /** Cycles to the next loaded schematic. */
    public static void cycleSchematic() {
        if (loadedSchematics.isEmpty()) {
            return;
        }
        if (schematic == null) {
            selectSchematic(loadedSchematics.get(0));
            return;
        }
        int idx = loadedSchematics.indexOf(schematic);
        int next = (idx + 1) % loadedSchematics.size();
        selectSchematic(loadedSchematics.get(next));
    }

    // --- Persistence: save/restore loaded schematics across sessions ---

    /** Saves the list of currently loaded schematics for the given world/server. */
    public static void saveLoadedSchematics(String worldServerName) {
        if (worldServerName == null || worldServerName.isEmpty()) return;
        try {
            File file = new File(ConfigurationHandler.schematicDirectory, "LoadedSchematics.json");
            Map<String, List<LoadedSchematicEntry>> allData;
            if (file.exists()) {
                try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                    allData = gson.fromJson(reader, loadedSchematicsDataType);
                } catch (Exception e) {
                    allData = new HashMap<>();
                }
            } else {
                allData = new HashMap<>();
            }
            if (allData == null) allData = new HashMap<>();

            List<LoadedSchematicEntry> entries = new ArrayList<>();
            for (SchematicWorld sw : loadedSchematics) {
                LoadedSchematicEntry entry = new LoadedSchematicEntry();
                entry.filename = sw.sourceFilename;
                entry.directory = sw.sourceDirectory != null ? sw.sourceDirectory.getAbsolutePath() : "";
                entry.X = sw.position.x;
                entry.Y = sw.position.y;
                entry.Z = sw.position.z;
                entry.RotationX = sw.rotationStateX;
                entry.RotationY = sw.rotationStateY;
                entry.RotationZ = sw.rotationStateZ;
                entry.FlipX = sw.flipStateX;
                entry.FlipY = sw.flipStateY;
                entry.FlipZ = sw.flipStateZ;
                entry.isActive = (sw == schematic);
                entries.add(entry);
            }
            allData.put(worldServerName, entries);

            try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(allData, loadedSchematicsDataType, writer);
                writer.flush();
            }
            Reference.logger.info("Saved {} loaded schematics for '{}'", entries.size(), worldServerName);
        } catch (Exception e) {
            Reference.logger.error("Failed to save loaded schematics", e);
        }
    }

    /** Restores previously loaded schematics for the given world/server. */
    public static void restoreLoadedSchematics(String worldServerName) {
        if (worldServerName == null || worldServerName.isEmpty()) return;
        try {
            File file = new File(ConfigurationHandler.schematicDirectory, "LoadedSchematics.json");
            if (!file.exists()) return;

            Map<String, List<LoadedSchematicEntry>> allData;
            try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                allData = gson.fromJson(reader, loadedSchematicsDataType);
            }
            if (allData == null || !allData.containsKey(worldServerName)) return;

            List<LoadedSchematicEntry> entries = allData.get(worldServerName);
            if (entries == null || entries.isEmpty()) return;

            SchematicWorld activeWorld = null;
            for (LoadedSchematicEntry entry : entries) {
                if (entry.filename == null || entry.filename.isEmpty()) continue;
                File dir = (entry.directory != null && !entry.directory.isEmpty())
                    ? new File(entry.directory)
                    : ConfigurationHandler.schematicDirectory;

                ISchematic schematicData = SchematicFormat.readFromFile(dir, entry.filename);
                if (schematicData == null) {
                    Reference.logger.warn("Failed to restore schematic: {}", entry.filename);
                    continue;
                }

                SchematicWorld world = new SchematicWorld(schematicData, entry.filename);
                world.sourceDirectory = dir;
                world.sourceFilename = entry.filename;
                world.isRendering = true;

                // Restore position
                world.position.set(entry.X, entry.Y, entry.Z);

                // Restore rotations
                for (int i = 0; i < entry.RotationX; i++) world.rotate(ForgeDirection.EAST);
                for (int i = 0; i < entry.RotationY; i++) world.rotate(ForgeDirection.UP);
                for (int i = 0; i < entry.RotationZ; i++) world.rotate(ForgeDirection.SOUTH);

                // Restore flips
                for (int i = 0; i < entry.FlipX; i++) world.flip(ForgeDirection.EAST);
                for (int i = 0; i < entry.FlipY; i++) world.flip(ForgeDirection.UP);
                for (int i = 0; i < entry.FlipZ; i++) world.flip(ForgeDirection.SOUTH);

                loadedSchematics.add(world);
                // Create render data for EVERY restored schematic so it's visible
                RendererSchematicGlobal.INSTANCE.createRendererSchematicChunks(world);
                if (entry.isActive) {
                    activeWorld = world;
                }
            }

            if (activeWorld != null) {
                selectSchematic(activeWorld);
            } else if (!loadedSchematics.isEmpty()) {
                selectSchematic(loadedSchematics.get(0));
            }

            Reference.logger.info("Restored {} schematics for '{}'", loadedSchematics.size(), worldServerName);
        } catch (Exception e) {
            Reference.logger.error("Failed to restore loaded schematics", e);
        }
    }

    // --- Persistence: save/restore area selection across sessions ---

    /** Saves the current area selection for the given world/server. */
    public static void saveAreaSelection(String worldServerName) {
        if (worldServerName == null || worldServerName.isEmpty()) return;
        try {
            File file = new File(ConfigurationHandler.schematicDirectory, "AreaSelection.json");
            Map<String, AreaSelectionData> allData;
            if (file.exists()) {
                try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                    allData = gson.fromJson(reader, areaSelectionDataType);
                } catch (Exception e) {
                    allData = new HashMap<>();
                }
            } else {
                allData = new HashMap<>();
            }
            if (allData == null) allData = new HashMap<>();

            AreaSelectionData data = new AreaSelectionData();
            data.ax = pointA.x; data.ay = pointA.y; data.az = pointA.z;
            data.bx = pointB.x; data.by = pointB.y; data.bz = pointB.z;
            data.renderingGuide = isRenderingGuide;
            allData.put(worldServerName, data);

            try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(allData, areaSelectionDataType, writer);
                writer.flush();
            }
            Reference.logger.debug("Saved area selection for '{}'", worldServerName);
        } catch (Exception e) {
            Reference.logger.error("Failed to save area selection", e);
        }
    }

    /** Restores previously saved area selection for the given world/server. */
    public static void restoreAreaSelection(String worldServerName) {
        if (worldServerName == null || worldServerName.isEmpty()) return;
        try {
            File file = new File(ConfigurationHandler.schematicDirectory, "AreaSelection.json");
            if (!file.exists()) return;

            Map<String, AreaSelectionData> allData;
            try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                allData = gson.fromJson(reader, areaSelectionDataType);
            }
            if (allData == null || !allData.containsKey(worldServerName)) return;

            AreaSelectionData data = allData.get(worldServerName);
            pointA.set(data.ax, data.ay, data.az);
            pointB.set(data.bx, data.by, data.bz);
            isRenderingGuide = data.renderingGuide;
            updatePoints();
            Reference.logger.debug("Restored area selection for '{}'", worldServerName);
        } catch (Exception e) {
            Reference.logger.error("Failed to restore area selection", e);
        }
    }

    @Override
    public boolean isPlayerQuotaExceeded(EntityPlayer player) {
        return false;
    }

    @Override
    public File getPlayerSchematicDirectory(EntityPlayer player, boolean privateDirectory) {
        return ConfigurationHandler.schematicDirectory;
    }
}
