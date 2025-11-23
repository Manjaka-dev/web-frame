package webframe.core.util;

import webframe.core.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Utilitaire pour résoudre automatiquement les paramètres de méthodes
 * à partir des données de requête HTTP.
 */
public class ParameterResolver {

    /**
     * Résout les paramètres d'une méthode à partir d'une requête HTTP.
     *
     * @param method la méthode dont les paramètres doivent être résolus
     * @param request la requête HTTP contenant les données
     * @return un tableau d'objets représentant les arguments à passer à la méthode
     * @throws IllegalArgumentException si un paramètre requis est manquant ou si la conversion échoue
     */
    public static Object[] resolveParameters(Method method, HttpServletRequest request) {
        return resolveParameters(method, request, null);
    }

    /**
     * Résout les paramètres d'une méthode à partir d'une requête HTTP et de paramètres d'URL.
     *
     * @param method la méthode dont les paramètres doivent être résolus
     * @param request la requête HTTP contenant les données
     * @param urlParameters paramètres extraits de l'URL (ex: {id} -> "123"), peut être null
     * @return un tableau d'objets représentant les arguments à passer à la méthode
     * @throws IllegalArgumentException si un paramètre requis est manquant ou si la conversion échoue
     */
    public static Object[] resolveParameters(Method method, HttpServletRequest request, java.util.Map<String, String> urlParameters) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            RequestParam annotation = param.getAnnotation(RequestParam.class);

            // Déterminer le nom du paramètre à rechercher dans la requête
            String paramName = getParameterName(param, annotation);

            // Récupérer la valeur: d'abord dans les paramètres d'URL, puis dans la requête HTTP
            String paramValue = null;
            if (urlParameters != null && urlParameters.containsKey(paramName)) {
                paramValue = urlParameters.get(paramName);
            } else {
                paramValue = request.getParameter(paramName);
            }

