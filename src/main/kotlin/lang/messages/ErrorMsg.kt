package lang.messages

import lang.tokens.Pos

class ErrorMsg(
    val stage: CompileStage,
    val message: String,
    val pos: Pos? = null
) {
    override fun toString(): String {
        return "${stageToString()}: $message at $pos"
    }

    fun stageToString(): String {
        return when (stage) {
            CompileStage.LEXICAL_ANALYSIS -> "Lexical error"
            CompileStage.SYNTAX_ANALYSIS -> "Syntax error"
            CompileStage.SEMANTIC_ANALYSIS -> "Semantic error"
            else -> "IO error"
        }
    }

    fun format(): String {
        val src = pos?.src
        return buildString {
            if (pos != null) {
                val lineIndex = pos.line - 1
                val lineText = src?.getLine(lineIndex)

                val posStr = pos.line.toString()
                val separatorStr = "    | "
                val pointerOffset = posStr.length + separatorStr.length

                append("\n\n ")
                    .append(posStr)
                    .append(separatorStr)

                if (src != null) {
                    append(AnsiColors.CYAN)
                        .append(lineText)
                        .append('\n')
                        .append(" ".repeat(pos.col - 1 + pointerOffset))

                    append(AnsiColors.color("^^^", AnsiColors.ERROR)).append('\n')
                        .append(src?.file?.absoluteFile ?: "src")
                        .append(" (")
                        .append(pos)
                        .append("): ")
                }
            }

            append(AnsiColors.ERROR)
                .append(stageToString())
                .append(": ")
                .append(AnsiColors.color(message, AnsiColors.ERROR, null, true))
                .append(AnsiColors.RESET)
                .append('\n')

        }
    }
}