package lang.core

/*
import lang.compiler.ModuleManager
import lang.lexer.Lexer
import lang.tokens.TokenStream
import lang.messages.AnsiColors
import lang.messages.CompileStage
import lang.messages.ErrorHandler
import lang.messages.ErrorMsg
import lang.messages.Messages
import lang.nodes.ModuleNode
import lang.parser.IParser
import lang.parser.Parser
import lang.semantics.SemanticAnalyzer
import java.io.File
import java.io.IOException
import kotlin.random.Random

object CompilerObj {
    private const val LEXER_RESULT_PATH = "C:/TMP txt/lang/lexer_result.txt"
    private const val PARSER_RESULT_PATH = "C:/TMP txt/lang/parser_result.txt"

    private const val DEBUG_PRINT_TOKENS = true
    private const val DEBUG_PRINT_AST = true

    private val timing = Timing()

    fun parser(
        src: SourceCode,
        errorHandler: ErrorHandler
    ): IParser? {
        timing.begin()
        val lexer = Lexer(
            sourceFile = src,
            errorHandler = errorHandler,
            langSpec = LangSpec
        )

        val tokenStream = TokenStream(
            lexer = lexer,
            langSpec = LangSpec,
            errorHandler = errorHandler
        )

        val time = timing.timeMillis

        if (DEBUG_PRINT_TOKENS)
            File(LEXER_RESULT_PATH).printWriter().use { out ->
                tokenStream.getTokens().forEach { out.println(it) }
            }

        var parser: IParser? = null
        checkErrors(CompileStage.LEXICAL_ANALYSIS, errorHandler, time) {
            parser = Parser(
                ts = tokenStream,
                errorHandler = errorHandler
            )
        }

        return parser
    }

    fun randModuleName() = "Module${Random.nextInt()}"

    fun analiseModule(
        src: SourceCode,
        name: String = src.file?.nameWithoutExtension ?: randModuleName(),
        errorHandler: ErrorHandler = ErrorHandler()
    ): ModuleNode? {
        val parser = parser(src, errorHandler)
        val moduleAST = parser?.parseModule(name = name, path = src.file?.absolutePath)
        checkErrors(CompileStage.SYNTAX_ANALYSIS, errorHandler)

        if (errorHandler.hasErrors || moduleAST == null)
            return null

        val analyzer = SemanticAnalyzer(
            errorHandler = errorHandler,
            moduleMgr = ModuleManager(errorHandler)
        )

        analyzer.resolve(node = moduleAST)
        checkErrors(CompileStage.SEMANTIC_ANALYSIS, errorHandler)
        return moduleAST
    }

    fun compileProgram(
        src: SourceCode,
        errorHandler: ErrorHandler = ErrorHandler()
    ): ModuleNode? {
        val parser = parser(src, errorHandler)

        timing.begin()
        val moduleAST = parser?.parseModule(name = "program", path = src.file?.absolutePath)

        var time = timing.timeMillis
        checkErrors(CompileStage.SYNTAX_ANALYSIS, errorHandler, time)

        if (errorHandler.hasErrors || moduleAST == null)
            return null

        val analyzer = SemanticAnalyzer(
            errorHandler = errorHandler,
        )

        timing.begin()
        analyzer.resolve(node = moduleAST)
        time = timing.timeMillis

        if (DEBUG_PRINT_AST)
            File(PARSER_RESULT_PATH).printWriter().use { out ->
                out.println(
                    Serializer.formatNode(
                        node = moduleAST,
                        semanticContext = analyzer.semanticContext
                    )
                )
            }

        checkErrors(CompileStage.SEMANTIC_ANALYSIS, errorHandler, time)

        return moduleAST
    }

    fun compileProgram(
        moduleRoots: List<String>,
        errorHandler: ErrorHandler = ErrorHandler()
    ) {
        val src = readSourceFiles(moduleRoots, errorHandler)

        checkErrors(CompileStage.SOURCE_READING, errorHandler, timing.timeMillis) {
//            compileProgram(src = src)
        }
    }

    fun readSourceFiles(
        pathList: List<String>,
        errorHandler: ErrorHandler
    ): List<SourceCode> {
        return pathList.flatMap { root ->
            File(root).listFiles().map { file ->
                readSourceFile(file, errorHandler)
            }.filterNotNull()
        }
    }

    fun readSourceFile(file: File, errorHandler: ErrorHandler): SourceCode? {
        var src: SourceCode? = null

        try {
            val content = file.readText()

            src = SourceCode(
                content = content,
                file = file
            )
        } catch (_: IOException) {
            val fileName = file.absoluteFile.path

            errorHandler.addError(
                ErrorMsg(
                    stage = CompileStage.SOURCE_READING,
                    message = Messages.CANNOT_OPEN_SOURCE_FILE.format(fileName)
                )
            )
        }

        return src
    }

    private fun checkErrors(
        stage: CompileStage,
        errorHandler: ErrorHandler,
        time: Long? = null,
        onSuccess: (() -> Unit)? = null
    ): Boolean {
        fun printMsg(a: String, b: String) = println("$a $stage\t\t(${time} ms)$b")

        return if (errorHandler.hasErrors) {
            val count = errorHandler.errors.size
            val errorText = if (count == 1) "($count error)" else "$count errors"
            val a = AnsiColors.color(errorText, AnsiColors.ERROR, null, true)
            printMsg(AnsiColors.color("✗", AnsiColors.ERROR), "\t- $a")

            val errorsStr = errorHandler.formatErrors()
            println(errorsStr)
            true
        } else {
            printMsg(AnsiColors.color("✓", AnsiColors.SUCCESS), "")
            onSuccess?.invoke()
            false
        }
    }
}*/
