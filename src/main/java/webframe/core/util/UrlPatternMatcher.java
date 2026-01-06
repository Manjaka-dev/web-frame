package webframe.core.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utilitaire pour matcher des patterns d'URL avec des paramètres dynamiques.
 * Cette classe fournit des méthodes pour :
 * <ul>
 *   <li>Normaliser des URLs concrètes en patterns (/users/123 → /users/{int})</li>
 *   <li>Trouver des patterns correspondants dans une collection de routes</li>
 *   <li>Extraire les paramètres de valeur depuis les URLs (/users/123 avec pattern /users/{id})</li>
 *   <li>Supporter différents types de paramètres (int, uuid, string)</li>
 * </ul>
 *
 * Exemples d'utilisation :
 * <pre>
 * // Normalisation d'URL
 * String pattern = UrlPatternMatcher.normalizeUrl("/users/123/profile");
 * // Résultat: "/users/{int}/profile"
 *
 * // Recherche de pattern correspondant
 * Map&lt;String, ModelView&gt; routes = ...; // routes avec patterns comme /users/{id}
 * String matching = UrlPatternMatcher.findMatchingPattern(routes, "/users/123");
 * // Résultat: "/users/{id}" si ce pattern existe dans routes
 *
 * // Extraction de paramètres
 * Map&lt;String, String&gt; params = UrlPatternMatcher.extractParameters("/users/123", "/users/{id}");
 * // Résultat: {"id" → "123"}
 * </pre>
 *
 * Types de paramètres supportés :
 * <ul>
 *   <li><code>{id}</code> - Paramètre nommé, accepte toute valeur</li>
 *   <li><code>{int}</code> - Détecté automatiquement pour les nombres entiers</li>
 *   <li><code>{uuid}</code> - Détecté automatiquement pour les UUIDs</li>
 * </ul>
 *
 * @see webframe.core.ApplicationContext
 * @see webframe.core.tools.ModelView
 * @see webframe.core.util.ParameterResolver
 */
public final class UrlPatternMatcher {

    private UrlPatternMatcher() {}

    /**
     * Normalise une URL concrète en remplaçant les segments numériques par {int}.
     * Exemple: /url/123 → /url/{int}
     */
    public static String normalizeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "/";
        }

        // Enlever les paramètres de requête et le fragment
        String path = url.split("[?#]", 2)[0];
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        String[] segments = path.split("/");
        StringBuilder normalized = new StringBuilder();

        for (String segment : segments) {
            if (segment.isEmpty()) {
                continue;
            }

            normalized.append("/");
            if (isInteger(segment)) {
                normalized.append("{int}");
            } else if (isUuid(segment)) {
                normalized.append("{uuid}");
            } else {
                normalized.append(segment);
            }
        }

        return normalized.length() == 0 ? "/" : normalized.toString();
    }

    /**
     * Vérifie si un segment est un entier.
     */
    private static boolean isInteger(String segment) {
        if (segment == null || segment.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(segment);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Vérifie si un segment est un UUID.
     */
    private static boolean isUuid(String segment) {
        if (segment == null || segment.length() != 36) {
            return false;
        }
        return segment.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }

    /**
     * Convertit un pattern template (ex: /url/{id}) en Pattern regex.
     * Les placeholders {xxx} matchent n'importe quel segment non-vide.
     */
    public static Pattern templateToRegex(String template) {
        if (template == null || template.isEmpty()) {
            return Pattern.compile("^/$");
        }

        String path = template.split("[?#]", 2)[0];
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        // D'abord échapper les caractères spéciaux qui pourraient être dans le path
        String regex = getRegex(path);

        return Pattern.compile("^" + regex + "$");
    }

    private static String getRegex(String path) {
        String escaped = path.replace(".", "\\.")
                            .replace("+", "\\+")
                            .replace("*", "\\*")
                            .replace("?", "\\?")
                            .replace("(", "\\(")
                            .replace(")", "\\)")
                            .replace("^", "\\^")
                            .replace("$", "\\$")
                            .replace("|", "\\|");

        // Puis remplacer les placeholders par des groupes regex (après l'échappement)
        return escaped.replaceAll("\\{[^}]+}", "[^/]+");
    }

    /**
     * Vérifie si une URL correspond à un pattern template.
     * Exemple: matchesPattern("/url/123", "/url/{id}") → true
     */
    public static boolean matchesPattern(String url, String template) {
        Pattern pattern = templateToRegex(template);
        String cleanUrl = url.split("[?#]", 2)[0];
        return pattern.matcher(cleanUrl).matches();
    }

    /**
     * Trouve le pattern template qui correspond à l'URL donnée dans une carte de patterns.
     * Retourne null si aucun pattern ne correspond.
     */
    public static String findMatchingPattern(Map<String, ?> patternMap, String url) {
        String cleanUrl = url.split("[?#]", 2)[0];

        // D'abord, chercher une correspondance exacte
        if (patternMap.containsKey(cleanUrl)) {
            return cleanUrl;
        }

        // Ensuite, tester contre tous les patterns
        for (String pattern : patternMap.keySet()) {
            if (matchesPattern(cleanUrl, pattern)) {
                return pattern;
            }
        }

        // Fallback: comparer les versions normalisées
        String normalizedUrl = normalizeUrl(cleanUrl);
        for (String pattern : patternMap.keySet()) {
            String normalizedPattern = normalizeUrl(pattern);
            if (normalizedUrl.equals(normalizedPattern)) {
                return pattern;
            }
        }

        return null;
    }

    /**
     * Extrait les paramètres d'une URL basée sur un pattern template.
     * Exemple: extractParameters("/url/123", "/url/{id}") → {"id": "123"}
     * Note: Cette méthode retourne les valeurs brutes, sans conversion de type.
     */
    public static Map<String, String> extractParameters(String url, String template) {
        Map<String, String> parameters = new HashMap<>();

        String cleanUrl = url.split("[?#]", 2)[0];
        String cleanTemplate = template.split("[?#]", 2)[0];

        if (!cleanUrl.startsWith("/")) cleanUrl = "/" + cleanUrl;
        if (!cleanTemplate.startsWith("/")) cleanTemplate = "/" + cleanTemplate;

        String[] urlSegments = cleanUrl.split("/");
        String[] templateSegments = cleanTemplate.split("/");

        if (urlSegments.length != templateSegments.length) {
            return parameters; // Pas de match possible
        }

        for (int i = 0; i < templateSegments.length; i++) {
            String templateSegment = templateSegments[i];
            if (templateSegment.startsWith("{") && templateSegment.endsWith("}")) {
                String paramName = templateSegment.substring(1, templateSegment.length() - 1);
                parameters.put(paramName, urlSegments[i]);
            }
        }

        return parameters;
    }

}
