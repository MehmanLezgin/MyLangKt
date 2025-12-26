package lang.core

import lang.tokens.KeywordInfo
import lang.tokens.KeywordType
import lang.tokens.OperatorInfo
import lang.tokens.OperatorType
import lang.tokens.TokenRule
import lang.tokens.TokenType

object LangSpec : ILangSpec {
    override val keywords = listOf(
        KeywordInfo("var", KeywordType.VAR),
        KeywordInfo("let", KeywordType.LET),
        KeywordInfo("func", KeywordType.FUNC),

        KeywordInfo("continue", KeywordType.CONTINUE),
        KeywordInfo("for", KeywordType.FOR),
        KeywordInfo("do", KeywordType.DO),
        KeywordInfo("while", KeywordType.WHILE),
        KeywordInfo("match", KeywordType.MATCH),

        KeywordInfo("if", KeywordType.IF),
        KeywordInfo("else", KeywordType.ELSE),
        KeywordInfo("elif", KeywordType.ELIF),
        KeywordInfo("private", KeywordType.PRIVATE),
        KeywordInfo("public", KeywordType.PUBLIC),
        KeywordInfo("protected", KeywordType.PROTECTED),
        KeywordInfo("const", KeywordType.CONST),
        KeywordInfo("static", KeywordType.STATIC),
        KeywordInfo("open", KeywordType.OPEN),
        KeywordInfo("override", KeywordType.OVERRIDE),

        KeywordInfo("break", KeywordType.BREAK),
        KeywordInfo("try", KeywordType.TRY),
        KeywordInfo("catch", KeywordType.CATCH),
        KeywordInfo("finally", KeywordType.FINALLY),
        KeywordInfo("return", KeywordType.RETURN),

        KeywordInfo("class", KeywordType.CLASS),
        KeywordInfo("interface", KeywordType.INTERFACE),
        KeywordInfo("import", KeywordType.IMPORT),
        KeywordInfo("enum", KeywordType.ENUM),
        KeywordInfo("constructor", KeywordType.CONSTRUCTOR),
        KeywordInfo("destructor", KeywordType.DESTRUCTOR),
        KeywordInfo("namespace", KeywordType.NAMESPACE),
        KeywordInfo("using", KeywordType.USING),
        KeywordInfo("type", KeywordType.TYPE),
    )

    private val operatorsRaw = arrayOf(
        arrayOf(
            OperatorInfo("sizeof", OperatorType.SIZEOF),
            OperatorInfo("new", OperatorType.NEW),
            OperatorInfo("delete", OperatorType.DELETE),
        ),
        arrayOf(
            OperatorInfo("++", OperatorType.INCREMENT),
            OperatorInfo("--", OperatorType.DECREMENT),
            OperatorInfo("!!", OperatorType.NOT_NULL_ASSERTION),
        ),
        arrayOf(
            OperatorInfo("!", OperatorType.NOT),
            OperatorInfo("~", OperatorType.BIN_NOT),
            OperatorInfo("as", OperatorType.AS),
            OperatorInfo("is", OperatorType.IS),
        ),
        arrayOf(
            OperatorInfo("*", OperatorType.MUL),
            OperatorInfo("/", OperatorType.DIV),
            OperatorInfo("%", OperatorType.REMAINDER),
        ),
        arrayOf(
            OperatorInfo("+", OperatorType.PLUS),
            OperatorInfo("-", OperatorType.MINUS),
        ),
        arrayOf(
            OperatorInfo("<<", OperatorType.SHIFT_LEFT),
            OperatorInfo(">>", OperatorType.SHIFT_RIGHT),
        ),
        arrayOf(
            OperatorInfo(".", OperatorType.DOT),
            OperatorInfo("?.", OperatorType.DOT_NULL_SAFE),
        ),
        arrayOf(
            OperatorInfo("<", OperatorType.LESS),
            OperatorInfo("<=", OperatorType.LESS_EQUAL),
            OperatorInfo(">", OperatorType.GREATER),
            OperatorInfo(">=", OperatorType.GREATER_EQUAL),
        ),
        arrayOf(
            OperatorInfo("==", OperatorType.EQUAL),
            OperatorInfo("!=", OperatorType.NOT_EQUAL),
        ),
        arrayOf(
            OperatorInfo("&", OperatorType.AMPERSAND),
        ),
        arrayOf(
            OperatorInfo("^", OperatorType.XOR),
        ),
        arrayOf(
            OperatorInfo("|", OperatorType.BIN_OR),
        ),
        arrayOf(
            OperatorInfo("&&", OperatorType.AND),
        ),
        arrayOf(
            OperatorInfo("||", OperatorType.OR),
        ),
        arrayOf(
            OperatorInfo("?", OperatorType.QUESTION),
            OperatorInfo(":", OperatorType.COLON),
        ),


        arrayOf(
            OperatorInfo("=", OperatorType.ASSIGN),
            OperatorInfo("+=", OperatorType.PLUS_ASSIGN),
            OperatorInfo("-=", OperatorType.MINUS_ASSIGN),
            OperatorInfo("*=", OperatorType.MUL_ASSIGN),
            OperatorInfo("/=", OperatorType.DIV_ASSIGN),
            OperatorInfo("%=", OperatorType.REMAINDER_ASSIGN),
            OperatorInfo("&=", OperatorType.BIN_AND_ASSIGN),
            OperatorInfo("|=", OperatorType.BIN_OR_ASSIGN),
            OperatorInfo("^=", OperatorType.BIN_XOR_ASSIGN),
            OperatorInfo("<<=", OperatorType.SHIFT_LEFT_ASSIGN),
            OperatorInfo(">>=", OperatorType.SHIFT_RIGHT_ASSIGN),
        ),

        arrayOf(
            OperatorInfo("..", OperatorType.DOUBLE_DOT)
        ),
        arrayOf(
            OperatorInfo("in", OperatorType.IN),
            OperatorInfo("until", OperatorType.UNTIL),
            OperatorInfo("?:", OperatorType.ELVIS),
            OperatorInfo("->", OperatorType.ARROW),
            OperatorInfo(",", OperatorType.COMMA)
        )
    )



