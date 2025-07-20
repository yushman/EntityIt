package io.github.yushman.entityit.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import io.github.yushman.entityit.ext.relaxedDefaultString

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
        val importSpecs = mutableListOf<Pair<String, String>>()

        meta.forEach { p, pm ->
            val resultName = pm.resultName
            val propName = p.simpleName.asString()
            val propType = pm.resultType
            val entityMapper = pm.mapper
            val isNullableProperty = isNullableProperty(sourceMeta, pm)
            val isEntityAnnotated = pm.isEntityAnnotated
            val resultType = propType.let { type ->
                if (isEntityAnnotated) {
                    propType.toClassName().let { ClassName("${it.packageName}.entity", "${it.simpleName}Entity") }
                } else {
                    type.toTypeName()
                }
            }.let {
                if (isNullableProperty) it.copy(nullable = true) else it
            }

            addConstructorStatements(
                isNullableProperty,
                entityConstructorBuilder,
                entityProperties,
                resultName,
                resultType,
            )
            if (sourceMeta.generateMappers) {
                addMapperStatements(
                    isNullableProperty,
                    isEntityAnnotated,
                    entityProperties,
                    importSpecs,
                    toDomainMapperStatements,
                    toEntityStatements,
                    propName,
                    resultName,
                    entityMapper,
                    mappersClass,
                    resultType,
                    sourceClass,
                )
            }
        }

        if (sourceMeta.generateMappers && mappers.isNotEmpty()) {
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
            importSpecs,
            toDomainMapperStatements,
            toEntityStatements,
        )
    }

    private fun addConstructorStatements(
        isNullableProperty: Boolean,
        entityConstructorBuilder: FunSpec.Builder,
        entityProperties: MutableList<PropertySpec>,
        resultName: String,
        resultType: TypeName,
    ) {
        logger.info("generating constructor param: val $resultName:$resultType")
        entityConstructorBuilder.addParameter(
            ParameterSpec.builder(resultName, resultType)
                .apply { if (isNullableProperty) defaultValue("null") }
                .build(),
        )
        entityProperties.add(PropertySpec.builder(resultName, resultType).initializer(resultName).build())
    }

    private fun addMapperStatements(
        isNullableProperty: Boolean,
        isEntityAnnotated: Boolean,
        entityProperties: MutableList<PropertySpec>,
        importSpecs: MutableList<Pair<String, String>>,
        toDomainMapperStatements: CodeBlock.Builder,
        toEntityStatements: CodeBlock.Builder,
        propName: String,
        resultName: String,
        entityMapper: KSClassDeclaration?,
        mappersClass: ClassName,
        resultType: TypeName,
        sourceClass: ClassName,
    ) {
        val nullabilityOption =
            isNullableProperty.getNullabilityOption { resultType.relaxedDefaultString()?.let { " ?: $it" } ?: "!!" }
        val (toDomain, toEntity) = if (isEntityAnnotated) {
            importSpecs.add((resultType as ClassName).packageName to "toEntity")
            "${resultName}.toDomain()" to "$propName.toEntity()"
        } else {
            "$resultName$nullabilityOption" to propName
        }
        if (entityMapper != null) {
            val mapperName = entityMapper.toClassName().canonicalName.replaceDots()
            toDomainMapperStatements.addStatement(
                "$propName = %T.$mapperName.mapEntityToDomain($toDomain),",
                mappersClass,
            )
            entityProperties.add(
                PropertySpec.builder(resultName, resultType).initializer(resultName).build(),
            )

            toEntityStatements.addStatement(
                "$resultName = %T.$mapperName.mapDomainToEntity($toEntity),",
                mappersClass
            )
        } else {
            toDomainMapperStatements.addStatement(
                "$propName = $toDomain,",
                sourceClass
            )
            toEntityStatements.addStatement("$resultName = $toEntity,")
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
        entityProperties: List<PropertySpec>,
        importSpecs: List<Pair<String, String>>,
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
                importSpecs.onEach {
                    addImport(it.first, it.second)
                }
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


    private fun KSPropertyDeclaration.resolveType() = this.type.resolve()
}