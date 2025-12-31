package lang.core

import lang.lexer.ILexer
import lang.lexer.NormalLexer
import lang.tokens.ITokenStream
import lang.tokens.TokenStream
import lang.messages.AnsiColors
import lang.messages.CompileStage
import lang.messages.ErrorHandler
import lang.messages.ErrorMsg
import lang.messages.Messages
import lang.parser.IParser
import lang.parser.Parser
import lang.nodes.BlockNode
import lang.semantics.ISemanticAnalyzer
import lang.semantics.SemanticAnalyzer
import java.io.File
import java.io.IOException

object Compiler : ICompiler {
    private const val LEXER_RESULT_PATH = "C:/TMP txt/lang/lexer_result.txt"
    private const val PARSER_RESULT_PATH = "C:/TMP txt/lang/parser_result.txt"

    private const val DEBUG_PRINT_TOKENS = false
    private const val DEBUG_PRINT_AST = true

    private val timing = Timing()

    fun lexer(src: SourceCode, errorHandler: ErrorHandler): ILexer {
        return NormalLexer(
            sourceFile = src,
            langSpec = LangSpec,
            errorHandler = errorHandler
        )
    }

    fun tokenStream(
        lexer: ILexer,
        errorHandler: ErrorHandler
    ): ITokenStream {
        return TokenStream(
            lexer = lexer,
            langSpec = LangSpec,
            errorHandler = errorHandler
        )
    }

    fun parser(
        tokenStream: ITokenStream,
        errorHandler: ErrorHandler
    ): IParser {
        return Parser(
            ts = tokenStream,
            errorHandler = errorHandler
        )
    }

    fun parser(
        src: SourceCode,
        errorHandler: ErrorHandler
    ): IParser? {
        timing.begin()
        val lexer = lexer(src = src, errorHandler = errorHandler)
        val tokenStream = tokenStream(lexer = lexer, errorHandler = errorHandler)
        val time = timing.timeMillis

        if (DEBUG_PRINT_TOKENS)
        File(LEXER_RESULT_PATH).printWriter().use { out ->
            tokenStream.getTokens().forEach { out.println(it); }
        }

        var parser: IParser? = null
        checkErrors(CompileStage.LEXICAL_ANALYSIS, errorHandler, src, time) {
            parser = parser(
                tokenStream = tokenStream,
                errorHandler = errorHandler
            )
        }

        return parser
    }

    fun semanticAnalyzer(
        errorHandler: ErrorHandler
    ): ISemanticAnalyzer {
        val semanticAnalyzer = SemanticAnalyzer(
            errorHandler = errorHandler
        )

        return semanticAnalyzer
    }

    fun compile(
        src: SourceCode,
        errorHandler: ErrorHandler = ErrorHandler()
    ): BlockNode? {
        val parser = parser(src, errorHandler)

        timing.begin()
        val ast = parser?.parseFile()

        var time = timing.timeMillis
        checkErrors(CompileStage.SYNTAX_ANALYSIS, errorHandler, src, time)

        if (errorHandler.hasErrors || ast == null)
            return null

        val analyzer = semanticAnalyzer(
            errorHandler = errorHandler
        )

        timing.begin()
        analyzer.resolve(node = ast)
        time = timing.timeMillis

        if (DEBUG_PRINT_AST)
            File(PARSER_RESULT_PATH).printWriter().use { out ->
                out.println(Serializer.formatNode(ast))
            }

        checkErrors(CompileStage.SEMANTIC_ANALYSIS, errorHandler, src, time)

        if (errorHandler.hasErrors)
            return null

        return ast
    }

    fun compile(path: String) {
        var src: SourceCode
        val errorHandler = ErrorHandler()

        try {
            src = readFile(path = path)
        } catch (_: IOException) {
            val fileName = File(path).absoluteFile.path
            src = SourceCode("")

            errorHandler.addError(
                ErrorMsg(
                    stage = CompileStage.SOURCE_READING,
                    message = "${Messages.CANNOT_OPEN_SOURCE_FILE} '$fileName'",
                )
            )
        }

        checkErrors(CompileStage.SOURCE_READING, errorHandler, src, timing.timeMillis) {
            compile(src = src)
        }
    }

    private fun checkErrors(
        stage: CompileStage,
        errorHandler: ErrorHandler,
        sourceFile: SourceCode,
        time: Long? = null,
        onSuccess: (() -> Unit)? = null
    ): Boolean {
        fun printMsg(a: String) = println("$stage  \t(${time} ms)\t- $a")

        return if (errorHandler.hasErrors) {
            val count = errorHandler.errors.size
            val a = AnsiColors.color("ERROR ($count)", AnsiColors.ERROR, null, true)
            printMsg(a)

            val errorsStr = errorHandler.formatErrors(sourceFile)
            println(errorsStr)
            true
        } else {
            val a = AnsiColors.color("SUCCESS!", AnsiColors.SUCCESS, null, true)
            printMsg(a)

            onSuccess?.invoke()
            false
        }
    }

    private fun readFile(path: String): SourceCode {
        val file = File(path)
        val content = file.readText()

        return SourceCode(
            source = content,
            file = file
        )
    }
}