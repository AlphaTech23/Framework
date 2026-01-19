package com.example.framework.utils;

import java.io.InputStream;
import java.util.Properties;

public class AppProperties {

    private static Properties properties = new Properties();

    static {
        try (InputStream in = AppProperties.class.getClassLoader().getResourceAsStream("app.properties")) {
            if (in != null) {
                properties.load(in);
            } else {
                System.err.println("app.properties not found in classpath!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
