package com.github.lunatrius.schematica.tool;

import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import com.github.lunatrius.core.util.vector.Vector3i;
import com.github.lunatrius.schematica.client.gui.load.GuiSchematicLoad;
import com.github.lunatrius.schematica.client.renderer.RendererSchematicGlobal;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.nbt.NBTHelper;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Reference;

import cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry;
import cpw.mods.fml.common.registry.GameData;

/**
 * Handles tool actions for each ToolMode. Executes the actual operations
 * when the player uses the tool (right-click / left-click with tool item).
 * <p>
 * Ported from Litematica's tool action system, adapted for Schematica 1.7.10.
 *
 * @author HackerRouter (ported from Litematica by masa)
 */
public class ToolHandler {

    private ToolHandler() {}

    /**
     * Called on right-click (use action) with the tool active.
     * Behavior depends on the current ToolMode.
     */
    public static boolean onToolUse(EntityPlayer player, MovingObjectPosition mop) {
        ToolMode mode = ToolManager.getCurrentMode();

        // Modes that don't require a block target
        switch (mode) {
            case PASTE_SCHEMATIC:
                return handlePasteUse(player);
            case MOVE:
                return handleMoveUse(player);
            case FILL:
                return handleFillUse(player);
            case SCHEMATIC_PLACEMENT:
                return handlePlacementUse(player, mop);
            default:
                break;
        }

        // All remaining modes require a valid block hit
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return false;
        }

