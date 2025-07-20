package io.github.yushman.entityit.sample.inner

import io.github.yushman.entityit.annotation.Entity

@Entity(
    kotlinSerializable = false
)
data class InnerClassNonPackage(
    val sosme: String,
)