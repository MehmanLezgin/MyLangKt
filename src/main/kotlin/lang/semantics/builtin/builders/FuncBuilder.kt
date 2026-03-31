package lang.semantics.builtin.builders

import lang.core.operators.OperatorType
import lang.semantics.builtin.PrimitivesScope
import lang.semantics.symbols.*
import lang.semantics.types.Type

class OperFuncBuilder(val oper: OperatorType) : FuncBuilder(name = oper.fullName) {
    override fun build(): FuncSymbol {
        return BuiltInOperatorFuncSymbol(
            operator = oper,
            params = funcParams,
            returnType = returnType,
            modifiers = modifiers,
        )
    }
}

class ConstructorBuilder : FuncBuilder(name = FuncKind.CONSTRUCTOR.kindName) {
    override fun build(): FuncSymbol {
        return ConstructorSymbol(
            params = funcParams,
            returnType = returnType,
            modifiers = modifiers
        )
    }
}

open class FuncBuilder(val name: String) {
    internal var funcParams: FuncParamListSymbol = FuncParamListSymbol()
    internal var returnType: Type = PrimitivesScope.void
    internal var modifiers: Modifiers = Modifiers()

    open fun build(): FuncSymbol {
        return FuncSymbol(
            name = name,
            params = funcParams,
            initialReturnType = returnType,
            modifiers = modifiers,
        )
    }

    fun FuncBuilder.modifiers(modifiers: Modifiers) {
        this.modifiers = modifiers
    }

    fun FuncBuilder.returns(type: Type) {
        this.returnType = type
    }

    fun params(block: FuncParamBuilder.() -> Unit) {
        funcParams = FuncParamBuilder(
            funcParams = funcParams.list.toMutableList()
        ).apply(block).build()
    }
}