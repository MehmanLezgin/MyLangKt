package lang.semantics.types

import lang.semantics.symbols.Symbol

open class Type(
    open val isConst: Boolean,
    open val isReference: Boolean,
)

open class PrimitiveType(
    open val name: String,
    override val isConst: Boolean = false,
    override val isReference: Boolean = false,
) : Type(
    isConst = isConst,
    isReference = isReference,
)

data class PointerType(
    val base: Type,
    override val isConst: Boolean = false,
    override val isReference: Boolean = false
) : Type(
    isConst = isConst,
    isReference = isReference,
)

data class FuncType(
    val name: String,
    val params: List<Type>,
    val returnType: Type,
    override val isConst: Boolean = false,
    override val isReference: Boolean = false
) : Type(
    isConst = isConst,
    isReference = isReference,
)

sealed class TypeArg {
    data class ArgType(
        val type: Type,
    ) : TypeArg()

    data class ArgConstValue<T: Any>(
        val value: ConstValue<T>
    ) : TypeArg()
}

data class UserType(
    val name: String,
    val typeArgs: List<TypeArg>,
    val declaration: Symbol,
    override val isConst: Boolean = false,
    override val isReference: Boolean = false
) : Type(
    isConst = isConst,
    isReference = isReference,
)