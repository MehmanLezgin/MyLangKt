package lang.nodes

import lang.tokens.Pos

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
    override val pos: Pos
) : StmtNode(pos) {
    override fun mapRecursive(mapper: NodeTransformFunc) = mapper(this)
}