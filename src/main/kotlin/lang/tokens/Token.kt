@file:OptIn(ExperimentalUnsignedTypes::class)

package lang.tokens

import lang.core.KeywordType
import lang.core.operators.OperatorType
import lang.core.SourceRange

sealed class Token(
    open val raw: String,
    open val range: SourceRange
) {
    data class Unknown(override val raw: String, override val range: SourceRange) : Token(raw, range)
    data class EOF(override val range: SourceRange) : Token("", range)
    data class LParen(override val raw: String, override val range: SourceRange) : Token(raw, range)
    data class RParen(override val raw: String, override val range: SourceRange) : Token(raw, range)
    data class LBracket(override val raw: String, override val range: SourceRange) : Token(raw, range)
    data class RBracket(override val raw: String, override val range: SourceRange) : Token(raw, range)
    data class LBrace(override val raw: String, override val range: SourceRange) : Token(raw, range)
    data class RBrace(override val raw: String, override val range: SourceRange) : Token(raw, range)
    data class UnclosedQuote(override val raw: String, override val range: SourceRange) : Token(raw, range)
    data class UnclosedComment(override val raw: String, override val range: SourceRange) : Token(raw, range)
    data class Semicolon(override val raw: String, override val range: SourceRange) : Token(raw, range)

    data class Int32(val value: Int, override val raw: String, override val range: SourceRange) : Token(raw, range)
    data class Int64(val value: Long, override val raw: String, override val range: SourceRange) : Token(raw, range)
    data class UInt32(val value: UInt, override val raw: String, override val range: SourceRange) : Token(raw, range)
    data class UInt64(val value: ULong, override val raw: String, override val range: SourceRange) : Token(raw, range)
    data class Float32(val value: Float, override val raw: String, override val range: SourceRange) : Token(raw, range)
    data class Double64(val value: Double, override val raw: String, override val range: SourceRange) : Token(raw, range)
    data class Bool(val value: Boolean, override val raw: String, override val range: SourceRange) : Token(raw, range)
    data class Null(override val raw: String, override val range: SourceRange) : Token(raw, range)
    data class Str(val value: String, override val raw: String, override val range: SourceRange) : Token(raw, range)
    data class Character(val value: Char, override val raw: String, override val range: SourceRange) : Token(raw, range)

    data class Identifier(val value: String, override val raw: String, override val range: SourceRange) : Token(raw, range)
    data class Keyword(val type: KeywordType, override val raw: String, override val range: SourceRange) : Token(raw, range)
    data class Operator(
        val type: OperatorType,
        val precedence: Int,
        override val raw: String,
        override val range: SourceRange
    ) : Token(raw, range)
}

fun Token.areOnSameLine(other: Token): Boolean {
    return this.range.end.line == other.range.start.line
}