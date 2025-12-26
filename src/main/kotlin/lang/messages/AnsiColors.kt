package lang.messages

object AnsiColors {
    // Basic ANSI colors

    const val RESET = "\u001B[0m"

    const val BLACK = "\u001B[30m"
    const val RED = "\u001B[31m"
    const val GREEN = "\u001B[32m"
    const val YELLOW = "\u001B[33m"
    const val BLUE = "\u001B[34m"
    const val PURPLE = "\u001B[35m"
    const val CYAN = "\u001B[36m"
    const val WHITE = "\u001B[37m"

    // Bright colors
    const val BRIGHT_BLACK = "\u001B[90m"
    const val BRIGHT_RED = "\u001B[91m"
    const val BRIGHT_GREEN = "\u001B[92m"
    const val BRIGHT_YELLOW = "\u001B[93m"
    const val BRIGHT_BLUE = "\u001B[94m"
    const val BRIGHT_PURPLE = "\u001B[95m"
    const val BRIGHT_CYAN = "\u001B[96m"
    const val BRIGHT_WHITE = "\u001B[97m"

    // Background colors
    const val BG_BLACK = "\u001B[40m"
    const val BG_RED = "\u001B[41m"
    const val BG_GREEN = "\u001B[42m"
    const val BG_YELLOW = "\u001B[43m"
    const val BG_BLUE = "\u001B[44m"
    const val BG_PURPLE = "\u001B[45m"
    const val BG_CYAN = "\u001B[46m"
    const val BG_WHITE = "\u001B[47m"

    // Bright background colors
    const val BG_BRIGHT_BLACK = "\u001B[100m"
    const val BG_BRIGHT_RED = "\u001B[101m"
    const val BG_BRIGHT_GREEN = "\u001B[102m"
    const val BG_BRIGHT_YELLOW = "\u001B[103m"
    const val BG_BRIGHT_BLUE = "\u001B[104m"
    const val BG_BRIGHT_PURPLE = "\u001B[105m"
    const val BG_BRIGHT_CYAN = "\u001B[106m"
    const val BG_BRIGHT_WHITE = "\u001B[107m"

    val ERROR = RED
    val SUCCESS = fg256(42)
    val WARNING = YELLOW
    val INFO = CYAN


    fun fg256(code: Int): String = "\u001B[38;5;${code}m"

    fun bg256(code: Int): String = "\u001B[48;5;${code}m"

    // Helper to color a string
    fun color(text: String, color: String, bg: String? = null, bold: Boolean = false): String {
        val boldCode = if (bold) "\u001B[1m" else ""
        val bgCode = bg ?: ""
        return "$boldCode$color$bgCode$text$RESET"
    }
}