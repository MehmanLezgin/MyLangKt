package lang.semantics.builtin.builders

import lang.semantics.symbols.FuncParamListSymbol
import lang.semantics.symbols.FuncParamSymbol
import lang.semantics.types.Type

class FuncParamBuilder(
    val funcParams: MutableList<FuncParamSymbol> = mutableListOf()
) {
    infix fun String.ofType(type: Type) {
        val param = FuncParamSymbol(
            name = this,
            type = type,
            defaultValue = null
        )

        funcParams.add(param)
    }

    fun build() = FuncParamListSymbol(list = funcParams)
}