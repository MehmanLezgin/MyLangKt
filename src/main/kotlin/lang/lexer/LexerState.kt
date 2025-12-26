package lang.lexer

internal data class LexerState(
    var index: Int = 0,
    var line: Int = 1,
    var col: Int = 1
) {
    fun reset() {
        index = 0
        line = 1
        col = 1
    }
}