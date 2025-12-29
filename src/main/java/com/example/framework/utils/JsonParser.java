package com.example.framework.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.*;

public class JsonParser {

    public static String success(Object data) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"status\":\"success\",");
        sb.append("\"data\":").append(toJsonValue(data)).append(",");
        sb.append("\"error\":null");
        sb.append("}");
        return sb.toString();
    }

    public static String error(String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"status\":\"error\",");
        sb.append("\"data\":null,");
        sb.append("\"error\":").append(toJsonString(message));
        sb.append("}");
        return sb.toString();
    }

    private static String toJsonValue(Object obj) {
        return toJsonValue(obj, 0);
    }

    private static String toJsonValue(Object obj, int depth) {
        if (obj == null) return "null";

        if (obj instanceof Optional<?> opt) {
            return toJsonValue(opt.orElse(null), depth);
        }

        if (obj instanceof String s) return toJsonString(s);
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();

        if (obj instanceof Map<?, ?> map) {
            return mapToJson(map, depth);
        }

        if (obj instanceof Collection<?> col) {
            return collectionToJson(col, depth);
        }

        if (obj.getClass().isArray()) {
            return arrayToJson(obj, depth);
        }

        return objectToJson(obj, depth);
    }

    private static String toJsonString(String s) {
        if (s == null) return "null";
        s = s.replace("\\", "\\\\")
             .replace("\"", "\\\"")
             .replace("\n", "\\n")
             .replace("\r", "\\r")
             .replace("\t", "\\t");
        return "\"" + s + "\"";
    }

    private static String collectionToJson(Collection<?> col, int depth) {
        StringBuilder sb = new StringBuilder();
        if (depth == 0) {
            sb.append("{\"items\":[");
        } else {
            sb.append("[");
        }

        boolean first = true;
        for (Object o : col) {
            if (!first) sb.append(",");
            sb.append(toJsonValue(o, depth + 1));
            first = false;
        }

        if (depth == 0) {
            sb.append("],\"count\":").append(col.size()).append("}");
        } else {
            sb.append("]");
        }

        return sb.toString();
    }

    private static String arrayToJson(Object array, int depth) {
        int length = Array.getLength(array);
        StringBuilder sb = new StringBuilder();
        if (depth == 0) {
            sb.append("{\"items\":[");
        } else {
            sb.append("[");
        }

        for (int i = 0; i < length; i++) {
            if (i > 0) sb.append(",");
            sb.append(toJsonValue(Array.get(array, i), depth + 1));
        }

        if (depth == 0) {
            sb.append("],\"count\":").append(length).append("}");
        } else {
            sb.append("]");
        }

        return sb.toString();
    }

    private static String mapToJson(Map<?, ?> map, int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append(toJsonString(String.valueOf(e.getKey())));
            sb.append(":");
            sb.append(toJsonValue(e.getValue(), depth + 1));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private static String objectToJson(Object obj, int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Method method : obj.getClass().getMethods()) {
            if (method.getParameterCount() == 0 &&
                method.getName().startsWith("get") &&
                !method.getName().equals("getClass")) {

                Object value;
                try {
                    value = method.invoke(obj);
                } catch (Exception e) {
                    value = null;
                }

                String name = method.getName().substring(3);
                name = Character.toLowerCase(name.charAt(0)) + name.substring(1);

                if (!first) sb.append(",");
                sb.append(toJsonString(name)).append(":").append(toJsonValue(value, depth + 1));
                first = false;
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
