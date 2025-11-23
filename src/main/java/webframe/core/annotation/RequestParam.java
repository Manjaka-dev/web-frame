package webframe.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour mapper les paramètres de requête HTTP aux paramètres de méthode.
 * Peut être utilisée pour les paramètres GET et POST.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface RequestParam {

    /**
     * Le nom du paramètre dans la requête HTTP.
     * Si non spécifié, utilise le nom du paramètre de la méthode.
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
     * @return la valeur par défaut
     */
    String defaultValue() default "";
}
