package webframe.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation définissant le rôle requis pour exécuter une méthode 
 * (ou n'importe quelle méthode d'une classe).
 * S'utilise généralement en complément de @Authorized.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Role {
    String value();
}
