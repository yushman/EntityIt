package ru.tomindapps.entityit.annotation

import ru.tomindapps.entityit.mapper.EntityMapper
import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Entity(
    val kotlinSerializable: Boolean = true,
    val nullableValues: Boolean = true,
    val generateMappers: Boolean = true,
){

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.FIELD)
    annotation class Named(val name: String)


    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.FIELD)
    annotation class MapWith(val clazz: KClass<out EntityMapper<*, *>>)

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.FIELD)
    annotation class NotNull
}
