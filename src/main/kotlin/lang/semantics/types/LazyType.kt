package lang.semantics.types

class LazyType(
    private val typeProvider: () -> Type,
) : Type(declaration = null) {
    private var isResolving = false
    private var resolvedType: Type? = null

    val type: Type
        get() {
            if (resolvedType != null) return resolvedType!!
            else if (isResolving) return UnresolvedType
            else {
                isResolving = true
                resolvedType = typeProvider()
                isResolving = false
                return resolvedType!!
            }
        }

    override fun copyWithFlags(flags: TypeFlags) = type.copyWithFlags(flags)
    override fun toString() = type.toString()
}

fun lazyType(block: () -> Type): LazyType {
    return LazyType(block)
}