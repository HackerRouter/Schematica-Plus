
package com.github.lunatrius.schematica.tool;

import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
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
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

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
     * PASTE/DELETE/FILL/REPLACE are NOT handled here — they use onExecute() via Enter key.
     * FILL/REPLACE right-click picks secondaryBlock instead.
     */
    public static boolean onToolUse(EntityPlayer player, MovingObjectPosition mop) {
        ToolMode mode = ToolManager.getCurrentMode();

        // Modes that pick secondaryBlock on right-click
        if (mode == ToolMode.REPLACE_BLOCK) {
            return pickBlockFromCrosshair(player, mop, false);
        }

        // Modes with no right-click action (Enter-key only)
        if (mode == ToolMode.PASTE_SCHEMATIC || mode == ToolMode.DELETE || mode == ToolMode.FILL) {
            return false;
        }

        // Modes that don't require a block target
        switch (mode) {
            case SCHEMATIC_PLACEMENT:
                // PLACEMENT: if a schematic is already selected, import a new one;
                // otherwise do precise placement (target block or move to player)
                return handlePlacementUse(player, mop);
            case MOVE:
                // MOVE: precise placement — move current schematic to targeted block or to player
                return handleMoveUse(player, mop);
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
            default:
                return false;
        }
    }

    /**
     * Called on left-click (attack action) with the tool active.
     * FILL/REPLACE left-click picks primaryBlock.
     */
    public static boolean onToolAttack(EntityPlayer player, MovingObjectPosition mop) {
        ToolMode mode = ToolManager.getCurrentMode();

        // Modes that pick primaryBlock on left-click
        if (mode == ToolMode.FILL || mode == ToolMode.REPLACE_BLOCK) {
            return pickBlockFromCrosshair(player, mop, true);
        }

        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return false;
        }

        if (mode == ToolMode.AREA_SELECTION) {
            return handleAreaSelectionAttack(player, mop);
        }

        return false;
    }

    /**
     * Called from the Execute keybinding (default: Enter).
     * Dispatches to the appropriate handler based on current tool mode.
     * Only works when holding the tool item.
     */
    public static void onExecute(EntityPlayer player) {
        if (player == null) {
            return;
        }

        ToolMode mode = ToolManager.getCurrentMode();

        switch (mode) {
            case PASTE_SCHEMATIC:
                SchematicWorld schematic = ClientProxy.schematic;
                if (schematic != null) {
                    handlePasteUse(player);
                } else {
                    sendChat(player, EnumChatFormatting.RED + "No schematic loaded.");
                }
                break;
            case DELETE:
                handleDeleteExecute(player);
                break;
            case FILL:
                handleFillExecute(player);
                break;
            case REPLACE_BLOCK:
                handleReplaceExecute(player);
                break;
            default:
                break;
        }
    }

    // --- Block Picking (Litematica-style) ---

    /**
     * Picks the block at the crosshair and stores it as primaryBlock or secondaryBlock
     * on the current ToolMode. Works with both real world and schematic world blocks.
     *
     * @param primary true = set primaryBlock, false = set secondaryBlock
     */
    public static boolean pickBlockFromCrosshair(EntityPlayer player, MovingObjectPosition mop, boolean primary) {
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            sendChat(player, EnumChatFormatting.RED + "No block targeted.");
            return false;
        }

        ToolMode mode = ToolManager.getCurrentMode();
        World world = player.worldObj;
        Block block = world.getBlock(mop.blockX, mop.blockY, mop.blockZ);
        int meta = world.getBlockMetadata(mop.blockX, mop.blockY, mop.blockZ);

        if (block == null || block == Blocks.air) {
            sendChat(player, EnumChatFormatting.RED + "Cannot pick air.");
            return false;
        }

        String blockName = GameData.getBlockRegistry().getNameForObject(block);

        if (primary) {
            mode.setPrimaryBlock(block, meta);
        } else {
            mode.setSecondaryBlock(block, meta);
        }

        return true;
    }

    // --- AREA_SELECTION ---

    /**
     * Right-click sets point B (second corner of selection box).
     */
    private static boolean handleAreaSelectionUse(EntityPlayer player, MovingObjectPosition mop) {
        ClientProxy.pointB.set(mop.blockX, mop.blockY, mop.blockZ);
        ClientProxy.updatePoints();
        ClientProxy.isRenderingGuide = true;
        return true;
    }

    /**
     * Left-click sets point A (first corner of selection box).
     */
    private static boolean handleAreaSelectionAttack(EntityPlayer player, MovingObjectPosition mop) {
        ClientProxy.pointA.set(mop.blockX, mop.blockY, mop.blockZ);
        ClientProxy.updatePoints();
        ClientProxy.isRenderingGuide = true;
        return true;
    }

    // --- SCHEMATIC_PLACEMENT ---

    /**
     * Right-click in placement mode:
     * - If a schematic is already selected/loaded, open load GUI to import a new one.
     * - If no schematic is loaded, do precise placement (target block or move to player).
     */
    private static boolean handlePlacementUse(EntityPlayer player, MovingObjectPosition mop) {
        SchematicWorld schematic = ClientProxy.schematic;
        if (schematic != null) {
            // Already have a selected instance — import a new schematic
            Minecraft.getMinecraft().displayGuiScreen(new GuiSchematicLoad(Minecraft.getMinecraft().currentScreen));
            return true;
        }
        // No schematic loaded — open load GUI to load the first one
        Minecraft.getMinecraft().displayGuiScreen(new GuiSchematicLoad(Minecraft.getMinecraft().currentScreen));
        return true;
    }

    // --- MOVE ---

    /**
     * Right-click in move mode: if targeting a block, place schematic origin on top of it;
     * otherwise move schematic to player position.
     */
    private static boolean handleMoveUse(EntityPlayer player, MovingObjectPosition mop) {
        SchematicWorld schematic = ClientProxy.schematic;
        if (schematic == null) {
            sendChat(player, EnumChatFormatting.RED + "No schematic loaded.");
            return false;
        }

        if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            int placeY = mop.blockY + 1;
            schematic.position.set(mop.blockX, placeY, mop.blockZ);
        } else {
            ClientProxy.moveSchematicToPlayer(schematic);
        }
        RendererSchematicGlobal.INSTANCE.refresh();
        return true;
    }

    // --- PASTE_SCHEMATIC ---

    private static final FMLControlledNamespacedRegistry<Block> BLOCK_REGISTRY = GameData.getBlockRegistry();

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

        WorldServer serverWorld = getServerWorld(player);
        int count;
        if (serverWorld != null) {
            int[] result = pasteDirectly(serverWorld, schematic);
            count = result[0];
            int entityCount = result[1];
            String msg = "Pasted " + count + " blocks";
            if (!schematic.isPastingBlockNBT) {
                msg += " (no NBT)";
            }
            msg += " and " + entityCount + " entities directly.";
            sendChat(player, msg);
        } else {
            int[] result = pasteWithSetblock(player, schematic);
            count = result[0];
            int entityCount = result[1];
            sendChat(player, "Pasted " + count + " blocks and " + entityCount + " entities via commands.");
        }
        return true;
    }

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
     * @return int[2]: [0] = block count, [1] = entity count
     */
    private static int[] pasteDirectly(WorldServer serverWorld, SchematicWorld schematic) {
        int count = 0;
        int entityCount = 0;
        Vector3i pos = schematic.position;

        // First pass: place all blocks using raw ExtendedBlockStorage operations
        // to completely bypass onBlockAdded/onNeighborBlockChanged callbacks.
        // This prevents doors and other fragile multi-block structures from
        // breaking during paste due to incomplete neighbor checks.
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

                    // Direct ExtendedBlockStorage manipulation — no callbacks at all
                    Chunk chunk = serverWorld.getChunkFromBlockCoords(wx, wz);
                    int cy = wy >> 4; // section index
                    ExtendedBlockStorage[] storageArrays = chunk.getBlockStorageArray();
                    ExtendedBlockStorage ebs = storageArrays[cy];
                    if (ebs == null) {
                        ebs = new ExtendedBlockStorage(cy << 4, !serverWorld.provider.hasNoSky);
                        storageArrays[cy] = ebs;
                    }
                    int lx = wx & 15;
                    int ly = wy & 15;
                    int lz = wz & 15;
                    ebs.func_150818_a(lx, ly, lz, block);
                    ebs.setExtBlockMetadata(lx, ly, lz, metadata);
                    chunk.isModified = true;
                    count++;

                    // Only paste tile entity NBT if enabled
                    if (schematic.isPastingBlockNBT) {
                        TileEntity te = schematic.getTileEntity(x, y, z);
                        if (te != null) {
                            try {
                                NBTTagCompound nbt = new NBTTagCompound();
                                te.writeToNBT(nbt);
                                nbt.setInteger("x", wx);
                                nbt.setInteger("y", wy);
                                nbt.setInteger("z", wz);
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
        }

        // Only paste entities if enabled
        if (schematic.isRenderingEntities) {
            for (Entity entity : schematic.getEntities()) {
                try {
                    NBTTagCompound nbt = new NBTTagCompound();
                    entity.writeToNBTOptional(nbt);

                    NBTTagList posList = new NBTTagList();
                    posList.appendTag(new NBTTagDouble(entity.posX + pos.x));
                    posList.appendTag(new NBTTagDouble(entity.posY + pos.y));
                    posList.appendTag(new NBTTagDouble(entity.posZ + pos.z));
                    nbt.setTag("Pos", posList);

                    nbt.removeTag("UUIDMost");
                    nbt.removeTag("UUIDLeast");

                    Entity newEntity = EntityList.createEntityFromNBT(nbt, serverWorld);
                    if (newEntity != null) {
                        serverWorld.spawnEntityInWorld(newEntity);
                        entityCount++;
                    }
                } catch (Exception e) {
                    Reference.logger.debug("Failed to paste entity at schematic offset", e);
                }
            }
        }

        // Second pass: notify clients for rendering updates only.
        // markBlockForUpdate sends block change packets without triggering game logic.
        for (int y = 0; y < schematic.getHeight(); y++) {
            for (int x = 0; x < schematic.getWidth(); x++) {
                for (int z = 0; z < schematic.getLength(); z++) {
                    Block block = schematic.getBlock(x, y, z);
                    if (block == Blocks.air || block == null) {
                        continue;
                    }
                    int wx = pos.x + x;
                    int wy = pos.y + y;
                    int wz = pos.z + z;

                    serverWorld.markBlockForUpdate(wx, wy, wz);
                }
            }
        }

        return new int[] { count, entityCount };
    }

    /**
     * @return int[2]: [0] = block count, [1] = entity count
     */
    private static int[] pasteWithSetblock(EntityPlayer player, SchematicWorld schematic) {
        EntityClientPlayerMP clientPlayer = Minecraft.getMinecraft().thePlayer;
        if (clientPlayer == null) {
            return new int[] { 0, 0 };
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

        int entityCount = 0;
        // Only paste entities if enabled
        if (schematic.isRenderingEntities) {
            for (Entity entity : schematic.getEntities()) {
                try {
                    String entityName = EntityList.getEntityString(entity);
                    if (entityName == null) continue;
                    double wx = entity.posX + pos.x;
                    double wy = entity.posY + pos.y;
                    double wz = entity.posZ + pos.z;
                    String cmd = "/summon " + entityName + " " + wx + " " + wy + " " + wz;
                    clientPlayer.sendChatMessage(cmd);
                    entityCount++;
                } catch (Exception e) {
                    Reference.logger.debug("Failed to summon entity via command", e);
                }
            }
        }

        return new int[] { count, entityCount };
    }

    // --- DELETE (Enter-key only) ---

    private static boolean handleDeleteExecute(EntityPlayer player) {
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

    // --- FILL (Enter-key only, uses picked primaryBlock) ---

    private static boolean handleFillExecute(EntityPlayer player) {
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

        ToolMode mode = ToolMode.FILL;
        Block block = mode.getPrimaryBlock();
        if (block == null || block == Blocks.air) {
            sendChat(player, EnumChatFormatting.RED + "No primary block set. Left-click a block to pick it.");
            return false;
        }

        int meta = mode.getPrimaryMeta();
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

        String blockName = GameData.getBlockRegistry().getNameForObject(block);
        sendChat(player, "Filled " + count + " blocks with " + blockName + ":" + meta);
        return true;
    }

    // --- REPLACE_BLOCK (Enter-key only, uses picked primaryBlock + secondaryBlock) ---

    private static boolean handleReplaceExecute(EntityPlayer player) {
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

        ToolMode mode = ToolMode.REPLACE_BLOCK;
        Block replaceBlock = mode.getPrimaryBlock();
        if (replaceBlock == null || replaceBlock == Blocks.air) {
            sendChat(player, EnumChatFormatting.RED + "No primary block set. Left-click a block to pick the replacement.");
            return false;
        }

        Block targetBlock = mode.getSecondaryBlock();
        if (targetBlock == null) {
            sendChat(player, EnumChatFormatting.RED + "No secondary block set. Right-click a block to pick the target.");
            return false;
        }

        int replaceMeta = mode.getPrimaryMeta();
        int targetMeta = mode.getSecondaryMeta();
        World world = player.worldObj;
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

        String targetName = GameData.getBlockRegistry().getNameForObject(targetBlock);
        String replaceName = GameData.getBlockRegistry().getNameForObject(replaceBlock);
        sendChat(player, "Replaced " + count + " blocks of " +
            targetName + ":" + targetMeta +
            " with " + replaceName + ":" + replaceMeta);
        return true;
    }

    // --- Utility ---

    private static void sendChat(EntityPlayer player, String message) {
        player.addChatMessage(new ChatComponentText(
            EnumChatFormatting.GREEN + "[Schematica] " + EnumChatFormatting.RESET + message));
    }
}
