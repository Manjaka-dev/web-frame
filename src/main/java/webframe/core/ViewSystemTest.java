package webframe.core;

import webframe.core.tools.ModelView;

import java.util.Map;

/**
 * Test simple du nouveau système de vues
 */
public class ViewSystemTest {

    public static void main(String[] args) {
        System.out.println("=== TEST DU NOUVEAU SYSTÈME DE VUES ===\n");

        try {
            // Initialiser le contexte
            ApplicationContext context = ApplicationContext.getInstance();

            // Obtenir toutes les routes
            Map<String, ModelView> routes = context.getAllRoutes();

            System.out.println("📍 Routes chargées dans le contexte :");
            for (Map.Entry<String, ModelView> entry : routes.entrySet()) {
                String url = entry.getKey();
                ModelView route = entry.getValue();

                System.out.println("  " + url + " → Vue: '" + route.getView() + "'");
                System.out.println("    Contrôleur: " + route.getController().getSimpleName());
                System.out.println("    Méthode: " + route.getMethod().getName() + "()");
                System.out.println();
            }

            System.out.println("🔍 Test de recherche de routes :");

            // Tester quelques URLs
            String[] testUrls = {"/demo", "/api/status", "/test", "/inexistante"};

            for (String testUrl : testUrls) {
                ModelView foundRoute = context.findRoute(testUrl);
                if (foundRoute != null) {
                    System.out.println("  ✅ " + testUrl + " → Vue: '" + foundRoute.getView() + "'");
                } else {
                    System.out.println("  ❌ " + testUrl + " → 404 (non trouvée)");
                }
            }

            System.out.println("\n✅ Test terminé avec succès !");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
