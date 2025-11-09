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
        // Construire l'URL demandée (sans query string pour le matching)
        String requestPath = req.getRequestURI();

        // Préparer la réponse HTML
        resp.setContentType("text/html;charset=UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);

        // Scan des contrôleurs disponibles
        List<Class<?>> controllerClasses = AnnotationScanner.findControllerClasses();

        // Scan des routes disponibles
        List<ModelView> allRoutes = AnnotationScanner.findAllRoutes();

        // Chercher une route correspondante à l'URL demandée
        ModelView matchingRoute = findMatchingRoute(requestPath, allRoutes);

        try (PrintWriter out = resp.getWriter()) {
            if (matchingRoute != null) {
                // Invoquer la méthode de la route trouvée
                invokeRouteMethod(matchingRoute, out);
            } else {
                // Afficher la page de démonstration par défaut
                showDemoPage(requestPath, controllerClasses, allRoutes, out);
            }
        }
    }

    /**
     * Trouve une route correspondante à l'URL demandée
     */
    private ModelView findMatchingRoute(String requestPath, List<ModelView> routes) {
        for (ModelView route : routes) {
            if (route.getUrl().equals(requestPath)) {
                return route;
            }
        }
        return null;
    }

    /**
     * Invoque la méthode d'une route et affiche le résultat
     */
    private void invokeRouteMethod(ModelView route, PrintWriter out) {
        try {
            // Créer une instance du contrôleur
            Class<?> controllerClass = route.getController();
            Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();

            // Invoquer la méthode
            Object result = route.getMethod().invoke(controllerInstance);

            // Vérifier le type de retour et afficher
            out.println("<!doctype html>");
            out.println("<html lang=\"fr\">\n<head>\n<meta charset=\"utf-8\">\n<title>Route: " + route.getUrl() + "</title>\n</head>");
            out.println("<body>");
            out.println("<h1>Résultat de la route: " + escapeHtml(route.getUrl()) + "</h1>");
            out.println("<p><strong>Contrôleur:</strong> " + escapeHtml(controllerClass.getSimpleName()) + "</p>");
            out.println("<p><strong>Méthode:</strong> " + escapeHtml(route.getMethod().getName()) + "</p>");
            out.println("<p><strong>Vue:</strong> " + escapeHtml(route.getView()) + "</p>");

            out.println("<h2>Résultat de l'exécution:</h2>");

            if (result instanceof String) {
                out.println("<div style='background-color:#e8f5e8; padding:15px; border-radius:5px;'>");
                out.println("<p><strong>Type:</strong> String</p>");
                out.println("<p><strong>Contenu:</strong></p>");
                out.println("<pre>" + escapeHtml((String) result) + "</pre>");
                out.println("</div>");
            } else {
                throw new IllegalReturnTypeException(
                    "La méthode " + route.getMethod().getName() +
                    " du contrôleur " + controllerClass.getSimpleName() +
                    " doit retourner un String. Type retourné: " +
                    (result != null ? result.getClass().getSimpleName() : "null")
                );
            }

            out.println("<p><a href='/'>← Retour à la page d'accueil</a></p>");
            out.println("</body>\n</html>");

        } catch (IllegalReturnTypeException e) {
            showError(out, "Erreur de type de retour", e.getMessage());
        } catch (Exception e) {
            showError(out, "Erreur lors de l'invocation", e.getMessage());
        }
    }

    /**
     * Affiche une page d'erreur
     */
    private void showError(PrintWriter out, String title, String message) {
        out.println("<!doctype html>");
        out.println("<html lang=\"fr\">\n<head>\n<meta charset=\"utf-8\">\n<title>Erreur - Web Frame</title>\n</head>");
        out.println("<body>");
        out.println("<h1 style='color:red;'>❌ " + escapeHtml(title) + "</h1>");
        out.println("<div style='background-color:#ffe6e6; padding:15px; border-radius:5px; border-left:5px solid red;'>");
        out.println("<p><strong>Message d'erreur:</strong></p>");
        out.println("<pre>" + escapeHtml(message) + "</pre>");
        out.println("</div>");
        out.println("<p><a href='/'>← Retour à la page d'accueil</a></p>");
        out.println("</body>\n</html>");
    }

    /**
     * Affiche la page de démonstration par défaut
     */
    private void showDemoPage(String requestPath, List<Class<?>> controllerClasses, List<ModelView> allRoutes, PrintWriter out) {
        out.println("<!doctype html>");
        out.println("<html lang=\"fr\">\n<head>\n<meta charset=\"utf-8\">\n<title>Web Frame Demo</title>\n</head>");
        out.println("<body>");
        out.println("<h1>Web Frame - Framework de Scanner d'Annotations</h1>");

        out.println("<h2>URL demandée</h2>");
        out.println("<p>" + escapeHtml(requestPath) + "</p>");

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
            out.println("<p><em>Cliquez sur une URL pour tester la route !</em></p>");
            out.println("<table border='1' style='border-collapse:collapse; width:100%;'>");
            out.println("<tr><th>URL</th><th>Méthode</th><th>Vue</th><th>Contrôleur</th><th>Action</th></tr>");
            for (ModelView route : allRoutes) {
                out.println("<tr>");
                out.println("<td><a href='" + escapeHtml(route.getUrl()) + "'>" + escapeHtml(route.getUrl()) + "</a></td>");
                out.println("<td>" + escapeHtml(route.getMethod().getName()) + "</td>");
                out.println("<td>" + escapeHtml(route.getView()) + "</td>");
                out.println("<td>" + escapeHtml(route.getController().getSimpleName()) + "</td>");
                out.println("<td><a href='" + escapeHtml(route.getUrl()) + "' style='background-color:#4CAF50; color:white; padding:5px 10px; text-decoration:none; border-radius:3px;'>Tester</a></td>");
                out.println("</tr>");
            }
            out.println("</table>");
        }

        out.println("<h2>Fonctionnalités du Framework</h2>");
        out.println("<ul>");
        out.println("<li><strong>Scanner automatique:</strong> Détecte tous les contrôleurs @Controller</li>");
        out.println("<li><strong>Scanner de routes:</strong> Détecte toutes les méthodes @Router</li>");
        out.println("<li><strong>Invocation automatique:</strong> Exécute les méthodes des routes automatiquement</li>");
        out.println("<li><strong>Validation du retour:</strong> Vérifie que les méthodes retournent un String</li>");
        out.println("<li><strong>Compatible JAR:</strong> Fonctionne quand packageé en JAR</li>");
        out.println("</ul>");

        out.println("</body>\n</html>");
    }

    /**
     * Exception personnalisée pour les types de retour invalides
     */
    private static class IllegalReturnTypeException extends Exception {
        public IllegalReturnTypeException(String message) {
            super(message);
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
