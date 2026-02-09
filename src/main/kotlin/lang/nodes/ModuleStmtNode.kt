package lang.nodes

import lang.core.SourceRange

data class ModuleStmtNode(
    override val name: IdentifierNode,
    val body: BlockNode,
    override val range: SourceRange
) : DeclStmtNode<IdentifierNode>(
    modifiers = null,
    name = name,
    range = range
) {
    val nestedModules: List<ModuleStmtNode>
        get() = body.nodes.filterIsInstance<ModuleStmtNode>()

    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = copy(
            name = name?.mapRecursive(mapper) as? IdentifierNode ?: name,
            body = body.mapRecursive(mapper) as? BlockNode ?: body
        )
        return mapper(newNode)
    }
}