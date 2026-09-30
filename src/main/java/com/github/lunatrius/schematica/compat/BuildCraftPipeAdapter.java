package com.github.lunatrius.schematica.compat;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import com.github.lunatrius.schematica.api.ISchematicVisualAdapter;

final class BuildCraftPipeAdapter implements ISchematicVisualAdapter {
    @Override public String id() { return "buildcraft:pipes"; }
    @Override public String protocol() { return StreamVisualAdapter.version("BuildCraft|Transport"); }
    @Override public boolean supports(TileEntity tile) {
        return Reflect.is(tile, "buildcraft.transport.TileGenericPipe");
    }

    @Override public NBTTagCompound capture(TileEntity tile) throws Exception {
        NBTTagCompound tag = new NBTTagCompound();
        for (byte id = 1; id <= 3; id++) {
            Object state = id == 3 ? Reflect.get(tile, "pipe")
                : Reflect.call(tile, "getStateInstance", new Class<?>[] { byte.class }, id);
            if (Reflect.is(state, "buildcraft.api.core.ISerializable")) {
                tag.setByteArray("State" + id, VisualStreams.writeBuffer(state, "writeData"));
            }
        }
        Object pipe = Reflect.get(tile, "pipe");
        if (pipe != null) {
            Object transport = Reflect.get(pipe, "transport");
            NBTTagCompound state = new NBTTagCompound();
            if (Reflect.is(transport, "buildcraft.transport.PipeTransportPower")) {
                VisualFields.capture(transport, state, "displayPower", "overload");
            } else if (Reflect.is(transport, "buildcraft.transport.PipeTransportFluids")) {
                Object cache = Reflect.get(transport, "renderCache");
                VisualFields.capture(cache, state, "amount", "color", "flags");
                Fluid fluid = FluidRegistry.getFluid((Integer) Reflect.get(cache, "fluidID"));
                if (fluid != null) state.setString("Fluid", fluid.getName());
            }
            tag.setTag("Transport", state);
        }
        return tag;
    }

    @Override public void restore(TileEntity tile, NBTTagCompound tag) throws Exception {
        for (byte id = 1; id <= 3; id++) {
            if (!tag.hasKey("State" + id, 7)) continue;
            Object state = Reflect.call(tile, "getStateInstance", new Class<?>[] { byte.class }, id);
            VisualStreams.readBuffer(state, "readData", tag.getByteArray("State" + id));
            Reflect.call(tile, "afterStateUpdated", new Class<?>[] { byte.class }, id);
        }
        Object pipe = Reflect.get(tile, "pipe");
        if (pipe != null && tag.hasKey("Transport", 10)) {
            Object transport = Reflect.get(pipe, "transport");
            NBTTagCompound state = tag.getCompoundTag("Transport");
            if (Reflect.is(transport, "buildcraft.transport.PipeTransportPower")) {
                VisualFields.restore(transport, state, "displayPower", "overload");
            } else if (Reflect.is(transport, "buildcraft.transport.PipeTransportFluids")) {
                Object cache = Reflect.get(transport, "renderCache");
                VisualFields.restore(cache, state, "amount", "color", "flags");
                Fluid fluid = FluidRegistry.getFluid(state.getString("Fluid"));
                Reflect.field(cache.getClass(), "fluidID").setInt(cache, fluid == null ? 0 : fluid.getID());
            }
        }
    }
}
