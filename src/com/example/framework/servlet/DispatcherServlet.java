package com.example.framework.servlet;

import com.example.framework.core.ModelView;
import com.example.framework.core.RouteMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public class DispatcherServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            dispatch(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void dispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String url = req.getRequestURI().substring(req.getContextPath().length());

        if (getServletContext().getResource(url) != null) {
            getServletContext().getNamedDispatcher("default").forward(req, resp);
            return;
        }

        Map<String, RouteMapping> mappings = (Map<String, RouteMapping>) getServletContext()
                .getAttribute("urlMappings");

        if (mappings == null || !mappings.containsKey(url)) {
            resp.setStatus(404);
            resp.getWriter().println("404 Not Found: " + url);
            return;
        }

        RouteMapping mapping = mappings.get(url);
        Class<?> returnType = mapping.getMethod().getReturnType();
        Object controllerInstance = mapping.getControllerClass().getDeclaredConstructor().newInstance();
        Object result = mapping.getMethod().invoke(controllerInstance);
        if(returnType == String.class) {
            resp.getWriter().println(result);
        }
        if(returnType == ModelView.class) {
            ModelView model = (ModelView) result;
            Map<String, Object> attributes = model.getAttributes();
            for(String key : attributes.keySet()) {
                req.setAttribute(key, attributes.get(key));
            }
            req.getRequestDispatcher(model.getView()).forward(req, resp);
        }
    }
}
