package lang.semantics.types

import lang.semantics.symbols.OverloadedFuncSymbol

open class OverloadedFuncType(
    open val name: String,
    open val overloadedFuncSym: OverloadedFuncSymbol,
    override var flags: TypeFlags = TypeFlags(),
) : Type(
    flags = flags,
    declaration = null
) {
    override fun copyWithFlags(flags: TypeFlags) =
        OverloadedFuncType(
            name = name,
            overloadedFuncSym = overloadedFuncSym,
            flags = flags
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as OverloadedFuncType

        if (name != other.name) return false
        if (overloadedFuncSym != other.overloadedFuncSym) return false

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


data class OverloadedMethodType(
    val ownerType: Type,
    override val name: String,
    override val overloadedFuncSym: OverloadedFuncSymbol,
    override var flags: TypeFlags = TypeFlags(),
) : OverloadedFuncType(
    name = name,
    overloadedFuncSym = overloadedFuncSym,
    flags = flags,
)