package lang.messages

import lang.tokens.Pos
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

    fun warn(stage: CompileStage, msg: String, pos: Pos? = null) {
        addMessage(
            Message(
                type = MessageType.WARNING,
                msg = msg,
                stage = stage,
                pos = pos
            )
        )
    }

    fun err(stage: CompileStage, msg: String, pos: Pos? = null) {
        addMessage(
            Message(
                type = MessageType.ERROR,
                msg = msg,
                stage = stage,
                pos = pos
            )
        )
    }

    fun info(stage: CompileStage, msg: String, pos: Pos? = null) {
        addMessage(
            Message(
                type = MessageType.INFO,
                msg = msg,
                stage = stage,
                pos = pos
            )
        )
    }

    fun sourceReadingError(path: String?, msg: String) {
        err(
            stage = CompileStage.SOURCE_READING,
            msg = "'$path': $msg"
        )
    }

    fun lexicalError(msg: String, pos: Pos? = null) {
        err(
            stage = CompileStage.LEXICAL_ANALYSIS,
            msg = msg,
            pos = pos
        )
    }

    fun syntaxError(msg: String, pos: Pos) {
        err(
            stage = CompileStage.SYNTAX_ANALYSIS,
            msg = msg,
            pos = pos
        )
    }

    fun semanticError(msg: String, pos: Pos? = null) {
        err(
            stage = CompileStage.SEMANTIC_ANALYSIS,
            msg = msg,
            pos = pos
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