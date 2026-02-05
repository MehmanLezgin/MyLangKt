package lang.compiler

import lang.core.LangSpec
import lang.core.SourceCode
import lang.lexer.ILexer
import lang.lexer.Lexer
import lang.messages.ErrorHandler
import lang.messages.Msg
import lang.nodes.IdentifierNode
import lang.parser.IParser
import lang.parser.Parser
import lang.semantics.SemanticAnalyzer
import lang.semantics.SemanticContext
import lang.tokens.ITokenStream
import lang.tokens.Pos
import lang.tokens.TokenStream
import java.io.File
import java.io.IOException
import java.nio.file.Path


class Program(val path: String) {
    var errorHandler: ErrorHandler = ErrorHandler()

    var moduleManager: ModuleManager = ModuleManager(
        errorHandler = errorHandler,
        path = path
    )


}

typealias ModuleCallback = (m: Module, ts: ITokenStream) -> Unit

class ModuleManager(
    val errorHandler: ErrorHandler,
    path: String
) {
    private val basePath = Path.of(path)
    internal var fileExtension = "i"

    val modules = mutableMapOf<String, Module>()
    var entryModule: Module? = null

    private fun String.absolute() =
        basePath.resolve(this)
            .normalize()
            .toAbsolutePath()
            .toFile()

    fun addFromRoot(path: String = "", callback: ModuleCallback): List<Module>? {
        val dir = path.absolute()

        val errorMsg = when {
            !dir.exists() -> Msg.F_NO_SUCH_DIRECTORY
            !dir.isDirectory -> Msg.F_NOT_A_DIRECTORY
            else -> null
        }

        if (errorMsg != null) {
            errorHandler.sourceReadingError(
                path = dir.path,
                message = errorMsg
            )
            return null
        }

        val files = dir.listFiles()
            ?.filter {
                it.isFile && it.extension == fileExtension
            } ?: return null

        return files.mapNotNull { file ->
            addFromFile(path = file.path, callback)
        }
    }

    fun addFromFile(path: String, callback: ModuleCallback): Module? {
        val file = path.absolute()

        val src = readSourceFile(file, errorHandler) ?: return null

        val lexer: ILexer = Lexer(
            src = src,
            errorHandler = errorHandler,
            langSpec = LangSpec
        )

        val tokenStream: ITokenStream = TokenStream(
            lexer = lexer,
            langSpec = LangSpec,
            src = src,
            errorHandler = errorHandler
        )

        val parser: IParser = Parser(
            ts = tokenStream,
            errorHandler = errorHandler
        )

        var moduleNameId = parser.parseModuleName()
        var moduleName = moduleNameId?.value ?: ""

        if (moduleNameId == null) {
            moduleNameId = randModuleName(file.path, src)
            moduleName = moduleNameId.value
        } else {
            val existingModule = modules[moduleNameId.value]

            if (existingModule != null) {
                errorHandler.sourceReadingError(
                    path = file.path,
                    message = Msg.MODULE_ALREADY_EXISTS_IN.format(
                        moduleName, existingModule.src.file
                    )
                )
            }
        }

        val ast = parser.parseModule(moduleName)

        val module = Module(
            name = moduleName,
            src = src,
            ast = ast
        )

        modules[moduleName] = module

        callback(module, tokenStream)
        return module
    }

    private fun randModuleName(path: String, src: SourceCode) =
        IdentifierNode(
            value = "\$Module${path.hashCode()}",
            pos = Pos(src = src)
        )

    private fun readSourceFile(file: File, errorHandler: ErrorHandler): SourceCode? {
        var src: SourceCode? = null

        try {
            val content = file.readText()

            src = SourceCode(
                content = content,
                file = file
            )
        } catch (_: IOException) {
            errorHandler.sourceReadingError(
                path = file.absoluteFile.path,
                message = Msg.CANNOT_OPEN_SOURCE_FILE
            )
        }

        return src
    }
}

fun errorHandler() = ErrorHandler()

fun program(
    path: String,
    block: Program.() -> Unit
): Program {
    return Program(path).apply(block)
}

fun Program.analise(): SemanticContext? {
    moduleManager.entryModule?.ast ?: return null

    val analyzer = SemanticAnalyzer(
        errorHandler = errorHandler,
        moduleMgr = moduleManager
    )

    val entryModule = moduleManager.entryModule
    if (entryModule == null) {
        errorHandler.sourceReadingError(path, Msg.ENTRY_SOURCE_NOT_DEFINED)
        return null
    }

    analyzer.resolve(module = entryModule)
    return analyzer.semanticContext
}

fun Program.modules(block: ModuleManager.() -> Unit): MutableMap<String, Module> {
    return moduleManager.apply(block).modules
}

fun Program.printErrors() {
    errorHandler.printAll()
}

fun ModuleManager.root(
    path: String = "",
    callback: ModuleCallback = {m,ts -> }
) =
    addFromRoot(path, callback)

fun ModuleManager.file(
    path: String,
    callback: ModuleCallback = {m,ts -> }
) =
    addFromFile(path, callback)

fun ModuleManager.entry(
    path: String,
    callback: ModuleCallback = {m,ts -> }
) {
    val module = addFromFile(path, callback)
    this.entryModule = module
}

fun ModuleManager.extension(
    value: String
) {
    this.fileExtension = value
}