            // Traiter le paramètre selon l'annotation
            args[i] = processParameter(param, annotation, paramValue, paramName);
        }

        return args;
    }

    /**
     * Détermine le nom du paramètre à rechercher dans la requête HTTP.
     */
    private static String getParameterName(Parameter param, RequestParam annotation) {
        if (annotation != null) {
            // Priorité à 'value', puis 'name', puis nom du paramètre Java
            if (!annotation.value().isEmpty()) {
                return annotation.value();
            }
            if (!annotation.name().isEmpty()) {
                return annotation.name();
            }
        }
        return param.getName();
    }

    /**
     * Traite un paramètre individuel en appliquant les règles de l'annotation @RequestParam.
     */
    private static Object processParameter(Parameter param, RequestParam annotation,
                                         String paramValue, String paramName) {
        Class<?> paramType = param.getType();

        // Si le paramètre est null ou vide
        if (paramValue == null || paramValue.trim().isEmpty()) {
            return handleMissingParameter(param, annotation, paramName, paramType);
        }

        // Convertir la valeur au type approprié
        return convertValue(paramValue, paramType, paramName);
    }

    /**
     * Gère les cas où le paramètre est manquant ou vide.
     */
    private static Object handleMissingParameter(Parameter param, RequestParam annotation,
                                               String paramName, Class<?> paramType) {
        if (annotation != null) {
            // Vérifier si le paramètre est requis
            if (annotation.required()) {
                throw new IllegalArgumentException(
                    String.format("Paramètre requis manquant: '%s' pour le paramètre '%s'",
                                paramName, param.getName()));
            }

            // Utiliser la valeur par défaut si elle est spécifiée
            if (!annotation.defaultValue().isEmpty()) {
                return convertValue(annotation.defaultValue(), paramType, paramName);
            }
        }

        // Retourner une valeur par défaut selon le type
        return getDefaultValue(paramType);
    }

    /**
     * Convertit une chaîne de caractères vers le type approprié.
     */
    private static Object convertValue(String value, Class<?> targetType, String paramName) {
        try {
            if (targetType == String.class) {
                return value;
            } else if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(value);
            } else if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(value);
            } else if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(value);
            } else if (targetType == float.class || targetType == Float.class) {
                return Float.parseFloat(value);
            } else if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(value) || "on".equalsIgnoreCase(value) || "1".equals(value);
            } else if (targetType == short.class || targetType == Short.class) {
                return Short.parseShort(value);
            } else if (targetType == byte.class || targetType == Byte.class) {
                return Byte.parseByte(value);
            } else if (targetType == char.class || targetType == Character.class) {
                return !value.isEmpty() ? value.charAt(0) : '\0';
            }

            throw new IllegalArgumentException(
                String.format("Type non supporté pour le paramètre '%s': %s", paramName, targetType.getSimpleName()));

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                String.format("Impossible de convertir '%s' en %s pour le paramètre '%s'",
                            value, targetType.getSimpleName(), paramName));
        }
    }

    /**
     * Retourne une valeur par défaut pour les types primitifs.
     */
    private static Object getDefaultValue(Class<?> type) {
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0.0f;
        if (type == boolean.class) return false;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return '\0';
        return null; // Pour les types objets
    }

    /**
     * Valide qu'un contrôleur utilise correctement @RequestParam.
     * Vérifie que les noms des paramètres dans les annotations correspondent
     * aux noms attendus dans les formulaires HTML.
     *
     * @param controllerClass la classe du contrôleur à valider
     * @return un rapport de validation
     */
    public static ValidationReport validateController(Class<?> controllerClass) {
        ValidationReport report = new ValidationReport();

        for (Method method : controllerClass.getDeclaredMethods()) {
            Parameter[] parameters = method.getParameters();

            for (Parameter param : parameters) {
                RequestParam annotation = param.getAnnotation(RequestParam.class);
                if (annotation != null) {
                    validateParameterAnnotation(param, annotation, method, report);
                }
            }
        }

        return report;
    }

    private static void validateParameterAnnotation(Parameter param, RequestParam annotation,
                                                  Method method, ValidationReport report) {
        String methodName = method.getName();
        String paramName = param.getName();
        String annotationValue = getParameterName(param, annotation);

        // Vérifier les noms de paramètres
        if (!annotationValue.equals(paramName) && annotation.value().isEmpty() && annotation.name().isEmpty()) {
            report.addWarning(String.format(
                "Méthode %s: Le paramètre '%s' n'a pas d'annotation explicite. " +
                "Considérez l'ajout de @RequestParam(\"%s\")",
                methodName, paramName, paramName));
        }

        // Vérifier les valeurs par défaut pour les paramètres non requis
        if (!annotation.required() && annotation.defaultValue().isEmpty() && param.getType().isPrimitive()) {
            report.addWarning(String.format(
                "Méthode %s: Le paramètre '%s' n'est pas requis mais n'a pas de valeur par défaut " +
                "et est de type primitif. Considérez l'ajout d'une defaultValue.",
                methodName, paramName));
        }
    }

    /**
     * Classe pour le rapport de validation.
     */
    public static class ValidationReport {
        private final java.util.List<String> warnings = new java.util.ArrayList<>();
        private final java.util.List<String> errors = new java.util.ArrayList<>();

        public void addWarning(String message) {
            warnings.add(message);
        }

        public void addError(String message) {
            errors.add(message);
        }

        public java.util.List<String> getWarnings() {
            return warnings;
        }

        public java.util.List<String> getErrors() {
            return errors;
        }

        public boolean hasIssues() {
            return !warnings.isEmpty() || !errors.isEmpty();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Rapport de Validation ===\n");

            if (errors.isEmpty() && warnings.isEmpty()) {
                sb.append("✅ Aucun problème détecté\n");
            } else {
                if (!errors.isEmpty()) {
                    sb.append("❌ Erreurs:\n");
                    errors.forEach(e -> sb.append("  - ").append(e).append("\n"));
                }

                if (!warnings.isEmpty()) {
                    sb.append("⚠️  Avertissements:\n");
                    warnings.forEach(w -> sb.append("  - ").append(w).append("\n"));
                }
            }

            return sb.toString();
        }
    }
}
