package lang.semantics.types

import lang.core.PrimitivesScope
import lang.core.builders.init
import lang.core.builders.registerImplicitCasts
import lang.semantics.scopes.BaseTypeScope
import lang.semantics.symbols.PrimitiveTypeSymbol

enum class PrimitiveFamily() {
    BOOL,
    CHAR,
    INT,
    UINT,
    FLOAT,
    VOID,
}

open class PrimitiveType(
    val name: String,
    val family: PrimitiveFamily = PrimitiveFamily.INT,
    val signed: Boolean = true,
    val primitiveSize: PrimitiveSize,
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

    open fun initWith(scope: BaseTypeScope) {
        val sym = PrimitiveTypeSymbol(
            primitiveType = this,
            scope = scope
        )

        this.declaration = sym
        PrimitivesScope.define(sym)

        scope.init {
            registerImplicitCasts()
        }
    }

    protected open fun recreate(flags: TypeFlags): PrimitiveType =
        PrimitiveType(
            name = name,
            primitiveSize = primitiveSize,
            flags = flags,
            family = family,
            signed = signed
        )

    override fun copyWithFlags(flags: TypeFlags): PrimitiveType {
        val type = recreate(flags)
        type.declaration = this.declaration
        return type
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
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