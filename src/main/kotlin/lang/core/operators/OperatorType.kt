package lang.core.operators

enum class OperatorType(val raw: String) {
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
        get() = "\$operator $raw"

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