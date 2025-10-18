package webframe.core;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/")
public class DispatcherServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Construire l'URL complète (request URL + query string si présente)
        StringBuffer requestURL = req.getRequestURL();
        String queryString = req.getQueryString();
        String fullUrl = (queryString == null) ? requestURL.toString() : requestURL.append('?').append(queryString).toString();

        // Préparer la réponse HTML
        resp.setContentType("text/html;charset=UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);

        try (PrintWriter out = resp.getWriter()) {
            out.println("<!doctype html>");
            out.println("<html lang=\"fr\">\n<head>\n<meta charset=\"utf-8\">\n<title>URL demande</title>\n</head>");
            out.println("<body>");
            out.println("<h1>URL demandée</h1>");
            out.println("<p>" + escapeHtml(fullUrl) + "</p>");
            out.println("</body>\n</html>");
        }
    }

    // Méthode utilitaire minimale pour échapper les caractères HTML basiques
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
