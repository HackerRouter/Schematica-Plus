package com.github.lunatrius.schematica.compat;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.lunatrius.schematica.api.ISchematicVisualAdapter;
import com.github.lunatrius.schematica.util.SchematicTransform;

final class LogisticsPipesVisualAdapter implements ISchematicVisualAdapter {
    private static final String[] STATES = {"renderState", "bcPlugableState", "pipe"};
    @Override public String id() { return "logisticspipes:pipe_state"; }
    @Override public String protocol() { return VisualAdapters.environmentProtocol(); }
    @Override public boolean supports(TileEntity tile) { return Reflect.is(tile, "logisticspipes.pipes.basic.LogisticsTileGenericPipe"); }

    @Override public boolean transformsNBT(TileEntity tile) { return true; }
    @Override public void transformNBT(TileEntity tile, NBTTagCompound data, char operation) { transform(data, operation); }

    @Override public void transformPreview(TileEntity tile, char operation) {
        NBTTagCompound data = new NBTTagCompound();
        tile.writeToNBT(data);
        transform(data, operation);
        tile.readFromNBT(data);
    }

    private static int turn(char operation, int side) {
        return side >= 0 && side < 6 ? SchematicTransform.direction(operation, ForgeDirection.getOrientation(side)).ordinal() : side;
    }

    /**
     * The saved sides of a logistics pipe: the chassis "Orientation" (6: none), the "sneakydirection" of provider
     * and extractor modules at any depth, and the per-side turtle connections.
     */
    static void transform(NBTTagCompound tag, char operation) {
        if (tag.hasKey("Orientation", NBT.TAG_INT)) tag.setInteger("Orientation", turn(operation, tag.getInteger("Orientation")));
        boolean[] turtle = new boolean[6];
        boolean any = false;
        for (int side = 0; side < 6; side++) {
            any |= tag.hasKey("turtleConnect_" + side);
            turtle[side] = tag.getBoolean("turtleConnect_" + side);
        }
        if (any) for (int side = 0; side < 6; side++) tag.setBoolean("turtleConnect_" + turn(operation, side), turtle[side]);
        sneaky(tag, operation, 0);
    }

    private static void sneaky(NBTTagCompound tag, char operation, int depth) {
        if (depth > 32) return;
        if (tag.hasKey("sneakydirection", NBT.TAG_INT)) tag.setInteger("sneakydirection", turn(operation, tag.getInteger("sneakydirection")));
        for (Object key : tag.func_150296_c()) {
            NBTBase child = tag.getTag((String) key);
            if (child instanceof NBTTagCompound) sneaky((NBTTagCompound) child, operation, depth + 1);
            else if (child instanceof NBTTagList && ((NBTTagList) child).func_150303_d() == NBT.TAG_COMPOUND) {
                NBTTagList list = (NBTTagList) child;
                for (int i = 0; i < list.tagCount(); i++) sneaky(list.getCompoundTagAt(i), operation, depth + 1);
            }
        }
    }

    @Override public NBTTagCompound capture(TileEntity tile) throws Exception {
        NBTTagCompound tag = new NBTTagCompound();
        for (String name : STATES) {
            Object state = Reflect.get(tile, name);
            if (state != null) tag.setByteArray(name,
                VisualStreams.writeBuffer(state, "writeData", "logisticspipes.network.LPDataOutputStream"));
        }
        return tag;
    }

    @Override public void restore(TileEntity tile, NBTTagCompound tag) throws Exception {
        for (String name : STATES) {
            Object state = Reflect.get(tile, name);
            if (state != null && tag.hasKey(name, 7)) VisualStreams.readBuffer(state, "readData",
                "logisticspipes.network.LPDataInputStream", tag.getByteArray(name));
        }
        Reflect.call(tile, "afterStateUpdated");
    }
}
