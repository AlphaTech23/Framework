package com.example.framework.utils;

import jakarta.servlet.ServletContext;
import java.io.InputStream;
import java.util.Properties;

public class AppProperties {

    private static final Properties PROPS = new Properties();
    private static boolean initialized = false;

    private AppProperties() {
    }

    public static void init(ServletContext context) {
        if (initialized)
            return;

        try (InputStream in = context.getResourceAsStream("/WEB-INF/app.properties")) {

            if (in != null) {
                PROPS.load(in);
                initialized = true;
            } else
                System.out.println("WEB-INF/app.properties introuvable");

        } catch (Exception e) {
            throw new RuntimeException("Erreur chargement app.properties", e);
        }
    }

    public static String get(String key) {
        checkInit();
        return PROPS.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        checkInit();
        return PROPS.getProperty(key, defaultValue);
    }

    private static void checkInit() {
        if (!initialized) {
            throw new IllegalStateException(
                    "AppProperties non initialisé. Appelle AppProperties.init(ServletContext)");
        }
    }
}
