package com.github.gdelafosse;

import graphql.schema.idl.TypeDefinitionRegistry;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;

public class GraphQLJavaSourceGenerator {
    private Log log;
    private File outputDirectory;
    private String packageName;

    public GraphQLJavaSourceGenerator(Log log, File outputDirectory, String packageName) {
        this.log = log;
        this.outputDirectory = outputDirectory;
        this.packageName = packageName;
    }


    public void generate(TypeDefinitionRegistry typeDefinitionRegistry) throws IOException {
        File packageDir = mkdir(outputDirectory, packageName);
        log.debug(typeDefinitionRegistry.toString());
    }

    private File mkdir(File outputDirectory, String packageName) throws IOException {
        File packageDir = outputDirectory.toPath().resolve(packageName.replaceAll("\\.", File.separator)).toFile();
        boolean created = packageDir.mkdirs();
        if (!created) {
            throw new IOException(String.format("Fail to create package directory %s.", packageDir.getAbsolutePath()));
        }
        return packageDir;
    }
}
