package com.example.framework.utils;

import com.example.framework.annotations.Controller;
import com.example.framework.annotations.GetMapping;
import com.example.framework.annotations.PostMapping;
import com.example.framework.annotations.RequestMapping;
import com.example.framework.core.RouteMapping;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RouteScanner {

    public static Map<String, List<RouteMapping>> scanPackages(String packages) throws Exception {
        Map<String, List<RouteMapping>> mappings = new HashMap<>();

        for (String pkg : packages.split(",")) {
            pkg = pkg.trim();
            List<Class<?>> classes = ClassScanner.findClasses(pkg);

            for (Class<?> clazz : classes) {
                if (!clazz.isAnnotationPresent(Controller.class))
                    continue;

                for (Method method : clazz.getDeclaredMethods()) {
                    String path = null;
                    String httpMethod = null;

                    if (method.isAnnotationPresent(RequestMapping.class)) {
                        path = method.getAnnotation(RequestMapping.class).value();
                        httpMethod = "ALL";
                    } else if (method.isAnnotationPresent(GetMapping.class)) {
                        path = method.getAnnotation(GetMapping.class).value();
                        httpMethod = "GET";
                    } else if (method.isAnnotationPresent(PostMapping.class)) {
                        path = method.getAnnotation(PostMapping.class).value();
                        httpMethod = "POST";
                    }

                    if (path != null) {
                        RouteMapping mapping = new RouteMapping(clazz, method, httpMethod);
                        mappings.computeIfAbsent(path, k -> new ArrayList<>()).add(mapping);
                    }
                }
            }
        }

        return mappings;
    }

}
