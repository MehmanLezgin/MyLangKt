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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileSourceCode

        if (content != other.content) return false
        if (displayName != other.displayName) return false
        if (file != other.file) return false
        if (lines != other.lines) return false
        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        var result = content.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + file.hashCode()
        result = 31 * result + lines.hashCode()
        result = 31 * result + path.hashCode()
        return result
    }
}

class UnknownSourceCode(
    override val displayName: String = Terms.UNKNOWN,
    override val content: String = "",
) : ISourceCode {

    private val lines: List<String> = content.lines()
    override val path: String = ""

    override fun getLine(index: Int) = lines.getOrNull(index)
}