package lang.semantics.types

import lang.messages.Msg
import lang.semantics.builtin.PrimitivesScope.void
import lang.semantics.builtin.PrimitivesScope.voidPtr
import lang.semantics.symbols.TypeSymbol

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
            this.base == void

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

        return true
    }

    override fun hashCode(): Int {
        var result = isConst.hashCode()
        result = 31 * result + isLvalue.hashCode()
        result = 31 * result + isExprType.hashCode()
        return result
    }

    override fun toString() = stringify()

    fun stringify(pointerLevel: Int = 0): String {
        val type = this

        when (type) {
            is ErrorType -> return Msg.ERROR_TYPE
            is UnresolvedType -> return Msg.UNRESOLVED_TYPE
        }
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
        }
    }
}