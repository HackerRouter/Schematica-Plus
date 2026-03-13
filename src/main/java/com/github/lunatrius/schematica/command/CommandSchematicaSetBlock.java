package com.github.lunatrius.schematica.command;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.github.lunatrius.schematica.tool.ToolManager;
import com.github.lunatrius.schematica.tool.ToolMode;

import cpw.mods.fml.common.registry.GameData;

/**
 * Client-side command to set primary or target (secondary) block on the current tool mode.
 * Usage: /sblock <primary|target> <block_name> [meta]
 */
public class CommandSchematicaSetBlock extends CommandBase {

    @Override
    public String getCommandName() {
        return "sblock";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/sblock <primary|target> <block_name> [meta]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        String type = args[0].toLowerCase();
        boolean isPrimary;
        if ("primary".equals(type) || "p".equals(type)) {
            isPrimary = true;
        } else if ("target".equals(type) || "secondary".equals(type) || "t".equals(type) || "s".equals(type)) {
            isPrimary = false;
        } else {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        String blockName = args[1];
        int meta = 0;
        if (args.length >= 3) {
            try {
                meta = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.RED + "[Schematica] Invalid meta value: " + args[2]));
                return;
            }
        }

        Block block = (Block) GameData.getBlockRegistry().getObject(blockName);
        if (block == null || "minecraft:air".equals(GameData.getBlockRegistry().getNameForObject(block))) {
            // Try with minecraft: prefix
            if (!blockName.contains(":")) {
                block = (Block) GameData.getBlockRegistry().getObject("minecraft:" + blockName);
            }
            if (block == null || "minecraft:air".equals(GameData.getBlockRegistry().getNameForObject(block))) {
                sender.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.RED + "[Schematica] Block not found: " + blockName));
                return;
            }
        }

        ToolMode mode = ToolManager.getCurrentMode();
        String registryName = GameData.getBlockRegistry().getNameForObject(block);

        if (isPrimary) {
            mode.setPrimaryBlock(block, meta);
            sender.addChatMessage(new ChatComponentText(
                EnumChatFormatting.GREEN + "[Schematica] " + EnumChatFormatting.RESET +
                "Primary block set to " + registryName + ":" + meta +
                " (mode: " + mode.getDisplayName() + ")"));
        } else {
            mode.setSecondaryBlock(block, meta);
            sender.addChatMessage(new ChatComponentText(
                EnumChatFormatting.GREEN + "[Schematica] " + EnumChatFormatting.RESET +
                "Target block set to " + registryName + ":" + meta +
                " (mode: " + mode.getDisplayName() + ")"));
        }
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "primary", "target");
        }
        if (args.length == 2) {
            // Tab-complete block names
            List<String> blockNames = new ArrayList<>();
            for (Object name : GameData.getBlockRegistry().getKeys()) {
                blockNames.add((String) name);
            }
            return getListOfStringsMatchingLastWord(args, blockNames.toArray(new String[0]));
        }
        return null;
    }
}
