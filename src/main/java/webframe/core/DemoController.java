package webframe.core;

import webframe.core.annotation.Controller;
import webframe.core.annotation.Router;

@Controller
public class DemoController {

    @Router("/demo")
    public String demo() {
        return "demo_page";  // Nom de la vue
    }

    @Router("/api/status")
    public String status() {
        return "status_view";  // Nom de la vue
    }

    @Router("/test")
    public String test() {
        return "test_result_view";  // Nom de la vue
    }

    @Router("/products")
    public String products() {
        return "product_list_view";  // Nom de la vue
    }

    @Router("/about")
    public String about() {
        return "about_page_view";  // Nom de la vue
    }

    @Router("/error-test")
    public Integer errorTest() {
        // Cette méthode retourne un Integer au lieu d'un String
        // Elle devrait générer une erreur lors du chargement du contexte
        return 42;
    }
}
