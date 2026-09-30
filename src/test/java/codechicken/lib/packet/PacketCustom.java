package codechicken.lib.packet;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import io.netty.buffer.ByteBuf;

public final class PacketCustom implements MCDataInput, MCDataOutput {
    public final ByteBuf buffer;
    public PacketCustom(ByteBuf buffer) {
        this.buffer = buffer;
        if (buffer.readUnsignedByte() != 1) throw new AssertionError("Unexpected local codec header");
    }
    @Override public int readInt() { return buffer.readInt(); }
    @Override public MCDataOutput writeInt(int value) { buffer.writeInt(value); return this; }
}
