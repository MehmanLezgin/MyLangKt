package lang.nodes

import lang.infrastructure.SourceRange

data class TypeArgsListNode(
    override val nodes: List<ExprNode>,
    override val range: SourceRange
) : BlockNode(
    nodes = nodes,
    range = range
) {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = this.copy(
            nodes = nodes.map { mapper(it) }
        )
        return mapper(newNode)
    }
}