package lang.nodes

import lang.infrastructure.SourceRange

data class TemplateParamNode(
    val name: IdentifierNode,
    val bound: BaseDatatypeNode?,   // T : Base
    override val range: SourceRange
) : ExprNode {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = this.copy(
            name = name.mapRecursive(mapper) as? IdentifierNode ?: name,
            bound = bound
                ?.mapRecursive(mapper) as? BaseDatatypeNode
                ?: bound
        )
        return mapper(newNode)
    }
}

data class TemplateParamsListNode(
    val params: List<TemplateParamNode>,
    override val range: SourceRange
) : ExprNode {

    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = this.copy(
            params = params.map {
                it.mapRecursive(mapper) as? TemplateParamNode ?: it
            }
        )
        return mapper(newNode)
    }
}
