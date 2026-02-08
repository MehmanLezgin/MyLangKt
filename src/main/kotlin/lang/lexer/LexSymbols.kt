package lang.lexer

object LexSymbols {
    const val INT = "int" // for preventing conflict with 'in' oper: op(in) + id(t)
    const val TRUE = "true"
    const val FALSE = "false"
    const val NULL = "null"
    const val DOT = '.'
    const val LPAREN = '('
    const val RPAREN = ')'
    const val LBRACKET = '['
    const val RBRACKET = ']'
    const val LBRACE = '{'
    const val RBRACE = '}'
    const val COLON = ':'
    const val SEMICOLON = ';'
    const val SEMICOLON_STR = SEMICOLON.toString()
    const val B = 'b'
    const val E = 'f'
    const val F = 'f'
    const val L = 'l'
    const val U = 'u'
    const val X = 'x'
    const val ZERO = '0'
    const val RADIX_BIN = 2
    const val RADIX_DEC = 10
    const val RADIX_HEX = 16
    const val QUOTE_CHAR = '\''
    const val QUOTE_STRING = '"'
    const val COMMENT = "//"
    const val MULTILINE_COMMENT_OPEN = "/*"
    const val MULTILINE_COMMENT_CLOSE = "*/"

    const val NEW_LINE = '\n'
    const val BACK_SLASH = '\\'
    const val LINE_CONTINUATION = "\\\r\n"
    const val DOUBLE_BACK_SLASH = "\\\\"
}
