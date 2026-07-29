package webframe.core;

import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import webframe.core.util.AnnotationScanner;
import webframe.core.util.ParameterResolver;
import webframe.core.tools.ModelView;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servlet principal qui dispatche les requêtes HTTP vers les contrôleurs appropriés.
 *
 * Cette classe étend {@link HttpServlet} et agit comme un front controller pour
 * le framework web. Elle est responsable de :
 * <ul>
 *   <li>Initialiser le contexte de l'application au démarrage</li>
 *   <li>Router les requêtes HTTP (GET, POST, etc.) vers les bons contrôleurs</li>
 *   <li>Résoudre les paramètres de méthode à partir des requêtes HTTP et des URLs</li>
 *   <li>Exécuter les méthodes des contrôleurs avec injection de dépendances</li>
 *   <li>Gérer le rendu des vues et les erreurs</li>
 * </ul>
 *
 * Le servlet utilise l'annotation {@code @WebServlet("/")} pour capturer toutes
 * les requêtes et les dispatcher selon les routes définies dans les contrôleurs.
 *
 * Exemple d'utilisation dans web.xml ou via l'annotation :
 * <pre>
 * // Automatiquement configuré via l'annotation @WebServlet
 * // Toutes les requêtes HTTP sont dirigées vers ce servlet
 * </pre>
 *
 * @see ApplicationContext
 * @see ModelView
 * @see webframe.core.annotation.Controller
 * @see webframe.core.util.ParameterResolver
 */
@MultipartConfig
@WebServlet("/")
public class DispatcherServlet extends HttpServlet {

    private ApplicationContext appContext;
    private Map<Class<?>, Object> singletonControllers = new HashMap<>();

