package lang.nodes

import lang.tokens.Pos

sealed class ModifierNode(
    override val pos: Pos
) : ExprNode(pos) {
    data class Private(override val pos: Pos) : ModifierNode(pos)
    data class Public(override val pos: Pos) : ModifierNode(pos)
    data class Protected(override val pos: Pos) : ModifierNode(pos)
    data class Const(override val pos: Pos) : ModifierNode(pos)
    data class Static(override val pos: Pos) : ModifierNode(pos)
    data class Open(override val pos: Pos) : ModifierNode(pos)
    data class Override(override val pos: Pos) : ModifierNode(pos)

    override fun mapRecursive(mapper: NodeTransformFunc) = mapper(this)
}


data class ModifierSetNode(
    override val pos: Pos,
    override val nodes: List<ModifierNode>
) : BlockNode(nodes, pos) {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = this.copy(
            nodes = nodes.map { it.mapRecursive(mapper) as? ModifierNode ?: it }
        )

        return mapper(newNode)
    }
}