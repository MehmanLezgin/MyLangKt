package lang.semantics.builtin.builders

import lang.semantics.builtin.PrimitivesScope
import lang.semantics.symbols.BuiltInOperatorFuncSymbol
import lang.semantics.symbols.FuncParamListSymbol
import lang.semantics.symbols.FuncSymbol
import lang.semantics.symbols.Modifiers
import lang.semantics.types.Type
import lang.core.operators.OperatorType

class FuncBuilder(val name: String) {
    private var oper: OperatorType? = null
    private var funcParams: FuncParamListSymbol = FuncParamListSymbol()
    private var returnType: Type = PrimitivesScope.void
    private var modifiers: Modifiers = Modifiers()

    constructor(oper: OperatorType) : this(oper.fullName) {
        this.oper = oper
    }

    fun build(): FuncSymbol {
        val paramList = funcParams

        val func = if (oper == null)
            FuncSymbol(
                name = name,
                params = paramList,
                returnType = returnType,
                modifiers = modifiers,
            )
        else
            BuiltInOperatorFuncSymbol(
                operator = oper!!,
                params = paramList,
                returnType = returnType,
                modifiers = modifiers,
            )

        return func
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

