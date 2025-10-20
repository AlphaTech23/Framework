package com.example.framework;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class DispatcherServlet extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
 url = req.getRequestURI().substring(req.getContextPath().length());
        if(getServletContext().getResource(url) != null) {
            getServletContext().getNamedDispatcher("default").forward(req, resp);
        } else {
            resp.getWriter().print("URL : " + url);
        }
    }            dispatch(req, resp);
        } catch(Exception e) {
            throw new ServletException(e.getMessage());
        }
    }

    private void dispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception{
        String
}
