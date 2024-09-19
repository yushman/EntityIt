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
import io.github.yushman.entityit.annotation.Entity
import io.github.yushman.entityit.ext.toMeta

internal class EntityItProcessor(
    codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    private val generator = EntityCodeGenerator(codeGenerator, logger)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val entities = resolver.getSymbolsWithAnnotation(Entity::class.qualifiedName.toString())
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
        val nameAnnotation = this.annotations.firstOrNull { it.shortName.asString() == Entity.Named::class.simpleName }
        val mapperAnnotation =
            this.annotations.firstOrNull { it.shortName.asString() == Entity.MapWith::class.simpleName }
        val isNotNull = this.annotations.any { it.shortName.asString() == Entity.NotNull::class.simpleName }
        val name = if (nameAnnotation != null) {
            nameAnnotation.arguments.first().value as String
        } else {
            this.simpleName.getShortName()
        }
        val isNullable = this.type.resolve().isMarkedNullable
        val mapper =
            mapperAnnotation?.arguments?.first()?.value?.let { it as? KSType? }?.declaration as? KSClassDeclaration
        val isEntity =
            this.parentDeclaration?.annotations?.any { it.shortName.asString() == Entity::class.simpleName } == true
        return PropertyMeta(
            resultName = name,
            isNullable = isNullable,
            isNotNullAnnotated = isNotNull,
            isEntityAnnotated = isEntity,
            mapper = mapper,
        )
    }

    private fun KSClassDeclaration.getMeta(): SourceMeta {
        val annotation = this.annotations.first { it.shortName.asString() == Entity::class.simpleName }
        val params = annotation.arguments
        val kotlinSerializable = params.first { it.name?.asString() == "kotlinSerializable" }.value as Boolean
        val nullability = (params.first { it.name?.asString() == "nullability" }.value as KSType).declaration.simpleName.asString()
        val generateMappers = params.first { it.name?.asString() == "generateMappers" }.value as Boolean

        logger.info("nullability: $nullability")
        return SourceMeta(
            kotlinSerializable = kotlinSerializable,
            nullability = nullability.toMeta(),
            generateMappers = generateMappers,
            isInternal = this.isInternal(),
        )
    }
}


