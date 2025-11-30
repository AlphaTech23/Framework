package com.example.framework.servlet;

import com.example.framework.core.ModelView;
import com.example.framework.core.RouteMapping;
import com.example.framework.utils.RouteResolver;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
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

        HashMap<String, Object> resolved = RouteResolver.resolve(url, mappings);

        if (resolved == null) {
            resp.setStatus(404);
            resp.getWriter().println("404 Not Found: " + url);
            return;
        }

        RouteMapping mapping = (RouteMapping) resolved.get("mapping");
        Map<String, String> pathVars = (Map<String, String>) resolved.get("pathVars");
        
        Object controllerInstance = mapping.getControllerClass().getDeclaredConstructor().newInstance();
        Object result = mapping.getMethod().invoke(controllerInstance);
        if (result.getClass() == String.class) {
            resp.getWriter().println(result);
        }
        if (result.getClass() == ModelView.class) {
            ModelView model = (ModelView) result;
            Map<String, Object> attributes = model.getAttributes();
            for (String key : attributes.keySet()) {
                req.setAttribute(key, attributes.get(key));
            }
            req.getRequestDispatcher(model.getView()).forward(req, resp);
        }
    }
}
