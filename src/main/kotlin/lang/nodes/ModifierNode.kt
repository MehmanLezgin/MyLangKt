package lang.nodes

import lang.core.SourceRange
import lang.tokens.KeywordType
import kotlin.reflect.KClass

sealed class ModifierNode(
    open val keyword: KeywordType,
    override val range: SourceRange
) : ExprNode {
    data class Private      (override val range: SourceRange) : ModifierNode(KeywordType.PRIVATE, range)
    data class Public       (override val range: SourceRange) : ModifierNode(KeywordType.PUBLIC, range)
    data class Protected    (override val range: SourceRange) : ModifierNode(KeywordType.PROTECTED, range)
    data class Export       (override val range: SourceRange) : ModifierNode(KeywordType.EXPORT, range)
    data class Static       (override val range: SourceRange) : ModifierNode(KeywordType.STATIC, range)
    data class Override     (override val range: SourceRange) : ModifierNode(KeywordType.OVERRIDE, range)
    data class Open         (override val range: SourceRange) : ModifierNode(KeywordType.OPEN, range)
    data class Abstract     (override val range: SourceRange) : ModifierNode(KeywordType.ABSTRACT, range)

    override fun mapRecursive(mapper: NodeTransformFunc) = mapper(this)
}

data class ModifierSetNode(
    override val range: SourceRange,
    val nodes: Set<ModifierNode>
) : ExprNode {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = this.copy(
            nodes = nodes.toList().map { it.mapRecursive(mapper) as? ModifierNode ?: it }.toSet()
        )

        return mapper(newNode)
    }

    fun get(modifierType: KClass<out ModifierNode>) = nodes.find { it::class == modifierType }
}