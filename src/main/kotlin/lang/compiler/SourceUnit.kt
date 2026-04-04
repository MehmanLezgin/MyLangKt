package lang.compiler

import lang.infrastructure.ISourceCode
import lang.infrastructure.serializer.AstSerializer
import lang.nodes.BlockNode
import lang.semantics.SemanticContext
import lang.semantics.scopes.FileScope
import java.io.File

data class SourceUnit(
    val id: String,
    val src: ISourceCode,
    val ast: BlockNode,
) {
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
}

fun List<SourceUnit>.print(basePath: String, semanticContext: SemanticContext?) {
    this.forEach { unit ->
        val moduleName = unit.id

        unit.printAST(
            path = "${basePath}ast/ast_$moduleName.txt",
            semanticContext = semanticContext
        )
    }
}