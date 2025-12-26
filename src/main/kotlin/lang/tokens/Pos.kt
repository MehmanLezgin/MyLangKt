package lang.tokens

data class Pos(
    val line: Int = 1,
    val col: Int = 1
) {
    override fun toString(): String {
        return "$line:$col"
    }
}