package lang.semantics.symbols

import lang.nodes.ExprNode
import lang.semantics.types.FuncType
import lang.semantics.types.Type
import lang.semantics.types.TypeFlags
import lang.tokens.OperatorType

data class FuncParamSymbol(
    override val name: String,
    val type: Type,
    val defaultValue: ExprNode?
) : Symbol(name = name) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FuncParamSymbol) return false

        return type == other.type
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }
}

data class FuncParamListSymbol(
    val list: List<FuncParamSymbol> = listOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FuncParamListSymbol

        return list == other.list
    }

    override fun hashCode(): Int {
        return list.hashCode()
    }
}

data class OverloadedFuncSymbol(
    override val name: String,
    val overloads: MutableList<FuncSymbol> = mutableListOf(),
    override val modifiers: Modifiers = Modifiers()
) : Symbol(name = name, modifiers = modifiers) {
    fun hasOverload(funcSym: FuncSymbol?) : Boolean {
        if (funcSym == null) return false
        return overloads.find { it == funcSym } != null
    }
}

open class FuncSymbol(
    override val name: String,
//    open val typeNames: TypeNameListNode?,
    open val params: FuncParamListSymbol,
    open val returnType: Type,
    override val modifiers: Modifiers = Modifiers()
) : Symbol(name = name, modifiers = modifiers) {
    val paramTypes: List<Type>
        get() = params.list.map { it.type }

    fun toOverloadedFuncSymbol() = OverloadedFuncSymbol(
        name = name,
        overloads = mutableListOf(this)
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FuncSymbol

        if (name != other.name) return false
//        if (typeNames != other.typeNames) return false
        if (params != other.params) return false
        if (returnType != other.returnType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
//        result = 31 * result + (typeNames?.hashCode() ?: 0)
        result = 31 * result + params.hashCode()
        result = 31 * result + returnType.hashCode()
        return result
    }

    fun toFuncType() =
        FuncType(
            paramTypes = paramTypes,
            returnType = returnType,
            funcDeclaration = this,
            flags = TypeFlags(
                isExprType = true
            )
        )

    override fun toString(): String {
        return "FuncSymbol(name='$name', params=$params, returnType=$returnType)"
    }
}

data class OperatorFuncSymbol(
    val operator: OperatorType,
//    override val typeNames: TypeNameListNode?,
    override val params: FuncParamListSymbol,
    override val returnType: Type,
    override val modifiers: Modifiers = Modifiers()
) : FuncSymbol(
    name = operator.fullName,
//    typeNames = typeNames,
    params = params,
    returnType = returnType,
)

data class BuiltInOperatorFuncSymbol(
    val operator: OperatorType,
    override val params: FuncParamListSymbol,
    override val returnType: Type,
    override val modifiers: Modifiers = Modifiers()
) : FuncSymbol(
    name = operator.fullName,
//    typeNames = null,
    params = params,
    returnType = returnType,
)


data class ConstructorSymbol(
    override val name: String,
    override val params: FuncParamListSymbol,
    override val returnType: Type,
    override val modifiers: Modifiers = Modifiers()
) : FuncSymbol(
    name = name,
//    typeNames = null,
    params = params,
    returnType = returnType,
)
data class DestructorSymbol(
    override val name: String,
    override val returnType: Type
) : FuncSymbol(
    name = name,
//    typeNames = null,
    params = FuncParamListSymbol(list = emptyList()),
    returnType = returnType,
)
