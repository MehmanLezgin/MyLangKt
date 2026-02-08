package lang.tokens

enum class TokenType {
    EOF,
    LPAREN, // (
    RPAREN, // )
    LBRACKET, // [
    RBRACKET, // ]
    LBRACE, // {
    RBRACE, // }
    COLON,
    DOT,
    UNCLOSED_QUOTE,
    SEMICOLON,
    UNCLOSED_COMMENT,

    INT32,
    INT64,
    UINT32,
    UINT64,
    FLOAT,
    DOUBLE,
    TRUE,
    FALSE,
    NULL,

    IDENTIFIER,
    QUOTES_STR,
    QUOTES_CHAR,
    KEYWORD,
    OPER
}