package com.example.framework.core;

import com.example.framework.utils.RouteScanner;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.util.Map;
import java.util.List;

@WebListener
public class ContextInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            ServletContext context = sce.getServletContext();

            String packages = context.getInitParameter("controller-packages");
            if (packages == null || packages.isEmpty()) {
                return;
            }

            Map<String, List<RouteMapping>> mappings = RouteScanner.scanPackages(packages);
            context.setAttribute("urlMappings", mappings);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'initialisation du contexte", e);
        }
    }
}
