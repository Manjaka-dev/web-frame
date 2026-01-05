package webframe.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation spécialisée pour les routes POST.
 * Cette annotation est un raccourci pour {@code @Router(methods={"POST"})}.
 *
 * Exemple d'utilisation :
 * <pre>
 * {@code @POST("/users")}
 * public ModelView createUser(@RequestParam String name) { ... }
 * </pre>
 *
 * @see Router
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface POST {

    /**
     * L'URL de la route POST. Supporte les paramètres avec la syntaxe {nom}.
     * @return l'URL associée à cette méthode POST
     */
    String value();

    /**
     * La vue à retourner (optionnel).
     * @return le nom de la vue
     */
    String view() default "";
}
