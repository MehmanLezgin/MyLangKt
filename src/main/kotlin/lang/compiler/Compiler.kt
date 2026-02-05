package lang.compiler

import lang.core.FileSourceCode
import lang.core.ISourceCode
import lang.core.LangSpec
import lang.core.Pos
import lang.core.SourceRange
import lang.lexer.ILexer
import lang.lexer.Lexer
import lang.messages.Msg
import lang.messages.MsgHandler
import lang.nodes.IdentifierNode
import lang.parser.IParser
import lang.parser.Parser
import lang.semantics.SemanticAnalyzer
import lang.semantics.SemanticContext
import lang.tokens.ITokenStream
import lang.tokens.TokenStream
import java.io.File
import java.io.IOException
import java.nio.file.Path


class Program(val path: String) {
    var msgHandler: MsgHandler = MsgHandler()

    var moduleManager: ModuleManager = ModuleManager(
        msgHandler = msgHandler,
        path = path
    )


}

typealias ModuleCallback = (m: Module, ts: ITokenStream) -> Unit

class ModuleManager(
    val msgHandler: MsgHandler,
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

    fun addFromRoot(path: String = "", callback: ModuleCallback?): List<Module>? {
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
            addFromFile(path = file.path, callback)
        }
    }

    fun addFromFile(path: String, callback: ModuleCallback?): Module? {
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

        var moduleNameId = parser.parseModuleName()
        var moduleName = moduleNameId?.value ?: ""

        if (moduleNameId == null) {
            moduleNameId = randModuleName(file.path, src)
            moduleName = moduleNameId.value
        } else {
            val existingModule = modules[moduleNameId.value]

            if (existingModule != null) {
                msgHandler.sourceReadingError(
                    path = file.path,
                    msg = Msg.MODULE_ALREADY_EXISTS_IN.format(
                        moduleName,
                        existingModule.src.path,
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

        callback?.invoke(module, tokenStream)
        return module
    }

    private fun randModuleName(path: String, src: ISourceCode) =
        IdentifierNode(
            value = "\$Module${path.hashCode()}",
            range = SourceRange(src = src)
        )

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

fun Program.analise(): SemanticContext? {
    moduleManager.entryModule?.ast ?: return null

    val analyzer = SemanticAnalyzer(
        msgHandler = msgHandler,
        moduleMgr = moduleManager
    )

    val entryModule = moduleManager.entryModule
    if (entryModule == null) {
        msgHandler.sourceReadingError(path, Msg.ENTRY_SOURCE_NOT_DEFINED)
        return null
    }

    analyzer.resolve(module = entryModule)
    return analyzer.semanticContext
}

fun Program.modules(block: ModuleManager.() -> Unit): MutableMap<String, Module> {
    return moduleManager.apply(block).modules
}

fun Program.printErrors() {
    msgHandler.printAll()
}

fun ModuleManager.root(
    path: String = "",
    callback: ModuleCallback? = null
) =
    addFromRoot(path, callback)

fun ModuleManager.file(
    path: String,
    callback: ModuleCallback? = null
) =
    addFromFile(path, callback)

fun ModuleManager.entry(
    path: String,
    callback: ModuleCallback? = null
) {
    val module = addFromFile(path, callback)
    this.entryModule = module
}

fun ModuleManager.extension(
    value: String
) {
    this.fileExtension = value
}