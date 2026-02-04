package lang.messages

import lang.tokens.Pos
import java.lang.StringBuilder

class ErrorHandler {

    private val _errors: MutableList<ErrorMsg> = mutableListOf()

    val errors: List<ErrorMsg>
        get() = _errors

    val hasErrors: Boolean
        get() = _errors.isNotEmpty()

    fun clear() = _errors.clear()

    fun addError(error: ErrorMsg) =
        _errors.add(error)

    fun sourceReadingError(path: String?, message: String) {
        addError(
            ErrorMsg(
                stage = CompileStage.SOURCE_READING,
                message = "'$path': $message"
            )
        )
    }

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

    fun formatErrors(): String {
        val builder = StringBuilder(_errors.size * 120)

        for (err in _errors)
            builder.append(err.format())

        return builder.toString()
    }

    fun printAll(): Boolean {
        if (hasErrors)
            println(formatErrors())
        return hasErrors
    }

}