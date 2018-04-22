package com.github.gdelafosse;

import com.squareup.javapoet.*;
import graphql.language.*;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.apache.maven.plugin.logging.Log;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(interfaceTypeDefinition.getName())
                .addModifiers(Modifier.PUBLIC);

        interfaceTypeDefinition.getFieldDefinitions().stream()
                .map(this::buildAccessor)
                .forEach(builder::addMethod);

        generateTypeSpec(builder.build());
    }

    private void generateObject(ObjectTypeDefinition objectTypeDefinition) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(objectTypeDefinition.getName())
                .addModifiers(Modifier.PUBLIC);

        Map<String, FieldDefinition> fields = objectTypeDefinition.getFieldDefinitions().stream()
                .collect(Collectors.toMap(FieldDefinition::getName, Function.identity()));

        objectTypeDefinition.getImplements().stream()
                .peek(i -> {
                    InterfaceTypeDefinition interfaceTypeDefinition = typeDefinitionRegistry.getType(i, InterfaceTypeDefinition.class).get();
                    interfaceTypeDefinition.getFieldDefinitions()
                            .stream()
                            .peek(fieldDefinition -> { this.generateOverridenField(builder, fieldDefinition);})
                            .forEach(fieldDefinition -> {fields.remove(fieldDefinition.getName());});
                })
                .map(this::getClassName)
                .forEach(builder::addSuperinterface);

        fields.values().forEach(fieldDefinition -> this.generateField(builder, fieldDefinition));

        generateTypeSpec(builder.build());
    }

    private void generateField(TypeSpec.Builder builder, FieldDefinition fieldDefinition) {
        generateField(builder, fieldDefinition, false);
    }

    private void generateOverridenField(TypeSpec.Builder builder, FieldDefinition fieldDefinition) {
        generateField(builder, fieldDefinition, true);
    }

    private void generateField(TypeSpec.Builder builder, FieldDefinition fieldDefinition, boolean override) {
        builder.addField(FieldSpec.builder(getClassName(fieldDefinition), fieldDefinition.getName(), Modifier.PRIVATE).build());
        MethodSpec.Builder methodSpecBuilder = MethodSpec.methodBuilder(fieldDefinition.getName())
                .returns(getClassName(fieldDefinition))
                .addModifiers(Modifier.PUBLIC)
                .addCode(String.format("return %s;\n", fieldDefinition.getName()));

        if (override) {
            methodSpecBuilder.addAnnotation(Override.class);
        }

        builder.addMethod(methodSpecBuilder.build());
    }


    private void generateTypeSpec(TypeSpec typeSpec) {
        try {
            JavaFile.builder(packageName, typeSpec).build().writeTo(outputDirectory);
        } catch (IOException e) {
            log.error(e);
        }
    }

    private MethodSpec buildAccessor(FieldDefinition fieldDefinition) {
        return MethodSpec.methodBuilder(fieldDefinition.getName())
                .returns(getClassName(fieldDefinition))
                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                .build();
    }

    private ClassName getClassName(FieldDefinition fieldDefinition) {
        return getClassName(fieldDefinition.getType());
    }

    private ClassName getClassName(Type type) {
        return ClassName.get(packageName, typeDefinitionRegistry.getType(type).get().getName());
    }
}
