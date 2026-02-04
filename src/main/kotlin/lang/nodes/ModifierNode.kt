package lang.nodes

import lang.tokens.KeywordType
import lang.tokens.Pos
import kotlin.reflect.KClass

sealed class ModifierNode(
    open val keyword: KeywordType,
    override val pos: Pos
) : ExprNode(pos) {
    data class Private      (override val pos: Pos) : ModifierNode(KeywordType.PRIVATE, pos)
    data class Public       (override val pos: Pos) : ModifierNode(KeywordType.PUBLIC, pos)
    data class Protected    (override val pos: Pos) : ModifierNode(KeywordType.PROTECTED, pos)
    data class Export       (override val pos: Pos) : ModifierNode(KeywordType.EXPORT, pos)
    data class Static       (override val pos: Pos) : ModifierNode(KeywordType.STATIC, pos)
    data class Override     (override val pos: Pos) : ModifierNode(KeywordType.OVERRIDE, pos)
    data class Open         (override val pos: Pos) : ModifierNode(KeywordType.OPEN, pos)
    data class Abstract     (override val pos: Pos) : ModifierNode(KeywordType.ABSTRACT, pos)

    override fun mapRecursive(mapper: NodeTransformFunc) = mapper(this)
}

data class ModifierSetNode(
    override val pos: Pos,
    val nodes: Set<ModifierNode>
) : ExprNode(pos) {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = this.copy(
            nodes = nodes.toList().map { it.mapRecursive(mapper) as? ModifierNode ?: it }.toSet()
        )

        return mapper(newNode)
    }

    fun get(modifierType: KClass<out ModifierNode>) = nodes.find { it::class == modifierType }
}