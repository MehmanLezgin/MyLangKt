package lang.semantics.types

import lang.semantics.symbols.TypeSymbol

class FuncType(
    val paramTypes: List<Type>,
    val returnType: Type,
    override var flags: TypeFlags = TypeFlags()
) : Type(
    flags = flags,
    declaration = null
) {
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