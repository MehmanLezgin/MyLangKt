package lang.core

import lang.messages.Terms
import java.io.File

interface ISourceCode {
    val displayName: String
    val content: String
    val path: String

    fun getLine(index: Int): String?
}

class FileSourceCode(
    override val content: String,
    override val displayName: String,
    val file: File,
) : ISourceCode {

    private val lines: List<String> = content.lines()
    override val path: String = file.path

    override fun getLine(index: Int) = lines.getOrNull(index)
}

class UnknownSourceCode(
    override val displayName: String = Terms.UNKNOWN,
    override val content: String = "",
) : ISourceCode {

    private val lines: List<String> = content.lines()
    override val path: String = ""

    override fun getLine(index: Int) = lines.getOrNull(index)
}