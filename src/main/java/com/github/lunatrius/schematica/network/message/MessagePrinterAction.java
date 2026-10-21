// SPDX-License-Identifier: LGPL-3.0-only
package com.github.lunatrius.schematica.network.message;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.block.Block;

import com.github.lunatrius.schematica.handler.PrinterActions;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** A printer click uses the server's held item, never client-supplied item data. */
public final class MessagePrinterAction implements IMessage, IMessageHandler<MessagePrinterAction, IMessage> {
    public static final int PLACE = 3;
    public static final int BREAK = 4;
    public int action, x, y, z, side, slot;
    public float hitX, hitY, hitZ, yaw, pitch;
    public boolean sneaking;
    public ItemStack expectedItem;
    public int targetX, targetY, targetZ, clickedBlock, clickedMeta, targetBlock, targetMeta;
    public int minimumDurability;
    public boolean completeSlab;
    public final List<MessagePlacementIntent> intents = new ArrayList<>();

    public MessagePrinterAction() {}

    public boolean valid() {
        return action >= 0 && action <= BREAK && slot >= 0 && slot < 9 && side >= (action == 1 ? -1 : 0) && side <= 5
            && y >= 0 && y <= 255 && Math.abs((long) x) < 30_000_000 && Math.abs((long) z) < 30_000_000
            && Float.isFinite(yaw) && Float.isFinite(pitch) && pitch >= -90 && pitch <= 90
            && fraction(hitX) && fraction(hitY) && fraction(hitZ)
            && targetY >= 0 && targetY <= 255
            && Math.abs((long) targetX - x) + Math.abs((long) targetY - y) + Math.abs((long) targetZ - z) <= 1
            && clickedBlock >= 0 && targetBlock >= 0 && clickedMeta >= 0 && clickedMeta < 16 && targetMeta >= 0 && targetMeta < 16
            && minimumDurability >= 0 && minimumDurability <= 1000 && intents.size() <= 2
            && (!completeSlab || action == PLACE)
            && intents.stream().allMatch(i -> i.y >= 0 && i.y <= 255 && i.metadata >= 0 && i.metadata <= 15
                && Math.abs((long) i.x - targetX) + Math.abs((long) i.y - targetY) + Math.abs((long) i.z - targetZ) <= 1);
    }

    private static boolean fraction(float value) { return Float.isFinite(value) && value >= 0 && value <= 1; }

    public void snapshot(World world, int tx, int ty, int tz) {
        targetX = tx; targetY = ty; targetZ = tz;
        clickedBlock = Block.getIdFromBlock(world.getBlock(x, y, z));
        clickedMeta = world.getBlockMetadata(x, y, z);
        targetBlock = Block.getIdFromBlock(world.getBlock(tx, ty, tz));
        targetMeta = world.getBlockMetadata(tx, ty, tz);
    }

    public boolean matches(World world) {
        return clickedBlock == Block.getIdFromBlock(world.getBlock(x, y, z)) && clickedMeta == world.getBlockMetadata(x, y, z)
            && targetBlock == Block.getIdFromBlock(world.getBlock(targetX, targetY, targetZ))
            && targetMeta == world.getBlockMetadata(targetX, targetY, targetZ);
    }

    @Override public void fromBytes(ByteBuf buffer) {
        action = buffer.readUnsignedByte(); x = buffer.readInt(); y = buffer.readInt(); z = buffer.readInt();
        side = buffer.readByte(); slot = buffer.readUnsignedByte();
        hitX = buffer.readFloat(); hitY = buffer.readFloat(); hitZ = buffer.readFloat();
        yaw = buffer.readFloat(); pitch = buffer.readFloat(); sneaking = buffer.readBoolean();
        expectedItem = ByteBufUtils.readItemStack(buffer);
        targetX = buffer.readInt(); targetY = buffer.readInt(); targetZ = buffer.readInt();
        clickedBlock = buffer.readInt(); clickedMeta = buffer.readUnsignedByte();
        targetBlock = buffer.readInt(); targetMeta = buffer.readUnsignedByte();
        minimumDurability = buffer.readUnsignedShort(); completeSlab = buffer.readBoolean();
        int count = buffer.readUnsignedByte();
        if (count > 2) throw new IllegalArgumentException("Too many placement intents");
        intents.clear();
        for (int i = 0; i < count; i++) {
            MessagePlacementIntent intent = new MessagePlacementIntent();
            intent.fromBytes(buffer.readSlice(buffer.readUnsignedShort()));
            intents.add(intent);
        }
        if (buffer.isReadable() || !valid()) throw new IllegalArgumentException("Invalid printer action");
    }

    @Override public void toBytes(ByteBuf buffer) {
        buffer.writeByte(action); buffer.writeInt(x); buffer.writeInt(y); buffer.writeInt(z);
        buffer.writeByte(side); buffer.writeByte(slot);
        buffer.writeFloat(hitX); buffer.writeFloat(hitY); buffer.writeFloat(hitZ);
        buffer.writeFloat(yaw); buffer.writeFloat(pitch); buffer.writeBoolean(sneaking);
        ByteBufUtils.writeItemStack(buffer, expectedItem);
        buffer.writeInt(targetX); buffer.writeInt(targetY); buffer.writeInt(targetZ);
        buffer.writeInt(clickedBlock); buffer.writeByte(clickedMeta); buffer.writeInt(targetBlock); buffer.writeByte(targetMeta);
        buffer.writeShort(minimumDurability); buffer.writeBoolean(completeSlab);
        buffer.writeByte(intents.size());
        for (MessagePlacementIntent intent : intents) {
            int index = buffer.writerIndex();
            buffer.writeShort(0);
            intent.toBytes(buffer);
            buffer.setShort(index, buffer.writerIndex() - index - 2);
        }
    }

    @Override public IMessage onMessage(MessagePrinterAction message, MessageContext context) {
        PrinterActions.INSTANCE.enqueue(context.getServerHandler().playerEntity, message);
        return null;
    }
}
