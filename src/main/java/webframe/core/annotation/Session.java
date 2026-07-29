package webframe.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indiquant qu'un paramètre de type Map<String, Object> 
 * doit recevoir les attributs de la session HTTP courante.
 * Toute modification sur cette Map sera directement répercutée sur la session.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Session {
}
