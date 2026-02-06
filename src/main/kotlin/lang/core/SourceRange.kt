package lang.core

data class SourceRange(
    val start: Pos = Pos(),
    val end: Pos = Pos(),
    val src: ISourceCode = UnknownSourceCode()
) {
    override fun toString(): String {
        return "[$start -> $end]"
    }

    fun isInside(other: SourceRange): Boolean {
        return start <= other.start && other.end <= end
    }

    infix fun untilEndOf(other: SourceRange): SourceRange {
        return SourceRange(
            start = start,
            end = other.end,
            src = src
        )
    }

    fun horizontalCut(index: Int, length: Int): SourceRange {
        return copy(
            start = Pos(start.line, start.col + index),
            end = Pos(start.line, start.col + index + length)
        )
    }

    /*fun combineWith(other: SourceRange): SourceRange {
        return SourceRange(
            start = start.copy(
                col = min(start.col, other.start.col)
            ),
            end = other.end.copy(
                col = max(start.col, other.start.col)
            )
        )
    }*/
}

fun Pos.toSourceRange(src: ISourceCode) = SourceRange(
    start = this,
    end = this,
    src = src
)