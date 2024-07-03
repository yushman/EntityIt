package io.github.yushman.entityit.processor

internal data class SourceMeta(
    val kotlinSerializable: Boolean,
    val nullability: Nullability,
    val generateMappers: Boolean,
    val isInternal: Boolean,
){
    enum class Nullability { NONE, TRANSIENT, FULL }
}