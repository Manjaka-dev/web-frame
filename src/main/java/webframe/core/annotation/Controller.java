package webframe.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour marquer une classe comme contrôleur dans le framework web.
 * Les classes annotées avec {@code @Controller} sont automatiquement détectées
 * lors du scan du classpath et leurs méthodes annotées avec {@code @Router},
 * {@code @GET} ou {@code @POST} sont enregistrées comme routes.
 *
 * Exemple d'utilisation :
 * <pre>
 * {@code @Controller(base = "/api")}
 * public class UserController {
 *
 *     {@code @GET("/users")}
 *     public ModelView listUsers() { ... }
 *
 *     {@code @POST("/users")}
 *     public ModelView createUser() { ... }
 * }
 * </pre>
 *
 * @see Router
 * @see GET
 * @see POST
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Controller {

    /**
     * Préfixe de base pour toutes les routes de ce contrôleur.
     * Si spécifié, toutes les routes définies dans les méthodes de ce contrôleur
     * seront préfixées par cette valeur.
     *
     * @return le préfixe de base des routes (vide par défaut)
     */
    String base() default "";
}
