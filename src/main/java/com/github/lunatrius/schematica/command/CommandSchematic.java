package com.github.lunatrius.schematica.command;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;

import com.github.lunatrius.schematica.FileFilterSchematic;
import com.github.lunatrius.schematica.SchematicaPlus;
import com.github.lunatrius.schematica.api.SchematicRegion;
import com.github.lunatrius.schematica.cloud.SharedPlacement;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.DownloadHandler;
import com.github.lunatrius.schematica.handler.SelectionSaves;
import com.github.lunatrius.schematica.handler.SharedPlacements;
import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.FileUtils;
import com.github.lunatrius.schematica.util.MessageType;
import com.github.lunatrius.schematica.util.PlayerMessage;
import com.github.lunatrius.schematica.world.storage.RegionSelection;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

public class CommandSchematic extends CommandBase {
    private static final FileFilterSchematic FILE_FILTER_SCHEMATIC = new FileFilterSchematic(false);
    private static final int PAGE_SIZE = 9;

    @Override
    public String getCommandName() {
        return Names.Command.NAME;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return Names.Command.USAGE;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return sender instanceof EntityPlayerMP || super.canCommandSenderUseCommand(sender);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) throw new WrongUsageException(getCommandUsage(sender));
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        if (Names.Command.Shared.NAME.equalsIgnoreCase(args[0]) && ConfigurationHandler.sharedPlacementsEnabled) {
            shared(sender, rest);
            return;
        }
        if (!(sender instanceof EntityPlayerMP player)) throw new CommandException(Names.Command.Save.Message.PLAYERS_ONLY);
        if (!ConfigurationHandler.serversideSchematicsEnabled) throw new WrongUsageException(getCommandUsage(sender));
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case Names.Command.Save.NAME:
                save(player, rest);
                return;
            case Names.Command.List.NAME:
                list(player, rest);
                return;
            case Names.Command.Remove.NAME:
                remove(player, rest);
                return;
            case Names.Command.Download.NAME:
                if (!SchematicaPlus.proxy.dedicatedServer()) break;
                download(player, rest);
                return;
            default:
                break;
        }
        throw new WrongUsageException(getCommandUsage(sender));
    }

    private static void message(EntityPlayer player, MessageType type, String key, Object... args) {
        SchematicaPlus.proxy.sendMessage(player, new PlayerMessage(type, key, args));
    }

    private static File directory(EntityPlayer player) {
        File directory = SchematicaPlus.proxy.getPlayerSchematicDirectory(player, true);
        if (directory == null) {
            Reference.logger.warn("Unable to determine the schematic directory for player {}", player);
            return null;
        }
        if (!directory.isDirectory() && !directory.mkdirs()) {
            Reference.logger.warn("Could not create player schematic directory {}", directory.getAbsolutePath());
            return null;
        }
        return directory;
    }

    private static void save(EntityPlayerMP player, String[] args) {
        if (args.length == 1) {
            SelectionSaves.INSTANCE.request(player, args[0]);
            return;
        }
        if (args.length != 7) throw new WrongUsageException(Names.Command.Save.Message.USAGE);
        ChunkCoordinates position = player.getPlayerCoordinates();
        int[] corners = new int[6];
        for (int i = 0; i < 6; i++) {
            int base = i % 3 == 0 ? position.posX : i % 3 == 1 ? position.posY : position.posZ;
            corners[i] = MathHelper.floor_double(func_110666_a(player, base, args[i + 1]));
        }
        RegionSelection selection;
        try {
            selection = new RegionSelection(Collections.singletonList(new SchematicRegion(args[0],
                corners[0], corners[1], corners[2], corners[3], corners[4], corners[5])));
        } catch (IllegalArgumentException e) {
            message(player, MessageType.ERROR, Names.Command.Save.Message.SAVE_FAILED, args[0]);
            return;
        }
        save(player, args[0], selection);
    }

    public static void save(EntityPlayer player, String name, RegionSelection selection) {
        if (SchematicaPlus.proxy.isPlayerQuotaExceeded(player)) {
            message(player, MessageType.ERROR, Names.Command.Save.Message.QUOTA_EXCEEDED);
            return;
        }
        File directory = directory(player);
        if (directory == null) {
            message(player, MessageType.ERROR, Names.Command.Save.Message.PLAYER_SCHEMATIC_DIR_UNAVAILABLE);
            return;
        }
        String filename = name + ".schematic";
        Reference.logger.debug("Saving schematic {} for {}", filename, player.getCommandSenderName());
        if (!SchematicaPlus.proxy.saveSchematic(player, directory, filename, player.worldObj, selection)) {
            message(player, MessageType.ERROR, Names.Command.Save.Message.SAVE_FAILED, filename);
        }
    }

    private static void list(EntityPlayerMP player, String[] args) {
        if (args.length > 1) throw new WrongUsageException(Names.Command.List.Message.USAGE);
        int page = args.length == 0 ? 0 : Math.max(0, parseInt(player, args[0]) - 1);
        File directory = directory(player);
        if (directory == null) {
            message(player, MessageType.ERROR, Names.Command.Save.Message.PLAYER_SCHEMATIC_DIR_UNAVAILABLE);
            return;
        }
        File[] files = directory.listFiles(FILE_FILTER_SCHEMATIC);
        if (files == null || files.length == 0) {
            message(player, MessageType.INFO, Names.Command.List.Message.NO_SCHEMATICS);
            return;
        }
        int pages = (files.length + PAGE_SIZE - 1) / PAGE_SIZE;
        if (page >= pages) {
            message(player, MessageType.ERROR, Names.Command.List.Message.NO_SUCH_PAGE);
            return;
        }
        player.addChatMessage(new ChatComponentTranslation(Names.Command.List.Message.PAGE_HEADER, page + 1, pages)
            .setChatStyle(new ChatStyle().setColor(EnumChatFormatting.DARK_GREEN)));
        for (int i = page * PAGE_SIZE; i < Math.min(files.length, (page + 1) * PAGE_SIZE); i++) {
            File file = files[i];
            IChatComponent line = new ChatComponentText(String.format("%2d (%s): %s [", i + 1,
                FileUtils.humanReadableByteCount(file.length()), file.getName()));
            line.appendSibling(link(Names.Command.List.Message.REMOVE, Names.Command.Remove.NAME, file.getName(), EnumChatFormatting.RED));
            line.appendText("]");
            if (SchematicaPlus.proxy.dedicatedServer()) {
                line.appendText("[");
                line.appendSibling(link(Names.Command.List.Message.DOWNLOAD, Names.Command.Download.NAME, file.getName(), EnumChatFormatting.GREEN));
                line.appendText("]");
            }
            player.addChatMessage(line);
        }
    }

    private static IChatComponent link(String key, String subcommand, String argument, EnumChatFormatting color) {
        String command = String.format("/%s %s %s", Names.Command.NAME, subcommand, argument);
        return new ChatComponentTranslation(key).setChatStyle(
            new ChatStyle().setColor(color).setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
    }

    static String confirmation(String name) {
        return Hashing.md5().hashString(name, Charsets.UTF_8).toString();
    }

    private static void remove(EntityPlayerMP player, String[] args) {
        if (args.length < 1) throw new WrongUsageException(Names.Command.Remove.Message.USAGE);
        String name = String.join(" ", args);
        boolean confirmed = false;
        if (args.length > 1 && args[args.length - 1].length() == 32) {
            String unconfirmed = String.join(" ", Arrays.copyOfRange(args, 0, args.length - 1));
            if (args[args.length - 1].equals(confirmation(unconfirmed))) {
                name = unconfirmed;
                confirmed = true;
            }
        }
        File file = find(player, name, false);
        if (file == null) {
            message(player, MessageType.ERROR, Names.Command.Remove.Message.SCHEMATIC_NOT_FOUND, name);
            return;
        }
        if (!confirmed) {
            String command = String.format("/%s %s %s %s", Names.Command.NAME, Names.Command.Remove.NAME, file.getName(), confirmation(file.getName()));
            player.addChatMessage(new ChatComponentTranslation(Names.Command.Remove.Message.ARE_YOU_SURE_START, file.getName())
                .appendSibling(new ChatComponentText(" ["))
                .appendSibling(new ChatComponentTranslation(Names.Command.Remove.Message.YES).setChatStyle(
                    new ChatStyle().setColor(EnumChatFormatting.RED).setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))))
                .appendSibling(new ChatComponentText("]")));
        } else if (file.delete()) {
            message(player, MessageType.SUCCESS, Names.Command.Remove.Message.SCHEMATIC_REMOVED, file.getName());
        } else {
            message(player, MessageType.ERROR, Names.Command.Remove.Message.SCHEMATIC_NOT_FOUND, file.getName());
        }
    }

    private static void download(EntityPlayerMP player, String[] args) {
        if (args.length < 1) throw new WrongUsageException(Names.Command.Download.Message.USAGE);
        String name = String.join(" ", args);
        File file = find(player, name, ConfigurationHandler.useSchematicplusFormat);
        if (file == null) {
            message(player, MessageType.ERROR, Names.Command.Download.Message.DOWNLOAD_FAILED, name);
            return;
        }
        DownloadHandler.INSTANCE.read(player, file);
        message(player, MessageType.INFO, Names.Command.Download.Message.DOWNLOAD_STARTED, file.getName());
    }

    private static File find(EntityPlayer player, String name, boolean preferExtended) {
        File directory = SchematicaPlus.proxy.getPlayerSchematicDirectory(player, true);
        if (directory == null) return null;
        try {
            File file = FileUtils.findSchematicFile(directory, name, preferExtended);
            return file.isFile() ? file : null;
        } catch (IOException e) {
            Reference.logger.warn("Rejected schematic filename {}", name, e);
            return null;
        }
    }

    private static List<String> subcommands() {
        List<String> names = new ArrayList<>();
        if (ConfigurationHandler.serversideSchematicsEnabled) {
            names.addAll(Arrays.asList(Names.Command.Save.NAME, Names.Command.List.NAME, Names.Command.Remove.NAME));
            if (SchematicaPlus.proxy.dedicatedServer()) names.add(Names.Command.Download.NAME);
        }
        if (ConfigurationHandler.sharedPlacementsEnabled) names.add(Names.Command.Shared.NAME);
        return names;
    }

    private static void shared(ICommandSender sender, String[] args) {
        if (args.length == 0 || args.length == 1 && Names.Command.List.NAME.equalsIgnoreCase(args[0])) {
            List<SharedPlacement> placements = SharedPlacements.INSTANCE.placements();
            if (placements.isEmpty()) {
                sender.addChatMessage(new ChatComponentTranslation(Names.Command.Shared.Message.NONE));
                return;
            }
            sender.addChatMessage(new ChatComponentTranslation(Names.Command.Shared.Message.HEADER, placements.size())
                .setChatStyle(new ChatStyle().setColor(EnumChatFormatting.DARK_GREEN)));
            for (SharedPlacement placement : placements) {
                IChatComponent line = new ChatComponentTranslation(Names.Command.Shared.Message.ENTRY, placement.name, placement.ownerName,
                    placement.dimension, placement.origin[0], placement.origin[1], placement.origin[2]);
                line.appendText(" [");
                line.appendSibling(link(Names.Command.List.Message.REMOVE, Names.Command.Shared.NAME + " " + Names.Command.Remove.NAME,
                    placement.id.toString(), EnumChatFormatting.RED));
                line.appendText("]");
                sender.addChatMessage(line);
            }
            return;
        }
        if (args.length != 2 || !Names.Command.Remove.NAME.equalsIgnoreCase(args[0])) throw new WrongUsageException(Names.Command.Shared.Message.USAGE);
        SharedPlacement found = null;
        for (SharedPlacement placement : SharedPlacements.INSTANCE.placements()) {
            if (placement.id.toString().equalsIgnoreCase(args[1]) || placement.name.equalsIgnoreCase(args[1])) found = placement;
        }
        if (found == null) throw new CommandException(Names.Command.Shared.Message.NOT_FOUND, args[1]);
        boolean owner = sender instanceof EntityPlayerMP && found.ownerId.equals(((EntityPlayerMP) sender).getUniqueID().toString());
        if (!owner && !sender.canCommandSenderUseCommand(ConfigurationHandler.sharedPlacementManagePermissionLevel, Names.Command.NAME)) {
            throw new CommandException("commands.generic.permission");
        }
        SharedPlacements.INSTANCE.remove(found.id);
        sender.addChatMessage(new ChatComponentTranslation(Names.Command.Shared.Message.REMOVED, found.name));
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) return getListOfStringsFromIterableMatchingLastWord(args, subcommands());
        if (args.length == 2 && Names.Command.Shared.NAME.equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, Names.Command.List.NAME, Names.Command.Remove.NAME);
        }
        if (args.length == 3 && Names.Command.Shared.NAME.equalsIgnoreCase(args[0]) && Names.Command.Remove.NAME.equalsIgnoreCase(args[1])) {
            List<String> ids = new ArrayList<>();
            for (SharedPlacement placement : SharedPlacements.INSTANCE.placements()) ids.add(placement.id.toString());
            return getListOfStringsFromIterableMatchingLastWord(args, ids);
        }
        if (args.length != 2 || !(sender instanceof EntityPlayer player)) return null;
        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (!subcommand.equals(Names.Command.Remove.NAME)
            && !(SchematicaPlus.proxy.dedicatedServer() && subcommand.equals(Names.Command.Download.NAME))) return null;
        File directory = SchematicaPlus.proxy.getPlayerSchematicDirectory(player, true);
        File[] files = directory == null ? null : directory.listFiles(FILE_FILTER_SCHEMATIC);
        if (files == null) return null;
        List<String> names = new ArrayList<>();
        for (File file : files) names.add(file.getName());
        return getListOfStringsFromIterableMatchingLastWord(args, names);
    }
}
