import lang.compiler.*
import lang.tokens.Token
import java.io.File

private const val LEXER_RESULT_PATH = "C:/TMP txt/lang/lexer_result.txt"

fun main(args: Array<String>) {
    val basePath = "C:/TMP txt/lang/"

    program(basePath) {
        var tokens: List<Token> = emptyList()
        val moduleList = modules {
            extension("i")

//            root("modules")

            entry("./main.i") { module, ts ->
                tokens = ts.getTokens()
            }

        }.toList().map { it.second }

        val semanticContext = if (msgHandler.hasErrors) null else analise()

        moduleList.print(
            basePath = basePath,
            semanticContext = semanticContext
        )

        File(LEXER_RESULT_PATH).printWriter().use { out ->
            tokens.forEach { out.println(it) }
        }

        printErrors()
    }
}