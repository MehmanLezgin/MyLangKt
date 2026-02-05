package lang.core

import kotlin.math.max
import kotlin.math.min

data class SourceRange(
    val start: Pos = Pos(),
    val end: Pos = Pos(),
    val src: ISourceCode = UnknownSourceCode()
) {
    override fun toString(): String {
        return "$start..$end"
    }

    fun horizontalCut(index: Int, length: Int): SourceRange {
        return copy(
            start = Pos(start.line, start.col + index),
            end = Pos(start.line, start.col + index + length)
        )
    }

    fun combineWith(other: SourceRange): SourceRange {
        return SourceRange(
            start = start.copy(
                col = min(start.line, other.start.line)
            ),
            end = other.end.copy(
                col = max(start.line, other.start.line)
            )
        )
    }
}

fun Pos.toSourceRange(src: ISourceCode) = SourceRange(
    start = this,
    end = this,
    src = src
)