package lang.parser

sealed class ParsingContext {
    object Default : ParsingContext()
    object Header : ParsingContext()
    object FuncHeader : ParsingContext()
    object Condition : ParsingContext()
    object TypeArg : ParsingContext()

    fun canParseTrailingLambdas(): Boolean =
        this != Header && this != Condition

    fun canParseTypeArgs() : Boolean =
        this == TypeArg ||
        this == Header

}