package com.github.gdelafosse;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import graphql.language.EnumTypeDefinition;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.apache.maven.plugin.logging.Log;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class GraphQLJavaSourceGenerator {
    private Log log;
    private File outputDirectory;
    private String packageName;
    private File packageDirectory;


    public GraphQLJavaSourceGenerator(Log log, File outputDirectory, String packageName) {
        this.log = log;
        this.outputDirectory = outputDirectory;
        this.packageName = packageName;
    }

    public void generate(TypeDefinitionRegistry typeDefinitionRegistry) throws IOException {
        this.packageDirectory = mkdir();
        log.debug(String.format("Generating sources in %s", packageDirectory));

        generateEnums(typeDefinitionRegistry.getTypes(EnumTypeDefinition.class));
    }

    private File mkdir() throws IOException {
        File packageDir = outputDirectory.toPath().resolve(packageName.replaceAll("\\.", File.separator)).toFile();
        boolean created = packageDir.mkdirs();
        if (!created) {
            throw new IOException(String.format("Fail to create package directory %s.", packageDir.getAbsolutePath()));
        }
        return packageDir;
    }

    private void generateEnums(List<EnumTypeDefinition> enumTypes) {
        enumTypes.forEach(enumType -> {
            try {
                TypeSpec.Builder builder = TypeSpec.enumBuilder(enumType.getName())
                        .addModifiers(Modifier.PUBLIC);
                enumType.getEnumValueDefinitions().forEach(enumValue -> {
                    builder.addEnumConstant(enumValue.getName());
                });

                generateTypeSpec(builder.build());
            } catch (IOException e) {
                log.error(e);
            }
        });
    }

    private void generateTypeSpec(TypeSpec typeSpec) throws IOException {
        JavaFile.builder(packageName, typeSpec).build().writeTo(outputDirectory);
    }
}
