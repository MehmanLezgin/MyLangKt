package lang.core

import java.io.File

class SourceCode(
    val content: String,
    val file: File? = null,
) {
    private val lines: List<String> = content.lines()

    fun getLine(index: Int) = lines.getOrNull(index)
}