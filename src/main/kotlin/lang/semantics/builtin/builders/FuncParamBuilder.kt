package lang.semantics.builtin.builders

import lang.semantics.symbols.FuncParamListSymbol
import lang.semantics.symbols.FuncParamSymbol
import lang.semantics.types.Type

class FuncParamBuilder(
    private val funcParams: MutableList<FuncParamSymbol> = mutableListOf()
) {
    private fun nextParamName() = "x${funcParams.size}"

    fun addParam(name: String, type: Type) {
        val param = FuncParamSymbol(
            name = name,
            type = type
        )

        funcParams.add(param)
    }

    infix fun String.ofType(type: Type) {
        addParam(name = this, type = type)
    }

    val Type.param: Unit
        get() {
            addParam(name = nextParamName(), type = this)
        }

    fun build() = FuncParamListSymbol(list = funcParams)
}