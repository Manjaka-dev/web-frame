package webframe.test;

import webframe.core.util.AnnotationScanner;
import webframe.core.tools.ModelView;
import webframe.example.ExampleController;

import java.util.List;

/**
 * Classe de test pour démontrer les nouvelles fonctionnalités.
 */
public class FrameworkTest {

    public static void main(String[] args) {
        System.out.println("=== Test du Framework Web avec nouvelles annotations ===\n");

        // Test 1: Scanner les contrôleurs
        System.out.println("1. Scanner des contrôleurs:");
        List<Class<?>> controllers = AnnotationScanner.findControllerClasses();
        for (Class<?> controller : controllers) {
            System.out.println("   - " + controller.getName());
        }

        // Test 2: Scanner les routes
        System.out.println("\n2. Scanner des routes:");
        List<ModelView> routes = AnnotationScanner.findAllRoutes();
        for (ModelView route : routes) {
            System.out.println("   - URL: " + route.getUrl());
            System.out.println("     Vue: " + route.getView());
            System.out.println("     Contrôleur: " + route.getController().getSimpleName());
            System.out.println("     Verbes HTTP supportés:");
            route.getMethods().forEach((verb, method) ->
                System.out.println("       * " + verb + " -> " + method.getName() + "()")
            );
            System.out.println();
        }

        // Test 3: Test spécifique du contrôleur d'exemple
        System.out.println("3. Test spécifique du contrôleur ExampleController:");
        List<Class<?>> exampleControllers = new java.util.ArrayList<>();
        exampleControllers.add(ExampleController.class);
        List<ModelView> exampleRoutes = AnnotationScanner.findRouterMethods(exampleControllers);

        for (ModelView route : exampleRoutes) {
            System.out.println("   Route: " + route.getUrl());
            System.out.println("   Méthodes HTTP:");
            route.getMethods().forEach((verb, method) -> {
                System.out.println("     - " + verb + ": " + method.getName());
                // Test si la route supporte différents verbes
                if (route.hasMethod("GET")) {
                    System.out.println("       ✓ Supporte GET");
                }
                if (route.hasMethod("POST")) {
                    System.out.println("       ✓ Supporte POST");
                }
                if (route.hasMethod("PUT")) {
                    System.out.println("       ✓ Supporte PUT");
                }
            });
            System.out.println();
        }

        System.out.println("=== Tests terminés avec succès ===");
    }
}
