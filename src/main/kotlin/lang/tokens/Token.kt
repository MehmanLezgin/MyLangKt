package lang.tokens

sealed class Token(
    open val raw: String,
    open val pos: Pos
) {
    data class Unknown(override val raw: String, override val pos: Pos) : Token(raw, pos)
    data class EOF(override val pos: Pos) : Token("", pos)
    data class LParen(override val raw: String, override val pos: Pos) : Token(raw, pos)
    data class RParen(override val raw: String, override val pos: Pos) : Token(raw, pos)
    data class LBracket(override val raw: String, override val pos: Pos) : Token(raw, pos)
    data class RBracket(override val raw: String, override val pos: Pos) : Token(raw, pos)
    data class LBrace(override val raw: String, override val pos: Pos) : Token(raw, pos)
    data class RBrace(override val raw: String, override val pos: Pos) : Token(raw, pos)
//    data class Colon(override val raw: String, override val pos: SymbolPos) : Token(raw, pos)
    data class Dot(override val raw: String, override val pos: Pos) : Token(raw, pos)
//    data class Comma(override val raw: String, override val pos: SymbolPos) : Token(raw, pos)
    data class UnclosedQuote(override val raw: String, override val pos: Pos) : Token(raw, pos)
    data class UnclosedComment(override val raw: String, override val pos: Pos) : Token(raw, pos)
    data class Comment(override val raw: String, override val pos: Pos) : Token(raw, pos)
    data class Semicolon(override val raw: String, override val pos: Pos) : Token(raw, pos)

    data class Int32(val value: Int, override val raw: String, override val pos: Pos) : Token(raw, pos)
    data class Int64(val value: Long, override val raw: String, override val pos: Pos) : Token(raw, pos)
    data class UInt32(val value: UInt, override val raw: String, override val pos: Pos) : Token(raw, pos)
    data class UInt64(val value: ULong, override val raw: String, override val pos: Pos) : Token(raw, pos)
    data class Float32(val value: Float, override val raw: String, override val pos: Pos) : Token(raw, pos)
    data class Double64(val value: Double, override val raw: String, override val pos: Pos) : Token(raw, pos)
    data class Bool(val value: Boolean, override val raw: String, override val pos: Pos) : Token(raw, pos)
    data class Null(override val raw: String, override val pos: Pos) : Token(raw, pos)
    data class QuotesStr(val value: String, override val raw: String, override val pos: Pos) : Token(raw, pos)
    data class QuotesChar(val value: Char, override val raw: String, override val pos: Pos) : Token(raw, pos)

    data class Identifier(val value: String, override val raw: String, override val pos: Pos) : Token(raw, pos)
    data class Keyword(val type: KeywordType, override val raw: String, override val pos: Pos) : Token(raw, pos)
    data class Operator(
        val type: OperatorType,
        val precedence: Int,
        override val raw: String,
        override val pos: Pos
    ) : Token(raw, pos)
}
