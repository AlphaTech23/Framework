package com.example.framework.utils;

import com.example.framework.annotations.Controller;
import com.example.framework.annotations.Url;
import com.example.framework.core.RouteMapping;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RouteScanner {

    public static Map<String, RouteMapping> scanPackages(String packages) throws Exception {
        Map<String, RouteMapping> mappings = new HashMap<>();

        for (String pkg : packages.split(",")) {
            pkg = pkg.trim();
            try {
                List<Class<?>> classes = ClassScanner.findClasses(pkg);
                for (Class<?> clazz : classes) {
                    if (clazz.isAnnotationPresent(Controller.class)) {
                        for (Method method : clazz.getDeclaredMethods()) {
                            if (method.isAnnotationPresent(Url.class)) {
                                String path = method.getAnnotation(Url.class).value();
                                mappings.put(path, new RouteMapping(clazz, method));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw e;
            }
        }

        return mappings;
    }
}
