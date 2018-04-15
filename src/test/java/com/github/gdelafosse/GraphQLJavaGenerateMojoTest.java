package com.github.gdelafosse;

import io.takari.maven.testing.TestMavenRuntime;
import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenExecutionResult;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

public class GraphQLJavaGenerateMojoTest {
    @Rule
    public final TestResources resources = new TestResources();

    @Rule
    public final TestMavenRuntime maven = new TestMavenRuntime();

    @Test
    public void testStarwars() throws Exception {
        File basedir = resources.getBasedir("starwars");
        maven.executeMojo(basedir, "generate");
    }

}