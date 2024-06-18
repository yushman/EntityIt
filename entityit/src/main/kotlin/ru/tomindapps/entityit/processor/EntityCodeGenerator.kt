package ru.tomindapps.entityit.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

internal class EntityCodeGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) {
    fun generateEntities(
        classDeclaration: KSClassDeclaration,
        sourceMeta: SourceMeta,
        meta: Map<KSPropertyDeclaration, PropertyMeta>,
        mappers: List<KSClassDeclaration>
    ) {
        val packageName = classDeclaration.packageName.asString() + ".entity"
        val sourceName = classDeclaration.simpleName.asString()
        val entityName = sourceName + "Entity"
        val mappersName = sourceName + "Mappers"

        val sourceClass = classDeclaration.toClassName()
        val entityClass = ClassName(packageName, entityName)
        val mappersClass = ClassName(packageName, mappersName)

        val entityConstructorBuilder = FunSpec.constructorBuilder()
        val entityProperties = mutableListOf<PropertySpec>()
        val toDomainMapperStatements = CodeBlock.builder()
        val toEntityStatements = CodeBlock.builder()

        meta.forEach { (property, propertyMeta) ->
            val resultName = propertyMeta.resultName
            val propName = property.simpleName.asString()
            val type = property.type.toTypeName()
            val entityMapper = propertyMeta.mapper
            entityProperties.add(
                PropertySpec.builder(resultName, type.copy(nullable = true)).initializer(resultName).build()
            )
            if (entityMapper != null && sourceMeta.generateMappers) {
                val mapperType = entityMapper.getAllFunctions().toList()[1].parameters.first().type.toTypeName()
                val mapperName = entityMapper.toClassName().canonicalName.replaceDots()
                val resultType = if (sourceMeta.nullableValues && !propertyMeta.isNotNull) mapperType.copy(nullable = true) else mapperType
                entityConstructorBuilder.addParameter(
                    ParameterSpec.builder(resultName, resultType)
                        .apply { if (sourceMeta.nullableValues && !propertyMeta.isNotNull) defaultValue("null") }
                        .build(),
                )
                entityProperties.add(
                    PropertySpec.builder(resultName, resultType).initializer(resultName).build(),
                )
                toDomainMapperStatements.addStatement(
                    "$propName = %T.$mapperName.mapEntityToDomain($resultName!!),", // TODO add default implementations
                    mappersClass,
                )
                toEntityStatements.addStatement(
                    "$resultName = %T.$mapperName.mapDomainToEntity($propName),",
                    mappersClass
                )
            } else {
                val resultType = if (sourceMeta.nullableValues && !propertyMeta.isNotNull) type.copy(nullable = true) else type
                entityConstructorBuilder.addParameter(
                    ParameterSpec.builder(resultName, resultType)
                        .apply { if (sourceMeta.nullableValues  && !propertyMeta.isNotNull) defaultValue("null") }
                        .build(),
                )
                entityProperties.add(PropertySpec.builder(resultName, resultType).initializer(resultName).build())
                if (sourceMeta.generateMappers) {
                    toDomainMapperStatements.addStatement(
                        "$propName = $resultName!!,",
                        sourceClass
                    ) // TODO add default implementations
                    toEntityStatements.addStatement("$resultName = $propName,")
                }
            }
        }

        if (sourceMeta.generateMappers) {
            generateObjectMappers(
                sourceMeta,
                packageName,
                mappersName,
                mappersClass,
                mappers,
            )
        }

        generateEntity(
            sourceMeta,
            entityName,
            packageName,
            sourceClass,
            entityClass,
            entityConstructorBuilder,
            entityProperties,
            toDomainMapperStatements,
            toEntityStatements,
        )
    }

    private fun generateObjectMappers(
        sourceMeta: SourceMeta,
        packageName: String,
        mappersName: String,
        mappersClass: ClassName,
        mappersList: List<KSClassDeclaration>,
    ) {
        val propertySpecs = mutableListOf<PropertySpec>()
        val resultMap = HashMap<String, ClassName>()
        mappersList.forEach { mapper ->
            mapper.also {
                val className = it.toClassName()
                resultMap[className.canonicalName.replaceDots()] = className
            }
        }
        resultMap.forEach {
            propertySpecs.add(
                PropertySpec.builder(it.key, it.value)
                    .apply { if (sourceMeta.isInternal) addModifiers(KModifier.INTERNAL)}
                    .initializer("%T()", it.value)
                    .build(),
            )
        }
        val file = FileSpec.builder(packageName, mappersName)
            .addType(
                TypeSpec.objectBuilder(mappersClass)
                    .apply {
                        if (sourceMeta.isInternal) addModifiers(KModifier.INTERNAL)
                        propertySpecs.forEach { addProperty(it) }
                    }
                    .build(),
            )
        file.build().writeTo(codeGenerator, Dependencies.ALL_FILES)
    }

    private fun generateEntity(
        sourceMeta: SourceMeta,
        entityName: String,
        packageName: String,
        sourceClass: ClassName,
        entityClass: ClassName,
        entityConstructorBuilder: FunSpec.Builder,
        entityProperties: MutableList<PropertySpec>,
        toDomainMapperStatements: CodeBlock.Builder,
        toEntityStatements: CodeBlock.Builder
    ) {
        val serializableAnnotation = ClassName("kotlinx.serialization", "Serializable")

        val toEntityMapper = FunSpec.builder("toEntity")
            .apply { if (sourceMeta.isInternal) addModifiers(KModifier.INTERNAL)}
            .receiver(sourceClass)
            .returns(entityClass)
            .addCode("return %T(\n", entityClass)
            .addCode(toEntityStatements.build())
            .addCode(")")
            .build()

        val file = FileSpec.builder(packageName, entityName)
            .addType(
                TypeSpec.classBuilder(entityClass)
                    .addModifiers(KModifier.DATA)
                    .primaryConstructor(entityConstructorBuilder.build())
                    .apply {
                        if (sourceMeta.isInternal) addModifiers(KModifier.INTERNAL)
                        if (sourceMeta.kotlinSerializable) {
                            addAnnotation(serializableAnnotation)
                        }
                        if (sourceMeta.generateMappers) {
                            addFunction(
                                FunSpec.builder("toDomain")
                                    .returns(sourceClass)
                                    .addCode("return %T(\n", sourceClass)
                                    .addCode(toDomainMapperStatements.build())
                                    .addCode(")")
                                    .build(),
                            )
                        }
                        entityProperties.forEach { addProperty(it) }
                    }
                    .build(),
            )
            .apply {
                if (sourceMeta.generateMappers) {
                    addFunction(toEntityMapper)
                }
            }

        file.build().writeTo(codeGenerator, Dependencies.ALL_FILES)

    }

    private fun String.replaceDots(): String {
        return this.replace(".", "")
    }

    private companion object {
        private const val DEFAULT_STR = "default"
        private const val CONFIG_STR = "config"
        private const val ENTITY_STR = "entity"
        private const val FACTORY_STR = "factory"
        private const val CURRENT_STR = "current"
        private const val LOCALFEATUREFLAG_STR = "localFeatureFlag"
    }
}