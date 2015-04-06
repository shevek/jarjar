/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tonicsystems.jarjar.transform;

import com.tonicsystems.jarjar.classpath.ClassPath;
import com.tonicsystems.jarjar.transform.jar.DefaultJarProcessor;
import com.tonicsystems.jarjar.transform.jar.PathFilterJarProcessor;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Collections;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class JarTransformerTest extends AbstractJarTransformerTest {

    private static final Logger LOG = LoggerFactory.getLogger(JarTransformerTest.class);

    private final File outputJar = new File("build/tmp/output.jar");
    private final DefaultJarProcessor processor = new DefaultJarProcessor();
    private final JarTransformer transformer = new JarTransformer(outputJar, processor);
    private final ClassPath classPath = new ClassPath(new File("/"), jars);

    /*
     @Test
     public void testTransformNoop() throws Exception {
     processor.setSkipManifest(true);
     processor.add(new PathFilterJarProcessor(Collections.singleton("META-INF/jarjar-testdata.properties")));
     transformer.transform(classPath);
     }
     */
    @Test
    public void testTransformRename() throws Exception {
        processor.setSkipManifest(true);
        processor.add(new PathFilterJarProcessor(Collections.singleton("META-INF/jarjar-testdata.properties")));
        transformer.transform(classPath);

        Method m = getMethod(outputJar, "org.anarres.jarjar.testdata.pkg0.Main", "main", String[].class);
        m.invoke(null, (Object) new String[]{});
    }

}
