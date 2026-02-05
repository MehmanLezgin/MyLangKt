package lang.nodes

import lang.core.SourceRange

sealed class ImportKind {
    object Module : ImportKind()
    object Wildcard : ImportKind()
    data class Named(
        val symbols: List<IdentifierNode>
    ) : ImportKind()
}

data class ImportStmtNode(
    val moduleName: IdentifierNode,
    val kind: ImportKind,
    override val range: SourceRange
) : StmtNode {
    override fun mapRecursive(mapper: NodeTransformFunc) = mapper(this)
}