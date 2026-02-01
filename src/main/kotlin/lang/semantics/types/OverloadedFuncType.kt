package lang.semantics.types

import lang.semantics.symbols.FuncSymbol
import lang.semantics.symbols.TypeSymbol

class OverloadedFuncType(
    val name: String,
    val overloads: List<FuncSymbol>,
    override var flags: TypeFlags = TypeFlags(),
) : Type(
    flags = flags,
    declaration = null
) {
    override fun copyWithFlags(flags: TypeFlags) =
        OverloadedFuncType(
            name = name,
            overloads = overloads,
            flags = flags
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as OverloadedFuncType

        if (name != other.name) return false
        if (overloads != other.overloads) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
//        result = 31 * result + paramTypes.hashCode()
        return result
    }

    override fun toString() = stringify()
}