        switch (mode) {
            case AREA_SELECTION:
                return handleAreaSelectionUse(player, mop);
            case DELETE:
                return handleDeleteUse(player, mop);
            case REPLACE_BLOCK:
                return handleReplaceUse(player, mop);
            default:
                return false;
        }
    }

    /**
     * Called on left-click (attack action) with the tool active.
     */
    public static boolean onToolAttack(EntityPlayer player, MovingObjectPosition mop) {
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return false;
        }

        ToolMode mode = ToolManager.getCurrentMode();

        if (mode == ToolMode.AREA_SELECTION) {
            return handleAreaSelectionAttack(player, mop);
        }

        return false;
    }

    // --- AREA_SELECTION ---

    /**
     * Right-click sets point B (second corner of selection box).
     */
    private static boolean handleAreaSelectionUse(EntityPlayer player, MovingObjectPosition mop) {
        ClientProxy.pointB.set(mop.blockX, mop.blockY, mop.blockZ);
        ClientProxy.updatePoints();
        ClientProxy.isRenderingGuide = true;

        sendChat(player, "Point B set: " + mop.blockX + ", " + mop.blockY + ", " + mop.blockZ);
        return true;
    }

    /**
     * Left-click sets point A (first corner of selection box).
     */
    private static boolean handleAreaSelectionAttack(EntityPlayer player, MovingObjectPosition mop) {
        ClientProxy.pointA.set(mop.blockX, mop.blockY, mop.blockZ);
        ClientProxy.updatePoints();
        ClientProxy.isRenderingGuide = true;

        sendChat(player, "Point A set: " + mop.blockX + ", " + mop.blockY + ", " + mop.blockZ);
        return true;
    }

    // --- SCHEMATIC_PLACEMENT ---

    /**
     * Right-click in placement mode moves the schematic origin to the targeted block.
     * If no schematic is loaded, opens the load GUI instead.
     */
    private static boolean handlePlacementUse(EntityPlayer player, MovingObjectPosition mop) {
        SchematicWorld schematic = ClientProxy.schematic;
        if (schematic == null) {
            // No schematic loaded — open the load GUI so the player can pick one
            Minecraft.getMinecraft().displayGuiScreen(new GuiSchematicLoad(Minecraft.getMinecraft().currentScreen));
            return true;
        }

        // If no block is being looked at, fall back to MOVE behavior (move to player)
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            ClientProxy.moveSchematicToPlayer(schematic);
            RendererSchematicGlobal.INSTANCE.refresh();
            Vector3i pos = schematic.position;
            sendChat(player, "Schematic moved to player: " + pos.x + ", " + pos.y + ", " + pos.z);
            return true;
        }

        // Place one block above the targeted block
        int placeY = mop.blockY + 1;
        schematic.position.set(mop.blockX, placeY, mop.blockZ);
        RendererSchematicGlobal.INSTANCE.refresh();
        sendChat(player, "Schematic placed at: " + mop.blockX + ", " + placeY + ", " + mop.blockZ);
        return true;
    }

    // --- MOVE ---

    /**
     * Right-click in move mode moves the schematic to the player's position.
     */
    private static boolean handleMoveUse(EntityPlayer player) {
        SchematicWorld schematic = ClientProxy.schematic;
        if (schematic == null) {
            sendChat(player, EnumChatFormatting.RED + "No schematic loaded.");
            return false;
        }

        ClientProxy.moveSchematicToPlayer(schematic);
        RendererSchematicGlobal.INSTANCE.refresh();
        Vector3i pos = schematic.position;
        sendChat(player, "Schematic moved to player: " + pos.x + ", " + pos.y + ", " + pos.z);
        return true;
    }

    // --- PASTE_SCHEMATIC ---

    private static final FMLControlledNamespacedRegistry<Block> BLOCK_REGISTRY = GameData.getBlockRegistry();

    /**
     * Pastes the loaded schematic into the real world at its current position.
     * Creative-only operation. Uses direct server world access when available
     * (singleplayer) for full NBT support, falls back to /setblock commands.
     */
    private static boolean handlePasteUse(EntityPlayer player) {
        if (!player.capabilities.isCreativeMode) {
            sendChat(player, EnumChatFormatting.RED + "Paste requires creative mode.");
            return false;
        }

        SchematicWorld schematic = ClientProxy.schematic;
        if (schematic == null) {
            sendChat(player, EnumChatFormatting.RED + "No schematic loaded.");
            return false;
        }

        // Try direct server world placement first (preserves tile entity NBT fully).
        // Falls back to /setblock commands if no server world is available.
        WorldServer serverWorld = getServerWorld(player);
        int count;
        if (serverWorld != null) {
            count = pasteDirectly(serverWorld, schematic);
            int entityCount = schematic.getEntities().size();
            sendChat(player, "Pasted " + count + " blocks and " + entityCount + " entities directly.");
        } else {
            count = pasteWithSetblock(player, schematic);
            sendChat(player, "Pasted " + count + " blocks via setblock.");
        }
        return true;
    }

    /**
     * Gets the server-side world for the player's dimension, if available.
     * Returns null on dedicated servers (where client has no server access).
     */
    private static WorldServer getServerWorld(EntityPlayer player) {
        try {
            MinecraftServer server = MinecraftServer.getServer();
            if (server != null) {
                return server.worldServerForDimension(player.dimension);
            }
        } catch (Exception e) {
            // Not available
        }
        return null;
    }

    /**
     * Pastes the schematic directly into the server world, including full tile entity
     * NBT data (inventories, sign text, etc.). This bypasses chat command length limits.
     */
    private static int pasteDirectly(WorldServer serverWorld, SchematicWorld schematic) {
        int count = 0;
        Vector3i pos = schematic.position;

        // Pass 1: Place all blocks WITHOUT block updates (flag 2 = send to client only).
        // This prevents onBlockAdded() from resetting directional metadata (e.g. dispensers,
        // pistons, furnaces) based on surrounding context during placement.
        for (int y = 0; y < schematic.getHeight(); y++) {
            for (int x = 0; x < schematic.getWidth(); x++) {
                for (int z = 0; z < schematic.getLength(); z++) {
                    Block block = schematic.getBlock(x, y, z);
                    if (block == Blocks.air || block == null) {
                        continue;
                    }
                    int metadata = schematic.getBlockMetadata(x, y, z);
                    int wx = pos.x + x;
                    int wy = pos.y + y;
                    int wz = pos.z + z;

                    // Flag 2 = send to clients, no block update / no onBlockAdded
                    serverWorld.setBlock(wx, wy, wz, block, metadata, 2);
                    count++;

                    // Copy tile entity NBT data if present
                    TileEntity te = schematic.getTileEntity(x, y, z);
                    if (te != null) {
                        try {
                            NBTTagCompound nbt = new NBTTagCompound();
                            te.writeToNBT(nbt);
                            // Update coordinates to world position
                            nbt.setInteger("x", wx);
                            nbt.setInteger("y", wy);
                            nbt.setInteger("z", wz);
                            // Load the TE from NBT into the server world
                            TileEntity newTe = TileEntity.createAndLoadEntity(nbt);
                            if (newTe != null) {
                                serverWorld.setTileEntity(wx, wy, wz, newTe);
                            }
                        } catch (Exception e) {
                            Reference.logger.debug("Failed to paste tile entity NBT at {},{},{}", wx, wy, wz, e);
                        }
                    }
                }
            }
        }

        // Spawn entities with offset
        for (Entity entity : schematic.getEntities()) {
            try {
                NBTTagCompound nbt = new NBTTagCompound();
                entity.writeToNBTOptional(nbt);

                // Update the Pos tag list with world-offset coordinates
                NBTTagList posList = new NBTTagList();
                posList.appendTag(new NBTTagDouble(entity.posX + pos.x));
                posList.appendTag(new NBTTagDouble(entity.posY + pos.y));
                posList.appendTag(new NBTTagDouble(entity.posZ + pos.z));
                nbt.setTag("Pos", posList);

                // Remove old UUID so a new one is generated on creation
                nbt.removeTag("UUIDMost");
                nbt.removeTag("UUIDLeast");

                Entity newEntity = EntityList.createEntityFromNBT(nbt, serverWorld);
                if (newEntity != null) {
                    serverWorld.spawnEntityInWorld(newEntity);
                }
            } catch (Exception e) {
                Reference.logger.debug("Failed to paste entity at schematic offset", e);
            }
        }

        // Pass 2: Re-apply metadata for all non-air blocks to ensure it wasn't
        // corrupted, then notify neighbors so redstone etc. updates correctly.
        for (int y = 0; y < schematic.getHeight(); y++) {
            for (int x = 0; x < schematic.getWidth(); x++) {
                for (int z = 0; z < schematic.getLength(); z++) {
                    Block block = schematic.getBlock(x, y, z);
                    if (block == Blocks.air || block == null) {
                        continue;
                    }
                    int metadata = schematic.getBlockMetadata(x, y, z);
                    int wx = pos.x + x;
                    int wy = pos.y + y;
                    int wz = pos.z + z;

                    // Force-set the correct metadata and notify neighbors
                    serverWorld.setBlockMetadataWithNotify(wx, wy, wz, metadata, 3);
                }
            }
        }

        return count;
    }

    /**
     * Pastes the schematic using /setblock commands for each non-air block.
     * Used as fallback when direct server world access is not available.
     * Note: tile entity NBT data cannot be included via chat commands due to length limits.
     */
    private static int pasteWithSetblock(EntityPlayer player, SchematicWorld schematic) {
        EntityClientPlayerMP clientPlayer = Minecraft.getMinecraft().thePlayer;
        if (clientPlayer == null) {
            return 0;
        }

        int count = 0;
        Vector3i pos = schematic.position;

        for (int y = 0; y < schematic.getHeight(); y++) {
            for (int x = 0; x < schematic.getWidth(); x++) {
                for (int z = 0; z < schematic.getLength(); z++) {
                    Block block = schematic.getBlock(x, y, z);
                    if (block == Blocks.air || block == null) {
                        continue;
                    }
                    int metadata = schematic.getBlockMetadata(x, y, z);
                    int wx = pos.x + x;
                    int wy = pos.y + y;
                    int wz = pos.z + z;

                    String blockName = BLOCK_REGISTRY.getNameForObject(block);
                    String cmd = "/setblock " + wx + " " + wy + " " + wz + " " + blockName + " " + metadata + " replace";
                    clientPlayer.sendChatMessage(cmd);
                    count++;
                }
            }
        }

        // Spawn entities via /summon commands
        for (Entity entity : schematic.getEntities()) {
            try {
                String entityName = EntityList.getEntityString(entity);
                if (entityName == null) continue;
                double wx = entity.posX + pos.x;
                double wy = entity.posY + pos.y;
                double wz = entity.posZ + pos.z;
                String cmd = "/summon " + entityName + " " + wx + " " + wy + " " + wz;
                clientPlayer.sendChatMessage(cmd);
            } catch (Exception e) {
                Reference.logger.debug("Failed to summon entity via command", e);
            }
        }

        return count;
    }

    // --- DELETE ---

    /**
     * Deletes all blocks in the selected area (sets to air). Creative-only.
     */
    private static boolean handleDeleteUse(EntityPlayer player, MovingObjectPosition mop) {
        if (!player.capabilities.isCreativeMode) {
            sendChat(player, EnumChatFormatting.RED + "Delete requires creative mode.");
            return false;
        }

        Vector3i min = ClientProxy.pointMin;
        Vector3i max = ClientProxy.pointMax;

        if (min.x == max.x && min.y == max.y && min.z == max.z) {
            sendChat(player, EnumChatFormatting.RED + "No area selected. Use Area Selection mode first.");
            return false;
        }

        World world = player.worldObj;
        int count = 0;
        for (int x = min.x; x <= max.x; x++) {
            for (int y = min.y; y <= max.y; y++) {
                for (int z = min.z; z <= max.z; z++) {
                    if (!world.isAirBlock(x, y, z)) {
                        world.setBlock(x, y, z, Blocks.air, 0, 2);
                        count++;
                    }
                }
            }
        }

        sendChat(player, "Deleted " + count + " blocks in selection.");
        return true;
    }

    // --- FILL ---

    /**
     * Fills the selected area with the block the player is holding. Creative-only.
     */
    private static boolean handleFillUse(EntityPlayer player) {
        if (!player.capabilities.isCreativeMode) {
            sendChat(player, EnumChatFormatting.RED + "Fill requires creative mode.");
            return false;
        }

        Vector3i min = ClientProxy.pointMin;
        Vector3i max = ClientProxy.pointMax;

        if (min.x == max.x && min.y == max.y && min.z == max.z) {
            sendChat(player, EnumChatFormatting.RED + "No area selected. Use Area Selection mode first.");
            return false;
        }

        ItemStack held = player.getHeldItem();
        if (held == null) {
            sendChat(player, EnumChatFormatting.RED + "Hold a block to fill with.");
            return false;
        }

        Block block = Block.getBlockFromItem(held.getItem());
        if (block == Blocks.air) {
            sendChat(player, EnumChatFormatting.RED + "Held item is not a placeable block.");
            return false;
        }

        int meta = held.getItemDamage();
        World world = player.worldObj;
        int count = 0;

        for (int x = min.x; x <= max.x; x++) {
            for (int y = min.y; y <= max.y; y++) {
                for (int z = min.z; z <= max.z; z++) {
                    world.setBlock(x, y, z, block, meta, 2);
                    count++;
                }
            }
        }

        sendChat(player, "Filled " + count + " blocks with " + block.getLocalizedName() + ":" + meta);
        return true;
    }

    // --- REPLACE_BLOCK ---

    /**
     * Replaces all instances of the targeted block in the selection with the held block.
     * Creative-only.
     */
    private static boolean handleReplaceUse(EntityPlayer player, MovingObjectPosition mop) {
        if (!player.capabilities.isCreativeMode) {
            sendChat(player, EnumChatFormatting.RED + "Replace requires creative mode.");
            return false;
        }

        Vector3i min = ClientProxy.pointMin;
        Vector3i max = ClientProxy.pointMax;

        if (min.x == max.x && min.y == max.y && min.z == max.z) {
            sendChat(player, EnumChatFormatting.RED + "No area selected. Use Area Selection mode first.");
            return false;
        }

        World world = player.worldObj;
        Block targetBlock = world.getBlock(mop.blockX, mop.blockY, mop.blockZ);
        int targetMeta = world.getBlockMetadata(mop.blockX, mop.blockY, mop.blockZ);

        ItemStack held = player.getHeldItem();
        if (held == null) {
            sendChat(player, EnumChatFormatting.RED + "Hold a block to replace with.");
            return false;
        }

        Block replaceBlock = Block.getBlockFromItem(held.getItem());
        if (replaceBlock == Blocks.air) {
            sendChat(player, EnumChatFormatting.RED + "Held item is not a placeable block.");
            return false;
        }

        int replaceMeta = held.getItemDamage();
        int count = 0;

        for (int x = min.x; x <= max.x; x++) {
            for (int y = min.y; y <= max.y; y++) {
                for (int z = min.z; z <= max.z; z++) {
                    Block existing = world.getBlock(x, y, z);
                    int existingMeta = world.getBlockMetadata(x, y, z);
                    if (existing == targetBlock && existingMeta == targetMeta) {
                        world.setBlock(x, y, z, replaceBlock, replaceMeta, 2);
                        count++;
                    }
                }
            }
        }

        sendChat(player, "Replaced " + count + " blocks of " +
            targetBlock.getLocalizedName() + ":" + targetMeta +
            " with " + replaceBlock.getLocalizedName() + ":" + replaceMeta);
        return true;
    }

    // --- Utility ---

    private static void sendChat(EntityPlayer player, String message) {
        player.addChatMessage(new ChatComponentText(
            EnumChatFormatting.GREEN + "[Schematica] " + EnumChatFormatting.RESET + message));
    }
}
