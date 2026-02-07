package lang.messages

import lang.core.ISourceCode
import lang.core.Pos
import lang.core.SourceRange
import lang.core.toSourceRange
import java.lang.StringBuilder

class MsgHandler {

    private val _messages: MutableList<Message> = mutableListOf()

    val messages: List<Message>
        get() = _messages

    val hasErrors: Boolean
        get() = _messages.isNotEmpty()


    fun addMessage(msg: Message) =
        _messages.add(msg)

    fun clear() = _messages.clear()

    fun warn(stage: CompileStage, msg: String, range: SourceRange? = null) {
        addMessage(
            Message(
                type = MessageType.WARNING,
                msg = msg,
                stage = stage,
                range = range
            )
        )
    }

    fun err(stage: CompileStage, msg: String, range: SourceRange? = null) {
        addMessage(
            Message(
                type = MessageType.ERROR,
                msg = msg,
                stage = stage,
                range = range
            )
        )
    }

    fun info(stage: CompileStage, msg: String, range: SourceRange? = null) {
        addMessage(
            Message(
                type = MessageType.INFO,
                msg = msg,
                stage = stage,
                range = range
            )
        )
    }

    fun sourceReadingError(path: String?, msg: String) {
        err(
            stage = CompileStage.SOURCE_READING,
            msg = "'$path': $msg"
        )
    }

    fun lexicalError(msg: String, range: SourceRange? = null) {
        err(
            stage = CompileStage.LEXICAL_ANALYSIS,
            msg = msg,
            range = range
        )
    }

    fun lexicalError(msg: String, src: ISourceCode, pos: Pos? = null) {
        err(
            stage = CompileStage.LEXICAL_ANALYSIS,
            msg = msg,
            range = pos?.toSourceRange(src)

        )
    }

    fun syntaxError(msg: String, range: SourceRange) {
        err(
            stage = CompileStage.SYNTAX_ANALYSIS,
            msg = msg,
            range = range
        )
    }

    fun semanticError(msg: String, range: SourceRange? = null) {
        err(
            stage = CompileStage.SEMANTIC_ANALYSIS,
            msg = msg,
            range = range
        )
    }

    fun formatErrors(): String {
        val builder = StringBuilder(_messages.size * 120)

        for (err in _messages)
            builder.append(err.format())

        return builder.toString()
    }

    fun printAll(): Boolean {
        if (hasErrors)
            println(formatErrors())
        return hasErrors
    }

}