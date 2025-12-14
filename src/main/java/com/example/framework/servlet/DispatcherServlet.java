package com.example.framework.servlet;

import com.example.framework.annotations.Json;
import com.example.framework.core.ModelView;
import com.example.framework.core.RouteMapping;
import com.example.framework.utils.RouteResolver;
import com.example.framework.utils.JsonParser;
import com.example.framework.utils.ParameterResolver;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
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

        Map<String, List<RouteMapping>> mappings = (Map<String, List<RouteMapping>>) getServletContext()
                .getAttribute("urlMappings");

        HashMap<String, Object> resolved = RouteResolver.resolve(url, mappings, req.getMethod());

        if (resolved == null) {
            resp.setStatus(404);
            resp.getWriter().println("404 Not Found: " + req.getMethod() + " " + url);
            return;
        }

        RouteMapping mapping = (RouteMapping) resolved.get("mapping");
        Map<String, String> pathVars = (Map<String, String>) resolved.get("pathVars");

        
        Object controllerInstance = mapping.getControllerClass().getDeclaredConstructor().newInstance();
        Method method = mapping.getMethod();
        Object[] args = ParameterResolver.resolve(method, req, pathVars);

        Object result = null;

        try {
            result = method.invoke(controllerInstance, args);
            if (method.isAnnotationPresent(Json.class)) {
                Object data = result;
                if (result instanceof ModelView)
                    data = ((ModelView) result).getAttributes();
                String json = JsonParser.success(data);
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().print(json);
                return;
            }
        } catch (InvocationTargetException ex) {
            if (method.isAnnotationPresent(Json.class)) {
                String json = JsonParser.error(ex.getCause().getMessage());
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().print(json);

                return;
            }
            throw ex;
        }

        Object result = method.invoke(controllerInstance, args);

        if (result instanceof String) {
            String str = (String) result;
            resp.getWriter().println(str);
        } else if (result instanceof ModelView) {
            ModelView mv = (ModelView) result;
            mv.getAttributes().forEach(req::setAttribute);
            req.getRequestDispatcher(mv.getView()).forward(req, resp);
        }
    }
}
