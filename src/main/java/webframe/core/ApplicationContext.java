package webframe.core;

import webframe.core.tools.ModelView;
import webframe.core.util.AnnotationScanner;
import webframe.core.util.UrlPatternMatcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contexte principal de l'application qui gère le mapping URL -> Contrôleur/Vue.
 * Cette classe implémente le pattern Singleton et est responsable de :
 * <ul>
 *   <li>Scanner automatiquement les contrôleurs annotés avec {@code @Controller}</li>
 *   <li>Enregistrer les routes définies par {@code @Router}, {@code @GET}, {@code @POST}</li>
 *   <li>Résoudre les URLs avec support des paramètres (ex: /users/{id})</li>
 *   <li>Gérer les verbes HTTP multiples pour une même route</li>
 * </ul>
 *
 * Exemple d'utilisation :
 * <pre>
 * ApplicationContext context = ApplicationContext.getInstance();
 * ModelView route = context.findRoute("/users/123", "GET");
 * if (route != null) {
 *     Method method = route.getMethod("GET");
 *     // Exécuter la méthode du contrôleur...
 * }
 * </pre>
 *
 * @see ModelView
 * @see webframe.core.annotation.Controller
 * @see webframe.core.annotation.Router
 * @see webframe.core.annotation.GET
 * @see webframe.core.annotation.POST
 */
public class ApplicationContext {

    final private Map<String, ModelView> routeMap;
    private static ApplicationContext instance;

    private ApplicationContext() {
        routeMap = new HashMap<>();
        loadRoutes();
    }

    public static ApplicationContext getInstance() {
        if (instance == null) {
            instance = new ApplicationContext();
        }
        return instance;
    }

    /**
     * Charge toutes les routes du classpath
     */
    private void loadRoutes() {
        // Scanner spécifiquement le package webframe pour trouver les contrôleurs
        List<ModelView> allRoutes = AnnotationScanner.findAllRoutes("webframe");
        for (ModelView route : allRoutes) {
            // Exécuter la méthode pour obtenir la vue réelle
            String actualView = executeMethodForView(route);
            if (actualView != null) {
                // Mettre à jour la vue dans le ModelView
                route.setView(actualView);
            }
            routeMap.put(route.getUrl(), route);
        }
    }

    /**
     * Exécute la méthode du contrôleur pour obtenir le nom de la vue.
     * Utilise la méthode GET par défaut, ou la première méthode disponible.
     */
    private String executeMethodForView(ModelView route) {
        try {
            Object controllerInstance = route.getController().getDeclaredConstructor().newInstance();

            // Chercher une méthode à exécuter (priorité GET, sinon la première disponible)
            java.lang.reflect.Method method = route.getMethod("GET");
            if (method == null && !route.getMethods().isEmpty()) {
                method = route.getMethods().values().iterator().next();
            }

            if (method == null) {
                System.err.println("Aucune méthode trouvée pour la route: " + route.getUrl());
                return route.getView();
            }

            // Si la méthode a des paramètres, créer des valeurs par défaut pour l'initialisation
            Object[] args = new Object[method.getParameterCount()];
            for (int i = 0; i < args.length; i++) {
                Class<?> paramType = method.getParameterTypes()[i];
                args[i] = getDefaultValueForType(paramType);
            }

            Object result = method.invoke(controllerInstance, args);

            if (result instanceof ModelView) {
                ModelView returned = (ModelView) result;
                route.setView(returned.getView());
                for (Map.Entry<String,Object> e : returned.getData().entrySet()) {
                    route.addData(e.getKey(), e.getValue());
                }
                return route.getView();
            } else if (result instanceof String) {
                return result.toString();
            } else {
                System.err.println("Attention: La méthode " + method.getName() +
                                 " ne retourne pas un String. Vue par défaut utilisée.");
                return route.getView(); // Utiliser la vue par défaut
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'exécution de la méthode pour obtenir la vue: " + e.getMessage());
            return route.getView(); // Utiliser la vue par défaut
        }
    }

    /**
     * Trouve une route correspondante à l'URL pour un verbe HTTP spécifique.
     * Supporte les patterns avec paramètres comme /url/{id}.
     *
     * @param url l'URL de la requête
     * @param httpMethod le verbe HTTP (GET, POST, etc.)
     * @return la route correspondante ou null si non trouvée
     */
    public ModelView findRoute(String url, String httpMethod) {
        ModelView route = findRoute(url);
        if (route != null && route.hasMethod(httpMethod)) {
            return route;
        }
        return null;
    }

    /**
     * Trouve une route correspondante à l'URL.
     * Supporte les patterns avec paramètres comme /url/{id}.
     */
    public ModelView findRoute(String url) {
        // D'abord, chercher une correspondance exacte
        ModelView exactMatch = routeMap.get(url);
        if (exactMatch != null) {
            return exactMatch;
        }

        // Ensuite, chercher un pattern qui correspond
        String matchingPattern = UrlPatternMatcher.findMatchingPattern(routeMap, url);
        if (matchingPattern != null) {
            ModelView route = routeMap.get(matchingPattern);
            if (route != null) {
                // Créer une copie du ModelView pour cette requête spécifique
                ModelView specificRoute = new ModelView(url, route.getMethods(), route.getView(), route.getController());

                // Copier les données existantes
                specificRoute.getData().putAll(route.getData());

                // Extraire et ajouter les paramètres d'URL (pour usage futur)
                Map<String, String> urlParams = UrlPatternMatcher.extractParameters(url, matchingPattern);
                for (Map.Entry<String, String> param : urlParams.entrySet()) {
                    specificRoute.addData("urlParam_" + param.getKey(), param.getValue());
                }

                return specificRoute;
            }
        }

        return null;
    }

    /**
     * Retourne toutes les routes chargées
     */
    public Map<String, ModelView> getAllRoutes() {
        return new HashMap<>(routeMap);
    }

    /**
     * Retourne une valeur par défaut pour les types primitifs.
     */
    private static Object getDefaultValueForType(Class<?> type) {
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0.0f;
        if (type == boolean.class) return false;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return '\0';
        return null; // Pour les types objets
    }
}
