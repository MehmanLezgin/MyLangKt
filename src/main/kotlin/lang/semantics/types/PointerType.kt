package lang.semantics.types

class PointerType(
    val base: Type,
    val level: Int = 1,
    override var flags: TypeFlags = TypeFlags()
) : Type(
    flags = flags,
) {
    override fun copyWithFlags(flags: TypeFlags) =
        PointerType(
            base = base,
            level = level,
            flags = flags
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as PointerType

        if (level != other.level) return false
        if (base != other.base) return false
        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + level
        result = 31 * result + base.hashCode()
        return result
    }

    override fun toString() = stringify()
}