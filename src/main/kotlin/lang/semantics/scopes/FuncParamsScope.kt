package lang.semantics.scopes

import lang.nodes.VarDeclStmtNode
import lang.semantics.symbols.FuncParamSymbol
import lang.semantics.symbols.Symbol

data class FuncParamsScope(
    override val parent: Scope?,
) : Scope(parent = parent) {
    fun defineParam(node: VarDeclStmtNode) : Symbol? {
        val name = node.name.value

        val param = FuncParamSymbol(
            name = name,
            datatype = node.dataType,
            defaultValue = node.initializer
        )

        return define(param)
    }

    fun getParams(): List<Symbol?> {
        return symbols.keys.map { symbols[it] }
    }
}