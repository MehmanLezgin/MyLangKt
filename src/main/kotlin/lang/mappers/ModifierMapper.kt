package lang.mappers

import lang.core.KeywordType
import lang.nodes.ModifierNode
import lang.nodes.ModifierNode.*
import lang.tokens.Token

class ModifierMapper : IOneWayMapper<Token.Keyword, ModifierNode?> {
    override fun toSecond(a: Token.Keyword): ModifierNode? = when (a.type) {
        KeywordType.PRIVATE     -> Private(a.range)
        KeywordType.PUBLIC      -> Public(a.range)
        KeywordType.PROTECTED   -> Protected(a.range)
        KeywordType.STATIC      -> Static(a.range)
        KeywordType.OPEN        -> Open(a.range)
        KeywordType.ABSTRACT    -> Abstract(a.range)
        KeywordType.OVERRIDE    -> Override(a.range)
        KeywordType.INFIX       -> Infix(a.range)
        else                    -> null
    }
}
