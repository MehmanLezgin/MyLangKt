package lang.mappers

import lang.tokens.KeywordType
import lang.tokens.Pos
import lang.tokens.Token
import lang.nodes.ModifierNode

class ModifierMapper : IOneWayMapper<Token.Keyword, ModifierNode?> {

    private val map: Map<KeywordType, (KeywordType, Pos) -> ModifierNode> = mapOf(
        KeywordType.PRIVATE   to { kw, pos -> ModifierNode.Private(kw, pos) },
        KeywordType.PUBLIC    to { kw, pos -> ModifierNode.Public(kw, pos) },
        KeywordType.PROTECTED to { kw, pos -> ModifierNode.Protected(kw, pos) },
        KeywordType.STATIC    to { kw, pos -> ModifierNode.Static(kw, pos) },
        KeywordType.OPEN      to { kw, pos -> ModifierNode.Open(kw, pos) },
        KeywordType.ABSTRACT  to { kw, pos -> ModifierNode.Abstract(kw, pos) },
        KeywordType.OVERRIDE  to { kw, pos -> ModifierNode.Override(kw, pos) },
        KeywordType.CONST     to { kw, pos -> ModifierNode.Const(kw, pos) }
    )

    override fun toSecond(a: Token.Keyword): ModifierNode? {
        return map[a.type]?.invoke(a.type, a.pos)
    }
}
