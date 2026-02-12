import lang.compiler.*
import lang.tokens.Token
import java.io.File

private const val LEXER_RESULT_PATH = "C:/TMP txt/lang/lexer_result.txt"

fun main() {
    val basePath = "C:/TMP txt/lang/"

    program(basePath) {
        var tokens: List<Token> = emptyList()

        val moduleList = sources {
            extension("i")

            root("modules")

            entry("./main.i") { _, ts ->
                tokens = ts.getTokens()
            }
        }

        val semContext = analiseIfNoError()

        moduleList.print(
            basePath = basePath,
            semanticContext = semContext
        )

        File(LEXER_RESULT_PATH).printWriter().use { out ->
            tokens.forEach { out.println(it) }
        }

        printErrors()
    }
}