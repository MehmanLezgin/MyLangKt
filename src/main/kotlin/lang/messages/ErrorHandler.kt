package lang.messages

import lang.core.SourceCode
import lang.tokens.Pos
import java.lang.StringBuilder

class ErrorHandler {

    private val _errors: MutableList<ErrorMsg> = mutableListOf()

    val errors: List<ErrorMsg>
        get() = _errors

    val hasErrors: Boolean
        get() = _errors.isNotEmpty()

    fun clear() = _errors.clear()

    fun addError(error: ErrorMsg) = _errors.add(error)

    fun lexicalError(message: String, pos: Pos?) {
        addError(
            ErrorMsg(
                stage = CompileStage.LEXICAL_ANALYSIS,
                message = message,
                pos = pos
            )
        )
    }

    fun syntaxError(msg: String, pos: Pos) {
        addError(
            ErrorMsg(
                stage = CompileStage.SYNTAX_ANALYSIS,
                message = msg,
                pos = pos
            )
        )
    }

    fun semanticError(message: String, pos: Pos?) {
        addError(
            ErrorMsg(
                stage = CompileStage.SEMANTIC_ANALYSIS,
                message = message,
                pos = pos
            )
        )
    }

    fun formatErrors(src: SourceCode): String {

        val builder = StringBuilder(_errors.size * 120) // preallocate rough size

        for (err in _errors) {
            builder.append(err.format(src = src))

            /*val lineIndex = err.pos.line - 1
            val lineText = file.getLine(lineIndex)

            val posStr = err.pos.line.toString()
            val separatorStr = "   | "
            val pointerOffset = posStr.length + separatorStr.length

            builder.append(AnsiColors.CYAN)
                .append("\n\n ")
                .append(posStr)
                .append(separatorStr)
                .append(lineText)
                .append('\n')

            builder.append(" ".repeat(err.pos.col - 1 + pointerOffset))
            builder.append(AnsiColors.color("^^^", AnsiColors.ERROR)).append('\n')

            builder
                .append(file.file?.absoluteFile ?: "src")
                .append(" (")
                .append(err.pos)
                .append("): ")

            builder.append(AnsiColors.ERROR)
                .append(err.stageToString())
                .append(": ")
                .append(AnsiColors.color(err.message, AnsiColors.ERROR, null, true))
                .append(AnsiColors.RESET)
*/
        }

        return builder.toString()
    }

}