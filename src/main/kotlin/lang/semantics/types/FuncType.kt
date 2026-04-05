package lang.semantics.types

import lang.semantics.symbols.FuncSymbol
import lang.semantics.symbols.MethodFuncSymbol
import lang.semantics.symbols.TypeSymbol

open class FuncType(
    open val paramTypes: List<Type>,
    open val returnType: Type,
    open val funcDeclaration: FuncSymbol? = null,
    override var flags: TypeFlags = TypeFlags(),
    override var declaration: TypeSymbol? = null
) : Type() {
    fun toMethodType(ownerType: Type) =
        MethodType(
            ownerType = ownerType,
            paramTypes = paramTypes,
            returnType = returnType,
            flags = flags,
        )

    override fun copyWithFlags(flags: TypeFlags) =
        FuncType(
            paramTypes = paramTypes,
            returnType = returnType,
            flags = flags
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as FuncType

        if (paramTypes != other.paramTypes) return false
        if (returnType != other.returnType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + paramTypes.hashCode()
        result = 31 * result + returnType.hashCode()
        return result
    }

    override fun toString() = stringify()
}

data class MethodType(
    val ownerType: Type,
    override val paramTypes: List<Type>,
    override val returnType: Type,
    override val funcDeclaration: MethodFuncSymbol? = null,
    override var flags: TypeFlags = TypeFlags()
) : FuncType(
    paramTypes = paramTypes,
    returnType = returnType,
    funcDeclaration = funcDeclaration,
    flags = flags
) {
    override fun toString() = stringify()
}

