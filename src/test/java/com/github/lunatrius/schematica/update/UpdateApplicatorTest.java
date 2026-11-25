package com.github.lunatrius.schematica.update;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UpdateApplicatorTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private static void writeValidJar(File target, String marker) throws IOException {
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(target))) {
            out.putNextEntry(new ZipEntry("mcmod.info"));
            out.write(marker.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
    }

    @Test public void detachedSubprocessDeletesOldJarAndInstallsNewJar() throws Exception {
        File modsDir = temporary.newFolder("mods");
        File updatesDir = temporary.newFolder("updates");
        File oldJar = new File(modsDir, "schematica_plus-1.0.0-beta.5.jar");
        File stagedJar = new File(updatesDir, "schematica_plus-1.0.0-beta.6.jar");
        File targetJar = new File(modsDir, "schematica_plus-1.0.0-beta.6.jar");
        writeValidJar(oldJar, "old");
        writeValidJar(stagedJar, "new-release-content");
        byte[] expectedBytes = Files.readAllBytes(stagedJar.toPath());

        Process process = UpdateApplicatorLauncher.launch(updatesDir, oldJar, stagedJar, targetJar, 10000L);
        assertTrue("applicator process should complete in time", process.waitFor(15, TimeUnit.SECONDS));
        assertEquals(0, process.exitValue());
        assertFalse(oldJar.exists());
        assertFalse(stagedJar.exists());
        assertTrue(targetJar.isFile());
        assertArrayEquals(expectedBytes, Files.readAllBytes(targetJar.toPath()));

        File logFile = new File(updatesDir, "last_update.log");
        assertTrue(logFile.isFile());
        String log = new String(Files.readAllBytes(logFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(log, log.contains("Applied Schematica Plus update"));
    }

    @Test public void preservesOldJarWhenStagedArchiveIsCorrupt() throws Exception {
        File modsDir = temporary.newFolder("mods");
        File updatesDir = temporary.newFolder("updates");
        File oldJar = new File(modsDir, "schematica_plus-1.0.0-beta.5.jar");
        File stagedJar = new File(updatesDir, "schematica_plus-1.0.0-beta.6.jar");
        File targetJar = new File(modsDir, "schematica_plus-1.0.0-beta.6.jar");
        File logFile = new File(updatesDir, "last_update.log");
        writeValidJar(oldJar, "old");
        Files.write(stagedJar.toPath(), "not-a-zip".getBytes(StandardCharsets.UTF_8));

        int exit = UpdateApplicatorMain.apply(oldJar.toPath(), stagedJar.toPath(), targetJar.toPath(), logFile.toPath(), 1000L);
        assertEquals(1, exit);
        assertTrue(oldJar.isFile());
        assertFalse(targetJar.exists());
    }

    @Test public void cleansOnlyStaleHelperAndPartFiles() throws Exception {
        File updatesDir = temporary.newFolder("updates");
        File helper = new File(updatesDir, "applicator-12345.jar");
        File partial = new File(updatesDir, "schematica_plus-1.0.0.jar.part");
        File active = new File(updatesDir, "schematica_plus-1.0.0.jar");
        Files.write(helper.toPath(), new byte[] {1});
        Files.write(partial.toPath(), new byte[] {2});
        Files.write(active.toPath(), new byte[] {3});

        UpdateApplicatorLauncher.cleanupStaleFiles(updatesDir, active);
        assertFalse(helper.exists());
        assertFalse(partial.exists());
        assertTrue(active.isFile());
    }

    @Test public void appliesUpdateInlineOnUnixWithoutRaceCondition() throws Exception {
        if (System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win")) return;
        File modsDir = temporary.newFolder("mods_inline");
        File updatesDir = temporary.newFolder("updates_inline");
        File oldJar = new File(modsDir, "schematica_plus-1.0.0-beta.5.jar");
        File stagedJar = new File(updatesDir, "schematica_plus-1.0.0.jar");
        File targetJar = new File(modsDir, "schematica_plus-1.0.0.jar");
        writeValidJar(oldJar, "old");
        writeValidJar(stagedJar, "inline-new");
        byte[] expectedBytes = Files.readAllBytes(stagedJar.toPath());

        UpdateApplicatorLauncher.applyOnExit(updatesDir, oldJar, stagedJar, targetJar, 5000L);
        assertFalse(oldJar.exists());
        assertFalse(stagedJar.exists());
        assertTrue(targetJar.isFile());
        assertArrayEquals(expectedBytes, Files.readAllBytes(targetJar.toPath()));
    }
}
