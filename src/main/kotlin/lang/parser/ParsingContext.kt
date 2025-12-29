package lang.parser

sealed class ParsingContext {
    object Default : ParsingContext()
    object Header : ParsingContext()
    object FuncHeader : ParsingContext()
    object Condition : ParsingContext()

    fun canParseTrailingLambdas(): Boolean =
        this != Header && this != Condition
}