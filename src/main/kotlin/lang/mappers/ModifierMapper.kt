package lang.mappers

import lang.tokens.KeywordType
import lang.tokens.Pos
import lang.tokens.Token
import lang.nodes.ModifierNode

class ModifierMapper : IOneWayMapper<Token.Keyword, ModifierNode?> {

    private val map: Map<KeywordType, (Pos) -> ModifierNode> = mapOf(
        KeywordType.PRIVATE   to { pos -> ModifierNode.Private(pos) },
        KeywordType.PUBLIC    to { pos -> ModifierNode.Public(pos) },
        KeywordType.PROTECTED to { pos -> ModifierNode.Protected(pos) },
        KeywordType.EXPORT    to { pos -> ModifierNode.Export(pos) },
        KeywordType.STATIC    to { pos -> ModifierNode.Static(pos) },
        KeywordType.OPEN      to { pos -> ModifierNode.Open(pos) },
        KeywordType.ABSTRACT  to { pos -> ModifierNode.Abstract(pos) },
        KeywordType.OVERRIDE  to { pos -> ModifierNode.Override(pos) },
    )

    override fun toSecond(a: Token.Keyword): ModifierNode? {
        return map[a.type]?.invoke(a.pos)
    }
}
