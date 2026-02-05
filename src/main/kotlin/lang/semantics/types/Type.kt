package lang.semantics.types

import lang.messages.Msg
import lang.semantics.symbols.TypeSymbol
import lang.semantics.builtin.PrimitivesScope.void
import lang.semantics.builtin.PrimitivesScope.voidPtr
import lang.semantics.builtin.types.VoidPrimitive

abstract class Type(
    open var flags: TypeFlags = TypeFlags(),
    open var declaration: TypeSymbol?
) {
    val isConst: Boolean get() = flags.isConst
    val isLvalue: Boolean get() = flags.isLvalue
    val isExprType: Boolean get() = flags.isExprType
    val isMutable: Boolean get() = flags.isMutable

    fun isVoidPtr() = this == voidPtr ||
            this is PointerType &&
            this.level == 1 &&
            this.base is PrimitiveType &&
            this.base.name == void.name

    fun setFlags(
        isConst: Boolean = flags.isConst,
        isLvalue: Boolean = flags.isLvalue,
        isExprType: Boolean = flags.isExprType,
        isMutable: Boolean = flags.isMutable
    ): Type {
        return copyWithFlags(
            flags = TypeFlags(
                isConst = isConst,
                isLvalue = isLvalue,
                isExprType = isExprType,
                isMutable = isMutable
            )
        )
    }

    abstract fun copyWithFlags(flags: TypeFlags = this.flags): Type

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Type

        if (isConst != other.isConst) return false
        if (isLvalue != other.isLvalue) return false
        if (isExprType != other.isExprType) return false
        return true
    }

    override fun hashCode(): Int {
        var result = isConst.hashCode()
        result = 31 * result + isLvalue.hashCode()
        result = 31 * result + isExprType.hashCode()
        return result
    }

    override fun toString() = stringify()

    fun Type.castCost(to: Type): Int? {
        val from = this
        if (!from.canCastTo(to)) return null
        if (from == to) return 0

        return when {
            from is PrimitiveType && to is PrimitiveType ->
                when {
                    from.prec == to.prec -> 0
                    from.prec < to.prec -> 1
                    else -> 2
                }

            from is PointerType && to is PointerType ->
                if (to.isVoidPtr()) 10 else 0

            else -> 100
        }
    }

    fun canCastTo(to: Type): Boolean {
        val from = this
        if (from == to) return true
        if (!from.isConst && to.isConst) return false

        if (from.isVoidPtr()) {
            if (to is PointerType) return true
            if (to is FuncType) return true
            return false
        }

        when (from) {
            is PrimitiveType -> {
                if (to !is PrimitiveType) return false
                if (to is VoidPrimitive) return false
//                if (to == BuiltInTypes.) return false
//                if (from.isConst && from.isExprType)
//                    return true

                return from.prec <= to.prec
            }

            is PointerType -> {
                return when {
                    to.isVoidPtr() -> true
                    to is PointerType -> to.isVoidPtr() || from.level == to.level
                    else -> false
                }
            }

            is FuncType -> {
                if (to.isVoidPtr()) return true
                if (to !is FuncType) return false
                if (from.returnType != to.returnType) return false
                if (from.paramTypes != to.paramTypes) return false
                return true
            }

            is UserType -> {
                if (to !is UserType) return false
                if (from.name != to.name) return false
                if (from.declaration != to.declaration) return false
                return from.templateArgs == to.templateArgs
            }
        }

        return false
    }

    fun stringify(pointerLevel: Int = 0): String {
        val type = this

        if (type is ErrorType) return Msg.ERROR_TYPE
        val ptrStr = "*".repeat(pointerLevel)

        return buildString {
            if (type.isConst) {
                append(Msg.CONST)
                append(' ')
            }

            when (type) {
                is PrimitiveType -> {
                    append(type.name)
                    append(ptrStr)
                }

                is PointerType -> {
                    append(type.base.stringify(pointerLevel = type.level))
                }

                is FuncType -> {
                    append("func")
                    append(ptrStr)
                    append('(')
                    append(type.paramTypes.joinToString())
                    append("): ")
                    append(type.returnType.stringify())
                }

                is UserType -> {
                    append(type.name)
                    append(ptrStr)
                    if (type.templateArgs.isNotEmpty()) {
                        append('<')
                        append(type.templateArgs.joinToString(", "))
                        append('>')
                    }
                }
            }

//            if (type.isLvalue && withBase && type !is FuncType)
//                append('&')
        }
    }

}

/*fun <T: Type> T.attachType(vararg nodes: ExprNode): T {
    nodes.forEach { node -> node.type = this }
    return this
}*/


