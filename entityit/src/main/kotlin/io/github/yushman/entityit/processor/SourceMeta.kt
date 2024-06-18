package io.github.yushman.entityit.processor

data class SourceMeta(
    val kotlinSerializable: Boolean,
    val nullableValues: Boolean,
    val generateMappers: Boolean,
    val isInternal: Boolean,
)