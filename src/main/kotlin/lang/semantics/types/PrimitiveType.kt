package lang.semantics.types

import lang.semantics.builtin.PrimitivesScope.constCharPtr
import lang.semantics.builtin.operFunc
import lang.semantics.builtin.staticConstVar
import lang.semantics.scopes.BaseTypeScope
import lang.semantics.scopes.Scope
import lang.semantics.symbols.PrimitiveTypeSymbol

open class PrimitiveType(
    val name: String,
    val size: PrimitiveSize,
    val prec: Int,
    override var flags: TypeFlags = TypeFlags()
) : Type(
    flags = flags,
    declaration = null
) {
    private var constVersion: PrimitiveType? = null

    fun toConst(): PrimitiveType {
        constVersion?.let { return it }

        val c = recreate(flags)

        c.declaration = declaration

        constVersion = c
        return c
    }

    open fun initWith(scope: Scope) {
        val sym = PrimitiveTypeSymbol(
            type = this,
            scope = BaseTypeScope(
                parent = scope,
                errorHandler = scope.errorHandler,
                scopeName = this.name,
                superTypeScope = null
            )
        )

        this.declaration = sym
        scope.define(sym, null)

        this.staticConstVar("SIZE_BYTES", this, ConstValue(size.size))
            .staticConstVar("SIZE_BITS", this, ConstValue(size.size * 8))
    }

    protected open fun recreate(flags: TypeFlags): PrimitiveType =
        PrimitiveType(
            name = name,
            size = size,
            prec = prec,
            flags = flags
        )

    override fun copyWithFlags(flags: TypeFlags): PrimitiveType {
        val type = recreate(flags)
        type.declaration = this.declaration
        return type
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
//        if (javaClass != other?.javaClass) return false
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