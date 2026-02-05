package lang.semantics.types

object ErrorType : Type(
    flags = TypeFlags(),
    declaration = null
) {
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