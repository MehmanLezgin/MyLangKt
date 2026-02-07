package lang.compiler

import lang.core.LangSpec
import lang.core.serializer.AstSerializer
import lang.core.ISourceCode
import lang.core.serializer.ScopeSerializer
import lang.nodes.ModuleNode
import lang.semantics.SemanticContext
import lang.semantics.scopes.ModuleExportScope
import lang.semantics.scopes.ModuleScope
import lang.semantics.symbols.Symbol
import java.io.File

data class Module(
    val name: String?,
    val src: ISourceCode,
    val ast: ModuleNode,
    var scope: ModuleScope? = null
) {
    var isReady = false
    var isAnalysing = false

    //    val imports = mutableMapOf<String, Symbol>()
//    val exports = mutableMapOf<String, Symbol>()
//    val importsScope by lazy { Scope(parent = null, errorHandler = scope?.errorHandler ?: ErrorHandler()) }
    val exportsScope by lazy { ModuleExportScope() }

//    fun import(symbol: Symbol) {
//        importsScope.define(symbol, Pos(src = src))
//    }

    fun export(symbol: Symbol, namePath: List<String>) {
        exportsScope.export(namePath, symbol)
    }

    fun printAST(path: String, semanticContext: SemanticContext?) {
        File(path).printWriter().use { out ->
            out.println(
                AstSerializer.serialize(
                    root = this.ast,
                    semanticContext = semanticContext
                )
            )
        }
    }

    fun printScope(path: String) {
        val scope = this.scope ?: return

        File(path).printWriter().use { out ->
            out.println(
                ScopeSerializer.serialize(scope)
            )
        }
    }
}

fun List<Module>.print(basePath: String, semanticContext: SemanticContext?) {
    this.forEach { module ->
        val moduleName = module.name
            ?.replace(LangSpec.moduleNameSeparator.raw, ".")

        module.printAST(
            path = "${basePath}ast/ast_$moduleName.txt",
            semanticContext = semanticContext
        )

        module.printScope(
            path = "${basePath}scope_$moduleName.txt"
        )
    }
}