package webframe.core;

import junit.framework.TestCase;
import webframe.core.util.AnnotationScanner;

import java.util.List;

public class AnnotationScannerTest extends TestCase {

    public void testFindClassesWithController() {
        List<String> classes = AnnotationScanner.findClassesWithController("webframe");
        assertTrue("SampleController devrait être détecté", classes.contains("webframe.example.SampleController"));
    }
}

