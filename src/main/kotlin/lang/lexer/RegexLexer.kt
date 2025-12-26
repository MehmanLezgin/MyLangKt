package lang.lexer

import lang.messages.ErrorHandler
import lang.core.ILangSpec
import lang.messages.Messages
import lang.core.SourceCode
import lang.tokens.Token
import lang.tokens.TokenType


class RegexLexer(
    sourceFile: SourceCode,
    langSpec: ILangSpec,
    errorHandler: ErrorHandler,
) : BaseLexer(sourceFile, langSpec, errorHandler) {

    override fun matchToken() : Token? {
        val start = state.index
        val pos = getPos()

        for (rule in langSpec.tokenRules) {
            val match = rule.regex.find(source, start) ?: continue
            if (match.range.first != start) continue

            val rawValue = match.value

            val value = when (rule.type) {
                TokenType.QUOTES_STR,
                TokenType.QUOTES_CHAR -> unescapeString(rawValue, pos)

                TokenType.UNCLOSED_QUOTE -> {
                    errorHandler.lexicalError(Messages.EXPECTED_QUOTE, pos)
                    skipLine()
                    continue
                }

                TokenType.UNCLOSED_COMMENT -> {
                    errorHandler.lexicalError(Messages.EXPECTED_COMMENT_END, pos)
                    state.index = source.length
                    return null
                }

                else -> rawValue
            }

            trackNewlines(rawValue)

            state.index += rawValue.length
            return createToken(value, rule.type, pos)
        }

        return null
    }
}