package lang.tokens

import lang.messages.Msg
import kotlin.reflect.KClass

interface ITokenStream {
    val pos: Pos
    fun reset()
    fun save()
    fun restore()
    fun clearLastSave()
    fun prev(): Token
    fun peek(): Token
    fun next(): Token
    fun next222(): Token
    fun match(vararg types: KClass<out Token>): Boolean
    fun <T : Token> expect(clazz: KClass<T>, msg: String): Boolean
    fun expect(vararg classes: KClass<out Token>, msg: String): Boolean
    fun expectKeyword(type: KeywordType, msg: String): Boolean
    fun matchSemicolonOrLinebreak(): Boolean
    fun expectSemicolonOrLinebreak(msg: String = Msg.EXPECTED_SEMICOLON): Boolean
    fun skipTokens(vararg classes: KClass<out Token>)
    fun skipUntil(vararg classes: KClass<out Token>)
    fun skipEnclosed(openToken: KClass<out Token>, closeToken: KClass<out Token>)
    fun skipEnclosed(stopAtClosed: Boolean = false, check: (Token) -> Int)
    fun getEnclosedTriBracketsEndToken(): Token
    fun matchOperator(vararg types: OperatorType): Boolean
    fun splitOperators(mapTag: OperatorType)
    fun getTokens(): List<Token>
}
