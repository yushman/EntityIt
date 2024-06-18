package io.github.yushman.entityit.processor

import com.google.devtools.ksp.symbol.KSClassDeclaration

internal data class PropertyMeta(
    val resultName: String,
    val isNotNull: Boolean,
    val mapper: KSClassDeclaration?,
)
