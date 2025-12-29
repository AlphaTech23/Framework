package com.example.framework.utils;

import jakarta.servlet.http.HttpServletRequest;
import com.example.framework.annotations.PathVariable;
import com.example.framework.annotations.RequestParam;
import com.example.framework.core.MultipartFile;

import java.lang.reflect.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;

public class ParameterResolver {

    private static final String[] DATE_PATTERNS = {
            "yyyy-MM-dd", "yyyy-MM-dd HH:mm", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "dd-MM-yyyy", "dd-MM-yyyy HH:mm", "dd-MM-yyyy HH:mm:ss", "dd/MM/yyyy", "dd/MM/yyyy HH:mm",
            "dd/MM/yyyy HH:mm:ss", "yyyy/MM/dd", "yyyy/MM/dd HH:mm", "yyyy/MM/dd HH:mm:ss"
    };

    public static void printTree(Map<String, Object> root) {
        printTree(root, 0);
    }

    private static void printTree(Object obj, int indent) {
        String prefix = " ".repeat(indent * 2);
        if (obj instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> e : map.entrySet()) {
                System.out.println(prefix + e.getKey() + ":");
                printTree(e.getValue(), indent + 1);
            }
        } else if (obj instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                System.out.println(prefix + "[" + i + "]:");
                printTree(list.get(i), indent + 1);
            }
        } else {
            System.out.println(prefix + obj);
        }
    }

    public static Object[] resolve(Method method, HttpServletRequest req, Map<String, String> pathVars)
            throws Exception {
        Parameter[] params = method.getParameters();
        Object[] result = new Object[params.length];
        Map<String, Object> tree = buildTree(req.getParameterMap());
        Map<String, List<MultipartFile>> uploads = MultipartFileResolver.extractFiles(req);

        for (int i = 0; i < params.length; i++) {
            Class<?> type = params[i].getType();
            Type genericType = params[i].getParameterizedType();

            if (isMultipartMap(type, genericType)) {
                result[i] = uploads;
                continue;
            }

            if (isMapStringObject(type, genericType)) {
                result[i] = req.getParameterMap();
                continue;
            }

            String name = params[i].getName();
            if (params[i].isAnnotationPresent(PathVariable.class)) {
                name = params[i].getAnnotation(PathVariable.class).value();
            } else if (params[i].isAnnotationPresent(RequestParam.class)) {
                name = params[i].getAnnotation(RequestParam.class).value();
            }

            if (isUploadedFileList(type, genericType)) {
                result[i] = uploads != null
                        ? uploads.getOrDefault(name, List.of())
                        : List.of();
                continue;
            }

            if (type == MultipartFile.class) {
                List<MultipartFile> list = uploads != null ? uploads.get(name) : null;
                result[i] = (list != null && !list.isEmpty()) ? list.get(0) : null;
                continue;
            }

            Object value = (pathVars != null && pathVars.containsKey(name))
                    ? pathVars.get(name)
                    : tree.get(name);

            result[i] = convertValue(value, type, genericType);
        }

        return result;
    }

    private static Map<String, Object> buildTree(Map<String, String[]> params) {
        params = KeyNormalizer.normalize(params);
        Map<String, Object> root = new HashMap<>();
        Map<String, List<String>> simpleLists = new LinkedHashMap<>();

        for (Map.Entry<String, String[]> e : params.entrySet()) {
            String key = e.getKey();
            String[] values = e.getValue();
            insertPath(root, key.split("\\."), 0, values);
        }

        for (Map.Entry<String, List<String>> e : simpleLists.entrySet()) {
            insertPath(root, e.getKey().split("\\."), 0, e.getValue().toArray(new String[0]));
        }
        return root;
    }

    @SuppressWarnings("unchecked")
    private static void insertPath(Map<String, Object> parent, String[] parts, int idx, String[] values) {
        String segment = parts[idx];
        int firstBracket = segment.indexOf('[');
        String name = (firstBracket == -1) ? segment : segment.substring(0, firstBracket);

        List<Integer> indexes = new ArrayList<>();
        int pos = firstBracket;
        while (pos != -1 && pos < segment.length()) {
            int end = segment.indexOf(']', pos);
            if (end == -1)
                break;
            String idxStr = segment.substring(pos + 1, end);
            indexes.add(Integer.parseInt(idxStr));
            pos = segment.indexOf('[', end);
        }

        Object current = parent.get(name);

        if (indexes.isEmpty()) {
            if (idx == parts.length - 1) {
                parent.put(name, values.length == 1 ? values[0] : Arrays.asList(values));
                return;
            }

            if (!(current instanceof Map)) {
                current = new HashMap<String, Object>();
                parent.put(name, current);
            }

            insertPath((Map<String, Object>) current, parts, idx + 1, values);
            return;
        }

        List<Object> list;
        if (!(current instanceof List)) {
            list = new ArrayList<>();
            parent.put(name, list);
        } else {
            list = (List<Object>) current;
        }

        Object ref = list;
        for (int level = 0; level < indexes.size(); level++) {
            int arrayIndex = indexes.get(level);
            if (arrayIndex == -1) {
                arrayIndex = Math.max(((List<?>) ref).size() - 1, 0);
            }
            while (((List<?>) ref).size() <= arrayIndex) {
                ((List<Object>) ref).add(null);
            }

            Object next = ((List<?>) ref).get(arrayIndex);
            boolean lastIndex = (level == indexes.size() - 1);

            if (lastIndex) {
                if (idx == parts.length - 1) {
                    ((List<Object>) ref).set(arrayIndex, values.length == 1 ? values[0] : Arrays.asList(values));
                    return;
                }

                if (!(next instanceof Map)) {
                    next = new HashMap<String, Object>();
                    ((List<Object>) ref).set(arrayIndex, next);
                }

                insertPath((Map<String, Object>) next, parts, idx + 1, values);
                return;
            } else {
                if (!(next instanceof List)) {
                    next = new ArrayList<>();
                    ((List<Object>) ref).set(arrayIndex, next);
                }
            }

            ref = next;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Object convertValue(Object raw, Class<?> type, Type genericType) throws Exception {
        if (raw == null) {
            if (type == boolean.class)
                return false;
            if (Optional.class.isAssignableFrom(type))
                return Optional.empty();
            return null;
        }

        if (Optional.class.isAssignableFrom(type)) {
            if (!(genericType instanceof ParameterizedType pt))
                return Optional.empty();
            Type actualType = pt.getActualTypeArguments()[0];
            Object value = convertValue(raw, (Class<?>) actualType, actualType);
            return Optional.ofNullable(value);
        }

        if (type == boolean.class || type == Boolean.class) {
            if (raw instanceof Boolean)
                return raw;
            String str = raw.toString().toLowerCase();
            return str.equals("true") || str.equals("1") || str.equals("yes");
        }

        if (type == String.class)
            return raw.toString();
        if (isPrimitiveOrWrapper(type))
            return convertPrimitive(raw.toString(), type);
        if (type.isEnum())
            return Enum.valueOf((Class<Enum>) type, raw.toString());

        if (type == LocalDate.class)
            return LocalDate.parse(raw.toString());
        if (type == LocalDateTime.class)
            return LocalDateTime.parse(raw.toString());
        if (type == LocalTime.class)
            return LocalTime.parse(raw.toString());

        if (type == Date.class) {
            for (String p : DATE_PATTERNS) {
                try {
                    return new SimpleDateFormat(p).parse(raw.toString());
                } catch (ParseException ignored) {
                }
            }
            try {
                return Date.from(Instant.parse(raw.toString()));
            } catch (Exception ignored) {
            }
        }

        if (Map.class.isAssignableFrom(type))
            return raw;
        if (Collection.class.isAssignableFrom(type))
            return convertCollection(raw, type, genericType);
        if (type.isArray()) {
            return type.getComponentType().isArray()
                    ? convertMultiDimArray(raw, type)
                    : convertArray(raw, type.getComponentType());
        }
        if (raw instanceof Map)
            return convertObject((Map<String, Object>) raw, type);

        return raw;
    }

    private static Object convertObject(Map<String, Object> map, Class<?> type) throws Exception {
        Object instance = type.getConstructor().newInstance();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String field = e.getKey();
            Object rawVal = e.getValue();
            String method = "set" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
            for (Method m : type.getMethods()) {
                if (m.getName().equals(method) && m.getParameterCount() == 1) {
                    Class<?> pType = m.getParameterTypes()[0];
                    Type gType = m.getGenericParameterTypes()[0];
                    Object converted = convertValue(rawVal, pType, gType);
                    m.invoke(instance, converted);
                    break;
                }
            }
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    private static Object convertCollection(Object raw, Class<?> collType, Type gType) throws Exception {
        if (!(raw instanceof List<?>))
            return null;

        if (!(gType instanceof ParameterizedType pt)) {
            return new ArrayList<>((List<Object>) raw);
        }

        Type elemT = pt.getActualTypeArguments()[0];

        if (elemT instanceof ParameterizedType nested && List.class.isAssignableFrom((Class<?>) nested.getRawType())) {
            return convertMultiDimList(raw, collType, pt);
        }

        List<Object> rawList = (List<Object>) raw;
        Collection<Object> list = Set.class.isAssignableFrom(collType) ? new LinkedHashSet<>() : new ArrayList<>();
        Class<?> elemClass = (Class<?>) elemT;

        for (Object item : rawList) {
            if (item instanceof Map && !elemClass.isAssignableFrom(Map.class)) {
                list.add(convertObject((Map<String, Object>) item, elemClass));
            } else {
                list.add(convertValue(item, elemClass, elemT));
            }
        }

        return list;
    }

    private static Object convertArray(Object raw, Class<?> comp) throws Exception {
        if (!(raw instanceof List))
            return null;
        List<?> list = (List<?>) raw;
        Object array = Array.newInstance(comp, list.size());
        for (int i = 0; i < list.size(); i++) {
            Array.set(array, i, convertValue(list.get(i), comp, comp));
        }
        return array;
    }

    private static boolean isMapStringObject(Class<?> type, Type genericType) {
        if (!Map.class.isAssignableFrom(type))
            return false;
        if (!(genericType instanceof ParameterizedType))
            return false;

        ParameterizedType pt = (ParameterizedType) genericType;
        return pt.getActualTypeArguments()[0] == String.class
                && pt.getActualTypeArguments()[1] == Object.class;
    }

    private static boolean isMultipartMap(Class<?> type, Type gType) {
        if (!Map.class.isAssignableFrom(type))
            return false;
        if (!(gType instanceof ParameterizedType pt))
            return false;

        return pt.getActualTypeArguments()[0] == String.class
                && pt.getActualTypeArguments()[1] instanceof ParameterizedType p2
                && p2.getRawType() == List.class
                && p2.getActualTypeArguments()[0] == MultipartFile.class;
    }

    private static boolean isUploadedFileList(Class<?> type, Type gType) {
        if (!List.class.isAssignableFrom(type))
            return false;
        if (!(gType instanceof ParameterizedType pt))
            return false;
        return pt.getActualTypeArguments()[0] == MultipartFile.class;
    }

    private static boolean isPrimitiveOrWrapper(Class<?> t) {
        return t.isPrimitive() || t == Integer.class || t == Long.class || t == Double.class
                || t == Float.class || t == Boolean.class || t == Byte.class
                || t == Short.class || t == Character.class;
    }

    private static Object convertPrimitive(String val, Class<?> t) {
        if (t == int.class || t == Integer.class)
            return Integer.valueOf(val);
        if (t == long.class || t == Long.class)
            return Long.valueOf(val);
        if (t == double.class || t == Double.class)
            return Double.valueOf(val);
        if (t == float.class || t == Float.class)
            return Float.valueOf(val);
        if (t == boolean.class || t == Boolean.class) {
            val = val.toLowerCase();
            return val.equalsIgnoreCase("true") || val.equalsIgnoreCase("on") || val.equalsIgnoreCase("1")
                    || val.equalsIgnoreCase("yes");
        }
        if (t == byte.class || t == Byte.class)
            return Byte.valueOf(val);
        if (t == short.class || t == Short.class)
            return Short.valueOf(val);
        if (t == char.class || t == Character.class)
            return val.charAt(0);
        return null;
    }

    private static Object convertMultiDimArray(Object raw, Class<?> arrayType) throws Exception {
        if (!(raw instanceof List<?> rawList))
            return null;

        Class<?> componentType = arrayType.getComponentType();
        Object array = Array.newInstance(componentType, rawList.size());

        for (int i = 0; i < rawList.size(); i++) {
            Object item = rawList.get(i);
            if (componentType.isArray()) {
                Array.set(array, i, convertMultiDimArray(item, componentType));
                continue;
            }
            Array.set(array, i, convertValue(item, componentType, componentType));
        }

        return array;
    }

    @SuppressWarnings("unchecked")
    private static Object convertMultiDimList(Object raw, Class<?> collType, ParameterizedType gType) throws Exception {
        if (!(raw instanceof List<?> rawList))
            return null;

        Collection<Object> list = Set.class.isAssignableFrom(collType)
                ? new LinkedHashSet<>()
                : new ArrayList<>();

        Type elemType = gType.getActualTypeArguments()[0];

        for (Object item : rawList) {
            if (item instanceof List<?> && elemType instanceof ParameterizedType nestedPT
                    && ((Class<?>) nestedPT.getRawType()).isAssignableFrom(List.class)) {
                list.add(convertMultiDimList(item, (Class<?>) nestedPT.getRawType(), nestedPT));
                continue;
            }

            if (item instanceof Map && elemType instanceof Class<?> c && !Map.class.isAssignableFrom(c)) {
                list.add(convertObject((Map<String, Object>) item, c));
                continue;
            }

            list.add(convertValue(item, (Class<?>) elemType, elemType));
        }

        return list;
    }
}