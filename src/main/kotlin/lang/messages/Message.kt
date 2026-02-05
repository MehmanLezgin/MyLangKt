package lang.messages

import lang.core.SourceRange

data class Message(
    val stage: CompileStage,
    val type: MessageType = MessageType.INFO,
    val msg: String,
    val range: SourceRange? = null
) {
    fun stageToErrorString(): String {
        return when (stage) {
            CompileStage.LEXICAL_ANALYSIS -> "Lexical error"
            CompileStage.SYNTAX_ANALYSIS -> "Syntax error"
            CompileStage.SEMANTIC_ANALYSIS -> "Semantic error"
            else -> "IO error"
        }
    }

    private val msgColor = when (type) {
        MessageType.INFO -> AnsiColors.INFO
        MessageType.ERROR -> AnsiColors.ERROR
        MessageType.WARNING -> AnsiColors.WARNING
    }

    fun format(): String {
        val src = range?.src

        return buildString {
            if (range != null) {
                val start = range.start
                val end = range.end

                val lineIndex = start.line - 1
                val lineText = src?.getLine(lineIndex)

                val posStr = start.line.toString()
                val separatorStr = "    | "
                val pointerOffset = posStr.length + separatorStr.length

                val pointerLength = when {
                    start.line == end.line -> end.col - start.col
                    lineText != null -> lineText.length - start.col + 1
                    else -> 0
                }

                append("\n\n ")
                    .append(posStr)
                    .append(separatorStr)

                if (src != null) {
                    append(AnsiColors.CYAN)
                        .append(lineText)
                        .append('\n')
                        .append(" ".repeat(start.col + pointerOffset))

                    if (pointerLength > 0)
                        append(AnsiColors.color("^".repeat(pointerLength), msgColor)).append('\n')

                    append(src.path)
                        .append(" (")
                        .append(start)
                        .append("): ")
                }
            }

            append(msgColor)
                .append(stageToErrorString())
                .append(": ")
                .append(AnsiColors.color(msg, msgColor, null, true))
                .append(AnsiColors.RESET)
                .append('\n')

        }
    }
}