package lang.semantics

import lang.nodes.ExprNode
import lang.semantics.symbols.Symbol
import lang.semantics.types.Type

data class SemanticContext(
    val types: MutableMap<ExprNode, Type> = mutableMapOf(),
    val symbols: MutableMap<ExprNode, Symbol> = mutableMapOf()
)