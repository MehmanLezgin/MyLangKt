import lang.compiler.*

fun main(args: Array<String>) {
    val basePath = "C:/TMP txt/lang/"

    program(basePath) {
        val moduleList = modules {
            extension("i")

            root("modules")

            entry("./main.i")

        }.toList().map { it.second }

        val semanticContext = if (errorHandler.hasErrors) null else analise()

        moduleList.print(
            basePath = basePath,
            semanticContext = semanticContext
        )

        printErrors()
    }
}