package webframe.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indiquant qu'une méthode de contrôleur, 
 * ou toutes les méthodes d'un contrôleur, nécessitent 
 * une authentification (utilisateur connecté).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Authorized {
}
