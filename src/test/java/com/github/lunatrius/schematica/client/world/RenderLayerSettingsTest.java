package com.github.lunatrius.schematica.client.world;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class RenderLayerSettingsTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void savesEachWorldAndDimensionWithoutOverwritingOtherSessions() throws Exception {
        File file = new File(temporary.getRoot(), "RenderLayers.json");
        RenderLayerRange range = new RenderLayerRange();
        range.setMode(RenderLayerRange.Mode.SINGLE_LAYER);
        range.setHere(64);
        RenderLayerSettings.save(file, "server:example|dimension:0", range);
        range.setHere(90);
        RenderLayerSettings.save(file, "server:example|dimension:-1", range);
        range.load(RenderLayerSettings.read(file).getAsJsonObject("server:example|dimension:0"));
        assertEquals(64, range.value(false));
        range.load(null);
        RenderLayerSettings.save(file, "server:other|dimension:0", range);
        assertEquals(3, RenderLayerSettings.read(file).entrySet().size());
        range.load(RenderLayerSettings.read(file).getAsJsonObject("server:example|dimension:-1"));
        assertEquals(90, range.value(false));
    }

    @Test public void corruptExistingFileIsPreserved() throws Exception {
        File file = temporary.newFile();
        byte[] invalid = "{broken".getBytes(StandardCharsets.UTF_8);
        Files.write(file.toPath(), invalid);
        assertThrows(RuntimeException.class, () -> RenderLayerSettings.save(file, "world", new RenderLayerRange()));
        assertArrayEquals(invalid, Files.readAllBytes(file.toPath()));
    }
}
