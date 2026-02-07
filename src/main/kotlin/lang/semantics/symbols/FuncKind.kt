package lang.semantics.symbols

import lang.nodes.IdentifierNode
import lang.semantics.types.Type

sealed class FuncKind(open val nameId: IdentifierNode) {
    data class Default(override val nameId: IdentifierNode) : FuncKind(nameId)
    data class Extension(override val nameId: IdentifierNode, val type: Type) : FuncKind(nameId)
    data class Qualified(override val nameId: IdentifierNode, val type: Type) : FuncKind(nameId)
}