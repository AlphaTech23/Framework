package com.example.framework.core;

import java.util.HashMap;
import java.util.Map;

public class ParamsHolder {

    private static Map<String, Object> params = new HashMap<>();

    public static void add(String key, Object value) {
        params.put(key, value);
    }

    public static Object get(String key) {
        return params.get(key);
    }
}
