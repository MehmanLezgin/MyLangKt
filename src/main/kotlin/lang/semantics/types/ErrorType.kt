package lang.semantics.types

import lang.semantics.symbols.TypeSymbol

object ErrorType : Type() {
    override var flags: TypeFlags = TypeFlags()
    override var declaration: TypeSymbol? = null

    override fun copyWithFlags(flags: TypeFlags) : Type {
        return this
    }

    override fun toString() = stringify()

    override fun equals(other: Any?): Boolean {
        return other is ErrorType
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}

