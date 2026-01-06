package webframe.core.util;

import webframe.core.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilitaire pour résoudre automatiquement les paramètres de méthodes de contrôleur
 * à partir des données de requête HTTP et des paramètres d'URL.
 * Cette classe permet l'injection automatique de paramètres dans les méthodes
 * de contrôleur en supportant :
 * <ul>
 *   <li>Paramètres de requête HTTP classiques (?name=value)</li>
 *   <li>Paramètres extraits des URLs avec pattern (/users/{id})</li>
 *   <li>Conversion automatique vers les types primitifs Java</li>
 *   <li>Gestion des paramètres optionnels avec valeurs par défaut</li>
 *   <li>Support de l'annotation {@code @RequestParam}</li>
 *   <li><strong>Support des Map&lt;String, Object&gt; pour capturer tous les paramètres de formulaire</strong></li>
 *   <li><strong>Support des objets complexes avec injection automatique de champs</strong></li>
 *   <li><strong>Support des objets imbriqués avec notation pointée (emp.dept.nom)</strong></li>
 * </ul>
 *
 * Exemples d'utilisation :
 * <pre>
 * // Paramètres individuels
 * {@code @GET("/users/{id}")}
 * public ModelView getUser(@RequestParam String id, @RequestParam(defaultValue = "1") int page) {
 *     // id sera extrait de l'URL, page des paramètres de requête ou défaut à 1
 * }
 *
 * // Map pour capturer tous les paramètres d'un formulaire
 * {@code @POST("/users")}
 * public ModelView createUser(Map&lt;String, Object&gt; formData) {
 *     String name = (String) formData.get("name");
 *     String email = (String) formData.get("email");
 *     Integer age = (Integer) formData.get("age");
 *     // Tous les champs du formulaire sont automatiquement disponibles
 * }
 *
 * // Objets complexes avec injection automatique
 * {@code @POST("/employees")}
 * public ModelView saveEmployee(Employe emp, Departement dept) {
 *     // Les champs emp.nom, emp.poste, dept.nom, etc. sont automatiquement injectés
 *     // depuis les paramètres du formulaire HTML
 * }
 *
 * // Objets imbriqués avec notation pointée
 * {@code @POST("/employees")}
 * public ModelView saveEmployeeWithDept(Employe emp) {
 *     // Support des champs comme emp.nom, emp.dept.nom, emp.dept.code
 *     // L'objet Departement est automatiquement créé et assigné à emp.dept
 * }
 *
 * // Combinaison des approches
 * {@code @POST("/users/{id}")}
 * public ModelView updateUser(@RequestParam String id, User user, Map&lt;String, Object&gt; extraData) {
 *     // id extrait de l'URL, user créé depuis les paramètres, extraData contient tout
 * }
 *
 * // Résolution automatique dans le DispatcherServlet
 * Object[] args = ParameterResolver.resolveParameters(method, request, urlParams);
 * Object result = method.invoke(controllerInstance, args);
 * </pre>
 *
 * <h3>Convention pour les objets complexes :</h3>
 * <p>Les champs HTML doivent suivre la convention de nommage suivante :</p>
 * <pre>
 * &lt;input name="emp.nom" value="Dupont" /&gt;
 * &lt;input name="emp.poste" value="Développeur" /&gt;
 * &lt;input name="emp.dept.nom" value="IT" /&gt;
 * &lt;input name="emp.dept.code" value="DEV" /&gt;
 * </pre>
 * <p>Correspond à :</p>
 * <pre>
 * public class Employe {
 *     private String nom;
 *     private String poste;
 *     private Departement dept;
 *     // constructeurs, getters/setters...
 * }
 *
 * public class Departement {
 *     private String nom;
 *     private String code;
 *     // constructeurs, getters/setters...
 * }
 * </pre>
 *
 * @see webframe.core.annotation.RequestParam
 * @see webframe.core.DispatcherServlet
 * @see webframe.core.util.UrlPatternMatcher
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

        // Créer une seule fois la map de tous les paramètres du formulaire pour la réutiliser
        Map<String, Object> allFormParameters = null;

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> paramType = param.getType();

            // Vérifier si le paramètre est une Map<String, Object> pour capturer tous les paramètres
            if (isMapStringObject(param)) {
                if (allFormParameters == null) {
                    allFormParameters = createFormParametersMap(request, urlParameters);
                }
                args[i] = allFormParameters;
                continue;
            }

            // Vérifier si c'est un objet complexe (classe personnalisée)
            if (isCustomObject(paramType)) {
                if (allFormParameters == null) {
                    allFormParameters = createFormParametersMap(request, urlParameters);
                }
                args[i] = createObjectFromParameters(paramType, allFormParameters, param.getName());
                continue;
            }

            // Traitement classique pour les paramètres individuels avec @RequestParam
            RequestParam annotation = param.getAnnotation(RequestParam.class);
            String paramName = getParameterName(param, annotation);

            // Récupérer la valeur: d'abord dans les paramètres d'URL, puis dans la requête HTTP
            String paramValue;
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
     * Vérifie si un paramètre est de type Map<String, Object>.
     */
    private static boolean isMapStringObject(Parameter param) {
        Class<?> paramType = param.getType();

        // Vérifier si c'est une Map
        if (!Map.class.isAssignableFrom(paramType)) {
            return false;
        }

        // Vérifier les types génériques si disponibles
        Type genericType = param.getParameterizedType();
        if (genericType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            Type[] typeArguments = parameterizedType.getActualTypeArguments();

            if (typeArguments.length == 2) {
                // Vérifier que c'est Map<String, Object>
                return typeArguments[0] == String.class && typeArguments[1] == Object.class;
            }
        }

        // Si pas d'informations génériques, accepter quand même les Map
        return true;
    }

    /**
     * Détermine si une classe représente un objet complexe personnalisé
     * qui peut être créé à partir des paramètres de formulaire.
     */
    private static boolean isCustomObject(Class<?> type) {
        // Exclure les types primitifs, wrapper et collections Java standard
        if (type.isPrimitive()) return false;
        if (type == String.class) return false;
        if (type == Integer.class || type == int.class) return false;
        if (type == Long.class || type == long.class) return false;
        if (type == Double.class || type == double.class) return false;
        if (type == Float.class || type == float.class) return false;
        if (type == Boolean.class || type == boolean.class) return false;
        if (type == Short.class || type == short.class) return false;
        if (type == Byte.class || type == byte.class) return false;
        if (type == Character.class || type == char.class) return false;
        if (Map.class.isAssignableFrom(type)) return false;
        if (java.util.Collection.class.isAssignableFrom(type)) return false;
        if (type.getPackage() != null && type.getPackage().getName().startsWith("java.")) return false;
        if (type.getPackage() != null && type.getPackage().getName().startsWith("jakarta.")) return false;

        // Vérifier qu'il a un constructeur par défaut
        try {
            type.getDeclaredConstructor();
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * Crée une instance d'objet complexe à partir des paramètres de formulaire.
     * Cette méthode supporte les objets imbriqués en utilisant une notation pointée.
     *
     * Par exemple, pour un objet Employe avec un champ Departement :
     * - emp.nom -> définit le nom de l'employé
     * - emp.dept.nom -> définit le nom du département de l'employé
     *
     * @param objectType le type de classe à créer
     * @param allParameters tous les paramètres disponibles
     * @param parameterPrefix le préfixe du paramètre (nom du paramètre dans la méthode)
     * @return une instance de l'objet créé et populé
     */
    private static Object createObjectFromParameters(Class<?> objectType, Map<String, Object> allParameters, String parameterPrefix) {
        try {
            // Créer une instance de l'objet
            Constructor<?> constructor = objectType.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object instance = constructor.newInstance();

            // Obtenir tous les champs de la classe
            Field[] fields = objectType.getDeclaredFields();

            for (Field field : fields) {
                field.setAccessible(true);
                Class<?> fieldType = field.getType();
                String fieldName = field.getName();

                // Chercher des paramètres qui correspondent à ce champ
                // Format attendu: parameterPrefix.fieldName (ex: emp.nom)
                String fullFieldName = parameterPrefix + "." + fieldName;

                Object value = null;

                // Vérifier si on a une valeur directe pour ce champ
                if (allParameters.containsKey(fullFieldName)) {
                    value = allParameters.get(fullFieldName);
                    // Convertir au type approprié si nécessaire
                    value = convertValueToType(value, fieldType);
                } else if (allParameters.containsKey(fieldName)) {
                    // Essayer aussi sans le préfixe (format direct)
                    value = allParameters.get(fieldName);
                    value = convertValueToType(value, fieldType);
                }

                // Si c'est un objet complexe imbriqué, le créer récursivement
                if (value == null && isCustomObject(fieldType)) {
                    // Chercher des paramètres qui commencent par le préfixe de ce champ
                    Map<String, Object> nestedParameters = new HashMap<>();
                    String nestedPrefix = parameterPrefix + "." + fieldName;

                    for (Map.Entry<String, Object> entry : allParameters.entrySet()) {
                        if (entry.getKey().startsWith(nestedPrefix + ".")) {
                            nestedParameters.put(entry.getKey(), entry.getValue());
                        }
                    }

                    if (!nestedParameters.isEmpty()) {
                        value = createObjectFromParameters(fieldType, allParameters, nestedPrefix);
                    }
                }

                // Définir la valeur du champ si on en a trouvé une
                if (value != null) {
                    field.set(instance, value);
                }
            }

            return instance;

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création de l'objet " + objectType.getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Convertit une valeur vers le type spécifié.
     */
    private static Object convertValueToType(Object value, Class<?> targetType) {
        if (value == null) return null;

        // Si le type est déjà correct
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        // Convertir depuis String
        String stringValue = value.toString();

        try {
            if (targetType == String.class) {
                return stringValue;
            } else if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(stringValue);
            } else if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(stringValue);
            } else if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(stringValue);
            } else if (targetType == float.class || targetType == Float.class) {
                return Float.parseFloat(stringValue);
            } else if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(stringValue) || "on".equalsIgnoreCase(stringValue) || "1".equals(stringValue);
            } else if (targetType == short.class || targetType == Short.class) {
                return Short.parseShort(stringValue);
            } else if (targetType == byte.class || targetType == Byte.class) {
                return Byte.parseByte(stringValue);
            } else if (targetType == char.class || targetType == Character.class) {
                return !stringValue.isEmpty() ? stringValue.charAt(0) : '\0';
            }

            // Si aucune conversion n'est possible, retourner la valeur originale
            return value;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                String.format("Impossible de convertir '%s' en %s", stringValue, targetType.getSimpleName()));
        }
    }

    /**
     * Crée une map contenant tous les paramètres du formulaire avec conversion automatique des types.
     */
    private static Map<String, Object> createFormParametersMap(HttpServletRequest request, Map<String, String> urlParameters) {
        Map<String, Object> formData = new HashMap<>();

        // Ajouter tous les paramètres de la requête HTTP
        if (request.getParameterMap() != null) {
            for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
                String paramName = entry.getKey();
                String[] values = entry.getValue();

                if (values != null && values.length > 0) {
                    if (values.length == 1) {
                        // Un seul paramètre - convertir automatiquement le type
                        formData.put(paramName, convertToAppropriateType(values[0]));
                    } else {
                        // Paramètres multiples - garder comme tableau de String
                        formData.put(paramName, values);
                    }
                }
            }
        }

        // Ajouter les paramètres d'URL (ils écrasent les paramètres de requête s'ils existent)
        if (urlParameters != null) {
            for (Map.Entry<String, String> entry : urlParameters.entrySet()) {
                formData.put(entry.getKey(), convertToAppropriateType(entry.getValue()));
            }
        }

        return formData;
    }

    /**
     * Convertit automatiquement une chaîne vers le type le plus approprié.
     */
    private static Object convertToAppropriateType(String value) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }

        String trimmed = value.trim();

        // Tenter conversion en boolean
        if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed) ||
            "on".equalsIgnoreCase(trimmed) || "off".equalsIgnoreCase(trimmed) ||
            "1".equals(trimmed) || "0".equals(trimmed)) {
            return Boolean.parseBoolean(trimmed) || "on".equalsIgnoreCase(trimmed) || "1".equals(trimmed);
        }

        // Tenter conversion en entier
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            // Pas un entier
        }

        // Tenter conversion en double
        try {
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException e) {
            // Pas un double
        }

        // Rester en String par défaut
        return value;
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
