package lang.tokens

data class TokenRule(
    val regex: Regex,
    val type: TokenType
)