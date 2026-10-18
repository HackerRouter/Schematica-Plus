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
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.config.Property;

import com.github.lunatrius.schematica.client.gui.config.ConfigTranslations;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.reference.Reference;

public class CommandSchematicaPlus extends CommandBase {
    public static final String NAME = "schematicaplus";
    private static final String PREFIX = "schematica.command.config.";

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
            case "search":
                if (args.length < 2) break;
                search(sender, func_82360_a(sender, args, 1));
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
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, setCommand + property.getDefault()))
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

    @Override
    @SuppressWarnings("rawtypes")
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "list", "info", "set", "search", "reload", "save");
        if (args.length == 2 && ("info".equalsIgnoreCase(args[0]) || "set".equalsIgnoreCase(args[0]))) {
            return getListOfStringsFromIterableMatchingLastWord(args, settings().keySet());
        }
        if (args.length == 3 && "set".equalsIgnoreCase(args[0])) {
            Property property = settings().get(args[1]);
            if (property != null) return getListOfStringsFromIterableMatchingLastWord(args, examples(property));
        }
        return null;
    }
}
