package lang.messages

import lang.tokens.Pos

data class Message(
    val stage: CompileStage,
    val type: MessageType = MessageType.INFO,
    val msg: String,
    val pos: Pos? = null
) {
    override fun toString(): String {
        if (type == MessageType.INFO)
            return "${stageToErrorString()}: $msg at $pos"

        return "$stage: (${type.value}) $msg at $pos"
    }

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

                    append(AnsiColors.color("^^^", msgColor)).append('\n')
                        .append(src.file?.absoluteFile ?: "src")
                        .append(" (")
                        .append(pos)
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