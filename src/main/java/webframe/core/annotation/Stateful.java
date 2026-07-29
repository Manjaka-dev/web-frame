package webframe.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indiquant qu'un contrôleur doit être instancié
 * et conservé au niveau de la session HTTP (Stateful),
 * plutôt que d'être un Singleton partagé par toutes les requêtes.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Stateful {
}
