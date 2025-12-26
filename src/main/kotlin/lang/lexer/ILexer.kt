package lang.lexer

import lang.tokens.Token

interface ILexer {
    fun reset()
    fun save()
    fun restore()
    fun clearLastSave()
    fun tokenizeAll() : List<Token>
    fun nextToken() : Token?
}