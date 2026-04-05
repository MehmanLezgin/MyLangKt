package lang.semantics.types

import lang.semantics.symbols.TypeSymbol

class LazyType(
    private val typeProvider: () -> Type,
    override var declaration: TypeSymbol? = null,
    override var flags: TypeFlags = TypeFlags()
) : Type() {
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