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
import java.util.Map;

@WebServlet("/")
public class DispatcherServlet extends HttpServlet {

    private ApplicationContext appContext;

    @Override
    public void init() throws ServletException {
        // Initialiser le contexte de l'application
        appContext = ApplicationContext.getInstance();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Construire l'URL demandée (sans query string pour le matching)
        String requestPath = req.getRequestURI();

        // Préparer la réponse HTML
        resp.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = resp.getWriter()) {
            // Chercher une route dans le contexte
            ModelView matchingRoute = appContext.findRoute(requestPath);

            if (matchingRoute != null) {
                // Route trouvée - retourner la vue
                resp.setStatus(HttpServletResponse.SC_OK);
                showView(matchingRoute, out);
            } else if ("/".equals(requestPath)) {
                // Page d'accueil - afficher la démo
                resp.setStatus(HttpServletResponse.SC_OK);
                showDemoPage(requestPath, out);
            } else {
                // Route non trouvée - 404
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                show404Page(requestPath, out);
            }
        }
    }

    /**
     * Affiche la vue correspondante à une route
     */
    private void showView(ModelView route, PrintWriter out) {
        out.println("<!doctype html>");
        out.println("<html lang=\"fr\">\n<head>\n<meta charset=\"utf-8\">\n<title>Vue: " + escapeHtml(route.getView()) + "</title>\n</head>");
        out.println("<body>");
        out.println("<h1>🎯 Vue: " + escapeHtml(route.getView()) + "</h1>");

        out.println("<div style='background-color:#e8f5e8; padding:20px; border-radius:8px; margin:20px 0;'>");
        out.println("<h2>✅ Route trouvée et vue retournée</h2>");
        out.println("<p><strong>URL demandée:</strong> " + escapeHtml(route.getUrl()) + "</p>");
        out.println("<p><strong>Contrôleur:</strong> " + escapeHtml(route.getController().getSimpleName()) + "</p>");
        out.println("<p><strong>Méthode:</strong> " + escapeHtml(route.getMethod().getName()) + "</p>");
        out.println("<p><strong>Vue retournée:</strong> <code>" + escapeHtml(route.getView()) + "</code></p>");
        out.println("</div>");

        out.println("<div style='background-color:#f0f8ff; padding:15px; border-radius:5px; border-left:4px solid #007acc;'>");
        out.println("<h3>💡 Comment ça fonctionne</h3>");
        out.println("<p>1. L'URL <code>" + escapeHtml(route.getUrl()) + "</code> a été trouvée dans le contexte</p>");
        out.println("<p>2. La méthode <code>" + escapeHtml(route.getMethod().getName()) + "()</code> a été exécutée</p>");
        out.println("<p>3. Le retour de la méthode (<code>" + escapeHtml(route.getView()) + "</code>) est maintenant le nom de la vue</p>");
        out.println("<p>4. Dans une vraie application, cette vue serait rendue par un moteur de template</p>");
        out.println("</div>");

        out.println("<p style='margin-top:20px;'><a href='/' style='background-color:#007acc; color:white; padding:10px 15px; text-decoration:none; border-radius:5px;'>← Retour à l'accueil</a></p>");
        out.println("</body>\n</html>");
    }

    /**
     * Affiche une page 404 personnalisée
     */
    private void show404Page(String requestPath, PrintWriter out) {
        out.println("<!doctype html>");
        out.println("<html lang=\"fr\">\n<head>\n<meta charset=\"utf-8\">\n<title>404 - Page non trouvée</title>\n</head>");
        out.println("<body>");
        out.println("<h1 style='color:#d32f2f;'>❌ 404 - Page non trouvée</h1>");

        out.println("<div style='background-color:#ffebee; padding:20px; border-radius:8px; border-left:4px solid #d32f2f; margin:20px 0;'>");
        out.println("<h2>URL non trouvée</h2>");
        out.println("<p><strong>URL demandée:</strong> <code>" + escapeHtml(requestPath) + "</code></p>");
        out.println("<p>Cette URL ne correspond à aucune route définie dans l'application.</p>");
        out.println("</div>");

        out.println("<div style='background-color:#f5f5f5; padding:15px; border-radius:5px;'>");
        out.println("<h3>📋 Routes disponibles</h3>");
        out.println("<p>Voici les routes actuellement disponibles dans l'application :</p>");
        out.println("<ul>");

        // Afficher les routes disponibles
        for (ModelView route : appContext.getAllRoutes().values()) {
            out.println("<li><a href='" + escapeHtml(route.getUrl()) + "'>" + escapeHtml(route.getUrl()) + "</a> → Vue: " + escapeHtml(route.getView()) + "</li>");
        }

        out.println("</ul>");
        out.println("</div>");

        out.println("<p style='margin-top:20px;'>");
        out.println("<a href='/' style='background-color:#4caf50; color:white; padding:10px 15px; text-decoration:none; border-radius:5px; margin-right:10px;'>🏠 Accueil</a>");
        out.println("<a href='javascript:history.back()' style='background-color:#757575; color:white; padding:10px 15px; text-decoration:none; border-radius:5px;'>← Retour</a>");
        out.println("</p>");
        out.println("</body>\n</html>");
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
    private void showDemoPage(String requestPath, PrintWriter out) {
        // Obtenir les informations depuis le contexte
        Map<String, ModelView> allRoutes = appContext.getAllRoutes();
        List<Class<?>> controllerClasses = AnnotationScanner.findControllerClasses();

        out.println("<!doctype html>");
        out.println("<html lang=\"fr\">\n<head>\n<meta charset=\"utf-8\">\n<title>Web Frame - Framework MVC</title>\n</head>");
        out.println("<body>");
        out.println("<h1>🚀 Web Frame - Framework MVC avec Vues Dynamiques</h1>");

        out.println("<div style='background-color:#e8f5e8; padding:15px; border-radius:8px; margin:20px 0;'>");
        out.println("<h2>✨ Nouvelle Fonctionnalité</h2>");
        out.println("<p><strong>Le retour des méthodes @Router est maintenant le nom de la vue !</strong></p>");
        out.println("<p>Chaque méthode retourne un String qui devient le nom de la vue dans ModelView.</p>");
        out.println("</div>");

        out.println("<h2>📍 Routes et Vues Disponibles</h2>");
        if (allRoutes.isEmpty()) {
            out.println("<p>Aucune route trouvée dans les contrôleurs.</p>");
        } else {
            out.println("<p>Nombre de routes: <strong>" + allRoutes.size() + "</strong></p>");
            out.println("<p><em>Cliquez sur une URL pour voir sa vue !</em></p>");
            out.println("<table border='1' style='border-collapse:collapse; width:100%; margin:10px 0;'>");
            out.println("<tr style='background-color:#f5f5f5;'><th>URL</th><th>Vue (retour méthode)</th><th>Contrôleur</th><th>Méthode</th><th>Test</th></tr>");

            for (ModelView route : allRoutes.values()) {
                out.println("<tr>");
                out.println("<td><code>" + escapeHtml(route.getUrl()) + "</code></td>");
                out.println("<td><strong>" + escapeHtml(route.getView()) + "</strong></td>");
                out.println("<td>" + escapeHtml(route.getController().getSimpleName()) + "</td>");
                out.println("<td>" + escapeHtml(route.getMethod().getName()) + "()</td>");
                out.println("<td><a href='" + escapeHtml(route.getUrl()) + "' style='background-color:#2196F3; color:white; padding:5px 10px; text-decoration:none; border-radius:3px;'>Voir Vue</a></td>");
                out.println("</tr>");
            }
            out.println("</table>");
        }

        out.println("<h2>🎛️ Contrôleurs Détectés</h2>");
        if (controllerClasses.isEmpty()) {
            out.println("<p>Aucun contrôleur trouvé.</p>");
        } else {
            out.println("<p>Nombre: <strong>" + controllerClasses.size() + "</strong></p>");
            out.println("<ul>");
            for (Class<?> controllerClass : controllerClasses) {
                out.println("<li><code>" + escapeHtml(controllerClass.getName()) + "</code></li>");
            }
            out.println("</ul>");
        }

        out.println("<h2>🔧 Fonctionnalités</h2>");
        out.println("<ul>");
        out.println("<li><strong>Système de vues:</strong> Le retour des méthodes devient le nom de la vue</li>");
        out.println("<li><strong>Gestion 404:</strong> URL inconnue → page d'erreur automatique</li>");
        out.println("<li><strong>Contexte applicatif:</strong> Mapping URL → Vue en mémoire</li>");
        out.println("<li><strong>Scanner automatique:</strong> Détection des contrôleurs et routes</li>");
        out.println("<li><strong>Architecture MVC:</strong> Contrôleur → Méthode → Vue</li>");
        out.println("</ul>");

        out.println("<div style='background-color:#fff3cd; padding:15px; border-radius:5px; border-left:4px solid #ffc107; margin:20px 0;'>");
        out.println("<h3>🧪 Test du 404</h3>");
        out.println("<p>Essayez une URL qui n'existe pas : <a href='/url-inexistante'>/url-inexistante</a></p>");
        out.println("</div>");

        out.println("</body>\n</html>");
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
