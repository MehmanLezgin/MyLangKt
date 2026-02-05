package lang.semantics.scopes

import lang.nodes.VarDeclStmtNode
import lang.semantics.symbols.FuncParamListSymbol
import lang.semantics.symbols.FuncParamSymbol
import lang.semantics.types.Type

data class FuncParamsScope(
    override val parent: Scope?,
) : Scope(
    parent = parent,
    scopeName = ""
) {

    private val params = mutableListOf<FuncParamSymbol>()

    fun defineParam(node: VarDeclStmtNode, type: Type) : ScopeResult {
        val name = node.name

        val param = FuncParamSymbol(
            name = name.value,
            type = type,
            defaultValue = node.initializer
        )

        params.add(param)
        return define(param)
    }

    fun getParams() = FuncParamListSymbol(list = params.toList())
}