package lang.semantics.symbols

import lang.nodes.BaseDatatypeNode
import lang.nodes.ExprNode
import lang.nodes.TypeNameListNode
import lang.nodes.VoidDatatypeNode
import lang.tokens.OperatorType

data class FuncParamSymbol(
    override val name: String,
    val datatype: BaseDatatypeNode,
    val defaultValue: ExprNode?
) : Symbol(name) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FuncParamSymbol) return false

        return datatype == other.datatype
    }

    override fun hashCode(): Int {
        return datatype.hashCode()
    }
}

data class FuncParamListSymbol(
    val list: List<FuncParamSymbol>
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

/*data class FuncDefinition(
    val typeNames: TypeNameListNode?,
    val params: FuncParamListSymbol,
    val returnType: BaseDatatypeNode
)*/

data class OverloadedFuncSymbol(
    override val name: String,
    val overloads: MutableList<FuncSymbol> = mutableListOf(),
) : Symbol(name) {
    fun hasOverload(funcSym: FuncSymbol?) : Boolean {
        if (funcSym == null) return false
        return overloads.find { it == funcSym } != null
    }
}

open class FuncSymbol(
    override val name: String,
    open val typeNames: TypeNameListNode?,
    open val params: FuncParamListSymbol,
    open val returnType: BaseDatatypeNode
) : Symbol(name) {
    fun toOverloadedFuncSymbol() = OverloadedFuncSymbol(
        name = name,
        overloads = mutableListOf(this)
    )
}

data class OperatorFuncSymbol(
    val operator: OperatorType,
    override val typeNames: TypeNameListNode?,
    override val params: FuncParamListSymbol,
    override val returnType: BaseDatatypeNode
) : FuncSymbol(
    name = operator.name,
    typeNames = typeNames,
    params = params,
    returnType = returnType,
)

data class ConstructorSymbol(
    override val name: String,
    override val params: FuncParamListSymbol,
    override val returnType: BaseDatatypeNode
) : FuncSymbol(
    name = name,
    typeNames = null,
    params = params,
    returnType = returnType,
)
data class DestructorSymbol(
    override val name: String,
    override val returnType: BaseDatatypeNode
) : FuncSymbol(
    name = name,
    typeNames = null,
    params = FuncParamListSymbol(list = emptyList()),
    returnType = returnType,
)
