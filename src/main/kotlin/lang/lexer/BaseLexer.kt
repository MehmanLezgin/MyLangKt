package lang.lexer

import lang.messages.MsgHandler
import lang.core.ILangSpec
import lang.messages.Msg
import lang.core.SourceCode
import lang.tokens.Pos
import lang.tokens.Token
import lang.tokens.TokenType
import kotlin.text.iterator



open class BaseLexer(
    val src: SourceCode,
    val langSpec: ILangSpec,
    val msgHandler: MsgHandler,
) : ILexer {

    internal val source = src.content

    internal var state = LexerState()
    private var savedStates: ArrayDeque<LexerState> = ArrayDeque()

    internal val cur: Char
        get() = at(index)

    internal fun at(index: Int) = source.getOrNull(index) ?: Char.MIN_VALUE

    internal fun advance(count: Int = 1): Char? {
        index += count
        if (index >= source.length) return null
        return cur
    }

    internal var index: Int
        get() = state.index
        set(value) {
            state.index = value
        }

    override fun reset() {
        state.reset()
        savedStates.clear()
    }

    override fun save() {
        savedStates.add(state.copy())
    }

    override fun restore() {
        if (savedStates.isEmpty()) return
        state = savedStates.removeLast()
    }

    override fun clearLastSave() {
        savedStates.removeLast()
    }

    override fun tokenizeAll(): List<Token> {
        val tokens = mutableListOf<Token>()
        var token: Token? = null

        while (token !is Token.EOF) {
            token = nextToken()

            if (token != null) {
                tokens.add(token)
            }
        }

        return tokens
    }


    internal fun getPos() = Pos(
        line = state.line,
        col = state.col,
        src = src
    )

    override fun nextToken(): Token? {
        skipWhitespaces()

        if (state.index >= source.length)
            return Token.EOF(getPos())

        val token = matchToken() ?: return null

        if (token is Token.Comment)
            return null

        return token
    }

    open fun matchToken(): Token? = null

    internal fun skipWhitespaces() {
        val src = source
        val len = src.length

        while (state.index < len && src[state.index].isWhitespace()) {
            if (src[state.index] == '\n') {
                state.line++
                state.col = 1
            } else
                state.col++

            state.index++
        }
    }

    internal fun trackNewlines(rawValue: String?) {
        if (rawValue == null) return
        var newLines = 0
        var lastLineLen = 0

        for (c in rawValue) {
            if (c == '\n') {
                newLines++
                lastLineLen = 0
            } else {
                lastLineLen++
            }
        }

        if (newLines > 0) {
            state.line += newLines
            state.col = lastLineLen + 1
        } else {
            state.col += rawValue.length
        }
    }

    internal fun skipLine(): Boolean {
        var skipped = false

        for (i in index .. source.length) {
            val c = at(i)

            if (c == '\n' || c == Char.MIN_VALUE) {
                index = i
                index++
                skipped = true
                break
            }
        }

        state.line++
        state.col = 1
        return skipped
    }

    internal fun createToken(value: String, type: TokenType, pos: Pos): Token {
        return when (type) {
            TokenType.EOF -> Token.EOF(pos)

            TokenType.LPAREN -> Token.LParen(value, pos)
            TokenType.RPAREN -> Token.RParen(value, pos)

            TokenType.LBRACKET -> Token.LBracket(value, pos)
            TokenType.RBRACKET -> Token.RBracket(value, pos)

            TokenType.LBRACE -> Token.LBrace(value, pos)
            TokenType.RBRACE -> Token.RBrace(value, pos)

            TokenType.SEMICOLON -> Token.Semicolon(value, pos)
//            TokenType.DOT -> Token.Dot(value, pos)

            TokenType.UNCLOSED_QUOTE -> Token.UnclosedQuote(value, pos)
            TokenType.COMMENT -> Token.Comment(value, pos)
            TokenType.UNCLOSED_COMMENT -> Token.UnclosedComment(value, pos)

            TokenType.QUOTES_STR -> Token.Str(value, value, pos)
            TokenType.QUOTES_CHAR -> Token.Character(value[0], value, pos)

            TokenType.IDENTIFIER -> Token.Identifier(value, value, pos)

            TokenType.INT32 -> parseIntegerToken(value, pos)
            TokenType.INT64 -> parseIntegerToken(value, pos)
            TokenType.UINT32 -> parseIntegerToken(value, pos)
            TokenType.UINT64 -> parseIntegerToken(value, pos)
            TokenType.FLOAT -> parseFloatToken(value, pos)
            TokenType.DOUBLE -> parseDoubleToken(value, pos)
            TokenType.TRUE -> Token.Bool(true, value, pos)
            TokenType.FALSE -> Token.Bool(false, value, pos)
            TokenType.NULL -> Token.Null(value, pos)

            TokenType.KEYWORD -> {
                val keywordType = langSpec.getKeywordInfo(value)?.type ?: return Token.Unknown(raw = value, pos = pos)
                Token.Keyword(keywordType, value, pos)
            }

            TokenType.OPER -> {
                val operatorInfo = langSpec.getOperatorInfo(value) ?: return Token.Unknown(raw = value, pos = pos)
                Token.Operator(operatorInfo.type, operatorInfo.precedence, value, pos)
            }

            else -> Token.Unknown(raw = value, pos = pos)
        }
    }

    internal fun unescapeString(raw: String, pos: Pos): String {
        val s = raw.drop(1).dropLast(1) // remove surrounding quotes
        val sb = StringBuilder()

        var i = 0
        var line = pos.line
        var col = pos.col + 1

        // Constants for Unicode escape sequences
        val UNICODE_ESCAPE_LENGTH = 6        // \uXXXX
        val SURROGATE_PAIR_LENGTH = 12       // \uXXXX\uXXXX

        fun errorAt(msg: String) = lexicalError(msg, Pos(line, col, src))
        fun errorAndSkip(msg: String) {
            errorAt(msg)
            i++
            col++
        }

        fun advance(c: Char) {
            if (c == '\n') {
                line++; col = 1
            } else col++
            i++
        }

        fun readUnicode(offset: Int): Int? {
            if (offset + 4 > s.length) return null
            val hex = s.substring(offset, offset + 4)
            return hex.toIntOrNull(16)
        }

        fun isLowSurrogateStart(offset: Int) = offset + 1 < s.length && s[offset] == '\\' && s[offset + 1] == 'u'

        fun handleSurrogatePair(hi: Int) {
            val lowOffset = i + UNICODE_ESCAPE_LENGTH
            if (!isLowSurrogateStart(lowOffset)) {
                errorAndSkip("Expected low surrogate after high surrogate")
                return
            }

            val lo = readUnicode(lowOffset + 2)
            if (lo == null || lo !in 0xDC00..0xDFFF) {
                errorAndSkip("Invalid low surrogate")
                return
            }

            val codePoint = 0x10000 + ((hi - 0xD800) shl 10) + (lo - 0xDC00)
            sb.append(Character.toChars(codePoint))
            i += SURROGATE_PAIR_LENGTH
            col += SURROGATE_PAIR_LENGTH
        }

        fun handleLineContinuation(next: Char) {
            line++
            col = 1
            i += if (next == '\r' && i + 2 < s.length && s[i + 2] == '\n') 3 else 2
        }

        fun handleUnicodeEscape() {
            val hi = readUnicode(i + 2)
            if (hi == null) {
                errorAndSkip("Invalid unicode escape"); return
            }

            when {
                hi in 0xD800..0xDBFF -> handleSurrogatePair(hi)
                hi in 0xDC00..0xDFFF -> errorAndSkip("Unexpected low surrogate")
                else -> {
                    sb.append(hi.toChar())
                    i += UNICODE_ESCAPE_LENGTH
                    col += UNICODE_ESCAPE_LENGTH
                }
            }
        }

        fun handleEscape() {
            if (i + 1 >= s.length)
                errorAt(Msg.ILLEGAL_ESCAPE_SEQUENCE)

            val next = s[i + 1]

            when (next) {
                'n' -> sb.append('\n')
                'r' -> sb.append('\r')
                't' -> sb.append('\t')
                'b' -> sb.append('\b')
                'f' -> sb.append('\u000C')
                '\'', '"', '\\' -> sb.append(next)
                '\n', '\r' -> handleLineContinuation(next)
                'u' -> handleUnicodeEscape()
                else -> errorAt("${Msg.ILLEGAL_ESCAPE_SEQUENCE} \\$next")
            }

            if (next !in listOf('\n', '\r', 'u')) { // normal 2-char escape
                i += 2
                col += 2
            }
        }

        while (i < s.length) {
            val c = s[i]
            if (c != '\\') {
                sb.append(c)
                advance(c)
            } else handleEscape()
        }

        return sb.toString()
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    internal fun parseIntegerToken(value: String, pos: Pos): Token {
        val cleanValue = value.replace("[uUlL]".toRegex(), "") // убираем все буквы постфиксов
        val isUnsigned = value.contains('u', ignoreCase = true)
        val isLong = value.contains('l', ignoreCase = true)

        // Определяем систему счисления
        var number: ULong = 0.toULong()

        try {
            number = when {
                value.startsWith("0x", ignoreCase = true) -> cleanValue.removePrefix("0x").toULong(16)
                value.startsWith("0b", ignoreCase = true) -> cleanValue.removePrefix("0b").toULong(2)
                else -> cleanValue.toULong(10)
            }
        } catch (_: NumberFormatException) {
            lexicalError(Msg.INVALID_INT_LITERAL, pos)
        }

        return when {
            isUnsigned && isLong -> Token.UInt64(raw = value, value = number, pos = pos)
            isUnsigned -> Token.UInt32(raw = value, value = number.toUInt(), pos = pos)
            isLong -> Token.Int64(raw = value, value = number.toLong(), pos = pos)
            else -> Token.Int32(raw = value, value = number.toInt(), pos = pos)
        }
    }

    internal fun parseFloatToken(value: String, pos: Pos): Token.Float32 {
        var clean = 0.0f
        try {
            clean = value.removeSuffix("d").removeSuffix("D").toFloat()
        } catch (_: NumberFormatException) {
            lexicalError(Msg.INVALID_FLOAT_LITERAL, pos)
        }
        return Token.Float32(clean, value, pos)
    }

    internal fun parseDoubleToken(value: String, pos: Pos): Token.Double64 {
        var clean = 0.0
        try {
            clean = value.removeSuffix("d").removeSuffix("D").toDouble()
        } catch (_: NumberFormatException) {
            lexicalError(Msg.INVALID_DOUBLE_LITERAL, pos)
        }
        return Token.Double64(clean, value, pos)
    }

    internal fun lexicalError(message: String, pos: Pos) {
        msgHandler.lexicalError(message, pos)
    }
}