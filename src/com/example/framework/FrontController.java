package com.example.framework;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Map;

public class FrontController extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain; charset=UTF-8");

        // Récupération des infos de la requête
        String fullUrl = req.getRequestURL().toString();
        String path = req.getPathInfo();   // ce qu'il y a après /app/
        if (path == null) path = "/";
        String method = req.getMethod();   // GET, POST...
        String clientIp = req.getRemoteAddr();

        // Écrire les infos dans la réponse
        resp.getWriter().write("=== Informations sur la requête ===\n");
        resp.getWriter().write("Méthode HTTP : " + method + "\n");
        resp.getWriter().write("URL complète : " + fullUrl + "\n");
        resp.getWriter().write("Chemin demandé : " + path + "\n");
        resp.getWriter().write("Adresse IP client : " + clientIp + "\n\n");

        // Afficher les paramètres GET/POST
        resp.getWriter().write("=== Paramètres reçus ===\n");
        Map<String, String[]> params = req.getParameterMap();
        if (params.isEmpty()) {
            resp.getWriter().write("Aucun paramètre transmis.\n");
        } else {
            for (Map.Entry<String, String[]> entry : params.entrySet()) {
                resp.getWriter().write(entry.getKey() + " = ");
                for (String val : entry.getValue()) {
                    resp.getWriter().write(val + " ");
                }
                resp.getWriter().write("\n");
            }
        }
    }
}
