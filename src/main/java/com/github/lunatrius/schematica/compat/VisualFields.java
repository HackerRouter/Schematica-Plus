package com.github.lunatrius.schematica.compat;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.EnumSet;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;

final class VisualFields {
    private static final int MAX_ARRAY = 4096;

    private VisualFields() {}

    static String key(Field field) { return field.getDeclaringClass().getName() + "#" + field.getName(); }

    static void capture(Object source, Field field, NBTTagCompound data) throws ReflectiveOperationException {
        if (Modifier.isStatic(field.getModifiers())) return;
        field.setAccessible(true);
        NBTTagCompound entry = encode(field.getType(), field.get(source));
        if (entry != null) data.setTag(key(field), entry);
    }

    static void restore(Object target, Field field, NBTTagCompound data) throws ReflectiveOperationException {
        if (Modifier.isStatic(field.getModifiers()) || !data.hasKey(key(field), 10)) return;
        field.setAccessible(true);
        Class<?> type = field.getType();
        if (type == EnumSet.class && (!(field.getGenericType() instanceof ParameterizedType)
            || ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0] != ForgeDirection.class)) return;
        Object old = field.get(target);
        Object value = decode(type, data.getCompoundTag(key(field)));
        if (Modifier.isFinal(field.getModifiers())) {
            if (type.isArray() && old != null && value != null && Array.getLength(old) == Array.getLength(value)) {
                System.arraycopy(value, 0, old, 0, Array.getLength(old));
            }
        } else {
            field.set(target, value);
        }
    }

    static void capture(Object source, NBTTagCompound data, String... names) throws ReflectiveOperationException {
        for (String name : names) {
            try { capture(source, Reflect.field(source.getClass(), name), data); }
            catch (NoSuchFieldException ignored) {}
        }
    }

    static void restore(Object target, NBTTagCompound data, String... names) throws ReflectiveOperationException {
        for (String name : names) {
            try { restore(target, Reflect.field(target.getClass(), name), data); }
            catch (NoSuchFieldException ignored) {}
        }
    }

    private static boolean supported(Class<?> type) {
        return type.isPrimitive() || type == String.class || type.isEnum() || type == EnumSet.class
            || type == ItemStack.class || type == FluidStack.class || type == FluidTank.class
            || (type.isArray() && !type.getComponentType().isArray() && supported(type.getComponentType()));
    }

    private static NBTTagCompound encode(Class<?> type, Object value) {
        if (!supported(type)) return null;
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Type", type.getName());
        if (value == null) { tag.setBoolean("Null", true); return tag; }
        if (type == boolean.class) tag.setBoolean("Value", (Boolean) value);
        else if (type == byte.class) tag.setByte("Value", (Byte) value);
        else if (type == short.class) tag.setShort("Value", (Short) value);
        else if (type == int.class) tag.setInteger("Value", (Integer) value);
        else if (type == long.class) tag.setLong("Value", (Long) value);
        else if (type == float.class) tag.setFloat("Value", (Float) value);
        else if (type == double.class) tag.setDouble("Value", (Double) value);
        else if (type == char.class) tag.setInteger("Value", (Character) value);
        else if (type == String.class) tag.setString("Value", (String) value);
        else if (type.isEnum()) tag.setString("Value", ((Enum<?>) value).name());
        else if (type == EnumSet.class) {
            int mask = 0;
            for (Object direction : (EnumSet<?>) value) {
                if (!(direction instanceof ForgeDirection)) return null;
                mask |= 1 << ((ForgeDirection) direction).ordinal();
            }
            tag.setInteger("Value", mask);
        } else if (type == ItemStack.class) {
            ItemStack stack = (ItemStack) value;
            tag.setString("Item", Item.itemRegistry.getNameForObject(stack.getItem()));
            tag.setTag("Value", stack.writeToNBT(new NBTTagCompound()));
        } else if (type == FluidStack.class) tag.setTag("Value", ((FluidStack) value).writeToNBT(new NBTTagCompound()));
        else if (type == FluidTank.class) {
            tag.setInteger("Capacity", ((FluidTank) value).getCapacity());
            tag.setTag("Value", ((FluidTank) value).writeToNBT(new NBTTagCompound()));
        } else if (type.isArray()) {
            int length = Array.getLength(value);
            if (length > MAX_ARRAY) throw new IllegalArgumentException("Visual array too large");
            NBTTagList list = new NBTTagList();
            for (int i = 0; i < length; i++) list.appendTag(encode(type.getComponentType(), Array.get(value, i)));
            tag.setTag("Value", list);
        }
        return tag;
    }

    private static Object decode(Class<?> type, NBTTagCompound tag) {
        if (!supported(type) || !tag.getString("Type").equals(type.getName())) {
            throw new IllegalArgumentException("Mismatched visual field type");
        }
        if (tag.getBoolean("Null")) {
            if (type.isPrimitive()) throw new IllegalArgumentException("Null primitive visual field");
            return null;
        }
        int nbtType = type == boolean.class || type == byte.class ? 1 : type == short.class ? 2
            : type == int.class || type == char.class || type == EnumSet.class ? 3 : type == long.class ? 4
            : type == float.class ? 5 : type == double.class ? 6 : type == String.class || type.isEnum() ? 8
            : type.isArray() ? 9 : 10;
        if (!tag.hasKey("Value", nbtType)) throw new IllegalArgumentException("Missing visual field value");
        if (type == boolean.class) return tag.getBoolean("Value");
        if (type == byte.class) return tag.getByte("Value");
        if (type == short.class) return tag.getShort("Value");
        if (type == int.class) return tag.getInteger("Value");
        if (type == long.class) return tag.getLong("Value");
        if (type == float.class) return tag.getFloat("Value");
        if (type == double.class) return tag.getDouble("Value");
        if (type == char.class) return (char) tag.getInteger("Value");
        if (type == String.class) return tag.getString("Value");
        if (type.isEnum()) {
            for (Object constant : type.getEnumConstants()) {
                if (((Enum<?>) constant).name().equals(tag.getString("Value"))) return constant;
            }
            throw new IllegalArgumentException("Unknown visual enum value");
        }
        if (type == EnumSet.class) {
            EnumSet<ForgeDirection> result = EnumSet.noneOf(ForgeDirection.class);
            int mask = tag.getInteger("Value");
            if ((mask & ~127) != 0) throw new IllegalArgumentException("Invalid visual direction mask");
            for (ForgeDirection direction : ForgeDirection.values()) if ((mask & 1 << direction.ordinal()) != 0) result.add(direction);
            return result;
        }
        if (type == ItemStack.class) {
            Item item = (Item) Item.itemRegistry.getObject(tag.getString("Item"));
            if (item == null) return null;
            NBTTagCompound stack = (NBTTagCompound) tag.getCompoundTag("Value").copy();
            stack.setShort("id", (short) Item.getIdFromItem(item));
            return ItemStack.loadItemStackFromNBT(stack);
        }
        if (type == FluidStack.class) return FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("Value"));
        if (type == FluidTank.class) {
            if (tag.getInteger("Capacity") < 0) throw new IllegalArgumentException("Invalid visual tank capacity");
            return new FluidTank(tag.getInteger("Capacity")).readFromNBT(tag.getCompoundTag("Value"));
        }
        NBTTagList list = tag.getTagList("Value", 10);
        if (list.tagCount() > MAX_ARRAY) throw new IllegalArgumentException("Visual array too large");
        Object result = Array.newInstance(type.getComponentType(), list.tagCount());
        for (int i = 0; i < list.tagCount(); i++) Array.set(result, i, decode(type.getComponentType(), list.getCompoundTagAt(i)));
        return result;
    }
}
