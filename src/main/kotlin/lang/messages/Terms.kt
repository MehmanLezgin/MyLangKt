package lang.messages

object Terms {
    const val EXTENSION = "extension"
    const val UNKNOWN = "unknown"
    const val UNKNOWN_PATH = "unknown path"
    const val TYPE_NAME = "type name"
    const val CLASS = "class"
    const val NAMESPACE = "namespace"
    const val INTERFACE = "interface"
    const val ENUM = "enum"
    const val FUNCTION = "function"
    const val VARIABLE = "variable"
    const val SYMBOL = "symbol"
    const val PARAM = "parameter"
    const val NO = "no"
    const val EXACTLY = "exactly"
    const val OPERATOR = "operator"
    const val NON_STATIC = "non-static"
    const val STATIC = "static"
    const val ARGUMENT_TYPE = "argument type"
    const val TYPE = "type"
    const val CURRENT_SCOPE = "current scope"


    fun Int.exactly(): String {
        if (this == 0) return NO
        return "${Terms.EXACTLY} $this"
    }

    fun String.plural(count: Int = 2): String =
        if (count == 1) lowercase()
        else lowercase() + "s"

    val primaryVowelLetters = listOf('a', 'e', 'i', 'o', 'u')

    fun String.withIndefiniteArticle(): String {
        val word = lowercase()
        return if (word[0] in primaryVowelLetters) "an $word" else "a $word"
    }

    fun String.quotes() = "'$this'"

    fun Int.ordinal(): String {
        val n = this
        return when {
            n % 100 in 11..13 -> "${n}th"
            n % 10 == 1 -> "${n}st"
            n % 10 == 2 -> "${n}nd"
            n % 10 == 3 -> "${n}rd"
            else -> "${n}th"
        }
    }
}