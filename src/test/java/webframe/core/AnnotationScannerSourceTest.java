package webframe.core;

import junit.framework.TestCase;
import webframe.core.util.AnnotationScanner;

import java.io.File;
import java.util.List;

public class AnnotationScannerSourceTest extends TestCase {

    public void testFindSourceFilesWithController() {
        File baseDir = new File("src/main/java");
        List<File> sources = AnnotationScanner.findSourceFilesWithController(baseDir);
        boolean found = sources.stream().anyMatch(f -> f.getPath().replace('\\','/').endsWith("webframe/example/SampleController.java"));
        assertTrue("Le fichier source SampleController.java devrait être détecté", found);
    }
}

