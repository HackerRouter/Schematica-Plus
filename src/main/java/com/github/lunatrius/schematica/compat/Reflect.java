package com.github.lunatrius.schematica.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class Reflect {
    private Reflect() {}

    static Class<?> type(Class<?> type, String name) {
        if (type == null) return null;
        if (type.getName().equals(name)) return type;
        for (Class<?> face : type.getInterfaces()) {
            Class<?> match = type(face, name);
            if (match != null) return match;
        }
        return type(type.getSuperclass(), name);
    }

    static boolean is(Object target, String name) { return target != null && type(target.getClass(), name) != null; }

    static Field field(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> parent = type; parent != null; parent = parent.getSuperclass()) {
            try {
                Field field = parent.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
    }

    static Object get(Object target, String name) throws ReflectiveOperationException {
        return field(target.getClass(), name).get(target);
    }

    static Object call(Object target, String name, Class<?>[] parameters, Object... args)
        throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(name, parameters);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    static Object call(Object target, String name) throws ReflectiveOperationException {
        return call(target, name, new Class<?>[0]);
    }
}
