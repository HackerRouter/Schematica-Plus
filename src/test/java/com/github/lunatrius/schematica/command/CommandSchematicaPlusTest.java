package com.github.lunatrius.schematica.command;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.proxy.CommonProxy;
import com.github.lunatrius.schematica.update.UpdateHttpClient;
import com.github.lunatrius.schematica.update.UpdateManager;
import com.github.lunatrius.schematica.update.UpdateManagerTestHelper;
import com.github.lunatrius.schematica.update.UpdateRelease;

import cpw.mods.fml.relauncher.FMLInjectionData;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

public class CommandSchematicaPlusTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private Field minecraftHome;
    private Object previousHome;
    private Configuration previous;
    private CommonProxy previousProxy;
    private final CommandSchematicaPlus command = new CommandSchematicaPlus();

    @Before public void configure() throws Exception {
        minecraftHome = FMLInjectionData.class.getDeclaredField("minecraftHome");
        minecraftHome.setAccessible(true);
        previousHome = minecraftHome.get(null);
        minecraftHome.set(null, temporary.getRoot());
        previousProxy = SchematicaPlus.proxy;
        File data = temporary.getRoot();
        if (SchematicaPlus.proxy == null) SchematicaPlus.proxy = new CommonProxy() {
            @Override public File getDataDirectory() { return data; }
            @Override public boolean isPlayerQuotaExceeded(EntityPlayer player) { return false; }
            @Override public File getPlayerSchematicDirectory(EntityPlayer player, boolean privateDirectory) { return data; }
        };
        previous = ConfigurationHandler.configuration;
        ConfigurationHandler.configuration = new Configuration(new File(temporary.getRoot(), "schematica_plus.cfg"));
        ConfigurationHandler.loadConfiguration();
    }

    @After public void reset() throws Exception {
        ConfigurationHandler.configuration = previous;
        if (previous != null) ConfigurationHandler.loadConfiguration();
        SchematicaPlus.proxy = previousProxy;
        minecraftHome.set(null, previousHome);
    }

    private static final class Sender implements ICommandSender {
        final int level;
        final List<IChatComponent> messages = new ArrayList<>();
        Sender(int level) { this.level = level; }
        @Override public String getCommandSenderName() { return "Admin"; }
        @Override public IChatComponent func_145748_c_() { return null; }
        @Override public void addChatMessage(IChatComponent message) { messages.add(message); }
        @Override public boolean canCommandSenderUseCommand(int level, String command) { return level <= this.level; }
        @Override public ChunkCoordinates getPlayerCoordinates() { return new ChunkCoordinates(); }
        @Override public World getEntityWorld() { return null; }
    }

    private static String key(IChatComponent message) {
        return message instanceof ChatComponentTranslation ? ((ChatComponentTranslation) message).getKey() : message.getUnformattedText();
    }

    @Test public void setsValidatesAndSavesServerSettings() {
        Sender admin = new Sender(4);
        command.processCommand(admin, new String[] {"set", "printerMaxRange", "12"});
        assertEquals(12, ConfigurationHandler.printerMaxRange, 0);
        assertEquals("schematica.command.config.set_value", key(admin.messages.get(0)));
        assertTrue(new File(temporary.getRoot(), "schematica_plus.cfg").isFile());
        assertThrows(CommandException.class, () -> command.processCommand(admin, new String[] {"set", "printerMaxRange", "-1"}));
        assertThrows(CommandException.class, () -> command.processCommand(admin, new String[] {"set", "printerEnabled", "maybe"}));
        assertThrows(CommandException.class, () -> command.processCommand(admin, new String[] {"set", "unknownSetting", "1"}));
        command.processCommand(admin, new String[] {"set", "printerEnabled", "false"});
        assertFalse(ConfigurationHandler.printerEnabled);
        command.processCommand(admin, new String[] {"set", "cloudMaxUploadKilobytes", "2048"});
        assertEquals(2048, ConfigurationHandler.cloudMaxUploadKilobytes);
        command.processCommand(admin, new String[] {"set", "printerMaxRange"});
        assertEquals(Double.parseDouble(ConfigurationHandler.configuration.getCategory("server").get("printerMaxRange").getDefault()),
            ConfigurationHandler.printerMaxRange, 0);
    }

    @Test public void resetsOneOrAllServerSettings() {
        Sender admin = new Sender(4);
        command.processCommand(admin, new String[] {"set", "printerEnabled", "false"});
        command.processCommand(admin, new String[] {"set", "cloudMaxUploadKilobytes", "2048"});
        command.processCommand(admin, new String[] {"reset", "printerEnabled"});
        assertTrue(ConfigurationHandler.printerEnabled);
        assertEquals(2048, ConfigurationHandler.cloudMaxUploadKilobytes);
        command.processCommand(admin, new String[] {"set", "sharedPlacementsEnabled", "false"});
        admin.messages.clear();
        command.processCommand(admin, new String[] {"reset", "all"});
        assertEquals(8192, ConfigurationHandler.cloudMaxUploadKilobytes);
        assertTrue(ConfigurationHandler.sharedPlacementsEnabled);
        ChatComponentTranslation message = (ChatComponentTranslation) admin.messages.get(0);
        assertEquals("schematica.command.config.reset_all", message.getKey());
        assertEquals(2, message.getFormatArgs()[0]);
        assertThrows(CommandException.class, () -> command.processCommand(admin, new String[] {"reset", "unknownSetting"}));
        assertTrue(command.addTabCompletionOptions(admin, new String[] {"reset", "a"}).contains("all"));
    }

    @Test public void listsOnlyServerSettingsAndRequiresOperatorLevels() {
        Sender admin = new Sender(4);
        command.processCommand(admin, new String[] {"list"});
        List<String> names = new ArrayList<>();
        for (IChatComponent message : admin.messages) names.add(message.getUnformattedText().split(":")[0]);
        assertTrue(names.contains("printerEnabled"));
        for (String name : new String[] {"cloudEnabled", "cloudUploadPermissionLevel", "cloudMaxUploadKilobytes", "sharedPlacementsEnabled",
            "sharedPlacementManagePermissionLevel", "sharedPlacementsPerPlayer"}) assertTrue(name, names.contains(name));
        assertFalse(names.contains("pasteReplaceBehavior"));
        assertFalse(names.contains("sharedPlacementsAutoLoad"));
        Sender moderator = new Sender(3);
        command.processCommand(moderator, new String[0]);
        assertEquals("schematica.command.config.about", key(moderator.messages.get(0)));
        assertThrows(CommandException.class, () -> command.processCommand(moderator, new String[] {"list"}));
    }

    @Test public void runsServerUpdateCheckDownloadAndStopWorkflow() throws Exception {
        File data = temporary.getRoot();
        File modsDir = temporary.newFolder("mods_cmd");
        File updatesDir = temporary.newFolder("updates_cmd");
        File currentJar = new File(modsDir, "schematica_plus-1.0.0-beta.5.jar");
        Files.write(currentJar.toPath(), new byte[] {1});

        String json = "[{\"tag_name\":\"1.0.0\",\"name\":\"1.0.0\",\"body\":\"Release\",\"html_url\":\"https://example.invalid/r\","
            + "\"draft\":false,\"prerelease\":false,\"assets\":[{\"name\":\"schematica_plus-1.0.0.jar\",\"size\":12,\"browser_download_url\":\"https://example.invalid/100.jar\"}]}]";
        AtomicBoolean shutdownCalled = new AtomicBoolean(false);
        UpdateManager manager = UpdateManagerTestHelper.create(
            new UpdateManager.Transport() {
                @Override public String fetchReleasesJson() { return json; }
                @Override public void download(UpdateRelease release, File destination, AtomicBoolean cancelled,
                    UpdateHttpClient.ProgressListener listener) throws IOException {
                    Files.createDirectories(destination.toPath().getParent());
                    Files.write(destination.toPath(), new byte[] {1, 2, 3});
                    if (listener != null) listener.onProgress(3L, 3L, false);
                }
            },
            currentJar,
            updatesDir,
            new AtomicReference<>(""),
            shutdownCalled);

        CommandSchematicaPlus updateCommand = new CommandSchematicaPlus(manager);
        Sender admin = new Sender(4);

        assertTrue(updateCommand.addTabCompletionOptions(admin, new String[] {"u"}).contains("update"));
        assertTrue(updateCommand.addTabCompletionOptions(admin, new String[] {"update", "d"}).contains("download"));
        assertTrue(updateCommand.addTabCompletionOptions(admin, new String[] {"update", "s"}).contains("stop"));

        assertThrows(CommandException.class, () -> updateCommand.processCommand(admin, new String[] {"update", "download"}));

        SchematicaPlus.proxy = new CommonProxy() {
            @Override public boolean dedicatedServer() { return true; }
            @Override public File getDataDirectory() { return data; }
            @Override public boolean isPlayerQuotaExceeded(EntityPlayer player) { return false; }
            @Override public File getPlayerSchematicDirectory(EntityPlayer player, boolean privateDirectory) { return data; }
        };

        assertThrows(CommandException.class, () -> updateCommand.processCommand(admin, new String[] {"update", "stop"}));

        updateCommand.processCommand(admin, new String[] {"update", "check"});
        waitUntil(() -> manager.state() == UpdateManager.State.UPDATE_AVAILABLE && admin.messages.size() >= 2);
        assertEquals("schematica.command.update.available", key(admin.messages.get(admin.messages.size() - 1)));

        ConfigurationHandler.serverSelfUpdateEnabled = false;
        assertThrows(CommandException.class, () -> updateCommand.processCommand(admin, new String[] {"update", "download"}));
        ConfigurationHandler.serverSelfUpdateEnabled = true;

        admin.messages.clear();
        updateCommand.processCommand(admin, new String[] {"update", "download"});
        waitUntil(() -> manager.state() == UpdateManager.State.READY_TO_APPLY && admin.messages.size() >= 3);
        assertEquals("schematica.command.update.ready", key(admin.messages.get(1)));
        assertEquals("schematica.command.update.client_compat_note", key(admin.messages.get(2)));

        admin.messages.clear();
        updateCommand.processCommand(admin, new String[] {"update", "stop"});
        assertTrue(shutdownCalled.get());
        assertEquals("schematica.command.update.stopping", key(admin.messages.get(0)));
    }

    private static void waitUntil(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + 5000000000L;
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) throw new AssertionError("Timed out waiting for condition");
            Thread.sleep(20L);
        }
    }
}
