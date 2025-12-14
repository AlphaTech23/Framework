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
        if (obj == null) return "null";

        if (obj instanceof Optional<?> opt) {
            return toJsonValue(opt.orElse(null));
        }

        if (obj instanceof String s) return toJsonString(s);
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();

        if (obj instanceof Map<?, ?> map) {
            return mapToJson(map);
        }

        if (obj instanceof Collection<?> col) {
            return collectionToJson(col);
        }

        if (obj.getClass().isArray()) {
            return arrayToJson(obj);
        }

        return objectToJson(obj);
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

    private static String collectionToJson(Collection<?> col) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"items\":[");
        boolean first = true;
        for (Object o : col) {
            if (!first) sb.append(",");
            sb.append(toJsonValue(o));
            first = false;
        }
        sb.append("],\"count\":").append(col.size()).append("}");
        return sb.toString();
    }

    private static String arrayToJson(Object array) {
        int length = Array.getLength(array);
        StringBuilder sb = new StringBuilder();
        sb.append("{\"items\":[");
        for (int i = 0; i < length; i++) {
            if (i > 0) sb.append(",");
            Object elem = Array.get(array, i);
            sb.append(toJsonValue(elem));
        }
        sb.append("],\"count\":").append(length).append("}");
        return sb.toString();
    }

    private static String mapToJson(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append(toJsonString(String.valueOf(e.getKey())));
            sb.append(":");
            sb.append(toJsonValue(e.getValue()));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private static String objectToJson(Object obj) {
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
                sb.append(toJsonString(name)).append(":").append(toJsonValue(value));
                first = false;
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
