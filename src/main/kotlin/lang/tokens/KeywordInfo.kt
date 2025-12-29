package lang.tokens

enum class KeywordType {
    VAR, LET, FUNC,
    CONTINUE, FOR, DO, WHILE, MATCH,
    IF, ELSE, ELIF, PRIVATE, PUBLIC, PROTECTED, CONST, STATIC, OPEN, OVERRIDE,
    BREAK, TRY, CATCH, FINALLY, RETURN,
    CLASS, INTERFACE, IMPORT,
    ENUM, CONSTRUCTOR, DESTRUCTOR, NAMESPACE, USING, TYPE, OPERATOR
}

data class KeywordInfo(
    val value: String,
    val type: KeywordType
)