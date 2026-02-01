package lang.tokens

enum class KeywordType(val value: String) {
    VAR("var"),
    LET("let"),
    FUNC("func"),

    CONTINUE("continue"),
    FOR("for"),
    DO("do"),
    WHILE("while"),
    MATCH("match"),

    IF("if"),
    ELSE("else"),
    ELIF("elif"),

    PRIVATE("private"),
    PUBLIC("public"),
    PROTECTED("protected"),
    CONST("const"),
    STATIC("static"),
    OPEN("open"),
    ABSTRACT("abstract"),
    OVERRIDE("override"),

    BREAK("break"),
    TRY("try"),
    CATCH("catch"),
    FINALLY("finally"),
    RETURN("return"),

    CLASS("class"),
    INTERFACE("interface"),
    IMPORT("import"),

    ENUM("enum"),
    CONSTRUCTOR("constructor"),
    DESTRUCTOR("destructor"),
    NAMESPACE("namespace"),
    USING("using"),
    TYPE("type"),
    OPERATOR("operator");
}

data class KeywordInfo(
    val type: KeywordType
) {
    val value: String
        get() = type.value
}
