package lang.compiler

import lang.core.ISourceCode
import lang.core.LangSpec
import lang.core.serializer.AstSerializer
import lang.core.serializer.ScopeSerializer
import lang.nodes.ModuleStmtNode
import lang.semantics.SemanticContext
import lang.semantics.scopes.ModuleScope
import java.io.File

data class Module(
    val name: String?,
    val src: ISourceCode,
    val ast: ModuleStmtNode,
    var scope: ModuleScope? = null
) {
    var isReady = false
    var isAnalysing = false

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