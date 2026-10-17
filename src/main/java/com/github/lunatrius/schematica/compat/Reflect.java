package com.github.lunatrius.schematica.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class Reflect {
    private static final ClassValue<Map<String, Class<?>>> TYPES = new ClassValue<Map<String, Class<?>>>() {
        @Override protected Map<String, Class<?>> computeValue(Class<?> type) {
            Map<String, Class<?>> types = new HashMap<>();
            collect(type, types);
            return types;
        }
    };
    private static final ClassValue<Map<String, Field>> FIELDS = new ClassValue<Map<String, Field>>() {
        @Override protected Map<String, Field> computeValue(Class<?> type) { return new ConcurrentHashMap<>(); }
    };
    private static final ClassValue<Map<String, Method>> METHODS = new ClassValue<Map<String, Method>>() {
        @Override protected Map<String, Method> computeValue(Class<?> type) { return new ConcurrentHashMap<>(); }
    };

    private Reflect() {}

    private static void collect(Class<?> type, Map<String, Class<?>> types) {
        if (type == null || types.putIfAbsent(type.getName(), type) != null) return;
        for (Class<?> face : type.getInterfaces()) collect(face, types);
        collect(type.getSuperclass(), types);
    }

    static Class<?> type(Class<?> type, String name) {
        return type == null ? null : TYPES.get(type).get(name);
    }

    static boolean is(Object target, String name) { return target != null && type(target.getClass(), name) != null; }

    static Field field(Class<?> type, String name) throws NoSuchFieldException {
        Map<String, Field> fields = FIELDS.get(type);
        Field cached = fields.get(name);
        if (cached != null) return cached;
        for (Class<?> parent = type; parent != null; parent = parent.getSuperclass()) {
            try {
                Field field = parent.getDeclaredField(name);
                field.setAccessible(true);
                fields.put(name, field);
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
        Map<String, Method> methods = METHODS.get(target.getClass());
        String key = name + Arrays.toString(parameters);
        Method method = methods.get(key);
        if (method == null) {
            method = target.getClass().getMethod(name, parameters);
            method.setAccessible(true);
            methods.put(key, method);
        }
        return method.invoke(target, args);
    }

    static Object call(Object target, String name) throws ReflectiveOperationException {
        return call(target, name, new Class<?>[0]);
    }
}
