package com.example.framework.core;

import com.example.framework.utils.RouteScanner;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.annotation.WebListener;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@WebListener
public class ContextInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            ServletContext context = sce.getServletContext();
            ContextHolder.setServletContext(context);

            String uploadBase = context.getInitParameter("upload.root");
            if (uploadBase == null || uploadBase.isBlank()) {
                uploadBase = "uploads";
            }

            Path uploadRoot = Path.of(uploadBase);
            if (!uploadRoot.isAbsolute()) {
                uploadRoot = Path.of(context.getRealPath("/")).resolve(uploadBase);
            }
            uploadRoot = uploadRoot.normalize();
            System.out.println("Upload root: " + uploadRoot);
            Files.createDirectories(uploadRoot);
            context.setAttribute("upload.root", uploadRoot);

            long defaultFileSizeThreshold = 1024 * 1024;  // 1 MB
            long defaultMaxFileSize       = 5 * 1024 * 1024; // 5 MB
            long defaultMaxRequestSize    = 20 * 1024 * 1024; // 20 MB

            String fileSizeThreshold = context.getInitParameter("multipart.fileSizeThreshold");
            String maxFileSize       = context.getInitParameter("multipart.maxFileSize");
            String maxRequestSize    = context.getInitParameter("multipart.maxRequestSize");

            if (fileSizeThreshold != null) defaultFileSizeThreshold = Long.parseLong(fileSizeThreshold);
            if (maxFileSize != null)       defaultMaxFileSize       = Long.parseLong(maxFileSize);
            if (maxRequestSize != null)    defaultMaxRequestSize    = Long.parseLong(maxRequestSize);

            ServletRegistration.Dynamic dispatcher = (ServletRegistration.Dynamic) context.getServletRegistration("Dispatcher");
            if (dispatcher != null) {
                dispatcher.setMultipartConfig(
                    new MultipartConfigElement(
                        null,
                        defaultMaxFileSize,
                        defaultMaxRequestSize,
                        (int) defaultFileSizeThreshold
                    )
                );
            }

            String packages = context.getInitParameter("controller-packages");
            if (packages != null && !packages.isBlank()) {
                Map<String, List<RouteMapping>> mappings = RouteScanner.scanPackages(packages);
                context.setAttribute("urlMappings", mappings);
            }

        } catch (Exception e) {
            throw new RuntimeException(
                "Erreur lors de l'initialisation du contexte", e
            );
        }
    }
}
