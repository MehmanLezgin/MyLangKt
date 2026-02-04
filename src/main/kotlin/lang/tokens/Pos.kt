package lang.tokens

import lang.core.SourceCode

data class Pos(
    val line: Int = 1,
    val col: Int = 1,
    val src: SourceCode? = null
) {
    override fun toString(): String {
        return "$line:$col"
    }
}