package webframe.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation générique de routing pour les contrôleurs.
 * Permet de définir une route avec des verbes HTTP multiples.
 * Si aucun verbe n'est spécifié, la méthode sera accessible pour tous les verbes HTTP.
 *
 * Exemple d'utilisation :
 * <pre>
 * {@code @Router(value="/users/{id}", methods={"GET", "POST"})}
 * public ModelView handleUser(@RequestParam String id) { ... }
 *
 * {@code @Router("/dashboard")} // Accepte tous les verbes HTTP
 * public ModelView dashboard() { ... }
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Router {

    /**
     * L'URL de la route. Supporte les paramètres avec la syntaxe {nom}.
     * @return l'URL associée à cette méthode
     */
    String value() default "";

    /**
     * La vue à retourner (optionnel).
     * @return le nom de la vue
     */
    String view() default "";

    /**
     * Les verbes HTTP supportés par cette route.
     * Si vide, tous les verbes HTTP seront acceptés.
     * @return tableau des verbes HTTP (GET, POST, PUT, DELETE, etc.)
     */
    String[] methods() default {};
}
