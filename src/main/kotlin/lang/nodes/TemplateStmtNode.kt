package lang.nodes

import lang.infrastructure.SourceRange

data class TemplateStmtNode(
    val declStmt: DeclStmtNamedNode,
    val params: TemplateParamsListNode,
    override val range: SourceRange,
    override var modifiers: ModifierSetNode?
) : BaseDeclStmtNode {
    val name: IdentifierNode
        get() = declStmt.name

    override fun mapRecursive(mapper: NodeTransformFunc) =
        copy(
            declStmt = declStmt.mapRecursive(mapper) as? DeclStmtNamedNode ?: declStmt,
        )
}