    @Override
    public void init() {
        // Initialiser le contexte de l'application
        appContext = ApplicationContext.getInstance();
        
        // Initialiser les Singletons par défaut (Sprint 10)
        try {
            List<Class<?>> controllers = AnnotationScanner.findControllerClasses();
            for (Class<?> controllerClass : controllers) {
                if (!controllerClass.isAnnotationPresent(webframe.core.annotation.Stateful.class)) {
                    singletonControllers.put(controllerClass, controllerClass.getDeclaredConstructor().newInstance());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handleRequest(req, resp, "GET");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        handleRequest(req, resp, "POST");
    }

    /**
     * Gère une requête HTTP avec un verbe spécifique.
     *
     * @param req la requête HTTP
     * @param resp la réponse HTTP
     * @param httpMethod le verbe HTTP (GET, POST, etc.)
     */
    private void handleRequest(HttpServletRequest req, HttpServletResponse resp, String httpMethod) throws IOException {
        String requestPath = req.getRequestURI();

        try (PrintWriter out = resp.getWriter()) {
            ModelView matchingRoute = appContext.findRoute(requestPath, httpMethod);

            if (matchingRoute != null) {
                try {
                    Method method = matchingRoute.getMethod(httpMethod);
                    
                    if (method != null && method.isAnnotationPresent(webframe.core.annotation.Json.class)) {
                        // Cas Sprint 9 : API JSON
                        Object result = executeMethodDirectly(matchingRoute, req, httpMethod);
                        if (result instanceof ModelView) {
                            resp.setContentType("text/html;charset=UTF-8");
                            resp.setStatus(HttpServletResponse.SC_OK);
                            showView((ModelView) result, out, httpMethod);
                        } else {
                            resp.setContentType("application/json;charset=UTF-8");
                            resp.setStatus(HttpServletResponse.SC_OK);
                            out.print(new com.google.gson.Gson().toJson(result));
                        }
                    } else {
                        // Cas HTML classique
                        resp.setContentType("text/html;charset=UTF-8");
                        ModelView executed = executeRouteMethod(matchingRoute, req, httpMethod);
                        resp.setStatus(HttpServletResponse.SC_OK);
                        showView(executed, out, httpMethod);
                    }
                } catch (Exception e) {
                    resp.setContentType("text/html;charset=UTF-8");
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    showError(out, e.toString());
                }
            } else if ("/".equals(requestPath)) {
                resp.setContentType("text/html;charset=UTF-8");
                resp.setStatus(HttpServletResponse.SC_OK);
                showDemoPage(out);
            } else {
                resp.setContentType("text/html;charset=UTF-8");
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                show404Page(requestPath, out);
            }
        }
    }


    /**
     * Exécute directement la méthode du contrôleur et retourne le résultat brut.
     */
    private Object executeMethodDirectly(ModelView route, HttpServletRequest request, String httpMethod) throws Exception {
        if (route == null) return null;

        Method method = route.getMethod(httpMethod);
        Class<?> controllerClass = route.getController();
        if (method == null || controllerClass == null) return null;

        Object controllerInstance;
        
        if (controllerClass.isAnnotationPresent(webframe.core.annotation.Stateful.class)) {
            // Approche 4 : Stateful -> instance conservée dans la session du navigateur
            jakarta.servlet.http.HttpSession session = request.getSession();
            String sessionKey = "controller_" + controllerClass.getName();
            controllerInstance = session.getAttribute(sessionKey);
            if (controllerInstance == null) {
                controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
                session.setAttribute(sessionKey, controllerInstance);
            }
        } else {
            // Approche 3 : Singleton par défaut -> même instance pour tout le monde (attention concurrence)
            controllerInstance = singletonControllers.get(controllerClass);
            if (controllerInstance == null) { // Fallback de sécurité
                controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
                singletonControllers.put(controllerClass, controllerInstance);
            }
        }

        // Approche 2 : Injection des champs (fichiers ou autres paramètres) en tant qu'attributs du contrôleur
        injectAttributes(controllerInstance, request);

        method.setAccessible(true);

        // Extraire les paramètres d'URL depuis les données du ModelView
        Map<String, String> urlParameters = extractUrlParametersFromRoute(route);

        // Résoudre les paramètres de la méthode à partir de la requête HTTP et des paramètres d'URL
        Object[] args = ParameterResolver.resolveParameters(method, request, urlParameters);
        return method.invoke(controllerInstance, args);
    }

    /**
     * Injecte les paramètres HTTP et les fichiers (Part) dans les champs du contrôleur.
     */
    private void injectAttributes(Object controllerInstance, HttpServletRequest request) throws Exception {
        Class<?> controllerClass = controllerInstance.getClass();
        boolean isMultipart = request.getContentType() != null && request.getContentType().toLowerCase().startsWith("multipart/");
        
        for (java.lang.reflect.Field field : controllerClass.getDeclaredFields()) {
            field.setAccessible(true);
            String fieldName = field.getName();
            Class<?> fieldType = field.getType();
            
            if (fieldType == webframe.core.tools.FileUpload.class && isMultipart) {
                try {
                    jakarta.servlet.http.Part part = request.getPart(fieldName);
                    if (part != null && part.getSize() > 0) {
                        String fileName = part.getSubmittedFileName();
                        byte[] bytes = new byte[(int) part.getSize()];
                        try (java.io.InputStream is = part.getInputStream()) {
                            int read;
                            int total = 0;
                            while (total < bytes.length && (read = is.read(bytes, total, bytes.length - total)) != -1) {
                                total += read;
                            }
                        }
                        field.set(controllerInstance, new webframe.core.tools.FileUpload(fileName, bytes));
                    } else if (!fieldType.isPrimitive()) {
                        field.set(controllerInstance, null);
                    }
                } catch (Exception e) {
                    // Ignorer si la partie n'existe pas
                }
            } else {
                String paramValue = request.getParameter(fieldName);
                if (paramValue != null) {
                    if (fieldType == String.class) {
                        field.set(controllerInstance, paramValue);
                    } else if (fieldType == int.class || fieldType == Integer.class) {
                        field.set(controllerInstance, Integer.parseInt(paramValue));
                    } else if (fieldType == boolean.class || fieldType == Boolean.class) {
                        field.set(controllerInstance, Boolean.parseBoolean(paramValue));
                    } else if (fieldType == double.class || fieldType == Double.class) {
                        field.set(controllerInstance, Double.parseDouble(paramValue));
                    }
                } else if (!fieldType.isPrimitive()) {
                    field.set(controllerInstance, null);
                }
            }
        }
    }

    /**
     * Exécute la méthode du contrôleur et met à jour le ModelView retourné.
     *
     * @param route la route à exécuter
     * @param request la requête HTTP
     * @param httpMethod le verbe HTTP (GET, POST, etc.)
     */
    private ModelView executeRouteMethod(ModelView route, HttpServletRequest request, String httpMethod) throws Exception {
        Object result = executeMethodDirectly(route, request, httpMethod);

        if (result == null) {
            return route;
        }

        // Si la méthode retourne un ModelView -> utiliser directement
        if (result instanceof ModelView) {
            return (ModelView) result;
        }

        // Si la méthode retourne un String -> nom de la vue
        if (result instanceof String) {
            route.setView((String) result);
            return route;
        }

        // Si la méthode retourne une Map -> fusionner dans les data
        if (result instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> returnedMap = (Map<String, Object>) result;
            route.getData().putAll(returnedMap);
            return route;
        }

        // Autres types -> stocker sous une clé générique "result"
        route.getData().put("result", result);
        return route;
    }

    /**
     * Extrait les paramètres d'URL depuis les données du ModelView.
     * Les paramètres d'URL sont stockés avec le préfixe "urlParam_".
     */
    private Map<String, String> extractUrlParametersFromRoute(ModelView route) {
        Map<String, String> urlParameters = new java.util.HashMap<>();

        if (route.getData() != null) {
            for (Map.Entry<String, Object> entry : route.getData().entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("urlParam_")) {
                    String paramName = key.substring("urlParam_".length());
                    Object value = entry.getValue();
                    if (value != null) {
                        urlParameters.put(paramName, value.toString());
                    }
                }
            }
        }

        return urlParameters;
    }

    /**
     * Affiche la vue correspondante à une route (avec les données).
     *
     * @param route la route exécutée
     * @param out le writer pour la réponse
     * @param httpMethod le verbe HTTP utilisé
     */
    private void showView(ModelView route, PrintWriter out, String httpMethod) {
        out.println("<!doctype html>");
        out.println("<html lang=\"fr\">\n<head>\n<meta charset=\"utf-8\">\n<title>Vue: " + escapeHtml(route.getView()) + "</title>\n</head>");
        out.println("<body>");
        out.println("<h1>🎯 Vue: " + escapeHtml(route.getView()) + "</h1>");

        out.println("<div style='background-color:#e8f5e8; padding:20px; border-radius:8px; margin:20px 0;'>");
        out.println("<h2>✅ Route trouvée et vue retournée</h2>");
        out.println("<p><strong>URL demandée:</strong> " + escapeHtml(route.getUrl()) + "</p>");
        out.println("<p><strong>Contrôleur:</strong> " + escapeHtml(route.getController().getSimpleName()) + "</p>");
        out.println("<p><strong>Verbe HTTP:</strong> " + escapeHtml(httpMethod) + "</p>");

        Method executedMethod = route.getMethod(httpMethod);
        if (executedMethod != null) {
            out.println("<p><strong>Méthode exécutée:</strong> " + escapeHtml(executedMethod.getName()) + "</p>");
        }

        // Afficher toutes les méthodes disponibles pour cette route
        out.println("<p><strong>Méthodes disponibles:</strong> ");
        boolean first = true;
        for (Map.Entry<String, Method> entry : route.getMethods().entrySet()) {
            if (!first) out.print(", ");
            out.print(escapeHtml(entry.getKey()) + " → " + escapeHtml(entry.getValue().getName()));
            first = false;
        }
        out.println("</p>");

        out.println("<p><strong>Vue retournée:</strong> <code>" + escapeHtml(route.getView()) + "</code></p>");
        out.println("</div>");

        // Afficher les données envoyées depuis le contrôleur
        out.println("<div style='background-color:#fffde7; padding:15px; border-radius:5px; margin:10px 0;'>");
        out.println("<h3>📦 Données (ModelView.data)</h3>");
        if (route.getData() == null || route.getData().isEmpty()) {
            out.println("<p>Aucune donnée envoyée par le contrôleur.</p>");
        } else {
            out.println("<ul>");
            for (Map.Entry<String, Object> entry : route.getData().entrySet()) {
                String key = escapeHtml(entry.getKey());
                String value = escapeHtml(entry.getValue() != null ? entry.getValue().toString() : "null");
                out.println("<li><strong>" + key + ":</strong> " + value + "</li>");
            }
            out.println("</ul>");
        }
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
    private void showError(PrintWriter out, String message) {
        out.println("<!doctype html>");
        out.println("<html lang=\"fr\">\n<head>\n<meta charset=\"utf-8\">\n<title>Erreur - Web Frame</title>\n</head>");
        out.println("<body>");
        out.println("<h1 style='color:red;'>❌ " + escapeHtml("Erreur d'invocation") + "</h1>");
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
    private void showDemoPage(PrintWriter out) {
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

                // Afficher toutes les méthodes HTTP supportées
                StringBuilder methodsInfo = new StringBuilder();
                for (Map.Entry<String, Method> entry : route.getMethods().entrySet()) {
                    if (methodsInfo.length() > 0) methodsInfo.append(", ");
                    methodsInfo.append(entry.getKey()).append(": ").append(entry.getValue().getName()).append("()");
                }
                out.println("<td>" + escapeHtml(methodsInfo.toString()) + "</td>");

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
