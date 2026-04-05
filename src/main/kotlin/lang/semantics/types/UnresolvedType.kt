package lang.semantics.types

import lang.semantics.symbols.TypeSymbol
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

object UnresolvedType : Type() {
    override var declaration: TypeSymbol? = null
    override var flags: TypeFlags = TypeFlags()

    override fun copyWithFlags(flags: TypeFlags) : Type {
        return this
    }

    override fun toString() = stringify()

    override fun equals(other: Any?): Boolean {
        return other is UnresolvedType
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}

@OptIn(ExperimentalContracts::class)
fun Type?.isNullOrUnresolved(): Boolean {
    contract {
        returns(false) implies (this@isNullOrUnresolved != null)
    }

    return this == null || this == UnresolvedType
}