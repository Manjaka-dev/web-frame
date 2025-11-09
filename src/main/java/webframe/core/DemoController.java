package webframe.core;

import webframe.core.annotation.Controller;
import webframe.core.annotation.Router;

@Controller
public class DemoController {

    @Router(value = "/demo", view = "demo_page")
    public String demo() {
        return "✅ Page de démonstration - Le framework fonctionne parfaitement !";
    }

    @Router("/api/status")
    public String status() {
        return "✅ API Status: OPÉRATIONNEL\nFramework Web-Frame actif\nScanner d'annotations fonctionnel";
    }

    @Router(value = "/test", view = "test_page")
    public String test() {
        return "🧪 Test réussi !\nCette méthode a été invoquée automatiquement par le framework.";
    }

    @Router("/error-test")
    public Integer errorTest() {
        // Cette méthode retourne un Integer au lieu d'un String
        // Elle devrait déclencher une exception
        return 42;
    }
}
