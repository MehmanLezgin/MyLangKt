package lang.semantics.types

object UnresolvedType : Type(
    flags = TypeFlags(),
    declaration = null
) {
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