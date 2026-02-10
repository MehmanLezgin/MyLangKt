package lang.compiler

import lang.core.FileSourceCode
import lang.core.ISourceCode
import lang.core.LangSpec
import lang.lexer.ILexer
import lang.lexer.Lexer
import lang.messages.Msg
import lang.messages.MsgHandler
import lang.nodes.ModuleStmtNode
import lang.parser.IParser
import lang.parser.Parser
import lang.semantics.ISemanticAnalyzer
import lang.semantics.SemanticAnalyzer
import lang.semantics.SemanticContext
import lang.tokens.ITokenStream
import lang.tokens.TokenStream
import java.io.File
import java.io.IOException
import java.nio.file.Path


class Program(val path: String) {
    var msgHandler: MsgHandler = MsgHandler()

    var sourceManager = SourceManager(
        msgHandler = msgHandler,
        path = path
    )
}

typealias SourceCallback = (m: SourceUnit, ts: ITokenStream) -> Unit

class SourceManager(
    val msgHandler: MsgHandler,
    path: String
) {
    private val basePath = Path.of(path)
    internal var fileExtension = "i"

    val sources = mutableListOf<SourceUnit>()
    var entrySourceUnit: SourceUnit? = null

    private fun String.absolute() =
        basePath.resolve(this)
            .normalize()
            .toAbsolutePath()
            .toFile()

    fun addSourcesFromRoot(path: String = "", callback: SourceCallback?): List<SourceUnit>? {
        val dir = path.absolute()

        val errorMsg = when {
            !dir.exists() -> Msg.F_NO_SUCH_DIRECTORY
            !dir.isDirectory -> Msg.F_NOT_A_DIRECTORY
            else -> null
        }?.format(dir.path)

        if (errorMsg != null) {
            msgHandler.sourceReadingError(
                path = dir.path,
                msg = errorMsg
            )
            return null
        }

        val files = dir.listFiles()
            ?.filter {
                it.isFile && it.extension == fileExtension
            } ?: return null

        return files.mapNotNull { file ->
            addSourceFromFile(path = file.path, callback)
        }
    }

    fun addSourceFromFile(path: String, callback: SourceCallback?): SourceUnit? {
        val file = path.absolute()

        val src = readSourceFile(file, msgHandler) ?: return null

        val lexer: ILexer = Lexer(
            src = src,
            msgHandler = msgHandler,
            langSpec = LangSpec
        )

        val tokenStream: ITokenStream = TokenStream(
            lexer = lexer,
            langSpec = LangSpec,
            src = src,
            msgHandler = msgHandler
        )

        val parser: IParser = Parser(
            ts = tokenStream,
            msgHandler = msgHandler
        )

        val id = path.hashCode().toString()
        val ast = parser.parseSource(sourceId = id)

        val sourceUnit = SourceUnit(
            id = id,
            src = src,
            ast = ast
        )

        sources.add(sourceUnit)

        callback?.invoke(sourceUnit, tokenStream)
        return sourceUnit
    }

    /*private fun createModuleName(id: String, list: List<ExprNode>): QualifiedName {
        return QualifiedName(
            parts = listOf(
                IdentifierNode(
                    value = $$"$anonymous_module_$$id",
                    range = SourceRange(src = ts.peek().range.src)
                )
            )
        )
    }*/

    private fun readSourceFile(file: File, msgHandler: MsgHandler): ISourceCode? {
        var src: ISourceCode? = null

        try {
            val content = file.readText()

            src = FileSourceCode(
                content = content,
                file = file,
                displayName = file.nameWithoutExtension
            )
        } catch (_: IOException) {
            msgHandler.sourceReadingError(
                path = file.absoluteFile.path,
                msg = Msg.CANNOT_OPEN_SOURCE_FILE.format(file.path)
            )
        }

        return src
    }
}

fun program(
    path: String,
    block: Program.() -> Unit
): Program {
    return Program(path).apply(block)
}

fun Program.analiseIfNoError(): SemanticContext? {
    if (msgHandler.hasErrors) return null
    return analise()
}

fun Program.analise(): SemanticContext? {
    sourceManager.entrySourceUnit?.ast ?: return null

    val analyzer: ISemanticAnalyzer = SemanticAnalyzer(
        msgHandler = msgHandler,
        moduleMgr = sourceManager
    )

    analyzer.registerSources(sourceManager.sources)

    val entrySourceUnit = sourceManager.entrySourceUnit

    if (entrySourceUnit == null) {
        msgHandler.sourceReadingError(path, Msg.ENTRY_SOURCE_NOT_DEFINED)
        return null
    }

    analyzer.resolve(sourceUnit = entrySourceUnit)

    return analyzer.semanticContext
}

fun Program.sources(block: SourceManager.() -> Unit): List<SourceUnit> {
    return sourceManager.apply(block).sources
}

fun Program.printErrors() {
    msgHandler.printAll()
}

fun SourceManager.root(
    path: String = "",
    callback: SourceCallback? = null
) =
    addSourcesFromRoot(path, callback)

fun SourceManager.file(
    path: String,
    callback: SourceCallback? = null
) =
    addSourceFromFile(path, callback)

fun SourceManager.entry(
    path: String,
    callback: SourceCallback? = null
) {
    val module = addSourceFromFile(path, callback)
    this.entrySourceUnit = module
}

fun SourceManager.extension(
    value: String
) {
    this.fileExtension = value
}