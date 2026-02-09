package lang.nodes

import lang.core.SourceRange

data class QualifiedName(
    val parts: List<IdentifierNode>
)

sealed class NameSpecifier(open val target: QualifiedName, val range: SourceRange?) {
    data class Direct(override val target: QualifiedName) : NameSpecifier(
        target = target,
        range = target.parts.firstOrNull()?.range
    )

    data class Alias(override val target: QualifiedName, val alias: IdentifierNode) :
        NameSpecifier(
            target = target,
            range = target.parts.firstOrNull()?.range?.untilEndOf(alias.range)
        )
}

sealed class NameClause {
    object Wildcard : NameClause()
    data class Items(val items: List<NameSpecifier>) : NameClause()
}

sealed interface BaseImportStmtNode : StmtNode {
    val items: NameClause
}

data class ImportFromStmtNode(
    val sourceName: NameSpecifier,
    override val items: NameClause,
    override val range: SourceRange
) : BaseImportStmtNode {
    override fun mapRecursive(mapper: NodeTransformFunc) = mapper(this)
}

data class ImportModulesStmtNode(
    override val items: NameClause,
    override val range: SourceRange
) : BaseImportStmtNode {
    override fun mapRecursive(mapper: NodeTransformFunc) = mapper(this)
}