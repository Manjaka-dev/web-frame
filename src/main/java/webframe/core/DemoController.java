package webframe.core;

import webframe.core.annotation.Controller;
import webframe.core.annotation.Router;

@Controller
public class DemoController {

    @Router(value = "/demo", view = "demo_page")
    public String demo() {
        return "Page de démonstration";
    }

    @Router("/api/status")
    public String status() {
        return "API Status OK";
    }
}
