package lang.semantics.types

import lang.semantics.symbols.ConstructorSymbol

enum class ConversionKind {
    INVALID,
    IDENTITY,
    PRIMITIVE,
    CONSTRUCTOR,
    VOID_PTR,
    POINTER,
    FUNCTION
}

sealed class ConversionInfo {
    abstract val fromType: Type
    abstract val toType: Type

    companion object {
        const val COST_NONE = Int.MAX_VALUE
        const val COST_IDENTITY = 0
//        const val COST_PRIMITIVE = 1
        const val COST_CAST = 60
        const val COST_CONSTRUCTOR = 100
    }

    data object None : ConversionInfo() {
        override val fromType = ErrorType
        override val toType = ErrorType
    }

    data class Identity(
        override val fromType: Type,
    ) : ConversionInfo() {
        override val toType: Type = fromType
    }

    data class Primitive(
        override val fromType: PrimitiveType,
        override val toType: PrimitiveType,
        val constructor: ConstructorSymbol,
    ) : ConversionInfo()

    data class Cast(
        override val fromType: Type,
        override val toType: Type,
    ) : ConversionInfo()


    data class Constructor(
        override val fromType: Type,
        override val toType: Type,
        val constructor: ConstructorSymbol,
    ) : ConversionInfo()
}

fun ConversionInfo.exists() = this != ConversionInfo.None
fun ConversionInfo.notExists() = this == ConversionInfo.None