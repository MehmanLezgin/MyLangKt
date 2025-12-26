package lang.core

import java.io.File

class SourceCode(
    val source: String,
    val file: File? = null,
) {
    private val lines: List<String> = source.lines()

    fun getLine(index: Int) = lines.getOrNull(index)
}