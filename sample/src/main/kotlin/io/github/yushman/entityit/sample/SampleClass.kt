package io.github.yushman.entityit.sample

import io.github.yushman.entityit.annotation.Entity
import io.github.yushman.entityit.sample.inner.InnerClassNonPackage

@Entity(
    kotlinSerializable = false,
    generateMappers = true,
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
    val innerClassNonPackage: InnerClassNonPackage,
    val innerClassSamePackage: InnerClassSamePackage,
//    val error: SampleMapper, // error - not an Entity marked, not a primitive, has no Mapper by @Entity.MapWith
    val array: Array<Int>?,
    val mlist: Collection<String>?,
    val map: Map<String, String>?,
)