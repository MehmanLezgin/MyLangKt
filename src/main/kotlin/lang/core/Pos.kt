package lang.core

data class Pos(
    val line: Int = 1,
    val col: Int = 1,
    val src: ISourceCode? = null
) {
    override fun toString(): String {
        return "$line:$col"
    }

    operator fun compareTo(other: Pos): Int =
        when {
            line != other.line -> line - other.line
            else -> col - other.col
        }
}

