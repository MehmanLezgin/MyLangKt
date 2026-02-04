package lang.semantics.types

import lang.semantics.symbols.TypeSymbol

class NamespaceType(
    val name: String,
    override var declaration: TypeSymbol?
) : Type(
    flags = TypeFlags(),
    declaration = declaration
) {
    override fun copyWithFlags(flags: TypeFlags) =
        NamespaceType(
            name = name,
            declaration = declaration
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as NamespaceType

        return declaration == other.declaration
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + declaration.hashCode()
        return result
    }

    override fun toString() = stringify()
}