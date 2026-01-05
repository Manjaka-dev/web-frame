package webframe.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour capturer des URLs spécifiques dans les contrôleurs.
 * Cette annotation permet de définir un gestionnaire pour une URL particulière,
 * généralement utilisée pour des cas spéciaux ou des intercepteurs.
 *
 * Exemple d'utilisation :
 * <pre>
 * {@code @Controller}
 * public class ErrorController {
 *
 *     {@code @UrlCatcher(url = "/404")}
 *     public ModelView handleNotFound() {
 *         return new ModelView("error-404", null, "error-404", this.getClass());
 *     }
 * }
 * </pre>
 *
 * @see Controller
 * @see Router
 * @deprecated Utilisez plutôt {@link Router}, {@link GET} ou {@link POST}
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface UrlCatcher {

    /**
     * L'URL à capturer.
     * @return l'URL à intercepter (par défaut "/")
     */
    String url() default "/";
}
