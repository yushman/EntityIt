package io.github.yushman.entityit.sample

import io.github.yushman.entityit.annotation.Entity

@Entity(
    kotlinSerializable = false
)
data class InnerClassSamePackage(
    val sosme: String,
)