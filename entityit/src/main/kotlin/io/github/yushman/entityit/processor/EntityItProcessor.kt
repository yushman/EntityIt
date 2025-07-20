package io.github.yushman.entityit.processor

import com.google.devtools.ksp.isInternal
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ksp.toTypeName
import io.github.yushman.entityit.annotation.Entity
import io.github.yushman.entityit.ext.isPrimitive
import io.github.yushman.entityit.ext.toMeta
import io.github.yushman.entityit.mapper.EntityMapper

internal class EntityItProcessor(
    codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    private val generator = EntityCodeGenerator(codeGenerator, logger)
    private val entityAnnotatedQ = Entity::class.qualifiedName.orEmpty()
    private val entityAnnotatedS = Entity::class.simpleName
    private val namedAnnotated = Entity.Named::class.simpleName
    private val mapWithAnnotated = Entity.MapWith::class.simpleName
    private val notNullAnnotated = Entity.NotNull::class.simpleName

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val entities = resolver.getSymbolsWithAnnotation(entityAnnotatedQ)
        val (validSymbols, invalidSymbols) = entities.partition { it.validate() }

        for (symbol in validSymbols) {
            if (symbol is KSClassDeclaration) {
                processInternal(symbol)
            }
        }
        return invalidSymbols
    }

    private fun processInternal(symbol: KSClassDeclaration) {
        logger.logging("Processing", symbol)
        val sourceMeta = symbol.getMeta()
        val propertiesToMeta = symbol.getAllProperties().associateWith { it.getMeta() }
        val mappers = propertiesToMeta.values.mapNotNull { it.mapper }
        generator.generateEntities(symbol, sourceMeta, propertiesToMeta, mappers)
    }

    private fun KSPropertyDeclaration.getMeta(): PropertyMeta {
        val isNotNull = this.annotations.any { it.shortName.asString() == notNullAnnotated }
        val ksType = this.type.resolve()
        val isNullable = ksType.isMarkedNullable
        val isEntity = ksType.isEntity()
        val name = this.getName()
        val mapper = this.getMapper()
        val mapperType = mapper?.getMapperType()
            validateProp(isEntity, ksType, mapperType)
        val propertyMeta = PropertyMeta(
            resultName = name,
            isNullable = isNullable,
            isNotNullAnnotated = isNotNull,
            isEntityAnnotated = isEntity,
            mapper = mapper,
            ksType = ksType,
            resultType = mapperType ?: ksType
        )
        logger.logging("Property Meta = $propertyMeta")
        return propertyMeta
    }

    private fun KSPropertyDeclaration.validateProp(
        isEntity: Boolean,
        ksType: KSType,
        mapperType: KSType?,
    ) {
        // checks property supports Entities or is a primitive
        when {
            // checks mapper supports Entities or is a primitive
            mapperType != null -> {
                when {
                    mapperType.isPrimitive -> Unit // ok
                    mapperType.isEntity() -> Unit // ok
                    else -> {
                        val propClass = mapperType.toTypeName().toString()
                        throw IllegalArgumentException(
                            "EntityMapper ($propClass) must return a class marked with \"Entity\" annotation" +
                                    " or a primitive type"
                        )
                    }
                }
            }

            isEntity -> Unit
            ksType.isPrimitive -> Unit
            else -> {
                val propName = this.simpleName.getShortName()
                val propClass = this.type.toTypeName().toString()
                throw IllegalArgumentException(
                    "Property [$propName] has class [$propClass] that is not marked with \"Entity\" annotation" +
                            " or it has no \"Entity.MapWith\" annotation or it not a primitive"
                )
            }

        }
    }

    private fun KSPropertyDeclaration.getName(): String {
        val nameAnnotation = this.annotations.firstOrNull { it.shortName.asString() == namedAnnotated }
        return if (nameAnnotation != null) {
            nameAnnotation.arguments.first().value as String
        } else {
            this.simpleName.getShortName()
        }
    }

    private fun KSPropertyDeclaration.getMapper(): KSClassDeclaration? {
        val mapperAnnotation =
            this.annotations.firstOrNull { it.shortName.asString() == mapWithAnnotated }
        val mapper =
            mapperAnnotation?.arguments?.first()?.value?.let { it as? KSType? }?.declaration as? KSClassDeclaration

        return mapper
    }

    private fun KSClassDeclaration.getMapperType(): KSType? {
        // find Super of mapper == EntityMapper
        val mapperSuper = this.superTypes.map { it.resolve() }.firstOrNull {
            it.declaration.qualifiedName?.asString().orEmpty() == EntityMapper::class.qualifiedName
        }!!
        // get second argument (second generic)
        return mapperSuper.arguments.getOrNull(1)?.type?.resolve()!!
    }

    private fun KSType.isEntity() =
        this.declaration.annotations.any { it.shortName.asString() == entityAnnotatedS }

    private fun KSClassDeclaration.getMeta(): SourceMeta {
        val annotation = this.annotations.first { it.shortName.asString() == entityAnnotatedS }
        val params = annotation.arguments
        val kotlinSerializable = params.first { it.name?.asString() == "kotlinSerializable" }.value as Boolean
        val nullability =
            (params.first { it.name?.asString() == "nullability" }.value as KSType).declaration.simpleName.asString()
        val generateMappers = params.first { it.name?.asString() == "generateMappers" }.value as Boolean
        val sourceMeta = SourceMeta(
            kotlinSerializable = kotlinSerializable,
            nullability = nullability.toMeta(),
            generateMappers = generateMappers,
            isInternal = this.isInternal(),
        )
        logger.logging("Source Meta = $sourceMeta")
        return sourceMeta
    }
}


