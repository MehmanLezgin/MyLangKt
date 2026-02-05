package lang.lexer

import lang.messages.MsgHandler
import lang.core.ILangSpec
import lang.core.LangSpec.operators
import lang.messages.Msg
import lang.core.ISourceCode
import lang.tokens.Token
import lang.tokens.TokenType


class Lexer(
    src: ISourceCode,
    langSpec: ILangSpec,
    msgHandler: MsgHandler,
) : BaseLexer(src, langSpec, msgHandler) {
    private val operatorsByLength = operators.sortedBy { -it.symbol.length }

    private companion object {
        const val DIGITS_10 = "0123456789"

        object Symbols {
            const val INT = "int" // for preventing conflict with 'in' oper: op(in) + id(t)
            const val TRUE = "true"
            const val FALSE = "false"
            const val NULL = "null"
            const val DOT = '.'
            const val LPAREN = '('
            const val RPAREN = ')'
            const val LBRACKET = '['
            const val RBRACKET = ']'
            const val LBRACE = '{'
            const val RBRACE = '}'
            const val COLON = ':'
            const val SEMICOLON = ';'
            const val B = 'b'
            const val E = 'f'
            const val F = 'f'
            const val L = 'l'
            const val U = 'u'
            const val X = 'x'
            const val ZERO = '0'
            const val RADIX_BIN = 2
            const val RADIX_DEC = 10
            const val RADIX_HEX = 16
            const val QUOTE_CHAR = '\''
            const val QUOTE_STRING = '"'
            const val COMMENT = "//"
            const val MULTILINE_COMMENT_OPEN = "/*"
            const val MULTILINE_COMMENT_CLOSE = "*/"

            const val NEW_LINE = '\n'
            const val BACK_SLASH = '\\'
            const val LINE_CONTINUATION = "\\\r\n"
            const val DOUBLE_BACK_SLASH = "\\\\"
        }

        private val radixMap = mapOf(
            2 to '0'..'1', 10 to '0'..'9', 16 to ('0'..'9') + ('a'..'f')
        )

        private val identifierChars = ('a'..'z') + ('0'..'9') + '_'
    }

    private fun getRadix() = if (cur == Symbols.ZERO) {
        val radix = when (at(index + 1)) {
            Symbols.X -> {
                advance(2); Symbols.RADIX_HEX
            }

            Symbols.B -> {
                advance(2); Symbols.RADIX_BIN
            }

            else -> Symbols.RADIX_DEC
        }

        radix
    } else Symbols.RADIX_DEC

    private fun getIntTypeByPostfix(): TokenType {
        val isUnsigned = cur.toLowerCase() == Symbols.U
        if (isUnsigned) advance()
        val isLong = cur.toLowerCase() == Symbols.L
        if (isLong) advance()

        val tokenType = when {
            isUnsigned && isLong -> TokenType.UINT64
            isUnsigned && !isLong -> TokenType.UINT32
            !isUnsigned && isLong -> TokenType.INT64
            else -> TokenType.INT32
        }
        return tokenType
    }

    private fun matchInt(): Token? {
        val start = index

        val radix = getRadix()

        val numberChars = radixMap[radix] ?: return null

        while (cur.toLowerCase() in numberChars) advance()

        if (start == index) return null
        val end = index

        val tokenType = getIntTypeByPostfix()

        val value = substring(start, end)
        val range = closeRange(value)
        return createToken(value, tokenType, range)
    }

    private fun matchFloat(): Token? {
        val pos = getPos()
        val start = index

        var dotFound = false
        var expFound = false

        if (cur == Symbols.DOT) {
            dotFound = true
            advance()
//            if (cur !in DIGITS_10) { }
        }

        while (cur in DIGITS_10) advance()

        if (cur == Symbols.DOT && !dotFound) {
            dotFound = true
            advance()
            while (cur in DIGITS_10) advance()
        }

        if (cur.toLowerCase() == 'e') {
            expFound = true
            advance()
            if (cur == '+' || cur == '-') advance()
            if (cur !in DIGITS_10) lexicalError("Invalid float exponent", pos)
            while (cur in DIGITS_10) advance()
        }

        val isFloat = cur.toLowerCase() == 'f'
        if (isFloat) advance()

        val tokenType = when {
            isFloat -> TokenType.FLOAT
            dotFound || expFound -> TokenType.DOUBLE
            else -> return null
        }

        val value = substring(start, index)
        val range = closeRange(value)
        return createToken(value, tokenType, range)
    }

    private fun matchNumber(): Token? {
        val startChar = at(index)
        val pos = getPos()

        if (!startChar.isDigit() && startChar != Symbols.DOT) return matchInt()

        var lookahead = index
        var dotFound = false
        var expFound = false

        fun hasSingleDot() = at(lookahead) == Symbols.DOT && at(lookahead + 1) != Symbols.DOT

        if (hasSingleDot()) {
            dotFound = true; lookahead++
        }
        while (at(lookahead).isDigit()) lookahead++
        if (!dotFound && hasSingleDot()) {
            dotFound = true; lookahead++
        }
        if (at(lookahead).toLowerCase() == Symbols.E) {
            expFound = true
        }
        val isFloat = at(lookahead).toLowerCase() == Symbols.F
        val numberToken = when {
            isFloat || dotFound || expFound -> matchFloat()
            else -> matchInt()
        }

        val nextChar = at(index)

        if (nextChar != Char.MIN_VALUE && nextChar.isIdentifierChar()) lexicalError(
            Msg.LITERALS_MUST_BE_SURROUNDED_BY_WHITESPACES,
            pos
        )

        return numberToken
    }

    private fun matchId(): Token {
        val start = index

        while (cur.isIdentifierChar()) advance()

        val value = substring(start, index)

        val tokenType = when {
            langSpec.keywords.find { it.value == value } != null -> TokenType.KEYWORD

            langSpec.operators.find { it.symbol == value } != null -> TokenType.OPER

            else -> when (value) {
                Symbols.INT -> TokenType.IDENTIFIER
                Symbols.TRUE -> TokenType.TRUE
                Symbols.FALSE -> TokenType.FALSE
                Symbols.NULL -> TokenType.NULL
                else -> TokenType.IDENTIFIER
            }
        }

        val range = closeRange(value)
        return createToken(value, tokenType, range)
    }

    private fun lexOperator(): Token? {
        val oper = operatorsByLength.firstOrNull { scanFor(it.symbol) } ?: return null
        val operLength = oper.symbol.length
        advance(operLength)
        val range = closeRange(oper.symbol)
        return Token.Operator(oper.type, oper.precedence, oper.symbol, range)
    }

    private fun lexOther(): Token? {
        val c = cur
        advance()

        val tokenType = when (c) {
            Symbols.LPAREN -> TokenType.LPAREN
            Symbols.RPAREN -> TokenType.RPAREN
            Symbols.LBRACKET -> TokenType.LBRACKET
            Symbols.RBRACKET -> TokenType.RBRACKET
            Symbols.LBRACE -> TokenType.LBRACE
            Symbols.RBRACE -> TokenType.RBRACE
            Symbols.COLON -> TokenType.COLON
            Symbols.SEMICOLON -> TokenType.SEMICOLON
            Symbols.DOT -> TokenType.DOT
            else -> null
        } ?: return null

        val range = closeRange(c.toString())
        return createToken(c.toString(), tokenType, range)
    }

    private fun matchStringLiteral(): Token? {
        val pos = getPos()
        val start = index

        val quote = cur

        if (!quote.isQuote()) return null

        advance()

        val quoteEscape = "\\$quote"

        while (index < source.length) {
            if (scanFor(quoteEscape)) {
                advance(quoteEscape.length)
                continue
            }

            if (scanFor(Symbols.DOUBLE_BACK_SLASH)) {
                advance(Symbols.DOUBLE_BACK_SLASH.length)
                continue
            }

            if (scanFor(Symbols.LINE_CONTINUATION)) {
                advance(Symbols.LINE_CONTINUATION.length)
                continue
            }

            when (cur) {
                Symbols.NEW_LINE -> {
                    if (at(index - 1) != Symbols.BACK_SLASH) {
                        lexicalError(Msg.EXPECTED_QUOTE, getPos())
                        return null
                    }
                }

                quote -> {
                    break
                }
            }

            advance()
        }

        val endChar = cur
        advance()

        if (endChar != quote) {
            lexicalError(Msg.EXPECTED_QUOTE, getPos())
            return null
        }

        val rawValue = substring(start, index)
        val range = closeRange(rawValue)
        val value = unescapeString(rawValue, pos)

        return when {
            quote == Symbols.QUOTE_CHAR && value.length == 1 -> Token.Character(value[0], rawValue, range)

            else -> Token.Str(value, rawValue, range)
        }
    }

    private fun skipComments() {
        when {
            scanFor(Symbols.COMMENT) -> {
                while (scanFor(Symbols.COMMENT)) {
                    skipLine()
                    skipWhitespaces()
                }
            }

            scanFor(Symbols.MULTILINE_COMMENT_OPEN) -> {
                val pos = getPos()
                var endFound = false
                advance(Symbols.MULTILINE_COMMENT_OPEN.length)

                while (index < source.length) {
                    endFound = scanFor(Symbols.MULTILINE_COMMENT_CLOSE)
                    if (endFound) break
                    advance()
                }

                if (endFound) {
                    advance(Symbols.MULTILINE_COMMENT_CLOSE.length)
                } else {
                    lexicalError(Msg.EXPECTED_COMMENT_END, pos)
                }
            }
        }
    }

    private fun Char.isQuote() = this == Symbols.QUOTE_STRING || this == Symbols.QUOTE_CHAR

    private fun scanFor(str: String): Boolean {
        if (index >= source.length) return false

        for (i in str.indices) {
            if (at(index + i) != str[i]) return false
        }
        return true
    }

    private fun substring(start: Int, end: Int): String {
        val end1 = if (end > source.length - 1) source.length else end
        return source.substring(start, end1)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun Char.isIdentifierChar() = this.lowercaseChar() in identifierChars

    override fun matchToken(): Token? {
        if (index >= source.length)
            return Token.EOF(closeRange())

        skipComments()

//        state.beginRange()

        val c = cur

        val token = when {
            c == Char.MIN_VALUE -> null
            c.isDigit() || c == Symbols.DOT && at(index + 1).isDigit() -> matchNumber() // d or .d
            c.isIdentifierChar() -> matchId()                                  // id or kw
            c.isQuote() -> matchStringLiteral()                                         // str and char literals
            else -> {
                val t = lexOperator() ?: lexOther()
                if (t == null) lexicalError(Msg.UNEXPECTED_TOKEN, getPos())
                t
            }
        }

//        trackNewlines(token?.raw)

        return token
    }
}