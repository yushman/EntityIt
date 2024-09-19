package io.github.yushman.entityit.ext

import io.github.yushman.entityit.annotation.Entity
import io.github.yushman.entityit.processor.SourceMeta

internal fun String.toMeta(): SourceMeta.Nullability {
    return when (this) {
        Entity.Nullability.TRANSIENT.name -> SourceMeta.Nullability.TRANSIENT
        Entity.Nullability.FULL.name -> SourceMeta.Nullability.FULL
        else -> SourceMeta.Nullability.TRANSIENT
    }
}