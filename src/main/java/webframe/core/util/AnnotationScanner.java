package webframe.core.util;

import webframe.core.annotation.Controller;
import webframe.core.annotation.Router;
import webframe.core.annotation.GET;
import webframe.core.annotation.POST;
import webframe.core.tools.ModelView;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utilitaire de scan pour découvrir automatiquement les contrôleurs et leurs routes annotées.
 *
 * Cette classe fournit des méthodes statiques pour scanner le classpath et découvrir :
 * <ul>
 *   <li>Les classes annotées avec {@code @Controller}</li>
 *   <li>Les méthodes annotées avec {@code @Router}, {@code @GET}, ou {@code @POST}</li>
 *   <li>La création automatique d'objets {@link ModelView} pour le routing</li>
 * </ul>
 *
 * Le scanner supporte :
 * <ul>
 *   <li>Scan complet du classpath ou d'un package spécifique</li>
 *   <li>Fonctionnement avec des JARs et des répertoires</li>
 *   <li>Gestion des annotations multiples sur une même méthode</li>
 *   <li>Support des verbes HTTP multiples avec {@code @Router}</li>
 *   <li>Exclusion automatique des classes synthétiques et internes</li>
 * </ul>
 *
 * Exemples d'utilisation :
 * <pre>
 * // Scanner tous les contrôleurs du classpath
 * List&lt;Class&lt;?&gt;&gt; controllers = AnnotationScanner.findControllerClasses();
 *
 * // Scanner un package spécifique
 * List&lt;Class&lt;?&gt;&gt; controllers = AnnotationScanner.findControllerClasses("com.monapp.controllers");
 *
 * // Scanner toutes les routes automatiquement
 * List&lt;ModelView&gt; routes = AnnotationScanner.findAllRoutes();
 *
 * // Scanner les routes d'un package spécifique
 * List&lt;ModelView&gt; routes = AnnotationScanner.findAllRoutes("com.monapp.controllers");
 *
 * // Scanner les méthodes de contrôleurs spécifiques
 * List&lt;ModelView&gt; routes = AnnotationScanner.findRouterMethods(controllerClasses);
 * </pre>
 *
 * Annotations supportées :
 * <ul>
 *   <li>{@code @Controller} - Marque une classe comme contrôleur</li>
 *   <li>{@code @Router} - Route générique avec support multi-verbes</li>
 *   <li>{@code @GET} - Route spécifique GET</li>
 *   <li>{@code @POST} - Route spécifique POST</li>
 * </ul>
 *
 * @see webframe.core.annotation.Controller
 * @see webframe.core.annotation.Router
 * @see webframe.core.annotation.GET
 * @see webframe.core.annotation.POST
 * @see webframe.core.tools.ModelView
 */
public final class AnnotationScanner {

    private AnnotationScanner() {}

    /**
     * Recherche toutes les classes du projet qui sont annotées avec {@link Controller}.
     * Scanne automatiquement tout le classpath.
     * @return la liste des classes annotées avec @Controller
     */
    public static List<Class<?>> findControllerClasses() {
        return findControllerClasses(null);
    }

