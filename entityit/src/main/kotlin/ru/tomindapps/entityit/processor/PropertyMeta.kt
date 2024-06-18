package ru.tomindapps.entityit.processor

import com.google.devtools.ksp.symbol.KSClassDeclaration
import ru.tomindapps.entityit.mapper.EntityMapper

internal data class PropertyMeta(
    val resultName: String,
    val isNotNull: Boolean,
    val mapper: KSClassDeclaration?,
)
