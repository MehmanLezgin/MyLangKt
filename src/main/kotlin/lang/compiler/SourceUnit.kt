package lang.compiler

import lang.core.ISourceCode
import lang.core.serializer.AstSerializer
import lang.nodes.BaseImportStmtNode
import lang.nodes.BlockNode
import lang.semantics.SemanticContext
import lang.semantics.scopes.FileScope
import lang.semantics.scopes.ModuleScope
import lang.semantics.symbols.ModuleSymbol
import java.io.File

data class SourceUnit(
    val id: String,
    val src: ISourceCode,
    val ast: BlockNode,
) {
    var isReady = false
    var isAnalysing = false
    var scope: FileScope? = null

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

    /*
        fun printScope(path: String) {
            val scope = this.scope ?: return

            File(path).printWriter().use { out ->
                out.println(
                    ScopeSerializer.serialize(scope)
                )
            }
        }
    */
}

fun List<SourceUnit>.print(basePath: String, semanticContext: SemanticContext?) {
    this.forEach { unit ->
        val moduleName = unit.id

        unit.printAST(
            path = "${basePath}ast/ast_$moduleName.txt",
            semanticContext = semanticContext
        )

        /*unit.printScope(
            path = "${basePath}scope_$moduleName.txt"
        )*/
    }
}