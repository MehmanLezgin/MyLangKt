package lang.compiler

import lang.core.LangSpec
import lang.core.Serializer
import lang.core.SourceCode
import lang.messages.ErrorHandler
import lang.nodes.ModuleNode
import lang.semantics.SemanticContext
import lang.semantics.scopes.ModuleExportScope
import lang.semantics.scopes.ModuleScope
import lang.semantics.symbols.Symbol
import lang.tokens.Pos
import java.io.File

data class Module(
    val name: String?,
    val src: SourceCode,
    val ast: ModuleNode,
    var scope: ModuleScope? = null
) {
    var isReady = false
    var isAnalysing = false
//    val imports = mutableMapOf<String, Symbol>()
//    val exports = mutableMapOf<String, Symbol>()
//    val importsScope by lazy { Scope(parent = null, errorHandler = scope?.errorHandler ?: ErrorHandler()) }
    val exportsScope by lazy { ModuleExportScope(
        ErrorHandler()
    ) }

//    fun import(symbol: Symbol) {
//        importsScope.define(symbol, Pos(src = src))
//    }

    fun export(symbol: Symbol, namePath: List<String>) {
        exportsScope.export(namePath, symbol, Pos(src = src))
    }

    fun print(path: String, semanticContext: SemanticContext?) {
        File(path).printWriter().use { out ->
            out.println(
                Serializer.formatNode(
                    node = this.ast,
                    semanticContext = semanticContext
                )
            )
        }
    }
}

fun List<Module>.print(basePath: String, semanticContext: SemanticContext?) {
    this.forEach { module ->
        val moduleName = module.name
            ?.replace(LangSpec.moduleNameSeparator.symbol, ".")

        module.print(
            path = "${basePath}ast/ast_$moduleName.txt",
            semanticContext = semanticContext
        )
    }
}