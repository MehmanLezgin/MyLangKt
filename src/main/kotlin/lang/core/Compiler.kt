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
import lang.nodes.IdentifierNode
import java.io.File
import java.io.IOException

object Compiler : ICompiler {
    private const val LEXER_RESULT_PATH = "C:/TMP txt/lang/lexer_result.txt"
    private const val PARSER_RESULT_PATH = "C:/TMP txt/lang/parser_result.txt"

    private val timing = Timing()

    private fun lexer(src: SourceCode, errorHandler: ErrorHandler): ILexer {
        return NormalLexer(
            sourceFile = src,
            langSpec = LangSpec,
            errorHandler = errorHandler
        )
    }

    private fun tokenStream(
        lexer: ILexer,
        errorHandler: ErrorHandler
    ): ITokenStream {
        return TokenStream(
            lexer = lexer,
            langSpec = LangSpec,
            errorHandler = errorHandler
        )
    }

    private fun parser(
        tokenStream: ITokenStream,
        errorHandler: ErrorHandler
    ): IParser {
        return Parser(
            ts = tokenStream,
            errorHandler = errorHandler
        )
    }

    private fun parser(
        src: SourceCode,
        errorHandler: ErrorHandler
    ): IParser? {
        timing.begin()
        val lexer = lexer(src = src, errorHandler = errorHandler)

        val tokenStream = tokenStream(lexer = lexer, errorHandler = errorHandler)
        println("Lexer time = ${timing.timeMillis}ms")

        File(LEXER_RESULT_PATH).printWriter().use { out ->
            tokenStream.getTokens().forEach { out.println(it); }
        }

        var parser: IParser? = null
        checkErrors(CompileStage.LEXICAL_ANALYSIS, errorHandler, src) {
            parser = parser(
                tokenStream = tokenStream,
                errorHandler = errorHandler
            )
        }

        return parser
    }

    private fun ast(
        src: SourceCode,
        errorHandler: ErrorHandler
    ): BlockNode? {
        val parser = parser(src, errorHandler)

        timing.begin()

        val ast = parser?.parseFile()?.mapRecursive { node ->
            if (node is IdentifierNode) {
                return@mapRecursive IdentifierNode("BIG_SHIT", node.pos)
            }
            node
        }

        println("Parser time = ${timing.timeMillis}ms")

        File(PARSER_RESULT_PATH).printWriter().use { out ->
            out.println(Serializer.formatNode(ast ?: BlockNode.EMPTY))
        }

        checkErrors(CompileStage.SYNTAX_ANALYSIS, errorHandler, src) {
            // next: semantic analysis
        }

        return ast as? BlockNode?
    }

    fun compile(
        src: SourceCode,
        errorHandler: ErrorHandler = ErrorHandler()
    ) {
        ast(
            src = src,
            errorHandler = errorHandler
        )
    }

    fun compile(path: String) {
        var src: SourceCode
        val errorHandler = ErrorHandler()

        try {
            src = readFile(path = path)
        } catch (e: IOException) {
            val fileName = File(path).absoluteFile.path
            src = SourceCode("")

            errorHandler.addError(
                ErrorMsg(
                    stage = CompileStage.SOURCE_READING,
                    message = "${Messages.CANNOT_OPEN_SOURCE_FILE} '$fileName'",
                )
            )
        }

        checkErrors(CompileStage.SOURCE_READING, errorHandler, src) {
            compile(src = src)
        }
    }

    private fun checkErrors(
        stage: CompileStage,
        errorHandler: ErrorHandler,
        sourceFile: SourceCode,
        onSuccess: () -> Unit
    ): Boolean {
        return if (errorHandler.hasErrors) {
            val count = errorHandler.errors.size
            val a = AnsiColors.color("ERROR ($count)", AnsiColors.ERROR, null, true)
            println("$stage - $a")

            val errorsStr = errorHandler.formatErrors(sourceFile)
            println(errorsStr)
            true
        } else {
            val a = AnsiColors.color("SUCCESS!", AnsiColors.SUCCESS, null, true)
            println("$stage - $a")

            onSuccess()
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