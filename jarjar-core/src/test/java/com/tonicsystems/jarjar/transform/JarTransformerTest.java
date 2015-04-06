/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tonicsystems.jarjar.transform;

import com.tonicsystems.jarjar.classpath.ClassPath;
import com.tonicsystems.jarjar.transform.JarTransformer;
import com.tonicsystems.jarjar.transform.config.Zap;
import com.tonicsystems.jarjar.transform.jar.DefaultJarProcessor;
import com.tonicsystems.jarjar.transform.jar.PathFilterJarProcessor;
import java.io.File;
import java.util.Arrays;
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

    private File outputJar = new File("build/tmp/output.jar");
    private DefaultJarProcessor processor = new DefaultJarProcessor();
    private JarTransformer transformer = new JarTransformer(outputJar, processor);

    @Test
    public void testTransform() throws Exception {
        LOG.info("j: " + jar);
        LOG.info("ja: " + Arrays.toString(jars));
        ClassPath classPath = new ClassPath(new File("/"), jars);

        processor.setSkipManifest(true);
        processor.add(new PathFilterJarProcessor(Collections.singleton("META-INF/jarjar-testdata.properties")));
        transformer.transform(classPath);
    }

}
