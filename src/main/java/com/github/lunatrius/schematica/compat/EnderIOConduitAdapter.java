package com.github.lunatrius.schematica.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import com.github.lunatrius.schematica.api.ISchematicVisualAdapter;
import com.github.lunatrius.schematica.util.SchematicTransform;

final class EnderIOConduitAdapter implements ISchematicVisualAdapter {
    private static final String BASE = "crazypants.enderio.conduit.AbstractConduit";
    private static final String[] SIDE_KEYS = {"inFilts", "outFilts", "speedUpgrades", "functionUpgrades",
        "inputFilterUpgrades", "outputFilterUpgrades", "extRM", "extSC", "selfFeed", "roundRobin",
        "priority", "inSC", "outSC", "pRsMode", "pRsCol"};

    @Override public String id() { return "enderio:conduits"; }
    @Override public boolean supports(TileEntity tile) {
        return Reflect.is(tile, "crazypants.enderio.conduit.TileConduitBundle");
    }
    @Override public boolean replacesDescriptionPacket(TileEntity tile) { return false; }
    @Override public NBTTagCompound capture(TileEntity tile) { return null; }
    @Override public void restore(TileEntity tile, NBTTagCompound data) {}

    @Override public boolean transformsNBT(TileEntity tile) { return true; }

    @Override public void transformNBT(TileEntity tile, NBTTagCompound data, char operation) {
        transformBundle(data, operation);
    }

    static void transformBundle(NBTTagCompound data, char operation) {
        NBTTagList conduits = data.getTagList("conduits", 10);
        for (int i = 0; i < conduits.tagCount(); i++) transformConduitNBT(conduits.getCompoundTagAt(i).getCompoundTag("conduit"), operation);
    }

    static void transformConduitNBT(NBTTagCompound data, char operation) {
        for (String key : new String[] {"connections", "externalConnections"}) {
            if (!data.hasKey(key, 11)) continue;
            int[] sides = data.getIntArray(key).clone();
            for (int i = 0; i < sides.length; i++) {
                if (sides[i] >= 0 && sides[i] < 6) sides[i] = SchematicTransform.direction(operation,
                    ForgeDirection.getOrientation(sides[i])).ordinal();
            }
            data.setIntArray(key, sides);
        }
        for (String key : new String[] {"conModes", "forcedConnections", "signalColors", "signalStrengths"}) {
            if (!data.hasKey(key, 7) || data.getByteArray(key).length != 6) continue;
            byte[] values = data.getByteArray(key).clone();
            SchematicTransform.sides(operation, values);
            data.setByteArray(key, values);
        }
        for (String prefix : SIDE_KEYS) {
            NBTBase[] values = new NBTBase[6];
            for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
                String key = prefix + "." + side.name();
                values[side.ordinal()] = data.getTag(key);
                data.removeTag(key);
            }
            for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
                NBTBase value = values[side.ordinal()];
                if (value != null) data.setTag(prefix + "." + SchematicTransform.direction(operation, side).name(), value);
            }
        }
        if (data.hasKey("roundRobin", 3)) data.setInteger("roundRobin", SchematicTransform.sideMask(operation, data.getInteger("roundRobin")));
    }

    @Override public void transformPreview(TileEntity tile, char operation) throws Exception {
        for (Object conduit : (Iterable<?>) Reflect.call(tile, "getConduits")) transformConduit(conduit, operation);
        Reflect.field(tile.getClass(), "collidablesDirty").setBoolean(tile, true);
        Reflect.field(tile.getClass(), "connectorsDirty").setBoolean(tile, true);
    }

    static void transformConduit(Object conduit, char operation) throws ReflectiveOperationException {
        if (!Reflect.is(conduit, BASE)) return;
        transformSideCollections(conduit, Reflect.type(conduit.getClass(), BASE), operation);
        if (Reflect.is(conduit, "crazypants.enderio.conduit.liquid.AbstractEnderLiquidConduit")) {
            Field field = Reflect.field(conduit.getClass(), "roundRobin");
            field.setInt(conduit, SchematicTransform.sideMask(operation, field.getInt(conduit)));
        }
        Reflect.field(conduit.getClass(), "collidablesDirty").setBoolean(conduit, true);
    }

    @SuppressWarnings("unchecked")
    static void transformSideCollections(Object target, Class<?> base, char operation) throws IllegalAccessException {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || !(field.getGenericType() instanceof ParameterizedType)) continue;
                ParameterizedType generic = (ParameterizedType) field.getGenericType();
                if (generic.getActualTypeArguments()[0] != ForgeDirection.class) continue;
                field.setAccessible(true);
                Object value = field.get(target);
                if (value instanceof Map) {
                    Map<ForgeDirection, Object> map = (Map<ForgeDirection, Object>) value;
                    Map<ForgeDirection, Object> rotated = new EnumMap<>(ForgeDirection.class);
                    for (Map.Entry<ForgeDirection, Object> entry : map.entrySet()) rotated.put(SchematicTransform.direction(operation, entry.getKey()), entry.getValue());
                    map.clear();
                    map.putAll(rotated);
                } else if (value instanceof Set) {
                    Set<ForgeDirection> set = (Set<ForgeDirection>) value;
                    Set<ForgeDirection> rotated = EnumSet.noneOf(ForgeDirection.class);
                    for (ForgeDirection side : set) rotated.add(SchematicTransform.direction(operation, side));
                    set.clear();
                    set.addAll(rotated);
                }
            }
            if (type == base) break;
        }
    }
}
