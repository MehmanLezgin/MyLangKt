package lang.semantics.symbols

import lang.core.SourceRange
import lang.core.operators.OperatorType
import lang.semantics.types.FuncType
import lang.semantics.types.Type
import lang.semantics.types.TypeFlags

open class FuncParamSymbol(
    override val name: String,
    override var type: Type,
    val range: SourceRange? = null
) : VarSymbol(
    name = name,
    type = type,
    isMutable = false,
    isParameter = true,
    modifiers = Modifiers()
) {
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
    val kind: FuncKind,
    val overloads: MutableList<FuncSymbol> = mutableListOf(),
) : Symbol(name = name, modifiers = Modifiers()) {
    fun hasOverload(funcSym: FuncSymbol?): Boolean {
        if (funcSym == null) return false
        return overloads.find { it == funcSym } != null
    }
}

open class FuncSymbol(
    override val name: String,
    open val params: FuncParamListSymbol,
    open val returnType: Type,
    val isExtension: Boolean = false,
    override val modifiers: Modifiers = Modifiers()
) : Symbol(name = name, modifiers = modifiers) {
    open val kind: FuncKind = FuncKind.FUNCTION

    val paramTypes: List<Type>
        get() = params.list.map { it.type }

    fun toOverloadedFuncSymbol() = OverloadedFuncSymbol(
        name = name,
        kind = kind,
        overloads = mutableListOf(this)
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FuncSymbol

        if (name != other.name) return false
        if (params != other.params) return false
        if (returnType != other.returnType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
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

open class OperatorFuncSymbol(
    open val operator: OperatorType,
    override val params: FuncParamListSymbol,
    override val returnType: Type,
    override val modifiers: Modifiers = Modifiers()
) : FuncSymbol(
    name = operator.fullName,
    params = params,
    returnType = returnType,
) {
    override val kind: FuncKind = FuncKind.OPERATOR
}

data class BuiltInOperatorFuncSymbol(
    override val operator: OperatorType,
    override val params: FuncParamListSymbol,
    override val returnType: Type,
    override val modifiers: Modifiers = Modifiers()
) : OperatorFuncSymbol(
    operator = operator,
    params = params,
    returnType = returnType,
    modifiers = modifiers
)


data class ConstructorSymbol(
    override val name: String,
    override val params: FuncParamListSymbol,
    override val returnType: Type,
    override val modifiers: Modifiers = Modifiers()
) : FuncSymbol(
    name = name,
    params = params,
    returnType = returnType,
) {
    override val kind: FuncKind = FuncKind.CONSTRUCTOR
}

data class DestructorSymbol(
    override val name: String,
    override val returnType: Type
) : FuncSymbol(
    name = name,
    params = FuncParamListSymbol(list = emptyList()),
    returnType = returnType,
) {
    override val kind: FuncKind = FuncKind.DESTRUCTOR
}