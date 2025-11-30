package com.example.framework.utils;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.*;
import java.util.*;

public class ParameterResolver {

    public static Object[] resolve(Method method, HttpServletRequest req) throws Exception {
        Parameter[] params = method.getParameters();
        Object[] result = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            String name = params[i].getName();
            String[] values = req.getParameterValues(name);
            Class<?> type = params[i].getType();
            Type genericType = params[i].getParameterizedType();

            result[i] = convert(values, type, genericType);
        }

        return result;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Object convert(String[] values, Class<?> type, Type genericType) throws Exception {

        if (values == null)
            return null;

        if (type == String.class)
            return values[0];

        if (type == int.class || type == Integer.class) return Integer.valueOf(values[0]);
        if (type == double.class || type == Double.class) return Double.valueOf(values[0]);
        if (type == long.class || type == Long.class) return Long.valueOf(values[0]);
        if (type == float.class || type == Float.class) return Float.valueOf(values[0]);
        if (type == boolean.class || type == Boolean.class) return Boolean.valueOf(values[0]);

        if (type.isEnum()) {
            return Enum.valueOf((Class<Enum>) type, values[0]);
        }

        if (List.class.isAssignableFrom(type)) {
            return convertList(values, genericType);
        }

        if (type.isArray()) {
            return convertArray(values, type.getComponentType());
        }

        try {
            Constructor<?> c = type.getConstructor(String.class);
            return c.newInstance(values[0]);
        } catch (NoSuchMethodException ignored) {}

        try {
            Method valueOf = type.getMethod("valueOf", String.class);
            return valueOf.invoke(null, values[0]);
        } catch (NoSuchMethodException ignored) {}

        return null;
    }

    private static List<?> convertList(String[] values, Type genericType) throws Exception {
        List<Object> list = new ArrayList<>();

        if (genericType instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) genericType;
            Type innerType = pType.getActualTypeArguments()[0];
            Class<?> clazz = (Class<?>) innerType;

            for (String v : values) {
                list.add(convert(new String[]{v}, clazz, innerType));
            }
        }
        return list;
    }

    private static Object convertArray(String[] values, Class<?> componentType) throws Exception {
        Object array = Array.newInstance(componentType, values.length);

        for (int i = 0; i < values.length; i++) {
            Array.set(array, i, convert(new String[]{values[i]}, componentType, componentType));
        }

        return array;
    }
}
