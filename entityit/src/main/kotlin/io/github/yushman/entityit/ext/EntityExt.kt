package io.github.yushman.entityit.ext

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.CHAR_ARRAY
import com.squareup.kotlinpoet.CHAR_SEQUENCE
import com.squareup.kotlinpoet.COLLECTION
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.ITERABLE
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.MUTABLE_COLLECTION
import com.squareup.kotlinpoet.MUTABLE_ITERABLE
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.MUTABLE_SET
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.U_BYTE
import com.squareup.kotlinpoet.U_INT
import com.squareup.kotlinpoet.U_LONG
import com.squareup.kotlinpoet.U_SHORT
import com.squareup.kotlinpoet.ksp.toTypeName
import io.github.yushman.entityit.annotation.Entity
import io.github.yushman.entityit.processor.SourceMeta

internal fun String.toMeta(): SourceMeta.Nullability {
    return when (this) {
        Entity.Nullability.TRANSIENT.name -> SourceMeta.Nullability.TRANSIENT
        Entity.Nullability.FULL.name -> SourceMeta.Nullability.FULL
        else -> SourceMeta.Nullability.TRANSIENT
    }
}

internal fun TypeName.relaxedDefaultString(): String? {
    val tnNotNull = this.copy(nullable = false)
    return when (tnNotNull) {
        CHAR -> "\'\\u0000\'"
        CHAR_SEQUENCE,
        CHAR_ARRAY,
        STRING -> "\"\""

        U_BYTE,
        U_SHORT,
        U_INT,
        BYTE,
        SHORT,
        INT -> "0"

        U_LONG,
        LONG -> "0L"

        FLOAT -> "0f"
        DOUBLE -> "0.0"
        BOOLEAN -> "false"

        else -> null
    } ?: when ((tnNotNull as? ParameterizedTypeName)?.rawType) {
        ARRAY -> "emptyArray()"
        COLLECTION,
        MUTABLE_COLLECTION,
        ITERABLE,
        MUTABLE_ITERABLE,
        LIST,
        MUTABLE_LIST -> "ArrayList()"

        MAP,
        MUTABLE_MAP -> "HashMap()"

        SET,
        MUTABLE_SET -> "HashSet()"

        else -> null
    }
}

internal val KSType.isPrimitive: Boolean
    get() = this.toTypeName().let {
        val tnNotNull = it.copy(nullable = false)
        return when (tnNotNull) {
            CHAR, CHAR_SEQUENCE, CHAR_ARRAY, STRING, U_BYTE, U_SHORT, U_INT, BYTE, SHORT, INT, U_LONG, LONG, FLOAT, DOUBLE,
            BOOLEAN -> true
            else -> null
        } ?: when ((tnNotNull as? ParameterizedTypeName)?.rawType) {
            ARRAY, COLLECTION, MUTABLE_COLLECTION, ITERABLE, MUTABLE_ITERABLE, LIST, MUTABLE_LIST , MAP, MUTABLE_MAP , SET,
            MUTABLE_SET -> true
            else -> false
        }
    }