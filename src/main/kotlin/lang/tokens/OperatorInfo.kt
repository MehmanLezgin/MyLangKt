package lang.tokens

enum class OperatorType {
    UNKNOWN,
    INCREMENT,          // ++
    DECREMENT,          // --
    ARROW,              // ->
    DOT,                // .
    COMMA,              // ,
    COLON,              // :
    MUL,                // *
    DIV,                // /
    REMAINDER,          // %
    PLUS,               // +
    MINUS,              // -
    SHIFT_LEFT,         // <<
    SHIFT_RIGHT,        // >>
    LESS,               // <
    LESS_EQUAL,         // <=
    GREATER,            // >
    GREATER_EQUAL,      // >=
    EQUAL,              // ==
    NOT_EQUAL,          // !=
    AMPERSAND,          // &
    XOR,                // ^
    BIN_OR,             // |
    AND,                // &&
    OR,                 // ||
    ASSIGN,             // =
    PLUS_ASSIGN,        // +=
    MINUS_ASSIGN,       // -=
    MUL_ASSIGN,         // *=
    DIV_ASSIGN,         // /=
    REMAINDER_ASSIGN,   // %=
    BIN_AND_ASSIGN,     // &=
    BIN_OR_ASSIGN,      // |=
    BIN_XOR_ASSIGN,     // ^=
    SHIFT_LEFT_ASSIGN,  // <<=
    SHIFT_RIGHT_ASSIGN, // >>=
    QUESTION,           // ?
    NOT,                // !
    BIN_NOT,            // ~
    SIZEOF,             // sizeof
    NEW,                // new
    DELETE,             // delete
    AS,                 // as
    IS,                 // is
    IN,                 // is
    UNTIL,              // is
    DOT_NULL_SAFE,      // ?.
    ELVIS,              // ?:
    NON_NULL_ASSERT,    // !!
    DOUBLE_DOT,         // ..
}

data class OperatorInfo(
    val symbol: String,
    val type: OperatorType,
    val precedence: Int = -1
)
