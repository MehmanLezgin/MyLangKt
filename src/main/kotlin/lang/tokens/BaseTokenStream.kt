package lang.tokens

import lang.core.RangeBuilder
import lang.core.SourceRange
import lang.lexer.ILexer
import lang.messages.MsgHandler
import kotlin.reflect.KClass

open class BaseTokenStream(
    private val lexer: ILexer,
    private val msgHandler: MsgHandler
) : ITokenStream {
    override val range: SourceRange
        get() = peek().range

    override val prevRange: SourceRange
        get() = prev().range

    internal open val eof: Token = Token.EOF(range = SourceRange())

    override fun <T> captureRangeToCur(block: RangeBuilder.() -> T): T {
        return RangeBuilder(range) { range }.block()
    }

    override fun <T> captureRange(block: RangeBuilder.() -> T): T {
        return RangeBuilder(range) { prevRange }.block()
    }

    override fun reset() {
        lexer.reset()
    }

    override fun prev(): Token = eof

    override fun peek(): Token = eof

    override fun next(): Token = eof

    override fun next222(): Token = eof

    override fun match(vararg types: KClass<out Token>): Boolean {
        val t = peek()
        return types.any { it.isInstance(t) }
    }

    override fun <T : Token> expect(clazz: KClass<T>, msg: String): Boolean {
        val t = peek()
        if (clazz.isInstance(t)) {
            return true
        }
        msgHandler.syntaxError(msg = msg, t.range)
        return false
    }

    override fun expect(vararg classes: KClass<out Token>, msg: String): Boolean {
        val t = peek()

        if (classes.any { it.isInstance(t) }) {
            return true
        }

        msgHandler.syntaxError(msg = msg, t.range)
        return false
    }


    override fun expectKeyword(type: KeywordType, msg: String): Boolean {
        val t = peek()
        if (t is Token.Keyword && t.type == type)
            return true

        msgHandler.syntaxError(msg = msg, t.range)
        return false
    }

    override fun matchSemicolonOrLinebreak() : Boolean {
        val t = peek()

        if (t is Token.Semicolon || t is Token.EOF) {
            next()
            return true
        }

        if (t.isTokenOnNewLine())
            return true

        return false
    }

    private fun Token.isTokenOnNewLine() =
        prev().range.end.line < this.range.start.line

    override fun expectSemicolonOrLinebreak(msg: String): Boolean {
        val t = peek()
//        if (t is Token.RBrace) return true

        if (t is Token.Semicolon || t is Token.EOF) {
            next()
            return true
        }

        if (t.isTokenOnNewLine())
            return true

        when (t) {
            is Token.RBrace,
            is Token.RParen,
            is Token.RBracket,
            is Token.Operator,
            is Token.Character,
            is Token.Str,
            is Token.UnclosedQuote,
            is Token.Unknown -> next()
            else -> {}
        }

        msgHandler.syntaxError(msg = msg, t.range)
        return false
    }

    override fun skipTokens(vararg classes: KClass<out Token>) {
        while (classes.any { it.isInstance(peek()) })
            next()
    }

    override fun skipUntil(vararg classes: KClass<out Token>) {
        while (!classes.any { it.isInstance(peek()) })
            next()
    }

    override fun skipEnclosed(openToken: KClass<out Token>, closeToken: KClass<out Token>) {
        next()
        var depth = 1

        while (!match(Token.EOF::class) && depth > 0) {
            when {
                match(openToken) -> depth++
                match(closeToken) -> depth--
            }

            next()
        }
    }

    override fun skipEnclosed(stopAtClosed: Boolean, check: (Token) -> Int) {
        next()
        var depth = 1

        while (!match(Token.EOF::class) && depth > 0) {
            depth += check(peek())

            if (stopAtClosed && depth == 0)
                continue

            next()
        }
    }

    override fun getEnclosedTriBracketsEndToken(): Token = eof


    override fun matchOperator(vararg types: OperatorType): Boolean {
        val t = peek()
        return t is Token.Operator && types.any { it == t.type }
    }

    override fun save() {
        lexer.save()
    }

    override fun restore() {
        lexer.restore()
    }

    override fun clearLastSave() {
        lexer.clearLastSave()
    }

    override fun splitOperators(mapTag: OperatorType) {

    }

    override fun getTokens(): List<Token> {
        return emptyList()
    }
}