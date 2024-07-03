package io.github.yushman.entityit.sample

import io.github.yushman.entityit.annotation.Entity

@Entity(
    kotlinSerializable = false,
    nullability = Entity.Nullability.FULL
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
    val double: Double,
    val float: Float,
    val boolean: Boolean,
    val char: Char,
    val string: String?,
    val byte: Byte,
    val short: Short,
    val int: Int,
    val array: Array<Int>?,
    val mlist: Collection<String>?,
    val map: Map<String, String>?,
)