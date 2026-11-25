// SPDX-License-Identifier: LGPL-3.0-only
// Servux ServuxCommand, adapted for 1.7.10 by HackerRouter, 2026.
package com.github.lunatrius.schematica.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.config.Property;

import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.client.gui.config.ConfigTranslations;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.PlayerHandler;
import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.update.UpdateManager;
import com.github.lunatrius.schematica.update.UpdateRelease;

public class CommandSchematicaPlus extends CommandBase {
    public static final String NAME = "schematicaplus";
    private static final String PREFIX = "schematica.command.config.";
    private static final String UPDATE_PREFIX = "schematica.command.update.";

    private final UpdateManager updates;

    public CommandSchematicaPlus() {
        this(UpdateManager.INSTANCE);
    }

    CommandSchematicaPlus(UpdateManager updates) {
        this.updates = updates;
    }

    @Override
    public String getCommandName() {
        return NAME;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return PREFIX + "usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 3;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.addChatMessage(new ChatComponentTranslation(PREFIX + "about", Reference.VERSION));
            return;
        }
        if (!sender.canCommandSenderUseCommand(4, NAME)) throw new CommandException("commands.generic.permission");
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload":
                ConfigurationHandler.configuration.load();
                ConfigurationHandler.loadConfiguration();
                sender.addChatMessage(new ChatComponentTranslation(PREFIX + "reloaded"));
                return;
            case "save":
                ConfigurationHandler.configuration.save();
                sender.addChatMessage(new ChatComponentTranslation(PREFIX + "saved"));
                return;
            case "list":
                if (args.length != 1) break;
                list(sender, new ArrayList<>(settings().values()));
                return;
            case "info":
                if (args.length != 2) break;
                info(sender, setting(args[1]));
                return;
            case "set":
                if (args.length < 2) break;
                set(sender, setting(args[1]), args.length == 2 ? null : func_82360_a(sender, args, 2));
                return;
            case "reset":
                if (args.length != 2) break;
                if ("all".equalsIgnoreCase(args[1])) resetAll(sender);
                else set(sender, setting(args[1]), null);
                return;
            case "search":
                if (args.length < 2) break;
                search(sender, func_82360_a(sender, args, 1));
                return;
            case "update":
                update(sender, Arrays.copyOfRange(args, 1, args.length));
                return;
            default:
                break;
        }
        throw new WrongUsageException(getCommandUsage(sender));
    }

    static Map<String, Property> settings() {
        Map<String, Property> settings = new LinkedHashMap<>();
        for (Property property : ConfigurationHandler.configuration.getCategory(Names.Config.Category.SERVER).getOrderedValues()) {
            if (property.showInGui()) settings.put(property.getName(), property);
        }
        return settings;
    }

    private static Property setting(String name) {
        Property property = settings().get(name);
        if (property == null) throw new CommandException(PREFIX + "error.unknown_setting");
        return property;
    }

    private static void list(ICommandSender sender, List<Property> settings) {
        if (settings.isEmpty()) {
            sender.addChatMessage(new ChatComponentTranslation(PREFIX + "error.no_settings"));
            return;
        }
        for (Property property : settings) {
            IChatComponent name = new ChatComponentText(property.getName());
            name.getChatStyle().setBold(true)
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + NAME + " info " + property.getName()))
                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentTranslation(PREFIX + "info.click_to_see_more")));
            IChatComponent text = new ChatComponentText("").appendSibling(name);
            if (property.getString().length() < 10) text.appendText(": " + property.getString());
            sender.addChatMessage(text);
        }
    }

    private static void info(ICommandSender sender, Property property) {
        String setCommand = "/" + NAME + " set " + property.getName() + " ";
        IChatComponent label = new ChatComponentTranslation(ConfigTranslations.label(property.getLanguageKey()));
        label.getChatStyle().setColor(EnumChatFormatting.YELLOW).setBold(true);
        IChatComponent name = new ChatComponentText(property.getName());
        name.getChatStyle().setColor(EnumChatFormatting.GRAY)
            .setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, setCommand))
            .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentTranslation(PREFIX + "info.click_to_set", property.getName())));
        sender.addChatMessage(new ChatComponentText("").appendSibling(label).appendText(" (").appendSibling(name).appendText(")"));
        IChatComponent comment = new ChatComponentTranslation(ConfigTranslations.comment(property.getLanguageKey()));
        comment.getChatStyle().setColor(EnumChatFormatting.GRAY);
        sender.addChatMessage(comment);
        IChatComponent value = new ChatComponentTranslation(PREFIX + "info.value", property.getString());
        value.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, setCommand))
            .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentTranslation(PREFIX + "info.click_to_set", property.getName())));
        IChatComponent line = new ChatComponentText("").appendSibling(value).appendText(" ");
        if (property.getString().equals(property.getDefault())) {
            IChatComponent suffix = new ChatComponentTranslation(PREFIX + "suffix.default_value");
            suffix.getChatStyle().setColor(EnumChatFormatting.GRAY);
            line.appendSibling(suffix);
        } else {
            IChatComponent suffix = new ChatComponentTranslation(PREFIX + "suffix.modified");
            suffix.getChatStyle().setColor(EnumChatFormatting.GREEN);
            IChatComponent reset = new ChatComponentTranslation(PREFIX + "info.reset");
            reset.getChatStyle().setColor(EnumChatFormatting.GRAY)
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + NAME + " reset " + property.getName()))
                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentTranslation(PREFIX + "info.click_to_reset_to", property.getDefault())));
            line.appendSibling(suffix).appendText(" ").appendSibling(reset);
        }
        sender.addChatMessage(line);
        List<String> examples = examples(property);
        if (examples.isEmpty()) return;
        IChatComponent text = new ChatComponentTranslation(PREFIX + "info.examples");
        for (String example : examples) {
            IChatComponent option = new ChatComponentText(example);
            option.getChatStyle().setColor(example.equals(property.getString()) ? EnumChatFormatting.GREEN : EnumChatFormatting.GRAY)
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, setCommand + example))
                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentTranslation(PREFIX + "info.click_to_set", example)));
            text.appendText(" ").appendSibling(option);
        }
        sender.addChatMessage(text);
    }

    static List<String> examples(Property property) {
        if (property.getValidValues() != null && property.getValidValues().length > 0) return Arrays.asList(property.getValidValues());
        switch (property.getType()) {
            case BOOLEAN: return Arrays.asList("true", "false");
            case INTEGER:
            case DOUBLE: return Arrays.asList(property.getMinValue(), property.getDefault(), property.getMaxValue());
            default: return Collections.emptyList();
        }
    }

    static boolean valid(Property property, String value) {
        if (property.getValidValues() != null && property.getValidValues().length > 0) return Arrays.asList(property.getValidValues()).contains(value);
        try {
            switch (property.getType()) {
                case BOOLEAN: return value.equals("true") || value.equals("false");
                case INTEGER: {
                    int parsed = Integer.parseInt(value);
                    return parsed >= Integer.parseInt(property.getMinValue()) && parsed <= Integer.parseInt(property.getMaxValue());
                }
                case DOUBLE: {
                    double parsed = Double.parseDouble(value);
                    return !Double.isNaN(parsed) && parsed >= Double.parseDouble(property.getMinValue()) && parsed <= Double.parseDouble(property.getMaxValue());
                }
                default: return true;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void set(ICommandSender sender, Property property, String value) {
        String next = value == null || value.isEmpty() ? property.getDefault() : value.trim();
        if (!valid(property, next)) throw new CommandException(PREFIX + "error.invalid_value");
        property.set(next);
        ConfigurationHandler.loadConfiguration();
        ConfigurationHandler.configuration.save();
        IChatComponent text = new ChatComponentTranslation(PREFIX + "set_value", property.getName(), property.getString());
        text.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + NAME + " info " + property.getName()));
        sender.addChatMessage(text);
    }

    private static void resetAll(ICommandSender sender) {
        int count = 0;
        for (Property property : settings().values()) {
            if (property.getString().equals(property.getDefault())) continue;
            property.set(property.getDefault());
            count++;
        }
        ConfigurationHandler.loadConfiguration();
        ConfigurationHandler.configuration.save();
        sender.addChatMessage(new ChatComponentTranslation(PREFIX + "reset_all", count));
    }

    private static void search(ICommandSender sender, String query) {
        List<Property> found = new ArrayList<>();
        for (Property property : settings().values()) {
            String text = (property.getName() + " " + StatCollector.translateToLocal(ConfigTranslations.label(property.getLanguageKey())) + " "
                + StatCollector.translateToLocal(ConfigTranslations.comment(property.getLanguageKey()))).toLowerCase(Locale.ROOT);
            boolean matches = true;
            for (String part : query.toLowerCase(Locale.ROOT).split(" ")) matches &= text.contains(part);
            if (matches) found.add(property);
        }
        if (found.isEmpty()) {
            sender.addChatMessage(new ChatComponentTranslation(PREFIX + "search.none", query));
            return;
        }
        sender.addChatMessage(new ChatComponentTranslation(PREFIX + "search.results", found.size(), query));
        list(sender, found);
    }

    private void update(ICommandSender sender, String[] args) {
        if (args.length == 0 || (args.length == 1 && "status".equalsIgnoreCase(args[0]))) {
            sendUpdateStatus(sender);
            return;
        }
        if (args.length != 1) throw new WrongUsageException(UPDATE_PREFIX + "usage");
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "check":
                updateCheck(sender);
                return;
            case "download":
                updateDownload(sender);
                return;
            case "cancel":
                updateCancel(sender);
                return;
            case "stop":
                updateStop(sender);
                return;
            default:
                throw new WrongUsageException(UPDATE_PREFIX + "usage");
        }
    }

    private void updateCheck(ICommandSender sender) {
        UpdateManager.State current = updates.state();
        if (current == UpdateManager.State.CHECKING || current == UpdateManager.State.DOWNLOADING
            || current == UpdateManager.State.READY_TO_APPLY) {
            sendUpdateStatus(sender);
            return;
        }
        sender.addChatMessage(new ChatComponentTranslation(UPDATE_PREFIX + "checking"));
        updates.checkForUpdates(true, () -> PlayerHandler.INSTANCE.runOnServerThread(() -> {
            if (!isSenderOnline(sender)) return;
            sendUpdateStatus(sender);
        }));
    }

    private void updateDownload(ICommandSender sender) {
        requireServerSelfUpdatePermission(sender);
        UpdateManager.State current = updates.state();
        if (current == UpdateManager.State.READY_TO_APPLY || current == UpdateManager.State.DOWNLOADING
            || current == UpdateManager.State.CHECKING) {
            sendUpdateStatus(sender);
            return;
        }
        UpdateRelease release = updates.availableRelease();
        if (release == null) {
            if (current == UpdateManager.State.UP_TO_DATE) {
                sender.addChatMessage(new ChatComponentTranslation(UPDATE_PREFIX + "up_to_date", updates.currentVersion()));
            } else {
                sender.addChatMessage(new ChatComponentTranslation(UPDATE_PREFIX + "check_first"));
            }
            return;
        }
        if (!updates.startDownload(() -> PlayerHandler.INSTANCE.runOnServerThread(() -> {
            if (!isSenderOnline(sender)) return;
            UpdateManager.State result = updates.state();
            if (result == UpdateManager.State.READY_TO_APPLY) {
                sendReadyStatus(sender);
            } else if (result == UpdateManager.State.FAILED) {
                sender.addChatMessage(new ChatComponentTranslation(UPDATE_PREFIX + "failed",
                    updates.errorMessage() == null ? "Unknown error" : updates.errorMessage()));
            } else if (result == UpdateManager.State.UPDATE_AVAILABLE) {
                sender.addChatMessage(new ChatComponentTranslation(UPDATE_PREFIX + "cancelled"));
            }
        }))) {
            if (updates.state() == UpdateManager.State.FAILED) {
                sender.addChatMessage(new ChatComponentTranslation(UPDATE_PREFIX + "failed",
                    updates.errorMessage() == null ? "Unknown error" : updates.errorMessage()));
            }
            return;
        }
        sender.addChatMessage(new ChatComponentTranslation(UPDATE_PREFIX + "downloading_started",
            release.tag, UpdateRelease.formatBytes(release.assetSize)));
    }

    private void updateCancel(ICommandSender sender) {
        requireServerSelfUpdatePermission(sender);
        if (updates.state() != UpdateManager.State.DOWNLOADING) {
            sender.addChatMessage(new ChatComponentTranslation(UPDATE_PREFIX + "no_download"));
            return;
        }
        updates.cancelDownload();
        sender.addChatMessage(new ChatComponentTranslation(UPDATE_PREFIX + "cancelling"));
    }

    private void updateStop(ICommandSender sender) {
        requireServerSelfUpdatePermission(sender);
        if (updates.state() != UpdateManager.State.READY_TO_APPLY) {
            throw new CommandException(UPDATE_PREFIX + "error.not_ready");
        }
        UpdateRelease release = updates.availableRelease();
        String tag = release == null ? "" : release.tag;
        Reference.logger.info("Stopping server to apply Schematica Plus update {}", tag);
        IChatComponent notice = new ChatComponentTranslation(UPDATE_PREFIX + "stopping", tag);
        sender.addChatMessage(notice);
        MinecraftServer server = MinecraftServer.getServer();
        if (server != null) {
            for (Object candidate : server.getConfigurationManager().playerEntityList) {
                EntityPlayerMP player = (EntityPlayerMP) candidate;
                if (player != sender) player.addChatMessage(notice);
            }
        }
        updates.applyNowAndShutdown();
    }

    private static void requireServerSelfUpdatePermission(ICommandSender sender) {
        if (!SchematicaPlus.proxy.dedicatedServer()) {
            throw new CommandException(UPDATE_PREFIX + "error.client_gui");
        }
        if (!ConfigurationHandler.serverSelfUpdateEnabled) {
            throw new CommandException(UPDATE_PREFIX + "error.disabled");
        }
        if (ConfigurationHandler.serverUpdateConsoleOnly && sender instanceof EntityPlayer) {
            throw new CommandException(UPDATE_PREFIX + "error.console_only");
        }
    }

    private void sendUpdateStatus(ICommandSender sender) {
        UpdateManager.State state = updates.state();
        UpdateRelease release = updates.availableRelease();
        switch (state) {
            case CHECKING:
                sender.addChatMessage(new ChatComponentTranslation(UPDATE_PREFIX + "checking"));
                return;
            case UPDATE_AVAILABLE:
                if (release != null) {
                    IChatComponent line = new ChatComponentTranslation(UPDATE_PREFIX + "available",
                        updates.currentVersion(), release.tag, UpdateRelease.formatBytes(release.assetSize));
                    if (SchematicaPlus.proxy.dedicatedServer() && ConfigurationHandler.serverSelfUpdateEnabled
                        && !(ConfigurationHandler.serverUpdateConsoleOnly && sender instanceof EntityPlayer)) {
                        line.appendText(" ").appendSibling(actionLink(UPDATE_PREFIX + "action.download",
                            "/" + NAME + " update download", EnumChatFormatting.GREEN));
                    }
                    sender.addChatMessage(line);
                    return;
                }
                break;
            case DOWNLOADING: {
                long done = updates.downloadedBytes();
                long total = updates.totalBytes();
                if (updates.verifying()) {
                    sender.addChatMessage(new ChatComponentTranslation(UPDATE_PREFIX + "verifying",
                        release == null ? "" : release.assetName));
                } else if (total > 0L) {
                    int percent = (int) Math.max(0L, Math.min(100L, done * 100L / total));
                    sender.addChatMessage(new ChatComponentTranslation(UPDATE_PREFIX + "downloading_progress",
                        UpdateRelease.formatBytes(done), UpdateRelease.formatBytes(total), percent));
                } else {
                    sender.addChatMessage(new ChatComponentTranslation(UPDATE_PREFIX + "downloading_unknown",
                        UpdateRelease.formatBytes(done)));
                }
                return;
            }
            case READY_TO_APPLY:
                sendReadyStatus(sender);
                return;
            case FAILED:
                sender.addChatMessage(new ChatComponentTranslation(UPDATE_PREFIX + "failed",
                    updates.errorMessage() == null ? "Unknown error" : updates.errorMessage()));
                return;
            case IDLE:
            case UP_TO_DATE:
            default:
                break;
        }
        IChatComponent line = new ChatComponentTranslation(UPDATE_PREFIX + "up_to_date", updates.currentVersion());
        line.appendText(" ").appendSibling(actionLink(UPDATE_PREFIX + "action.check",
            "/" + NAME + " update check", EnumChatFormatting.AQUA));
        sender.addChatMessage(line);
    }

    private void sendReadyStatus(ICommandSender sender) {
        UpdateRelease release = updates.availableRelease();
        String tag = release == null ? "" : release.tag;
        IChatComponent line = new ChatComponentTranslation(UPDATE_PREFIX + "ready", tag);
        if (SchematicaPlus.proxy.dedicatedServer() && ConfigurationHandler.serverSelfUpdateEnabled
            && !(ConfigurationHandler.serverUpdateConsoleOnly && sender instanceof EntityPlayer)) {
            line.appendText(" ").appendSibling(actionLink(UPDATE_PREFIX + "action.stop",
                "/" + NAME + " update stop", EnumChatFormatting.GOLD));
        }
        sender.addChatMessage(line);
        sender.addChatMessage(new ChatComponentTranslation(UPDATE_PREFIX + "client_compat_note", tag));
    }

    private static IChatComponent actionLink(String key, String command, EnumChatFormatting color) {
        IChatComponent link = new ChatComponentText("[").appendSibling(new ChatComponentTranslation(key)).appendText("]");
        link.getChatStyle().setColor(color)
            .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
            .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(command)));
        return link;
    }

    private static boolean isSenderOnline(ICommandSender sender) {
        if (!(sender instanceof EntityPlayerMP)) return true;
        MinecraftServer server = MinecraftServer.getServer();
        return server != null && server.getConfigurationManager().playerEntityList.contains(sender);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "list", "info", "set", "reset", "search", "reload", "save", "update");
        if (args.length == 2 && "update".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "status", "check", "download", "cancel", "stop");
        }
        if (args.length == 2 && ("info".equalsIgnoreCase(args[0]) || "set".equalsIgnoreCase(args[0]))) {
            return getListOfStringsFromIterableMatchingLastWord(args, settings().keySet());
        }
        if (args.length == 2 && "reset".equalsIgnoreCase(args[0])) {
            List<String> names = new ArrayList<>();
            names.add("all");
            names.addAll(settings().keySet());
            return getListOfStringsFromIterableMatchingLastWord(args, names);
        }
        if (args.length == 3 && "set".equalsIgnoreCase(args[0])) {
            Property property = settings().get(args[1]);
            if (property != null) return getListOfStringsFromIterableMatchingLastWord(args, examples(property));
        }
        return null;
    }
}
