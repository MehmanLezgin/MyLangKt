package lang.semantics.types

open class PrimitiveType(
    val name: String,
    val size: PrimitiveSize,
    val prec: Int,
    override var flags: TypeFlags = TypeFlags()
) : Type(
    flags = flags,
) {
    override fun copyWithFlags(flags: TypeFlags) =
        PrimitiveType(
            name = name,
            size = size,
            prec = prec,
            flags = flags
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as PrimitiveType
        if (name != other.name) return false
        if (size != other.size) return false
        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + size.hashCode()
        return result
    }

    override fun toString() = stringify()
}