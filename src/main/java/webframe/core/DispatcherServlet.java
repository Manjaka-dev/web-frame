package webframe.core;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import webframe.core.util.AnnotationScanner;
import webframe.core.tools.ModelView;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

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

        // Scan des contrôleurs disponibles
        List<Class<?>> controllerClasses = AnnotationScanner.findControllerClasses();

        // Scan des routes disponibles
        List<ModelView> allRoutes = AnnotationScanner.findAllRoutes();

        try (PrintWriter out = resp.getWriter()) {
            out.println("<!doctype html>");
            out.println("<html lang=\"fr\">\n<head>\n<meta charset=\"utf-8\">\n<title>Web Frame Demo</title>\n</head>");
            out.println("<body>");
            out.println("<h1>Web Frame - Framework de Scanner d'Annotations</h1>");

            out.println("<h2>URL demandée</h2>");
            out.println("<p>" + escapeHtml(fullUrl) + "</p>");

            out.println("<h2>Contrôleurs détectés</h2>");
            if (controllerClasses.isEmpty()) {
                out.println("<p>Aucun contrôleur trouvé dans le classpath.</p>");
            } else {
                out.println("<p>Nombre de contrôleurs trouvés: <strong>" + controllerClasses.size() + "</strong></p>");
                out.println("<ul>");
                for (Class<?> controllerClass : controllerClasses) {
                    out.println("<li>" + escapeHtml(controllerClass.getName()) + "</li>");
                }
                out.println("</ul>");
            }

            out.println("<h2>Routes détectées (@Router)</h2>");
            if (allRoutes.isEmpty()) {
                out.println("<p>Aucune route trouvée dans les contrôleurs.</p>");
            } else {
                out.println("<p>Nombre de routes trouvées: <strong>" + allRoutes.size() + "</strong></p>");
                out.println("<table border='1' style='border-collapse:collapse; width:100%;'>");
                out.println("<tr><th>URL</th><th>Méthode</th><th>Vue</th><th>Contrôleur</th></tr>");
                for (ModelView route : allRoutes) {
                    out.println("<tr>");
                    out.println("<td>" + escapeHtml(route.getUrl()) + "</td>");
                    out.println("<td>" + escapeHtml(route.getMethod().getName()) + "</td>");
                    out.println("<td>" + escapeHtml(route.getView()) + "</td>");
                    out.println("<td>" + escapeHtml(route.getController().getSimpleName()) + "</td>");
                    out.println("</tr>");
                }
                out.println("</table>");
            }

            out.println("<h2>Fonctionnalités du Framework</h2>");
            out.println("<ul>");
            out.println("<li><strong>Scanner automatique:</strong> Détecte tous les contrôleurs @Controller</li>");
            out.println("<li><strong>Scanner de routes:</strong> Détecte toutes les méthodes @Router</li>");
            out.println("<li><strong>Objets Class directement:</strong> Retourne des objets Class<?> prêts à utiliser</li>");
            out.println("<li><strong>Compatible JAR:</strong> Fonctionne quand packageé en JAR</li>");
            out.println("<li><strong>Scan par package:</strong> Peut scanner un package spécifique</li>");
            out.println("</ul>");

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
