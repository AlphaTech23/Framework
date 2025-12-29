package com.example.framework.core;

import jakarta.servlet.ServletContext;

public class ContextHolder {

    private static ServletContext servletContext;

    public static void setServletContext(ServletContext context) {
        servletContext = context;
    }

    public static ServletContext getServletContext() {
        if (servletContext == null) {
            throw new IllegalStateException("ServletContext non initialisé");
        }
        return servletContext;
    }
}
