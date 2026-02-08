package lang.lexer

import lang.messages.MsgHandler
import lang.core.LangSpec
import lang.core.LangSpec.operators
import lang.messages.Msg
import lang.core.ISourceCode
import lang.tokens.Token
import lang.tokens.TokenType


class Lexer(
    src: ISourceCode,
    langSpec: LangSpec,
    msgHandler: MsgHandler,
) : BaseLexer(src, langSpec, msgHandler) {
    private val operatorsByLength = operators.toList().map { it.second }.sortedBy { -it.raw.length }

    private companion object {
        const val DIGITS_10 = "0123456789"

        private val radixMap = mapOf(
            2 to '0'..'1', 10 to '0'..'9', 16 to ('0'..'9') + ('a'..'f')
        )

        private val identifierChars = ('a'..'z') + ('0'..'9') + '_'
    }


    private fun getRadix() = if (cur == LexSymbols.ZERO) {
        val radix = when (at(index + 1)) {
            LexSymbols.X -> {
                advance(2); LexSymbols.RADIX_HEX
            }

            LexSymbols.B -> {
                advance(2); LexSymbols.RADIX_BIN
            }

            else -> LexSymbols.RADIX_DEC
        }

        radix
    } else LexSymbols.RADIX_DEC

    private fun getIntTypeByPostfix(): TokenType {
        val isUnsigned = cur.lowercaseChar() == LexSymbols.U
        if (isUnsigned) advance()
        val isLong = cur.lowercaseChar() == LexSymbols.L
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

        while (cur.lowercaseChar() in numberChars) advance()

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

        if (cur == LexSymbols.DOT) {
            dotFound = true
            advance()
//            if (cur !in DIGITS_10) { }
        }

        while (cur in DIGITS_10) advance()

        if (cur == LexSymbols.DOT && !dotFound) {
            dotFound = true
            advance()
            while (cur in DIGITS_10) advance()
        }

        if (cur.lowercaseChar() == 'e') {
            expFound = true
            advance()
            if (cur == '+' || cur == '-') advance()
            if (cur !in DIGITS_10) lexicalError("Invalid float exponent", pos)
            while (cur in DIGITS_10) advance()
        }

        val isFloat = cur.lowercaseChar() == 'f'
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

        if (!startChar.isDigit() && startChar != LexSymbols.DOT) return matchInt()

        var lookahead = index
        var dotFound = false
        var expFound = false

        fun hasSingleDot() = at(lookahead) == LexSymbols.DOT && at(lookahead + 1) != LexSymbols.DOT

        if (hasSingleDot()) {
            dotFound = true; lookahead++
        }
        while (at(lookahead).isDigit()) lookahead++
        if (!dotFound && hasSingleDot()) {
            dotFound = true; lookahead++
        }
        if (at(lookahead).lowercaseChar() == LexSymbols.E) {
            expFound = true
        }
        val isFloat = at(lookahead).lowercaseChar() == LexSymbols.F
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
            langSpec.getKeywordInfo(value) != null -> TokenType.KEYWORD

            langSpec.getOperatorInfo(value) != null -> TokenType.OPER

            else -> when (value) {
                LexSymbols.INT -> TokenType.IDENTIFIER
                LexSymbols.TRUE -> TokenType.TRUE
                LexSymbols.FALSE -> TokenType.FALSE
                LexSymbols.NULL -> TokenType.NULL
                else -> TokenType.IDENTIFIER
            }
        }

        val range = closeRange(value)
        return createToken(value, tokenType, range)
    }

    private fun lexOperator(): Token? {
        val oper = operatorsByLength.firstOrNull { scanFor(it.raw) } ?: return null
        val operLength = oper.raw.length
        advance(operLength)
        val range = closeRange(oper.raw)
        return Token.Operator(oper.type, oper.precedence, oper.raw, range)
    }

    private fun lexOther(): Token? {
        val c = cur
        advance()

        val tokenType = when (c) {
            LexSymbols.LPAREN -> TokenType.LPAREN
            LexSymbols.RPAREN -> TokenType.RPAREN
            LexSymbols.LBRACKET -> TokenType.LBRACKET
            LexSymbols.RBRACKET -> TokenType.RBRACKET
            LexSymbols.LBRACE -> TokenType.LBRACE
            LexSymbols.RBRACE -> TokenType.RBRACE
            LexSymbols.COLON -> TokenType.COLON
            LexSymbols.SEMICOLON -> TokenType.SEMICOLON
            LexSymbols.DOT -> TokenType.DOT
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

            if (scanFor(LexSymbols.DOUBLE_BACK_SLASH)) {
                advance(LexSymbols.DOUBLE_BACK_SLASH.length)
                continue
            }

            if (scanFor(LexSymbols.LINE_CONTINUATION)) {
                advance(LexSymbols.LINE_CONTINUATION.length)
                continue
            }

            when (cur) {
                LexSymbols.NEW_LINE -> {
                    if (at(index - 1) != LexSymbols.BACK_SLASH) {
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
            quote == LexSymbols.QUOTE_CHAR && value.length == 1 -> Token.Character(value[0], rawValue, range)

            else -> Token.Str(value, rawValue, range)
        }
    }

    private fun Char.isQuote() = this == LexSymbols.QUOTE_STRING || this == LexSymbols.QUOTE_CHAR

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

    private var requiredSemicolon: Token? = null

    private fun skipWhitespacesAndComments(): Boolean {
        var preLine = state.endLine

        skipWhitespaces()

        when {
            scanFor(LexSymbols.COMMENT) -> {
                while (scanFor(LexSymbols.COMMENT)) {
                    skipLine()
                    requiredSemicolon = getSemicolonIfRequired()
                    return true
                }
            }

            scanFor(LexSymbols.MULTILINE_COMMENT_OPEN) -> {
                val pos = getPos()
                var endFound = false
                advance(LexSymbols.MULTILINE_COMMENT_OPEN.length)

                while (index < source.length) {
                    endFound = scanFor(LexSymbols.MULTILINE_COMMENT_CLOSE)
                    if (endFound) break
                    if (cur == LexSymbols.NEW_LINE)
                        skipLine()
                    else
                        advance()
                }

                if (endFound) {
                    advance(LexSymbols.MULTILINE_COMMENT_CLOSE.length)
                    closeRange()
                    requiredSemicolon = getSemicolonIfRequired()
                    return true
                } else {
                    lexicalError(Msg.EXPECTED_COMMENT_END, pos)
                }
            }
        }

        if (preLine != state.endLine)
            requiredSemicolon = getSemicolonIfRequired()

        return skipWhitespaces()
    }

    override fun nextToken(): Token? {
        while (true) {
            if (!skipWhitespacesAndComments()) {
                requiredSemicolon?.let {
                    requiredSemicolon = null
                    return it
                }

                break
            }
        }

        if (state.index >= source.length)
            return Token.EOF(closeRange())

        val c = cur

        val token = when {
            c == Char.MIN_VALUE -> null
            c.isDigit() || c == LexSymbols.DOT && at(index + 1).isDigit() -> matchNumber() // d or .d
            c.isIdentifierChar() -> matchId()                                  // id or kw
            c.isQuote() -> matchStringLiteral()                                         // str and char literals
            else -> {
                val t = lexOperator() ?: lexOther()
                if (t == null) {
                    lexicalError(Msg.UNEXPECTED_TOKEN, getPos())
                }
                return t
            }
        }

//        trackNewlines(token?.raw)

        return token
    }
}