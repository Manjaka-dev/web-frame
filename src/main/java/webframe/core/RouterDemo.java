package webframe.core;

import webframe.core.util.AnnotationScanner;
import webframe.core.tools.ModelView;

import java.util.List;

/**
 * Classe principale pour tester les nouvelles fonctionnalités.
 */
public class RouterDemo {

    public static void main(String[] args) {
        System.out.println("=== DÉMONSTRATION DU SCANNER DE ROUTES ===");

        try {
            // Test simple du scanner
            System.out.println("Scanner tous les contrôleurs...");
            List<Class<?>> controllers = AnnotationScanner.findControllerClasses();
            System.out.println("Contrôleurs trouvés: " + controllers.size());

            for (Class<?> controller : controllers) {
                System.out.println("- " + controller.getName());
            }

            // Test du scanner de routes
            System.out.println("\nScanner toutes les routes...");
            List<ModelView> routes = AnnotationScanner.findAllRoutes();
            System.out.println("Routes trouvées: " + routes.size());

            for (ModelView route : routes) {
                System.out.println("- " + route.getUrl() + " -> " +
                                 route.getController().getSimpleName() + "." +
                                 route.getMethod().getName() + " (vue: " + route.getView() + ")");
            }

            System.out.println("\n✅ Test terminé avec succès !");

        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
