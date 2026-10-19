package com.github.lunatrius.schematica.command;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

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
}
