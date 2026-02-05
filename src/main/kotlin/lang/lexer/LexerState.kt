package lang.lexer

import lang.core.Pos
import lang.core.ISourceCode
import lang.core.SourceRange

internal data class LexerState(
    var index: Int = 0,
    var startLine: Int = 1,
    var endLine: Int = 1,
    var startCol: Int = 1,
    var endCol: Int = 1
) {
    fun reset() {
        index = 0
        endLine = 1
        endCol = 1
    }

//    fun beginRange() {
//
//    }

    fun closeRange(src: ISourceCode): SourceRange {
        val range = SourceRange(
            start = Pos(
                line = startLine,
                col = startCol,
                src = src
            ),
            end = Pos(
                line = endLine,
                col = endCol,
                src = src
            ),
            src = src
        )

        startLine = endLine
        startCol = endCol
        return range
    }
}