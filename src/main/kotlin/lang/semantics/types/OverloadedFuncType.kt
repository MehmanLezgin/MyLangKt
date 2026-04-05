package lang.semantics.types

import lang.semantics.symbols.OverloadedFuncSymbol
import lang.semantics.symbols.TypeSymbol

open class OverloadedFuncType(
    open val overloadedFuncSym: OverloadedFuncSymbol,
    open val templateArgs: List<TemplateArg>,
    override var flags: TypeFlags = TypeFlags(),
    override var declaration: TypeSymbol? = null,
) : Type() {
    override fun copyWithFlags(flags: TypeFlags) =
        OverloadedFuncType(
            overloadedFuncSym = overloadedFuncSym,
            flags = flags,
            templateArgs = templateArgs,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as OverloadedFuncType

        return overloadedFuncSym == other.overloadedFuncSym
    }

    override fun hashCode(): Int {
        val result = super.hashCode()
        return result
    }

    override fun toString() = stringify()
}


data class OverloadedMethodType(
    val ownerType: Type,
    override val overloadedFuncSym: OverloadedFuncSymbol,
    override var flags: TypeFlags = TypeFlags(),
    override val templateArgs: List<TemplateArg>,
) : OverloadedFuncType(
    overloadedFuncSym = overloadedFuncSym,
    flags = flags,
    templateArgs = templateArgs
)