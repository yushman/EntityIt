package io.github.yushman.entityit.processor

import com.google.devtools.ksp.symbol.KSClassDeclaration

internal data class PropertyMeta(
    val resultName: String,
    val isNullable: Boolean,
    val isNotNullAnnotated: Boolean,
//    val isEntityAnnotated: Boolean,
    val mapper: KSClassDeclaration?,
)
