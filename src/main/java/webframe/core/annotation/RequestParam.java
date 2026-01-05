package webframe.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour mapper les paramètres de requête HTTP aux paramètres de méthode.
 * Supporte les paramètres GET, POST et les paramètres extraits des URLs avec la syntaxe {nom}.
 *
 * Exemples d'utilisation :
 * <pre>
 * // Paramètre de requête classique (?name=value)
 * {@code @GET("/search")}
 * public ModelView search(@RequestParam String query) { ... }
 *
 * // Paramètre extrait de l'URL (/users/{id})
 * {@code @GET("/users/{id}")}
 * public ModelView getUser(@RequestParam String id) { ... }
 *
 * // Paramètre avec nom spécifique et valeur par défaut
 * {@code @GET("/products")}
 * public ModelView listProducts(@RequestParam(name = "page", defaultValue = "1") int page) { ... }
 * </pre>
 *
 * @see Controller
 * @see Router
 * @see GET
 * @see POST
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface RequestParam {

    /**
     * Le nom du paramètre dans la requête HTTP ou dans l'URL.
     * Si non spécifié, utilise le nom du paramètre de la méthode.
     *
     * Pour les paramètres d'URL de type /users/{id}, ce nom doit correspondre
     * au nom entre accolades dans le pattern de l'URL.
     *
     * @return le nom du paramètre
     */
    String value() default "";

    /**
     * Le nom du paramètre dans la requête HTTP (alias pour value).
     * @return le nom du paramètre
     */
    String name() default "";

    /**
     * Indique si le paramètre est requis.
     * @return true si le paramètre est obligatoire, false sinon
     */
    boolean required() default true;

    /**
     * Valeur par défaut si le paramètre n'est pas présent.
     * Utilisée uniquement si required = false.
     * @return la valeur par défaut
     */
    String defaultValue() default "";
}
