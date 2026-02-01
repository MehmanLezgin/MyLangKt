package lang.nodes

import lang.tokens.KeywordType
import lang.tokens.Pos
import kotlin.reflect.KClass

sealed class ModifierNode(
    open val keyword: KeywordType,
    override val pos: Pos
) : ExprNode(pos) {
    data class Private      (override val keyword: KeywordType, override val pos: Pos) : ModifierNode(keyword, pos)
    data class Public       (override val keyword: KeywordType, override val pos: Pos) : ModifierNode(keyword, pos)
    data class Protected    (override val keyword: KeywordType, override val pos: Pos) : ModifierNode(keyword, pos)

    data class Const        (override val keyword: KeywordType, override val pos: Pos) : ModifierNode(keyword, pos)
    data class Static       (override val keyword: KeywordType, override val pos: Pos) : ModifierNode(keyword, pos)
    data class Override     (override val keyword: KeywordType, override val pos: Pos) : ModifierNode(keyword, pos)

    data class Open         (override val keyword: KeywordType, override val pos: Pos) : ModifierNode(keyword, pos)
    data class Abstract     (override val keyword: KeywordType, override val pos: Pos) : ModifierNode(keyword, pos)

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