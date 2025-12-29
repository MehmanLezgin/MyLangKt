package lang.semantics.scopes

import lang.messages.ErrorHandler
import lang.nodes.VarDeclStmtNode
import lang.semantics.symbols.FuncParamListSymbol
import lang.semantics.symbols.FuncParamSymbol
import lang.semantics.symbols.Symbol

data class FuncParamsScope(
    override val parent: Scope?,
    override val errorHandler: ErrorHandler
) : Scope(
    parent = parent,
    errorHandler = errorHandler
) {

    private val params = mutableListOf<FuncParamSymbol>()

    fun defineParam(node: VarDeclStmtNode) : Symbol? {
        val name = node.name

        val param = FuncParamSymbol(
            name = name.value,
            datatype = node.dataType,
            defaultValue = node.initializer
        )

        params.add(param)
        return define(param, name.pos)
    }

    fun getParams() = FuncParamListSymbol(list = params.toList())
}