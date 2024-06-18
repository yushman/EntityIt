package io.github.yushman.entityit.sample

import io.github.yushman.entityit.annotation.Entity

@Entity(
    kotlinSerializable = false,
)
internal data class SampleClass(
    @Entity.NotNull
    val id: String,
    val flag: Boolean,
    @Entity.Named("iAmFloat")
    val integer: Float,
    @Entity.Named("iAmDouble")
    @Entity.MapWith(SampleMapper::class)
    val long: Long,
)