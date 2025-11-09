package webframe.core.tools;

import java.lang.reflect.Method;

/**
 * Représente une route associant une URL, une méthode de contrôleur et une vue.
 */
public class ModelView {

    private String url;
    private Method method;
    private String view;
    private Class<?> controller;

    public ModelView() {}

    public ModelView(String url, Method method, String view, Class<?> controller) {
        this.url = url;
        this.method = method;
        this.view = view;
        this.controller = controller;
    }

    // Getters
    public String getUrl() {
        return url;
    }

    public Method getMethod() {
        return method;
    }

    public String getView() {
        return view;
    }

    public Class<?> getController() {
        return controller;
    }

    // Setters
    public void setUrl(String url) {
        this.url = url;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public void setView(String view) {
        this.view = view;
    }

    public void setController(Class<?> controller) {
        this.controller = controller;
    }

    @Override
    public String toString() {
        return String.format("ModelView{url='%s', method='%s', view='%s', controller='%s'}",
                           url,
                           method != null ? method.getName() : "null",
                           view,
                           controller != null ? controller.getSimpleName() : "null");
    }
}
