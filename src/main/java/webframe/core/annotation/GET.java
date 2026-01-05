package webframe.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation spécialisée pour les routes GET.
 * Cette annotation est un raccourci pour {@code @Router(methods={"GET"})}.
 *
 * Exemple d'utilisation :
 * <pre>
 * {@code @GET("/users/{id}")}
 * public ModelView getUser(@RequestParam String id) { ... }
 * </pre>
 *
 * @see Router
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GET {

    /**
     * L'URL de la route GET. Supporte les paramètres avec la syntaxe {nom}.
     * @return l'URL associée à cette méthode GET
     */
    String value();

    /**
     * La vue à retourner (optionnel).
     * @return le nom de la vue
     */
    String view() default "";
}
