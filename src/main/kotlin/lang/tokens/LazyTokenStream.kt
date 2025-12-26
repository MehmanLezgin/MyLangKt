package lang.tokens

import lang.messages.ErrorHandler
import lang.lexer.ILexer

private data class LazyTokenStreamState(
    var prevToken: Token = Token.EOF(Pos()),
    var peekToken: Token = Token.EOF(Pos())
)

class LazyTokenStream(
    private val lexer: ILexer,
    errorHandler: ErrorHandler
) : BaseTokenStream(
    lexer = lexer,
    errorHandler = errorHandler
) {
    private var state = LazyTokenStreamState()
    private var savedStates: ArrayDeque<LazyTokenStreamState> = ArrayDeque()

    init {
        next() // first token
    }

    override fun reset() {
        lexer.reset()
    }

    override fun prev() = state.prevToken

    override fun peek(): Token = state.peekToken

    override fun next() : Token {
        state.prevToken = state.peekToken

        while (true) {
            val t = lexer.nextToken() ?: continue
            state.peekToken = t
            break
        }

        return prev()
    }

    override fun save() {
        super.save()
        savedStates.add(state.copy())
    }

    override fun restore() {
        super.save()
        if (savedStates.isEmpty()) return
        state = savedStates.removeLast()
    }

    override fun clearLastSave() {
        super.save()
        savedStates.removeLast()
    }
}