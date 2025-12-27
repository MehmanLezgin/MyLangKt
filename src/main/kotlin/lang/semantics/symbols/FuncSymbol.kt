package lang.semantics.symbols

import lang.nodes.BaseDatatypeNode
import lang.nodes.ExprNode
import lang.nodes.TypeNameListNode

data class FuncParamSymbol(
    override val name: String,
    val datatype: BaseDatatypeNode,
    val defaultValue: ExprNode?
) : Symbol(name)

data class FuncDefinition(
    val typeNames: TypeNameListNode?,
    val params: List<FuncParamSymbol>,
    val returnType: BaseDatatypeNode
)

data class FuncSymbol(
    override val name: String,
    val definitions: MutableList<FuncDefinition> = mutableListOf(),
) : Symbol(name) {
    companion object {
        fun one(name: String, definition: FuncDefinition) = FuncSymbol(
            name = name,
            definitions = mutableListOf(definition)
        )
    }
}