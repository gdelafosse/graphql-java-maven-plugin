package com.github.gdelafosse;

import graphql.language.Document;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution= ResolutionScope.COMPILE)
@Execute(goal = "generate")
public class GraphQLJavaGenerateMojo extends AbstractMojo {

    @Parameter(readonly = true, defaultValue = "${project}")
    private MavenProject project;

    @Parameter(property = "schemaDirectory", defaultValue = "${project.basedir}")
    private File schemaDirectory;

    @Parameter(name="outputDirectory", defaultValue="target/generated-sources/graphql")
    private File outputDirectory;

    @Parameter(name="packageName", defaultValue = "com.graphql.generated")
    private String packageName;

    public void execute()
            throws MojoExecutionException {
        try {
            getLog().info("Generating Java sources from GraphQL schema");
            getLog().info(String.format("Scanning GraphQL schemas from %s.", schemaDirectory));
            List<Path> schemas = getSchemas();
            getLog().debug(String.format("%d schemas found in %s.", schemas.size(), schemaDirectory));
            schemas.forEach(s -> getLog().debug(s.toString()));

            TypeDefinitionRegistry typeDefinitionRegistry = parseSchemas(schemas);
            generateSources(typeDefinitionRegistry);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void generateSources(TypeDefinitionRegistry typeDefinitionRegistry) throws IOException {
        new GraphQLJavaSourceGenerator(getLog(), outputDirectory, packageName).generate(typeDefinitionRegistry);
    }

    private TypeDefinitionRegistry parseSchemas(List<Path> schemas) {
        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = new TypeDefinitionRegistry();
        for (Path schema : schemas) {
            typeDefinitionRegistry.merge(schemaParser.parse(schema.toFile()));
        }
        return typeDefinitionRegistry;
    }

    private List<Path> getSchemas() throws MojoExecutionException, IOException {
        if (!schemaDirectory.exists()) {
            throw new MojoExecutionException(String.format("%s doesn't exist.", schemaDirectory.getName()));
        }
        return Files.list(Paths.get(schemaDirectory.toURI())).filter(p -> p.toString().endsWith(".graphqls")).collect(Collectors.toList());
    }

}