package com.github.lunatrius.schematica.compat;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import com.github.lunatrius.schematica.api.ISchematicVisualAdapter;

final class NamedFieldsAdapter implements ISchematicVisualAdapter {
    private final String id, base;
    private final String[] fields;
    NamedFieldsAdapter(String id, String base, String... fields) { this.id = id; this.base = base; this.fields = fields; }
    @Override public String id() { return id; }
    @Override public boolean supports(TileEntity tile) { return Reflect.is(tile, base); }
    @Override public NBTTagCompound capture(TileEntity tile) throws Exception {
        NBTTagCompound tag = new NBTTagCompound();
        VisualFields.capture(tile, tag, fields);
        return tag;
    }
    @Override public void restore(TileEntity tile, NBTTagCompound tag) throws Exception { VisualFields.restore(tile, tag, fields); }
}
