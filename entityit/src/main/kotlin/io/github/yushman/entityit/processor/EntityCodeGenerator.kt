package io.github.yushman.entityit.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.jvm.jvmSuppressWildcards
import com.squareup.kotlinpoet.ksp.TypeParameterResolver
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

        val entityClass = ClassName(packageName, entityName)
        val sourceClass = classDeclaration.toClassName()
        val mappersClass = ClassName(packageName, mappersName)

        val entityConstructorBuilder = FunSpec.constructorBuilder()
        val entityProperties = mutableListOf<PropertySpec>()
        val toDomainMapperStatements = CodeBlock.builder()
        val toEntityStatements = CodeBlock.builder()

        meta.forEach { (property, propertyMeta) ->
            val resultName = propertyMeta.resultName
            val propName = property.simpleName.asString()
            val type = property.type.resolve().toTypeName()
            val entityMapper = propertyMeta.mapper
            entityProperties.add(
                PropertySpec.builder(resultName, type.copy(nullable = true)).initializer(resultName).build()
            )
            addMapperStatements(
                entityConstructorBuilder,
                entityProperties,
                toDomainMapperStatements,
                toEntityStatements,
                property,
                propName,
                propertyMeta,
                sourceMeta,
                resultName,
                entityMapper,
                mappersClass,
                type,
                sourceClass,
            )

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

    private fun addMapperStatements(
        entityConstructorBuilder: FunSpec.Builder,
        entityProperties: MutableList<PropertySpec>,
        toDomainMapperStatements: CodeBlock.Builder,
        toEntityStatements: CodeBlock.Builder,
        property: KSPropertyDeclaration,
        propName: String,
        propertyMeta: PropertyMeta,
        sourceMeta: SourceMeta,
        resultName: String,
        entityMapper: KSClassDeclaration?,
        mappersClass: ClassName,
        type: TypeName,
        sourceClass: ClassName,
    ) {
        val isNullableProperty = isNullableProperty(sourceMeta, propertyMeta)

        if (entityMapper != null && sourceMeta.generateMappers) {
            val mapperType = entityMapper.getAllFunctions().toList()[1].parameters.first().type.resolve().toTypeName()
            val mapperName = entityMapper.toClassName().canonicalName.replaceDots()
            val resultType =
                if (isNullableProperty) {
                    mapperType.copy(nullable = true)
                } else {
                    mapperType
                }
            val nullabilityOption = isNullableProperty.getNullabilityOption { resultType.relaxedDefaultString()?.let { " ?: $it" } ?: "!!" }
            entityConstructorBuilder.addParameter(
                ParameterSpec.builder(resultName, resultType)
                    .apply { if (isNullableProperty) defaultValue("null") }
                    .build(),
            )
            toDomainMapperStatements.addStatement(
                "$propName = %T.$mapperName.mapEntityToDomain($resultName$nullabilityOption),",
                mappersClass,
            )
            entityProperties.add(
                PropertySpec.builder(resultName, resultType).initializer(resultName).build(),
            )

            toEntityStatements.addStatement(
                "$resultName = %T.$mapperName.mapDomainToEntity($propName),",
                mappersClass
            )
        } else {
            val resultType =
                if (isNullableProperty) type.copy(nullable = true) else type
            val nullabilityOption = isNullableProperty.getNullabilityOption { resultType.relaxedDefaultString()?.let { " ?: $it" } ?: "!!" }
            entityConstructorBuilder.addParameter(
                ParameterSpec.builder(resultName, resultType)
                    .apply { if (isNullableProperty) defaultValue("null") }
                    .build(),
            )
            entityProperties.add(PropertySpec.builder(resultName, resultType).initializer(resultName).build())
            if (sourceMeta.generateMappers) {
                toDomainMapperStatements.addStatement(
                    "$propName = $resultName$nullabilityOption,",
                    sourceClass
                ) // TODO add default implementations
                toEntityStatements.addStatement("$resultName = $propName,")
            }
        }
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
                    .apply { if (sourceMeta.isInternal) addModifiers(KModifier.INTERNAL) }
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
        sourceClass: TypeName,
        entityClass: ClassName,
        entityConstructorBuilder: FunSpec.Builder,
        entityProperties: MutableList<PropertySpec>,
        toDomainMapperStatements: CodeBlock.Builder,
        toEntityStatements: CodeBlock.Builder
    ) {
        val serializableAnnotation = ClassName("kotlinx.serialization", "Serializable")

        val toEntityMapper = FunSpec.builder("toEntity")
            .apply { if (sourceMeta.isInternal) addModifiers(KModifier.INTERNAL) }
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

    private fun isNullableProperty(sourceMeta: SourceMeta, propertyMeta: PropertyMeta): Boolean {
        return (sourceMeta.nullability == SourceMeta.Nullability.FULL && !propertyMeta.isNotNullAnnotated)
                || (sourceMeta.nullability == SourceMeta.Nullability.TRANSIENT && propertyMeta.isNullable && !propertyMeta.isNotNullAnnotated)
    }

    private fun Boolean.getNullabilityOption(action: () -> String): String {
        return if (this) (action()) else ""
    }

    private fun String.replaceDots(): String {
        return this.replace(".", "")
    }

    private fun TypeName.relaxedDefaultString(): String? {
        val tnNotNull = this.copy(nullable = false)
        return when (tnNotNull) {
            CHAR -> "\'\\u0000\'"
            CHAR_SEQUENCE,
                CHAR_ARRAY,
            STRING -> "\"\""

            U_BYTE::class.asTypeName(),
            U_SHORT::class.asTypeName(),
            U_INT::class.asTypeName(),
            BYTE::class.asTypeName(),
            SHORT::class.asTypeName(),
            INT::class.asTypeName() -> "0"

            U_LONG::class.asTypeName(),
            LONG::class.asTypeName() -> "0L"

            FLOAT::class.asTypeName() -> "0f"
            DOUBLE::class.asTypeName() -> "0.0"
            BOOLEAN::class.asTypeName() -> "false"

            else -> null
        } ?: when((tnNotNull as? ParameterizedTypeName)?.rawType){
            ARRAY -> "emptyArray()"
            COLLECTION,
                MUTABLE_COLLECTION,
                ITERABLE,
                MUTABLE_ITERABLE,
            LIST,
            MUTABLE_LIST -> "ArrayList()"
            MAP,
            MUTABLE_MAP -> "HashMap()"
            SET,
            MUTABLE_SET -> "HashSet()"
            else -> null
        }
    }

    private fun KSPropertyDeclaration.resolveType() = this.type.resolve()
}