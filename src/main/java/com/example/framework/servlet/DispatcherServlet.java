package com.example.framework.servlet;

import com.example.framework.annotations.Allowed;
import com.example.framework.annotations.Authorized;
import com.example.framework.annotations.Json;
import com.example.framework.annotations.Session;
import com.example.framework.core.ModelView;
import com.example.framework.core.RouteMapping;
import com.example.framework.utils.RouteResolver;
import com.example.framework.utils.AppProperties;
import com.example.framework.utils.JsonParser;
import com.example.framework.utils.ParameterResolver;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
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

    private void printJSON(HttpServletResponse resp, String json) throws Exception {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().print(json);
    }

    private void printError(HttpServletResponse resp, Integer status, String message) throws Exception {
        resp.setStatus(status);
        resp.getWriter().println(message);
    }

    private Object getUserRole(HttpServletRequest req, HttpSession httpSession) {
        if (httpSession == null)
            httpSession = req.getSession(false);
        String roleAttrName = AppProperties.get("role-attribute-name");
        if(roleAttrName == null) roleAttrName = getServletContext().getInitParameter("role-attribute-name");
        if (roleAttrName != null)
            return httpSession.getAttribute(roleAttrName);
        return null;
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
            printError(resp, 404, "404 Not Found: " + req.getMethod() + " " + url);
            return;
        }

        RouteMapping mapping = (RouteMapping) resolved.get("mapping");
        Map<String, String> pathVars = (Map<String, String>) resolved.get("pathVars");

        Object controllerInstance = mapping.getControllerClass().getDeclaredConstructor().newInstance();
        Method method = mapping.getMethod();

        Map<String, Object> sessionMap = null;
        HttpSession httpSession = null;

        if (method.isAnnotationPresent(Authorized.class)) {
            if (getUserRole(req, httpSession) == null) {
                printError(resp, 503, "Access denied: you must be logged in to view this page.");
                return;
            }
        }

        if (method.isAnnotationPresent(Allowed.class)) {
            String[] roles = method.getAnnotation(Allowed.class).value();
            boolean allowed = false;
            Object userRole = getUserRole(req, httpSession);
            if (userRole != null) {
                for (String role : roles) {
                    if (getUserRole(req, httpSession).equals(role))
                        allowed = true;
                }
            }
            if (!allowed)
                printError(resp, 503, "Access denied: you must be logged as " + String.join(" or ", roles)
                        + " in to view this page.");
        }

        for (var p : method.getParameters()) {
            if (p.isAnnotationPresent(Session.class)) {
                if (httpSession == null)
                    httpSession = req.getSession(true);
                sessionMap = new HashMap<>();
                break;
            }
        }

        Object[] args = ParameterResolver.resolve(method, req, pathVars, sessionMap, httpSession);

        Object result = null;

        try {
            result = method.invoke(controllerInstance, args);
            if (sessionMap != null) {
                Enumeration<String> keys = httpSession.getAttributeNames();
                while (keys.hasMoreElements()) {
                    String key = keys.nextElement();
                    httpSession.removeAttribute(key);
                }
                sessionMap.forEach(httpSession::setAttribute);
            }

            if (method.isAnnotationPresent(Json.class)) {
                Object data = result;
                if (result instanceof ModelView)
                    data = ((ModelView) result).getAttributes();
                String json = JsonParser.success(data);
                printJSON(resp, json);
                return;
            }

        } catch (InvocationTargetException ex) {
            if (method.isAnnotationPresent(Json.class)) {
                String json = JsonParser.error(ex.getCause().getMessage());
                printJSON(resp, json);
                return;
            }
            throw ex;
        }

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
