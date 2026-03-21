package lang.semantics.types

import lang.semantics.builtin.builders.constVar
import lang.semantics.builtin.builders.init
import lang.semantics.scopes.BaseTypeScope
import lang.semantics.scopes.Scope
import lang.semantics.symbols.PrimitiveTypeSymbol

open class PrimitiveType(
    val name: String,
    val primitiveSize: PrimitiveSize,
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
                scopeName = this.name,
            )
        )

        this.declaration = sym
        scope.define(sym)

        scope.init {
            val type = this@PrimitiveType
            constVar("SIZE_BYTES", type, ConstValue(primitiveSize.size))
            constVar("SIZE_BITS", type, ConstValue(primitiveSize.size * 8))
        }
    }

    protected open fun recreate(flags: TypeFlags): PrimitiveType =
        PrimitiveType(
            name = name,
            primitiveSize = primitiveSize,
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
        if (primitiveSize != other.primitiveSize) return false
        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + primitiveSize.hashCode()
        return result
    }

    override fun toString() = stringify()
}