    override val operators =
        operatorsRaw
            .withIndex()
            .flatMap { (index, group) ->
                val precedence = operatorsRaw.size - index
                group.map { op -> op.copy(precedence = precedence) }
            }.toSet()

    /*setOf(
    OperatorInfo("++", 15, OperatorType.INCREMENT),
    OperatorInfo("--", 15, OperatorType.DECREMENT),
    OperatorInfo("->", 1, OperatorType.ARROW),
    OperatorInfo(".", 2, OperatorType.DOT),

    OperatorInfo("?.", 2, OperatorType.DOT_NULL_SAFE),
    OperatorInfo("?:", 1, OperatorType.ELVIS),
    OperatorInfo("!!", 15, OperatorType.NOT_NULL_ASSERTION),


    OperatorInfo(",", 1, OperatorType.COMMA),
    OperatorInfo("*", 13, OperatorType.MUL),
    OperatorInfo("/", 13, OperatorType.DIV),
    OperatorInfo("%", 13, OperatorType.REMAINDER),
    OperatorInfo("+", 12, OperatorType.PLUS),
    OperatorInfo("-", 12, OperatorType.MINUS),
    OperatorInfo("<<", 11, OperatorType.SHIFT_LEFT),
    OperatorInfo(">>", 11, OperatorType.SHIFT_RIGHT),
    OperatorInfo("<", 10, OperatorType.LESS),
    OperatorInfo("<=", 10, OperatorType.LESS_EQUAL),
    OperatorInfo(">", 10, OperatorType.GREATER),
    OperatorInfo(">=", 10, OperatorType.GREATER_EQUAL),
    OperatorInfo("==", 9, OperatorType.EQUAL),
    OperatorInfo("!=", 9, OperatorType.NOT_EQUAL),
    OperatorInfo("&", 8, OperatorType.AMPERSAND),
    OperatorInfo("^", 7, OperatorType.XOR),
    OperatorInfo("|", 6, OperatorType.BIN_OR),
    OperatorInfo("&&", 5, OperatorType.AND),
    OperatorInfo("||", 4, OperatorType.OR),
    OperatorInfo("=", 2, OperatorType.ASSIGN),
    OperatorInfo("+=", 2, OperatorType.PLUS_ASSIGN),
    OperatorInfo("-=", 2, OperatorType.MINUS_ASSIGN),
    OperatorInfo("*=", 2, OperatorType.MUL_ASSIGN),
    OperatorInfo("/=", 2, OperatorType.DIV_ASSIGN),
    OperatorInfo("%=", 2, OperatorType.REMAINDER_ASSIGN),
    OperatorInfo("&=", 2, OperatorType.BIN_AND_ASSIGN),
    OperatorInfo("|=", 2, OperatorType.BIN_OR_ASSIGN),
    OperatorInfo("^=", 2, OperatorType.BIN_XOR_ASSIGN),
    OperatorInfo("<<=", 2, OperatorType.SHIFT_LEFT_ASSIGN),
    OperatorInfo(">>=", 2, OperatorType.SHIFT_RIGHT_ASSIGN),
    OperatorInfo("?", 3, OperatorType.QUESTION),
    OperatorInfo(":", 3, OperatorType.COLON),
    OperatorInfo("!", 14, OperatorType.NOT),
    OperatorInfo("~", 14, OperatorType.BIN_NOT),
    OperatorInfo("sizeof", 16, OperatorType.SIZEOF),
    OperatorInfo("new", 16, OperatorType.NEW),
    OperatorInfo("delete", 16, OperatorType.DELETE),
    OperatorInfo("as", 14, OperatorType.AS),
    OperatorInfo("is", 14, OperatorType.IS)
)*/


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