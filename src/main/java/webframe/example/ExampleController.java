package webframe.example;

import webframe.core.annotation.Controller;
import webframe.core.annotation.Router;
import webframe.core.annotation.GET;
import webframe.core.annotation.POST;
import webframe.core.annotation.RequestParam;
import webframe.core.tools.ModelView;

/**
 * Contrôleur d'exemple démontrant l'utilisation des nouvelles annotations
 * GET, POST et Router avec support des paramètres d'URL.
 */
@Controller(base = "/demo")
public class ExampleController {

    /**
     * Route utilisant l'annotation @GET.
     * URL: /demo/users/{id}
     * Extrait l'ID depuis l'URL et retourne une vue avec ces données.
     */
    @GET("/users/{id}")
    public ModelView getUser(@RequestParam String id) {
        ModelView mv = new ModelView();
        mv.setView("user-details");
        mv.addData("userId", id);
        mv.addData("action", "GET - Affichage utilisateur");
        return mv;
    }

    /**
     * Route utilisant l'annotation @POST.
     * URL: /demo/users
     * Crée un nouvel utilisateur avec les données POST.
     */
    @POST("/users")
    public ModelView createUser(@RequestParam String name,
                               @RequestParam(defaultValue = "user@example.com") String email) {
        ModelView mv = new ModelView();
        mv.setView("user-created");
        mv.addData("userName", name);
        mv.addData("userEmail", email);
        mv.addData("action", "POST - Création utilisateur");
        return mv;
    }

    /**
     * Route utilisant l'annotation @Router avec verbes multiples.
     * URL: /demo/products/{id}
     * Méthodes: GET, POST, PUT
     */
    @Router(value = "/products/{id}", methods = {"GET", "POST", "PUT"})
    public ModelView handleProduct(@RequestParam String id,
                                  @RequestParam(required = false) String action) {
        ModelView mv = new ModelView();
        mv.setView("product-handler");
        mv.addData("productId", id);
        mv.addData("requestedAction", action != null ? action : "default");
        mv.addData("info", "Cette méthode gère GET, POST et PUT pour le produit " + id);
        return mv;
    }

    /**
     * Route utilisant l'annotation @Router sans restriction de verbe.
     * URL: /demo/catch-all/{path}
     * Accepte tous les verbes HTTP.
     */
    @Router("/catch-all/{path}")
    public ModelView catchAll(@RequestParam String path) {
        ModelView mv = new ModelView();
        mv.setView("catch-all");
        mv.addData("capturedPath", path);
        mv.addData("info", "Cette route accepte tous les verbes HTTP");
        return mv;
    }

    /**
     * Route combinant @GET et @POST sur la même URL.
     * Démontre comment une même URL peut avoir des comportements différents
     * selon le verbe HTTP utilisé.
     */
    @GET("/contact")
    public ModelView showContactForm() {
        ModelView mv = new ModelView();
        mv.setView("contact-form");
        mv.addData("action", "GET - Affichage du formulaire");
        return mv;
    }

    @POST("/contact")
    public ModelView submitContactForm(@RequestParam String name,
                                     @RequestParam String message) {
        ModelView mv = new ModelView();
        mv.setView("contact-success");
        mv.addData("contactName", name);
        mv.addData("contactMessage", message);
        mv.addData("action", "POST - Soumission du formulaire");
        return mv;
    }
}
