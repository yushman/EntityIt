package io.github.yushman.entityit.annotation

import io.github.yushman.entityit.mapper.EntityMapper
import kotlin.reflect.KClass

/**
 * Annotation to mark class to generate entity from it.
 *
 * @param kotlinSerializable if true, generated class will be annotated with [kotlinx.serialization]
 * @param nullability if [Nullability.FULL] all properties will be nullable,
 * if [Nullability.TRANSIENT] nullability will be copied to properties,
 * if [Nullability.NONE] all properties will be nonnullable.
 * @param generateMappers if true, generated class will have [toDomain] and [toEntity] methods
 *
 * @author yushman
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Entity(
    val kotlinSerializable: Boolean = true,
    val nullability: Nullability = Nullability.TRANSIENT,
    val generateMappers: Boolean = true,
){

    /**
     * Converts name for a property to a field [name].
     */
    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.FIELD)
    annotation class Named(val name: String)


    /**
     * Adds custom mapper for a property.
     */
    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.FIELD)
    annotation class MapWith(val clazz: KClass<out EntityMapper<*, *>>)

    /**
     * Marks property as nonnullable.
     */
    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.FIELD)
    annotation class NotNull

    enum class Nullability { NONE, TRANSIENT, FULL }
}
