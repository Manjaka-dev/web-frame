package webframe.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour marquer les méthodes comme routes dans les contrôleurs.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Router {

    /**
     * L'URL de la route.
     * @return l'URL associée à cette méthode
     */
    String value() default "";

    /**
     * La vue à retourner (optionnel).
     * @return le nom de la vue
     */
    String view() default "";
}
