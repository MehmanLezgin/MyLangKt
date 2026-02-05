package lang.core

import lang.tokens.KeywordInfo
import lang.tokens.KeywordType
import lang.tokens.OperatorInfo
import lang.tokens.OperatorType

object LangSpec : ILangSpec {
    override val moduleNameSeparator: OperatorType = OperatorType.SCOPE

    override val keywords = KeywordType.values().map { type ->
        KeywordInfo(type = type)
    }

    private val operatorsRaw = arrayOf(
        arrayOf(
            OperatorInfo(OperatorType.SIZEOF),
            OperatorInfo(OperatorType.NEW),
            OperatorInfo(OperatorType.DELETE),
        ),
        arrayOf(
            OperatorInfo(OperatorType.INCREMENT),
            OperatorInfo(OperatorType.DECREMENT),
            OperatorInfo(OperatorType.NON_NULL_ASSERT),
        ),
        arrayOf(
            OperatorInfo(OperatorType.NOT),
            OperatorInfo(OperatorType.BIN_NOT),
            OperatorInfo(OperatorType.AS),
            OperatorInfo(OperatorType.IS),
        ),
        arrayOf(
            OperatorInfo(OperatorType.MUL),
            OperatorInfo(OperatorType.DIV),
            OperatorInfo(OperatorType.REMAINDER),
        ),
        arrayOf(
            OperatorInfo(OperatorType.SHIFT_LEFT),
            OperatorInfo(OperatorType.SHIFT_RIGHT),
        ),
        arrayOf(
            OperatorInfo(OperatorType.PLUS),
            OperatorInfo(OperatorType.MINUS),
        ),
        arrayOf(
            OperatorInfo(OperatorType.DOT),
            OperatorInfo(OperatorType.SCOPE),
        ),
        arrayOf(
            OperatorInfo(OperatorType.AMPERSAND),
        ),
        arrayOf(
            OperatorInfo(OperatorType.BIN_XOR),
        ),
        arrayOf(
            OperatorInfo(OperatorType.BIN_OR),
        ),
        arrayOf(
            OperatorInfo(OperatorType.LESS),
            OperatorInfo(OperatorType.LESS_EQUAL),
            OperatorInfo(OperatorType.GREATER),
            OperatorInfo(OperatorType.GREATER_EQUAL),
            OperatorInfo(OperatorType.EQUAL),
            OperatorInfo(OperatorType.NOT_EQUAL),
        ),
        arrayOf(
            OperatorInfo(OperatorType.AND),
        ),
        arrayOf(
            OperatorInfo(OperatorType.OR),
        ),
        arrayOf(
            OperatorInfo(OperatorType.QUESTION),
            OperatorInfo(OperatorType.COLON),
        ),
        arrayOf(
            OperatorInfo(OperatorType.ASSIGN),
            OperatorInfo(OperatorType.PLUS_ASSIGN),
            OperatorInfo(OperatorType.MINUS_ASSIGN),
            OperatorInfo(OperatorType.MUL_ASSIGN),
            OperatorInfo(OperatorType.DIV_ASSIGN),
            OperatorInfo(OperatorType.REMAINDER_ASSIGN),
            OperatorInfo(OperatorType.BIN_AND_ASSIGN),
            OperatorInfo(OperatorType.BIN_OR_ASSIGN),
            OperatorInfo(OperatorType.BIN_XOR_ASSIGN),
            OperatorInfo(OperatorType.SHIFT_LEFT_ASSIGN),
            OperatorInfo(OperatorType.SHIFT_RIGHT_ASSIGN),
        ),
        arrayOf(
            OperatorInfo(OperatorType.DOUBLE_DOT)
        ),
        arrayOf(
            OperatorInfo(OperatorType.IN),
            OperatorInfo(OperatorType.UNTIL),
            OperatorInfo(OperatorType.ELVIS),
            OperatorInfo(OperatorType.ARROW),
            OperatorInfo(OperatorType.COMMA)
        )
    )

    override val operators =
        operatorsRaw
            .withIndex()
            .flatMap { (index, group) ->
                val precedence = operatorsRaw.size - index
                group.map { op -> op.copy(precedence = precedence) }
            }.toSet()


    override fun getKeywordInfo(value: String): KeywordInfo? {
        return keywords.find { it.value == value }
    }

    override fun getOperatorInfo(value: String): OperatorInfo? {
        return operators.find { it.symbol == value }
    }

    override fun getOperatorInfo(type: OperatorType): OperatorInfo? {
        return operators.find { it.type == type }
    }
}