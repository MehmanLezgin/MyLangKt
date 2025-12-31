package lang.nodes

enum class BinOpType {
    ARROW,           // ->
    COMMA,            // ,
    COLON,            // :
    MUL,            // *
    DIV,           // /
    REMAINDER,         // %
    PLUS,            // +
    MINUS,           // -
    SHIFT_LEFT,      // <<
    SHIFT_RIGHT,     // >>
    LESS,            // <
    LESS_EQUAL,      // <=
    GREATER,         // >
    GREATER_EQUAL,   // >=
    EQUAL,     // ==
    NOT_EQUAL,       // !=
    BIN_AND,             // &
    BIN_XOR,           // ^
    BIN_OR,            // |
    AND,         // &&
    OR,           // ||
    ASSIGN,           // =
//    PLUS_ASSIGN,      // +=
//    MINUS_ASSIGN,     // -=
//    MUL_ASSIGN,      // *=
//    DIV_ASSIGN,     // /=
//    REMAINDER_ASSIGN,   // %=
//    AND_ASSIGN,       // &=
//    OR_ASSIGN,      // |=
//    XOR_ASSIGN,     // ^=
//    SHIFT_LEFT_ASSIGN,// <<=
//    SHIFT_RIGHT_ASSIGN,// >>=
    CAST,
    IS,
    IN,
    UNTIL,
    ELVIS,
    DOUBLE_DOT
}

enum class UnaryOpType {
    NOT,
    NEW,
    DELETE,
    INCREMENT,
    DECREMENT,
    PLUS,
    MINUS,
    ADDRESS_OF,
    INDIRECTION,
    BITWISE_NOT,
    SIZEOF,
    IS,
    NON_NULL_ASSERT
}
