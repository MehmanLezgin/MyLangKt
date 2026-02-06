package lang.tokens

enum class OperatorType(val symbol: String) {
    UNKNOWN("?"),

    INFIX("\$infix"),
    INCREMENT("++"),
    DECREMENT("--"),
    NON_NULL_ASSERT("!!"),

    NOT("!"),
    BIN_NOT("~"),

    AS("as"),
    IS("is"),
    IN("in"),
    UNTIL("until"),

    SIZEOF("sizeof"),
    NEW("new"),
    DELETE("delete"),

    MUL("*"),
    DIV("/"),
    REMAINDER("%"),

    SHIFT_LEFT("<<"),
    SHIFT_RIGHT(">>"),

    PLUS("+"),
    MINUS("-"),

    DOT("."),
    SCOPE("::"),

    LESS("<"),
    LESS_EQUAL("<="),
    GREATER(">"),
    GREATER_EQUAL(">="),

    EQUAL("=="),
    NOT_EQUAL("!="),

    AMPERSAND("&"),
    BIN_XOR("^"),
    BIN_OR("|"),

    AND("&&"),
    OR("||"),

    QUESTION("?"),
    COLON(":"),

    ASSIGN("="),
    PLUS_ASSIGN("+="),
    MINUS_ASSIGN("-="),
    MUL_ASSIGN("*="),
    DIV_ASSIGN("/="),
    REMAINDER_ASSIGN("%="),
    BIN_AND_ASSIGN("&="),
    BIN_OR_ASSIGN("|="),
    BIN_XOR_ASSIGN("^="),
    SHIFT_LEFT_ASSIGN("<<="),
    SHIFT_RIGHT_ASSIGN(">>="),

    DOUBLE_DOT(".."),
    ELVIS("?:"),
    ARROW("->"),
    COMMA(",");

    val fullName: String
        get() = "\$operator $symbol"

    fun compoundToBinary() : OperatorType? {
        return when (this) {
            PLUS_ASSIGN -> PLUS
            MINUS_ASSIGN -> MINUS
            MUL_ASSIGN -> MUL
            DIV_ASSIGN -> DIV
            REMAINDER_ASSIGN -> REMAINDER
            BIN_AND_ASSIGN -> AMPERSAND
            BIN_OR_ASSIGN -> BIN_OR
            BIN_XOR_ASSIGN -> BIN_XOR
            SHIFT_LEFT_ASSIGN -> SHIFT_LEFT
            SHIFT_RIGHT_ASSIGN -> SHIFT_RIGHT
            else -> null
        }
    }
}


data class OperatorInfo(
//    val symbol: String,
    val type: OperatorType,
    val precedence: Int = -1
) {
    val symbol: String
        get() = type.symbol
}

object OperatorMaps {
    val triBracketsMap = mapOf(
        OperatorType.LESS_EQUAL to listOf(OperatorType.LESS, OperatorType.ASSIGN),
        OperatorType.GREATER_EQUAL to listOf(OperatorType.GREATER, OperatorType.ASSIGN),

        OperatorType.SHIFT_LEFT to listOf(OperatorType.LESS, OperatorType.LESS),
        OperatorType.SHIFT_LEFT_ASSIGN to listOf(OperatorType.LESS, OperatorType.LESS, OperatorType.ASSIGN),

        OperatorType.SHIFT_RIGHT to listOf(OperatorType.GREATER, OperatorType.GREATER),
        OperatorType.SHIFT_RIGHT_ASSIGN to listOf(OperatorType.GREATER, OperatorType.GREATER, OperatorType.ASSIGN),
    )

    val ampersandMap = mapOf(
        OperatorType.AMPERSAND to listOf(OperatorType.AMPERSAND),
        OperatorType.AND to listOf(OperatorType.AMPERSAND, OperatorType.AMPERSAND),
        OperatorType.BIN_AND_ASSIGN to listOf(OperatorType.AMPERSAND, OperatorType.ASSIGN)
    )

    val multiplyMap = mapOf(
        OperatorType.MUL to listOf(OperatorType.MUL),
        OperatorType.MUL_ASSIGN to listOf(OperatorType.MUL, OperatorType.ASSIGN),
    )

    val superMap = mapOf(
        OperatorType.LESS to triBracketsMap,
        OperatorType.AMPERSAND to ampersandMap,
        OperatorType.MUL to multiplyMap,
    )
}