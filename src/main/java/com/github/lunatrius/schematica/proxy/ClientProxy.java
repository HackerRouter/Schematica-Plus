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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.lunatrius.schematica.handler.client.WorldHandler;
import com.github.lunatrius.schematica.client.selection.AreaSelections;
import com.github.lunatrius.schematica.client.world.RenderLayerSettings;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3d;
import com.github.lunatrius.schematica.internal.lunatriuscore.util.vector.Vector3i;
import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.client.printer.SchematicPrinter;
import com.github.lunatrius.schematica.client.renderer.RendererSchematicGlobal;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.client.world.SchematicLibrary;
import com.github.lunatrius.schematica.client.world.SchematicSourceData;
import com.github.lunatrius.schematica.client.world.PlacementState;
import com.github.lunatrius.schematica.compat.ILOTRPresent;
import com.github.lunatrius.schematica.compat.NoLOTRProxy;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.client.ChatEventHandler;
import com.github.lunatrius.schematica.handler.client.OverlayHandler;
import com.github.lunatrius.schematica.handler.client.RenderTickHandler;
import com.github.lunatrius.schematica.handler.client.TickHandler;
import com.github.lunatrius.schematica.reference.Constants;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.Coordinates;
import com.github.lunatrius.schematica.world.schematic.SchematicFormat;
import com.github.lunatrius.schematica.command.CommandSchematicaSetBlock;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import cpw.mods.fml.client.config.GuiConfigEntries;
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
    @Override public void worldMoveFinished(long id, boolean success) {
        com.github.lunatrius.schematica.tool.WorldMoveController.finished(id, success);
    }
    public static ForgeDirection orientation = ForgeDirection.UNKNOWN;
    public static int rotationRender = 0;
    /** The currently active/selected schematic (for tools, printer, control GUI). */
    public static SchematicWorld schematic = null;
    public static final SchematicLibrary<SchematicSourceData, SchematicWorld> SCHEMATICS = new SchematicLibrary<>(SchematicSourceData::read);
    public static final List<SchematicWorld> loadedSchematics = SCHEMATICS.placements();
    public static MovingObjectPosition movingObjectPosition = null;
    public static ILOTRPresent lotrProxy = null;
    /** Tracks the last known world/server name for reliable save on disconnect. */
    public static String lastWorldServerName = null;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting()
        .create();
    private static final Type schematicDataType = new TypeToken<Map<String, Map<String, SchematicData>>>() {}.getType();
    private static final Type loadedSchematicsDataType = new TypeToken<Map<String, List<LoadedSchematicEntry>>>() {}.getType();

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
        net.minecraft.entity.player.EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (schematic != null && player != null) schematic.moveOriginTo((int) Math.floor(player.posX),
            (int) Math.floor(player.posY - player.yOffset), (int) Math.floor(player.posZ));
    }

    public static void moveSchematic(SchematicWorld schematic, Integer x, Integer y, Integer z) {
        if (schematic != null) {
            schematic.moveMinimumTo(x, y, z);
        }
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
        public String displayName;
        public String filename;
        public String directory;
        public int X, Y, Z;
        public int RotationX, RotationY, RotationZ;
        public int FlipX, FlipY, FlipZ;
        public boolean isActive;
        public List<String> transforms;
        public Boolean visible, entities, blockNBT, sourceOnly, layerMode;
        public Integer layer;
        public int[] origin;
        public com.google.gson.JsonObject subregions;
        public com.google.gson.JsonObject placementSettings;

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

        com.github.lunatrius.schematica.client.input.Hotkeys.initialize(
            event.getModConfigurationDirectory().toPath(), Minecraft.getMinecraft().mcDataDir.toPath());
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        FMLCommonHandler.instance().bus().register(
            com.github.lunatrius.schematica.handler.client.CommandEditQueue.INSTANCE);
        MinecraftForge.EVENT_BUS.register(com.github.lunatrius.schematica.handler.client.CommandEditQueue.INSTANCE);

        // Register client-side commands
        net.minecraftforge.client.ClientCommandHandler.instance.registerCommand(new CommandSchematicaSetBlock());

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
        MinecraftForge.EVENT_BUS.register(new com.github.lunatrius.schematica.handler.client.ModInfoHandler());
        MinecraftForge.EVENT_BUS.register(WorldHandler.INSTANCE);
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

        // Turn on the printer again.
        SchematicPrinter.INSTANCE.setEnabled(true);

        WorldHandler.INSTANCE.closeSession();
        clearWorldState();

        playerPosition.set(0, 0, 0);
        orientation = ForgeDirection.UNKNOWN;
        rotationRender = 0;
    }

    public static void clearWorldState() {
        com.github.lunatrius.schematica.client.verifier.VerificationManager.INSTANCE.clear();
        com.github.lunatrius.schematica.client.renderer.hud.BlockInfoHud.INSTANCE.clear();
        AreaSelections.clear();
        RenderLayerSettings.RANGE.load(null);
        unloadAllSchematics();
        lastWorldServerName = null;
        pointA.set(0, 0, 0);
        pointB.set(0, 0, 0);
        pointMin.set(0, 0, 0);
        pointMax.set(0, 0, 0);
        isRenderingGuide = false;
    }

    @Override
    public void unloadSchematic() {
        if (schematic != null) removePlacement(schematic);
    }

    public static void removePlacement(SchematicWorld world) {
        if (!SCHEMATICS.remove(world)) return;
        RendererSchematicGlobal.INSTANCE.removeRendererSchematicChunks(world);
        if (schematic == world) selectSchematic(loadedSchematics.isEmpty() ? null : loadedSchematics.get(0));
        WorldHandler.INSTANCE.saveSession();
    }

    public static void unloadSource(SchematicLibrary.Source<SchematicSourceData> source) {
        for (SchematicWorld world : SCHEMATICS.unload(source)) {
            RendererSchematicGlobal.INSTANCE.removeRendererSchematicChunks(world);
        }
        if (schematic != null && !loadedSchematics.contains(schematic)) {
            selectSchematic(loadedSchematics.isEmpty() ? null : loadedSchematics.get(0));
        }
        WorldHandler.INSTANCE.saveSession();
    }

    public static void unloadAllSchematics() {
        schematic = null;
        SCHEMATICS.clear();
        RendererSchematicGlobal.INSTANCE.destroyRendererSchematicChunks();
        SchematicPrinter.INSTANCE.setSchematic(null);
    }

    public static SchematicLibrary.Source<SchematicSourceData> loadSource(File file) throws IOException {
        return SCHEMATICS.load(file);
    }

    /** Registers a captured schematic that has no file; it is not restored in later sessions. */
    public static SchematicLibrary.Source<SchematicSourceData> addMemorySource(String name,
        com.github.lunatrius.schematica.world.schematic.SchematicFileSnapshot snapshot) throws IOException {
        String safe = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return SCHEMATICS.addMemory(new File("<memory>", (safe.isEmpty() ? "schematic" : safe) + ".schemplus"), SchematicSourceData.of(snapshot));
    }

    private static SchematicWorld instantiate(SchematicLibrary.Source<SchematicSourceData> source, SchematicSourceData data) throws IOException {
        SchematicWorld world = new SchematicWorld(data.instantiate(), source.file().getName());
        world.setPlacementSource(data);
        world.sourceDirectory = source.file().getParentFile();
        world.sourceFilename = source.file().getName();
        world.isRendering = true;
        return world;
    }

    public static SchematicWorld createPlacement(SchematicLibrary.Source<SchematicSourceData> source) throws IOException {
        SchematicWorld world = SCHEMATICS.create(source, (data, previous) -> instantiate(source, data));
        String base = world.name;
        int suffix = 2;
        java.util.Set<String> names = new java.util.HashSet<>();
        for (SchematicWorld other : loadedSchematics) if (other != world) names.add(other.name);
        while (names.contains(world.name)) world.name = base + " #" + suffix++;
        return world;
    }

    public static void reloadSource(SchematicLibrary.Source<SchematicSourceData> source) throws IOException {
        replaceSource(source, false);
    }

    /** Recomposes all placements of a source from its edited in-memory data. */
    public static void refreshSource(SchematicLibrary.Source<SchematicSourceData> source) throws IOException {
        replaceSource(source, true);
    }

    private static void replaceSource(SchematicLibrary.Source<SchematicSourceData> source, boolean memory) throws IOException {
        List<SchematicWorld> prepared = new ArrayList<>();
        Map<SchematicWorld, SchematicWorld> changes;
        try {
            SchematicLibrary.Factory<SchematicSourceData, SchematicWorld> factory = (data, previous) -> {
                SchematicWorld next = instantiate(source, data);
                PlacementState.copy(previous, next);
                prepared.add(next);
                RendererSchematicGlobal.INSTANCE.createRendererSchematicChunks(next);
                return next;
            };
            changes = memory ? SCHEMATICS.refresh(source, factory) : SCHEMATICS.reload(source, factory);
        } catch (IOException | RuntimeException e) {
            for (SchematicWorld world : prepared) RendererSchematicGlobal.INSTANCE.removeRendererSchematicChunks(world);
            throw e;
        }
        SchematicWorld selected = changes.containsKey(schematic) ? changes.get(schematic) : schematic;
        for (Map.Entry<SchematicWorld, SchematicWorld> entry : changes.entrySet()) {
            RendererSchematicGlobal.INSTANCE.removeRendererSchematicChunks(entry.getKey());
        }
        if (selected != schematic) selectSchematic(selected);
        WorldHandler.INSTANCE.saveSession();
    }

    public static void sourceRenamed(File previous, File next) throws IOException {
        SCHEMATICS.renamed(previous, next);
        for (SchematicWorld world : loadedSchematics) {
            File file = SCHEMATICS.sourceOf(world).file();
            world.sourceDirectory = file.getParentFile();
            world.sourceFilename = file.getName();
        }
    }

    @Override
    public boolean loadSchematic(EntityPlayer player, File directory, String filename) {
        SchematicWorld world = null;
        try {
            world = createPlacement(loadSource(new File(directory, filename)));
            selectSchematic(world);
            return true;
        } catch (IOException | RuntimeException e) {
            if (world != null) removePlacement(world);
            Reference.logger.error("Failed to load schematic", e);
            return false;
        }
    }

    /** Selects a schematic as the active one for tools/printer/control. */
    public static void selectSchematic(SchematicWorld world) {
        ClientProxy.schematic = world;
        RendererSchematicGlobal.INSTANCE.selectSchematic(world);
        SchematicPrinter.INSTANCE.setSchematic(world);
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
                    throw new IOException("Existing schematic settings are unreadable; preserving the file", e);
                }
            } else {
                allData = new HashMap<>();
            }
            if (allData == null) allData = new HashMap<>();

            List<LoadedSchematicEntry> entries = new ArrayList<>();
            for (SchematicWorld sw : loadedSchematics) {
                if (SCHEMATICS.sourceOf(sw).memory()) continue;
                LoadedSchematicEntry entry = new LoadedSchematicEntry();
                entry.displayName = sw.name;
                entry.filename = sw.sourceFilename;
                entry.directory = sw.sourceDirectory != null ? sw.sourceDirectory.getAbsolutePath() : "";
                entry.X = sw.position.x;
                entry.Y = sw.position.y;
                entry.Z = sw.position.z;
                entry.origin = sw.originPosition().coordinates();
                entry.subregions = sw.subregions() == null ? null : sw.subregions().toJson();
                entry.placementSettings = sw.placementSettings().toJson();
                entry.RotationX = sw.rotationStateX;
                entry.RotationY = sw.rotationStateY;
                entry.RotationZ = sw.rotationStateZ;
                entry.FlipX = sw.flipStateX;
                entry.FlipY = sw.flipStateY;
                entry.FlipZ = sw.flipStateZ;
                entry.isActive = (sw == schematic);
                entry.transforms = new ArrayList<>(sw.transformOperations);
                entry.visible = sw.isRendering;
                entry.entities = sw.isRenderingEntities;
                entry.blockNBT = sw.isPastingBlockNBT;
                entry.layerMode = sw.isRenderingLayer;
                entry.layer = sw.renderingLayer;
                entries.add(entry);
            }
            for (SchematicLibrary.Source<SchematicSourceData> source : SCHEMATICS.sources()) {
                boolean placed = false;
                for (SchematicWorld world : loadedSchematics) if (SCHEMATICS.sourceOf(world) == source) placed = true;
                if (placed || source.memory()) continue;
                LoadedSchematicEntry entry = new LoadedSchematicEntry();
                entry.filename = source.file().getName();
                entry.directory = source.file().getParentFile().getAbsolutePath();
                entry.sourceOnly = true;
                entries.add(entry);
            }
            allData.put(worldServerName, entries);

            com.github.lunatrius.schematica.util.FileUtils.writeUtf8Atomically(file,
                gson.toJson(allData, loadedSchematicsDataType));
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
                try {
                    if (entry == null || entry.filename == null || entry.filename.isEmpty()) continue;
                    File dir = entry.directory != null && !entry.directory.isEmpty() ? new File(entry.directory) : ConfigurationHandler.schematicDirectory;
                    SchematicLibrary.Source<SchematicSourceData> source = loadSource(new File(dir, entry.filename));
                    if (Boolean.TRUE.equals(entry.sourceOnly)) continue;
                    SchematicWorld world = SCHEMATICS.create(source, (data, previous) -> {
                        SchematicWorld restored = instantiate(source, data);
                        if (entry.displayName != null && !entry.displayName.trim().isEmpty()) restored.name = entry.displayName;
                        List<String> operations = entry.transforms;
                        if (operations == null) {
                            operations = new ArrayList<>();
                            int[] values = {entry.RotationX, entry.RotationY, entry.RotationZ, entry.FlipX, entry.FlipY, entry.FlipZ};
                            String[] axes = {"X", "Y", "Z", "x", "y", "z"};
                            for (int i = 0; i < values.length; i++) {
                                for (int j = 0; j < Math.floorMod(values[i], i < 3 ? 4 : 2); j++) operations.add(axes[i]);
                            }
                        }
                        PlacementState.applyTransforms(restored, operations);
                        restored.restoreSubregions(entry.subregions);
                        com.github.lunatrius.schematica.api.SchematicOrigin minimum = restored.getSchematic().getOrigin()
                            .restoredMinimum(entry.X, entry.Y, entry.Z, entry.origin);
                        restored.position.set(minimum.x, minimum.y, minimum.z);
                        if (entry.visible != null) restored.isRendering = entry.visible;
                        if (entry.entities != null) restored.isRenderingEntities = entry.entities;
                        if (entry.blockNBT != null) restored.isPastingBlockNBT = entry.blockNBT;
                        restored.isRenderingLayer = Boolean.TRUE.equals(entry.layerMode);
                        if (entry.layer != null) restored.renderingLayer = Math.max(0, Math.min(entry.layer, restored.getHeight() - 1));
                        restored.setPlacementSettings(com.github.lunatrius.schematica.client.world.PlacementSettings.fromJson(entry.placementSettings));
                        return restored;
                    });
                    RendererSchematicGlobal.INSTANCE.createRendererSchematicChunks(world);
                    if (entry.isActive) activeWorld = world;
                } catch (Exception e) {
                    Reference.logger.warn("Could not restore schematic session entry", e);
                }
            }
            selectSchematic(activeWorld);

            Reference.logger.info("Restored {} schematics for '{}'", loadedSchematics.size(), worldServerName);
        } catch (Exception e) {
            Reference.logger.error("Failed to restore loaded schematics", e);
        }
    }

    public static void saveAreaSelection(String worldServerName) {
        AreaSelections.save(worldServerName);
    }

    public static void restoreAreaSelection(String worldServerName) {
        AreaSelections.restore(worldServerName);
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
