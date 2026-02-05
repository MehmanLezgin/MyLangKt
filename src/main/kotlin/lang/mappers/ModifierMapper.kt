package lang.mappers

import lang.core.SourceRange
import lang.nodes.ModifierNode
import lang.tokens.KeywordType
import lang.tokens.Token

class ModifierMapper : IOneWayMapper<Token.Keyword, ModifierNode?> {

    private val map: Map<KeywordType, (SourceRange) -> ModifierNode> = mapOf(
        KeywordType.PRIVATE   to { range -> ModifierNode.Private(range) },
        KeywordType.PUBLIC    to { range -> ModifierNode.Public(range) },
        KeywordType.PROTECTED to { range -> ModifierNode.Protected(range) },
        KeywordType.EXPORT    to { range -> ModifierNode.Export(range) },
        KeywordType.STATIC    to { range -> ModifierNode.Static(range) },
        KeywordType.OPEN      to { range -> ModifierNode.Open(range) },
        KeywordType.ABSTRACT  to { range -> ModifierNode.Abstract(range) },
        KeywordType.OVERRIDE  to { range -> ModifierNode.Override(range) },
    )

    override fun toSecond(a: Token.Keyword): ModifierNode? {
        return map[a.type]?.invoke(a.range)
    }
}
