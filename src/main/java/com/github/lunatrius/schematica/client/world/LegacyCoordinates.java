package com.github.lunatrius.schematica.client.world;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.reference.Constants;
import com.github.lunatrius.schematica.util.Coordinates;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public final class LegacyCoordinates {
    private static final Gson gson = new Gson();
    private static final Type schematicDataType = new TypeToken<Map<String, Map<String, SchematicData>>>() {}.getType();

    private LegacyCoordinates() {}

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
            // Only read: placements are remembered in config/schematica_plus, an empty file is not created
            return coordinates;
        } else {
            throw new IOException("No read/write permission for coordinates file");
        }
        return coordinates;
    }

    /**
     * gets the coordinates if present
     *
     * @return {@link Coordinates} containing rotation, flip and position information, or null if not found.
     */
    public static Coordinates get(String worldServerName, String schematicName) {
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
}
