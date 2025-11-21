package webframe.core;

import webframe.core.tools.ModelView;
import webframe.core.util.AnnotationScanner;
import webframe.core.util.UrlPatternMatcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contexte de l'application qui gère le mapping URL -> Vue
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
     * Exécute la méthode du contrôleur pour obtenir le nom de la vue
     */
    private String executeMethodForView(ModelView route) {
        try {
            Object controllerInstance = route.getController().getDeclaredConstructor().newInstance();
            Object result = route.getMethod().invoke(controllerInstance);

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
                System.err.println("Attention: La méthode " + route.getMethod().getName() +
                                 " ne retourne pas un String. Vue par défaut utilisée.");
                return route.getView(); // Utiliser la vue par défaut
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'exécution de " + route.getMethod().getName() +
                             " pour obtenir la vue: " + e.getMessage());
            return route.getView(); // Utiliser la vue par défaut
        }
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
                ModelView specificRoute = new ModelView(url, route.getMethod(), route.getView(), route.getController());

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
}