    /**
     * Recherche toutes les classes du classpath sous le package fourni qui sont annotées avec {@link Controller}.
     * @param basePackage package de base à scanner (ex: "webframe"), null pour scanner tout le classpath
     * @return la liste des classes annotées avec @Controller
     */
    public static List<Class<?>> findControllerClasses(String basePackage) {
        List<String> classNames = findClassesWithController(basePackage);
        List<Class<?>> classes = new ArrayList<>();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className, false, cl);
                if (clazz.getAnnotation(Controller.class) != null) {
                    classes.add(clazz);
                }
            } catch (Throwable ignored) {
                // Classe non chargeable: on ignore
            }
        }

        return classes;
    }

    /**
     * Recherche toutes les classes du classpath sous le package fourni qui sont annotées avec {@link Controller}.
     * @param basePackage package de base à scanner (ex: "webframe"), null pour scanner tout le classpath
     * @return la liste des noms de classes pleinement qualifiés trouvés
     */
    public static List<String> findClassesWithController(String basePackage) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Set<String> classNames = new HashSet<>();

        // Si basePackage est null ou vide, on scanne tout le classpath
        boolean scanAll = (basePackage == null || basePackage.trim().isEmpty());
        String packagePath = scanAll ? "" : basePackage.replace('.', '/');

        try {
            if (scanAll) {
                // Scan complet du classpath
                scanFullClasspath(cl, classNames);
            } else {
                // 1) Ressources de type dossier sur le classpath
                Enumeration<URL> resources = cl.getResources(packagePath);
                while (resources.hasMoreElements()) {
                    URL url = resources.nextElement();
                    String protocol = url.getProtocol();
                    if ("file".equals(protocol)) {
                        scanDirectoryURL(url, basePackage, classNames);
                    } else if ("jar".equals(protocol)) {
                        scanJarURL(url, packagePath, classNames);
                    }
                }

                // 2) Parcours complémentaire du classpath (dossiers et jars) pour robustesse
                String cp = System.getProperty("java.class.path");
                if (cp != null) {
                    String[] entries = cp.split(java.io.File.pathSeparator);
                    for (String entry : entries) {
                        File file = new File(entry);
                        if (file.isDirectory()) {
                            File baseDir = new File(file, packagePath);
                            if (baseDir.isDirectory()) {
                                scanDirectory(baseDir, basePackage, classNames);
                            }
                        } else if (entry.endsWith(".jar") && file.isFile()) {
                            try (JarFile jarFile = new JarFile(file)) {
                                scanJarFile(jarFile, packagePath, classNames);
                            } catch (IOException ignored) {
                                // On ignore les jars illisibles
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            // En cas d'erreur d'accès aux ressources, on retourne ce qui a pu être trouvé
        }

        // Filtrer par annotation réellement présente
        List<String> annotated = new ArrayList<>();
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className, false, cl);
                if (clazz.getAnnotation(Controller.class) != null) {
                    // exclure les classes internes synthétiques
                    if (clazz.getName().contains("$")) continue;
                    annotated.add(className);
                }
            } catch (Throwable ignored) {
                // Class non chargeable: on ignore
            }
        }

        Collections.sort(annotated);
        return annotated;
    }

    /**
     * Scanne les méthodes annotées avec @Router, @GET ou @POST dans une liste de classes de contrôleurs.
     * @param controllerClasses liste des classes de contrôleurs à scanner
     * @return liste des ModelView représentant les routes trouvées
     */
    public static List<ModelView> findRouterMethods(List<Class<?>> controllerClasses) {
        List<ModelView> routes = new ArrayList<>();

        if (controllerClasses == null) {
            return routes;
        }

        for (Class<?> controllerClass : controllerClasses) {
            // Vérifier que c'est bien un contrôleur
            if (controllerClass.getAnnotation(Controller.class) == null) {
                continue;
            }

            // Scanner toutes les méthodes de la classe
            Method[] methods = controllerClass.getDeclaredMethods();
            for (Method method : methods) {
                ModelView modelView = processMethodAnnotations(method, controllerClass);
                if (modelView != null) {
                    routes.add(modelView);
                }
            }
        }

        return routes;
    }

    /**
     * Traite les annotations d'une méthode pour créer un ModelView.
     * Supporte @Router (avec verbes multiples ou tous), @GET et @POST.
     *
     * @param method la méthode à analyser
     * @param controllerClass la classe du contrôleur
     * @return ModelView créé ou null si aucune annotation de route trouvée
     */
    private static ModelView processMethodAnnotations(Method method, Class<?> controllerClass) {
        Router routerAnnotation = method.getAnnotation(Router.class);
        GET getAnnotation = method.getAnnotation(GET.class);
        POST postAnnotation = method.getAnnotation(POST.class);

        // Si aucune annotation de route, retourner null
        if (routerAnnotation == null && getAnnotation == null && postAnnotation == null) {
            return null;
        }

        String url = null;
        String fallbackView = null;
        Map<String, Method> methodMap = new HashMap<>();

        // Traitement de @Router
        if (routerAnnotation != null) {
            url = routerAnnotation.value();
            String annotationView = routerAnnotation.view();
            fallbackView = (annotationView != null && !annotationView.trim().isEmpty())
                ? annotationView : method.getName();

            String[] supportedMethods = routerAnnotation.methods();
            if (supportedMethods.length == 0) {
                // Si aucun verbe spécifié, accepter tous les verbes HTTP principaux
                methodMap.put("GET", method);
                methodMap.put("POST", method);
                methodMap.put("PUT", method);
                methodMap.put("DELETE", method);
                methodMap.put("PATCH", method);
            } else {
                // Ajouter seulement les verbes spécifiés
                for (String httpMethod : supportedMethods) {
                    methodMap.put(httpMethod.toUpperCase(), method);
                }
            }
        }

        // Traitement de @GET
        if (getAnnotation != null) {
            if (url == null) {
                url = getAnnotation.value();
            }
            String annotationView = getAnnotation.view();
            if (annotationView != null && !annotationView.trim().isEmpty()) {
                fallbackView = annotationView;
            }
            if (fallbackView == null) {
                fallbackView = method.getName();
            }
            methodMap.put("GET", method);
        }

        // Traitement de @POST
        if (postAnnotation != null) {
            if (url == null) {
                url = postAnnotation.value();
            }
            String annotationView = postAnnotation.view();
            if (annotationView != null && !annotationView.trim().isEmpty()) {
                fallbackView = annotationView;
            }
            if (fallbackView == null) {
                fallbackView = method.getName();
            }
            methodMap.put("POST", method);
        }

        // Créer le ModelView avec la map des méthodes
        return new ModelView(url, methodMap, fallbackView, controllerClass);
    }

    /**
     * Scanne automatiquement tous les contrôleurs et leurs routes @Router.
     * @return liste des ModelView représentant toutes les routes trouvées
     */
    public static List<ModelView> findAllRoutes() {
        List<Class<?>> controllers = findControllerClasses();
        return findRouterMethods(controllers);
    }

    /**
     * Scanne les contrôleurs d'un package spécifique et leurs routes @Router.
     * @param basePackage package de base à scanner
     * @return liste des ModelView représentant les routes trouvées dans ce package
     */
    public static List<ModelView> findAllRoutes(String basePackage) {
        List<Class<?>> controllers = findControllerClasses(basePackage);
        return findRouterMethods(controllers);
    }

    /**
     * Scanne tout le classpath pour trouver toutes les classes.
     */
    private static void scanFullClasspath(ClassLoader cl, Set<String> classNames) {
        // Parcours du classpath complet
        String cp = System.getProperty("java.class.path");
        if (cp != null) {
            String[] entries = cp.split(java.io.File.pathSeparator);
            for (String entry : entries) {
                File file = new File(entry);
                if (file.isDirectory()) {
                    scanDirectoryRecursive(file, "", classNames);
                } else if (entry.endsWith(".jar") && file.isFile()) {
                    try (JarFile jarFile = new JarFile(file)) {
                        scanJarFileComplete(jarFile, classNames);
                    } catch (IOException ignored) {
                        // On ignore les jars illisibles
                    }
                }
            }
        }
    }

    /**
     * Scanne un dossier récursivement pour toutes les classes.
     */
    private static void scanDirectoryRecursive(File dir, String currentPackage, Set<String> classNames) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                String nextPackage = currentPackage.isEmpty() ? file.getName() : currentPackage + "." + file.getName();
                scanDirectoryRecursive(file, nextPackage, classNames);
            } else if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
                String simple = file.getName().substring(0, file.getName().length() - 6); // enlever .class
                String className = currentPackage.isEmpty() ? simple : currentPackage + "." + simple;
                classNames.add(className);
            }
        }
    }

    /**
     * Scanne un JAR complet pour toutes les classes.
     */
    private static void scanJarFileComplete(JarFile jarFile, Set<String> classNames) {
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.endsWith(".class") && !name.contains("$") && !entry.isDirectory()) {
                String className = name.replace('/', '.').substring(0, name.length() - 6);
                classNames.add(className);
            }
        }
    }

    /**
     * Recherche tous les fichiers sources .java sous le dossier donné qui utilisent l'annotation Controller.
     * Heuristique: présence de "@Controller" avec import adéquat, ou "@webframe.core.annotation.Controller".
     * @param baseDir dossier racine (ex: src/main/java)
     * @return liste des fichiers sources correspondants
     */
    public static List<File> findSourceFilesWithController(File baseDir) {
        if (baseDir == null || !baseDir.isDirectory()) {
            return Collections.emptyList();
        }
        List<File> result = new ArrayList<>();
        collectJavaSources(baseDir, result);
        List<File> filtered = new ArrayList<>();
        for (File f : result) {
            try {
                // Lecture rapide du contenu
                String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                if (mentionsControllerAnnotation(content)) {
                    filtered.add(f);
                }
            } catch (IOException ignored) {
                // ignorer fichiers illisibles
            }
        }
        return filtered;
    }

    private static boolean mentionsControllerAnnotation(String content) {
        if (content == null) return false;
        // Simpliste mais suffisant pour la majorité des cas de test
        boolean hasSimple = content.contains("@Controller") &&
                (content.contains("import webframe.core.annotation.Controller") ||
                 content.contains("@webframe.core.annotation.Controller"));
        boolean hasFqn = content.contains("@webframe.core.annotation.Controller");
        return hasSimple || hasFqn;
    }

    private static void collectJavaSources(File dir, List<File> out) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                collectJavaSources(f, out);
            } else if (f.getName().endsWith(".java")) {
                out.add(f);
            }
        }
    }

    private static void scanDirectoryURL(URL url, String basePackage, Set<String> out) {
        try {
            // Décoder correctement les chemins (espaces, etc.)
            String decoded = URLDecoder.decode(url.getFile(), "UTF-8");
            File dir;
            try {
                dir = new File(new URI("file:" + decoded));
            } catch (Exception ex) {
                dir = new File(decoded);
            }
            if (dir.isDirectory()) {
                scanDirectory(dir, basePackage, out);
            }
        } catch (Exception e) {
            File dir = new File(url.getPath());
            if (dir.isDirectory()) {
                scanDirectory(dir, basePackage, out);
            }
        }
    }

    private static void scanDirectory(File baseDir, String currentPackage, Set<String> out) {
        File[] files = baseDir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                String nextPackage = currentPackage + "." + file.getName();
                scanDirectory(file, nextPackage, out);
            } else if (file.getName().endsWith(".class")) {
                String simple = file.getName().substring(0, file.getName().length() - 6); // enlever .class
                String className = currentPackage + "." + simple;
                out.add(className);
            }
        }
    }

    private static void scanJarURL(URL url, String packagePath, Set<String> out) {
        try {
            JarURLConnection conn = (JarURLConnection) url.openConnection();
            try (JarFile jarFile = conn.getJarFile()) {
                scanJarFile(jarFile, packagePath, out);
            }
        } catch (IOException | ClassCastException ignored) {
            // Pas un jar exploitable
        }
    }

    private static void scanJarFile(JarFile jarFile, String packagePath, Set<String> out) {
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry e = entries.nextElement();
            String name = e.getName();
            if (name.startsWith(packagePath) && name.endsWith(".class") && !name.contains("$")) {
                String className = name.replace('/', '.').substring(0, name.length() - 6);
                out.add(className);
            }
        }
    }
}
