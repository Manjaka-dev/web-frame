package webframe.core.tools;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Représente une route associant une URL, des méthodes de contrôleur par verbe HTTP et une vue.
 * Cette classe gère la liaison entre les URLs, les méthodes de contrôleur et les vues,
 * en supportant différents verbes HTTP (GET, POST, etc.).
 */
public class ModelView {

    private String url;
    private Map<String, Method> methods = new HashMap<>();
    private String view;
    private Class<?> controller;
    final private Map<String, Object> data = new HashMap<>();

    /**
     * Constructeur par défaut.
     */
    public ModelView() {}

    /**
     * Constructeur principal avec une map de méthodes par verbe HTTP.
     *
     * @param url l'URL de la route
     * @param methods map associant les verbes HTTP aux méthodes correspondantes
     * @param view le nom de la vue
     * @param controller la classe du contrôleur
     */
    public ModelView(String url, Map<String, Method> methods, String view, Class<?> controller) {
        this.url = url;
        this.methods = new HashMap<>();
        if (methods != null) {
            this.methods.putAll(methods);
        }
        this.view = view;
        this.controller = controller;
    }

    /**
     * Constructeur de compatibilité pour une seule méthode avec verbe spécifique.
     *
     * @param url l'URL de la route
     * @param method la méthode du contrôleur
     * @param view le nom de la vue
     * @param controller la classe du contrôleur
     * @param httpVerb le verbe HTTP spécifique (GET, POST, etc.)
     */
    public ModelView(String url, Method method, String view, Class<?> controller, String httpVerb) {
        this.url = url;
        this.methods = new HashMap<>();
        if (method != null && httpVerb != null) {
            this.methods.put(httpVerb.toUpperCase(), method);
        }
        this.view = view;
        this.controller = controller;
    }

    // Getters

    /**
     * Retourne l'URL de la route.
     *
     * @return l'URL de la route
     */
    public String getUrl() {
        return url;
    }

    /**
     * Retourne la méthode associée à un verbe HTTP spécifique.
     *
     * @param httpMethod le verbe HTTP (GET, POST, etc.)
     * @return la méthode correspondante ou null si non trouvée
     */
    public Method getMethod(String httpMethod) {
        return methods.get(httpMethod.toUpperCase());
    }

    /**
     * Retourne toutes les méthodes associées aux différents verbes HTTP.
     *
     * @return map des méthodes par verbe HTTP
     */
    public Map<String, Method> getMethods() {
        return new HashMap<>(methods);
    }

    /**
     * Retourne le nom de la vue associée à cette route.
     *
     * @return le nom de la vue
     */
    public String getView() {
        return view;
    }

    /**
     * Retourne la classe du contrôleur.
     *
     * @return la classe du contrôleur
     */
    public Class<?> getController() {
        return controller;
    }

    /**
     * Définit le nom de la vue pour cette route.
     *
     * @param view le nom de la vue
     */
    public void setView(String view) {
        this.view = view;
    }

    /**
     * Ajoute une méthode pour un verbe HTTP spécifique.
     *
     * @param httpMethod le verbe HTTP (GET, POST, etc.)
     * @param method la méthode du contrôleur
     */
    public void addMethod(String httpMethod, Method method) {
        this.methods.put(httpMethod.toUpperCase(), method);
    }

    /**
     * Vérifie si une méthode est définie pour un verbe HTTP donné.
     *
     * @param httpMethod le verbe HTTP à vérifier
     * @return true si une méthode est définie pour ce verbe HTTP
     */
    public boolean hasMethod(String httpMethod) {
        return methods.containsKey(httpMethod.toUpperCase());
    }

    @Override
    public String toString() {
        return String.format("ModelView{url='%s', methods=%s, view='%s', controller='%s'}",
                           url,
                           methods.toString(),
                           view,
                           controller != null ? controller.getSimpleName() : "null");
    }

    /**
     * Ajoute des données à la vue.
     *
     * @param key la clé des données
     * @param value la valeur des données
     */
    public void addData(String key, Object value) {
        data.put(key, value);
    }

    /**
     * Retourne toutes les données associées à cette vue.
     *
     * @return map des données
     */
    public Map<String, Object> getData() {
        return data;
    }
}
