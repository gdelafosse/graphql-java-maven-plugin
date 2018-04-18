package com.github.gdelafosse;

import com.squareup.javapoet.*;
import graphql.language.*;
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
    private TypeDefinitionRegistry typeDefinitionRegistry;

    public GraphQLJavaSourceGenerator(Log log, File outputDirectory, String packageName, TypeDefinitionRegistry typeDefinitionRegistry) {
        this.log = log;
        this.outputDirectory = outputDirectory;
        this.packageName = packageName;
        this.typeDefinitionRegistry = typeDefinitionRegistry;
    }

    public void generate() throws IOException {
        this.packageDirectory = mkdir();
        log.debug(String.format("Generating sources in %s", packageDirectory));

        typeDefinitionRegistry.getTypes(EnumTypeDefinition.class).forEach(this::generateEnum);
        typeDefinitionRegistry.getTypes(InterfaceTypeDefinition.class).forEach(this::generateInterface);
        typeDefinitionRegistry.getTypes(ObjectTypeDefinition.class).forEach(this::generateObject);
    }

    private File mkdir() throws IOException {
        File packageDir = outputDirectory.toPath().resolve(packageName.replaceAll("\\.", File.separator)).toFile();
        boolean created = packageDir.mkdirs();
        if (!created) {
            throw new IOException(String.format("Fail to create package directory %s.", packageDir.getAbsolutePath()));
        }
        return packageDir;
    }

    private void generateEnum(EnumTypeDefinition enumTypeDefinition) {
        TypeSpec.Builder builder = TypeSpec.enumBuilder(enumTypeDefinition.getName())
                .addModifiers(Modifier.PUBLIC);
        enumTypeDefinition.getEnumValueDefinitions().forEach(enumValue -> {
            builder.addEnumConstant(enumValue.getName());
        });

        generateTypeSpec(builder.build());
    }

    private void generateInterface(InterfaceTypeDefinition interfaceTypeDefinition) {
        generateClass(interfaceTypeDefinition, interfaceTypeDefinition.getFieldDefinitions(), Modifier.PUBLIC, Modifier.ABSTRACT);
    }

    private void generateObject(ObjectTypeDefinition objectTypeDefinition) {
        generateClass(objectTypeDefinition, objectTypeDefinition.getFieldDefinitions(), Modifier.PUBLIC);
    }

    private void generateClass(TypeDefinition<?> typeDefinition, List<FieldDefinition> fieldDefinitions,  Modifier ... modifiers) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(typeDefinition.getName())
                .addModifiers(modifiers);

        fieldDefinitions.stream()
                .map(this::generateField)
                .forEach(builder::addField);

        generateTypeSpec(builder.build());
    }

    private FieldSpec generateField(FieldDefinition fieldDefinition) {
        return FieldSpec.builder(ClassName.get(packageName, typeDefinitionRegistry.getType(fieldDefinition.getType()).get().getName()), fieldDefinition.getName(), Modifier.PUBLIC).build();
    }

    private void generateTypeSpec(TypeSpec typeSpec) {
        try {
            JavaFile.builder(packageName, typeSpec).build().writeTo(outputDirectory);
        } catch (IOException e) {
            log.error(e);
        }
    }
}
