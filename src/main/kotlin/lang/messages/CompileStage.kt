package lang.messages

enum class CompileStage {
    SOURCE_READING,
    LEXICAL_ANALYSIS,
    SYNTAX_ANALYSIS,
    SEMANTIC_ANALYSIS;

    override fun toString(): String {
        return when (this) {
            SOURCE_READING -> "Source reading"
            LEXICAL_ANALYSIS -> "Lexical analysis"
            SYNTAX_ANALYSIS -> "Syntax analysis"
            SEMANTIC_ANALYSIS -> "Semantic analysis"
        }
    }
}