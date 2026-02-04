package lang.core

import lang.tokens.KeywordInfo
import lang.tokens.KeywordType
import lang.tokens.OperatorInfo
import lang.tokens.OperatorType
import lang.tokens.TokenRule
import lang.tokens.TokenType

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

    private val keywordRules: List<TokenRule> = keywords.map { kw ->
        TokenRule(Regex("""\b${kw.value}\b"""), TokenType.KEYWORD)
    }

    // --- Генерация TokenRule для операторов ---
    private val operatorRules: List<TokenRule> =
        operators.sortedByDescending { it.symbol.length } // сначала длинные операторы
            .map { op ->
                val escaped = Regex.escape(op.symbol)
                TokenRule(Regex(escaped), TokenType.OPER)
            }


    private val floatRules = listOf(
        // 1.0f, 1.f, .1f, 3e1f, 1e-2f
        TokenRule(Regex("""\d+\.\d*([eE][+-]?\d+)?[fF]"""), TokenType.FLOAT),
        TokenRule(Regex("""\.\d+([eE][+-]?\d+)?[fF]"""), TokenType.FLOAT),
        TokenRule(Regex("""\d+([eE][+-]?\d+)[fF]"""), TokenType.FLOAT),  // 3e1f
        TokenRule(Regex("""\d+[fF]"""), TokenType.FLOAT)                 // 1f
    )

    // DOUBLE (по умолчанию, может быть .0, 1., 1.0, 1e10)
    private val doubleRules = listOf(
        // 1.0, 1., .1, 1e10, 3.0e-2
        TokenRule(Regex("""\d+\.\d*([eE][+-]?\d+)?"""), TokenType.DOUBLE),
        TokenRule(Regex("""\.\d+([eE][+-]?\d+)?"""), TokenType.DOUBLE),
        TokenRule(Regex("""\d+([eE][+-]?\d+)"""), TokenType.DOUBLE)
    )


    private val intRules = listOf(
        TokenRule(Regex("""\d+[uU][lL]"""), TokenType.UINT64),
        TokenRule(Regex("""0x[a-fA-F0-9]+[uU][lL]"""), TokenType.UINT64),
        TokenRule(Regex("""0b[01]+[uU][lL]"""), TokenType.UINT64),

        TokenRule(Regex("""\d+[lL]"""), TokenType.INT64),
        TokenRule(Regex("""0x[a-fA-F0-9]+[lL]"""), TokenType.INT64),
        TokenRule(Regex("""0b[01]+[lL]"""), TokenType.INT64),

        TokenRule(Regex("""\d+[uU]"""), TokenType.UINT32),
        TokenRule(Regex("""0x[a-fA-F0-9]+[uU]"""), TokenType.UINT32),
        TokenRule(Regex("""0b[01]+[uU]"""), TokenType.UINT32),

        TokenRule(Regex("""0x[a-fA-F0-9]+"""), TokenType.INT32),
        TokenRule(Regex("""0b[01]+"""), TokenType.INT32),
        TokenRule(Regex("""\d+"""), TokenType.INT32),
    )


    private val numberRules: List<TokenRule> = floatRules + doubleRules + intRules

    private val baseRules = listOf(
//        TokenRule(Regex("""//.*"""), TokenType.INLINE_COMMENT),
//        TokenRule(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), TokenType.MULTILINE_COMMENT_START),

        TokenRule(
            Regex("\"([^\"\\\\]|\\\\.|\\\\\r?\n)*\"", RegexOption.DOT_MATCHES_ALL), TokenType.QUOTES_STR
        ),

        TokenRule(
            Regex("\'([^\"\\\\]|\\\\.|\\\\\r?\n)*\'", RegexOption.DOT_MATCHES_ALL), TokenType.QUOTES_STR
        ),
//        TokenRule(Regex("""'([^'\\]|\\.)*'"""), TokenType.QUOTES_CHAR),


        TokenRule(Regex("""\d+(\.\d+)?([fFdD])?"""), TokenType.FLOAT),
//        TokenRule(Regex("""\d+"""), TokenType.INT32),

        TokenRule(Regex("""true"""), TokenType.TRUE),
        TokenRule(Regex("""false"""), TokenType.FALSE),
        TokenRule(Regex("""null"""), TokenType.NULL),

        TokenRule(Regex("""[a-zA-Z_]\w*"""), TokenType.IDENTIFIER),

        TokenRule(Regex("""["']"""), TokenType.UNCLOSED_QUOTE),

        TokenRule(Regex("""\("""), TokenType.LPAREN),
        TokenRule(Regex("""\)"""), TokenType.RPAREN),
        TokenRule(Regex("""\["""), TokenType.LBRACKET),
        TokenRule(Regex("""]"""), TokenType.RBRACKET),
        TokenRule(Regex("""\{"""), TokenType.LBRACE),
        TokenRule(Regex("""}"""), TokenType.RBRACE),

        TokenRule(Regex(""":"""), TokenType.COLON),
        TokenRule(Regex("""\."""), TokenType.DOT),
//        TokenRule(Regex(""","""), TokenType.COMMA),
        TokenRule(Regex(""";"""), TokenType.SEMICOLON)
    )

    private val identifierRules = listOf(
        TokenRule(Regex("""int"""), TokenType.IDENTIFIER), // prevent op(in) + id(t)
        TokenRule(Regex("//.*(?:\\r?\\n|$)"), TokenType.COMMENT),
        TokenRule(Regex("/\\*(.|\\r|\\n)*?\\*/", RegexOption.DOT_MATCHES_ALL), TokenType.COMMENT),
        TokenRule(
            Regex("/\\*(.|\\r|\\n)*", RegexOption.DOT_MATCHES_ALL),
            TokenType.UNCLOSED_COMMENT
        ),


        )

    override val tokenRules: List<TokenRule> = keywordRules + identifierRules + operatorRules + numberRules + baseRules

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