package lang.tokens

import lang.messages.ErrorHandler
import lang.lexer.ILexer
import kotlin.reflect.KClass

open class BaseTokenStream(
    private val lexer: ILexer,
    private val errorHandler: ErrorHandler
) : ITokenStream {
    private val curToken: Token
        get() = peek()

    override fun reset() {
        lexer.reset()
    }

    companion object {
        val eof = Token.EOF(Pos())
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
        errorHandler.syntaxError(msg = msg, t.pos)
        return false
    }

    override fun expect(vararg classes: KClass<out Token>, msg: String): Boolean {
        val t = peek()

        if (classes.any { it.isInstance(t) }) {
            return true
        }

        errorHandler.syntaxError(msg = msg, t.pos)
        return false
    }


    override fun expectKeyword(type: KeywordType, msg: String): Boolean {
        val t = peek()
        if (t is Token.Keyword && t.type == type)
            return true

        errorHandler.syntaxError(msg = msg, t.pos)
        return false
    }

    override fun matchSemicolonOrLinebreak() : Boolean {
        val t = peek()

        if (t is Token.Semicolon || t is Token.EOF) {
            next()
            return true
        }

        if (prev().pos.line < t.pos.line)
            return true

        return false
    }

    override fun expectSemicolonOrLinebreak(msg: String): Boolean {
        val t = peek()
//        if (t is Token.RBrace) return true

        if (t is Token.Semicolon || t is Token.EOF) {
            next()
            return true
        }

        if (prev().pos.line < t.pos.line)
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

        errorHandler.syntaxError(msg = msg, t.pos)
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