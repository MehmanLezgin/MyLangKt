package lang.nodes

import lang.tokens.Pos

data class TypeNameNode(
    val name: IdentifierNode,
    val bound: DatatypeNode?,   // T : Base
    override val pos: Pos
) : ExprNode(pos) {

    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = this.copy(
            name = name.mapRecursive(mapper) as? IdentifierNode ?: name,
            bound = bound
                ?.mapRecursive(mapper) as? DatatypeNode
                ?: bound
        )
        return mapper(newNode)
    }
}

data class TypeNameListNode(
    val params: List<TypeNameNode>,
    override val pos: Pos
) : ExprNode(pos) {

    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = this.copy(
            params = params.map {
                it.mapRecursive(mapper) as? TypeNameNode ?: it
            }
        )
        return mapper(newNode)
    }
}
