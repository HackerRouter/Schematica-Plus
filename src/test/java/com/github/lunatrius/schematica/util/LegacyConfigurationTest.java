package com.github.lunatrius.schematica.util;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class LegacyConfigurationTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void copiesLegacyConfigOnceWithoutChangingEitherExistingFile() throws Exception {
        File legacy = temporary.newFile("Schematica.cfg");
        byte[] oldSettings = "tool { B:pasteOnlyAir=true }".getBytes(StandardCharsets.UTF_8);
        Files.write(legacy.toPath(), oldSettings);
        File target = new File(temporary.getRoot(), "schematica_plus.cfg");
        FileUtils.migrateLegacyConfiguration(target);
        assertArrayEquals(oldSettings, Files.readAllBytes(target.toPath()));
        byte[] newSettings = "tool { B:pasteOnlyAir=false }".getBytes(StandardCharsets.UTF_8);
        Files.write(target.toPath(), newSettings);
        FileUtils.migrateLegacyConfiguration(target);
        assertArrayEquals(newSettings, Files.readAllBytes(target.toPath()));
        assertArrayEquals(oldSettings, Files.readAllBytes(legacy.toPath()));
    }

    @Test public void acceptsLowercaseLegacyNameAndFreshInstalls() throws Exception {
        File target = new File(temporary.getRoot(), "schematica_plus.cfg");
        FileUtils.migrateLegacyConfiguration(target);
        assertFalse(target.exists());
        File legacy = temporary.newFile("schematica.cfg");
        Files.write(legacy.toPath(), new byte[] {1, 2, 3});
        FileUtils.migrateLegacyConfiguration(target);
        assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(target.toPath()));
    }
}
